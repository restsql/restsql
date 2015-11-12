/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.RequestDeserializer;
import org.restsql.core.RequestLogger;
import org.restsql.core.RequestValue;
import org.restsql.core.RequestValue.Operator;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.WriteResponse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Processes requests represented in an XML document using a SAX parser. It expects a document with the "request"
 * element, containing the resource elements. In the case of hierarchical resources, the top-level objects may be
 * parents containing a set of child elements, for example:
 * 
 * <pre>
 * <![CDATA[
 * <request>
 *  	<lang language_id="100" langName="New Esperanto">
 * 			<movie year="2011" title="ESCAPE FROM TOMORROW" film_id="5000" />
 * 			<movie year="2012" title="BLOOD PURPLE" film_id="5001" />
 *  	</lang>
 *  	<lang language_id="101" langName="New Greek">
 * 			<movie year="2012" title="THE DARKENING" film_id="5002" />
 *  		<movie year="2012" title="THE LIGHTENING" film_id="5003" />
 *  	</lang>
 * </request>
 * ]]>
 * </pre>
 * 
 * Each parent is sent off as one request to the SQL resource. If there are children, then those are parsed in full, and
 * then included in the request with the parent attributes. Note that the framework will only operate on children if
 * they are included, not parents and children simultaneously.
 * 
 * @author Mark Sawers
 */
public class XmlRequestDeserializer implements RequestDeserializer {

	/** Executes write request. */
	@Override
	public WriteResponse execWrite(final HttpRequestAttributes httpAttributes,
			final Type requestType, final List<RequestValue> resIds, final SqlResource sqlResource,
			final String requestBody, final RequestLogger requestLogger) throws SqlResourceException {
		final Handler handler = new Handler(httpAttributes, requestType, resIds, sqlResource, requestLogger);
		final SAXParser parser;
		final ByteArrayInputStream inputStream;
		try {
			parser = SAXParserFactory.newInstance().newSAXParser();
			inputStream = new ByteArrayInputStream(requestBody.getBytes());
			parser.parse(inputStream, handler);
		} catch (final Exception exception) {
			throw new InvalidRequestException("Error parsing request body: " + exception.toString());
		}
		final SqlResourceException handlerException = handler.getHandlerException();
		if (handlerException != null) {
			throw handlerException;
		}
		return handler.getWriteResponse();
	}

	@Override
	public String getSupportedMediaType() {
		return "application/xml";
	}

	static class Handler extends DefaultHandler {
		private static final int DEFAULT_CHILDREN_SIZE = 10;

		private static final String TAG_REQUEST = "request";

		private List<List<RequestValue>> childrenParams;
		private SqlResourceException handlerException;
		private final HttpRequestAttributes httpAttributes;
		private List<RequestValue> params;
		private List<RequestValue> parentAttributes;
		private final List<RequestValue> parentRequestResIds;
		private final RequestLogger requestLogger;
		private final Request.Type requestType;
		private List<RequestValue> resIds;
		private final SqlResource sqlResource;
		private WriteResponse response;

		Handler(final HttpRequestAttributes httpAttributes, final Request.Type requestType,
				final List<RequestValue> parentRequestResIds, final SqlResource sqlResource,
				final RequestLogger requestLogger) {
			this.httpAttributes = httpAttributes;
			this.requestType = requestType;
			this.parentRequestResIds = parentRequestResIds;
			this.sqlResource = sqlResource;
			this.requestLogger = requestLogger;
		}

		@Override
		public void endElement(final String uri, final String localName, final String qName)
				throws SAXException {
			if (qName.equals(sqlResource.getMetaData().getParent().getRowAlias())) {
				if (childrenParams != null) {
					// Apply operation to the children
					// Parent is only for giving context to child inserts and updates
					extractResIdsParamsFromParentAttributes(false); // extract only resIds
				} else {
					// Apply operation to the parent
					if (requestType == Type.UPDATE) {
						extractResIdsParamsFromParentAttributes(true); // extract resIds and params
					} else { // Type.INSERT or Type.DELETE
						resIds = null;
						params = parentAttributes;
					}
				}
				executeRequest();
				if (childrenParams != null) {
					// Clear children for the next parent
					childrenParams = null;
				}
			} else if (parentRequestResIds != null && qName.equals(TAG_REQUEST)) {
				resIds = parentRequestResIds;
				executeRequest();
			}
			// else ignore child element
		}

		public SqlResourceException getHandlerException() {
			return handlerException;
		}

		public WriteResponse getWriteResponse() {
			return response;
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName,
				final Attributes attributes) throws SAXException {
			if (!qName.equals(TAG_REQUEST)) {
				if (qName.equals(sqlResource.getMetaData().getParent().getRowAlias())) {
					parentAttributes = parseAttributes(attributes);
				} else { // child element
					if (childrenParams == null) {
						childrenParams = new ArrayList<List<RequestValue>>(DEFAULT_CHILDREN_SIZE);
					}
					final List<RequestValue> childParams = parseAttributes(attributes);
					childrenParams.add(childParams);
				}
			}
		}

		private void executeRequest() {
			try {
				final Request request = Factory.getRequest(httpAttributes, requestType,
						sqlResource.getName(), resIds, params, childrenParams, requestLogger);
				WriteResponse localResponse = sqlResource.write(request);
				if (response == null) {
					response = localResponse;
				} else {
					response.addWriteResponse(localResponse);
				}
			} catch (final SqlResourceException exception) {
				handlerException = exception;
			}
		}

		private void extractResIdsParamsFromParentAttributes(final boolean extractParams) {
			resIds = new ArrayList<RequestValue>(sqlResource.getMetaData().getParent().getPrimaryKeys()
					.size());
			if (extractParams) {
				params = new ArrayList<RequestValue>(parentAttributes.size() - resIds.size());
			}
			for (final RequestValue attrib : parentAttributes) {
				for (final ColumnMetaData column : sqlResource.getMetaData().getParent().getPrimaryKeys()) {
					if (column.getColumnLabel().equals(attrib.getName())) {
						resIds.add(attrib);
					} else if (extractParams) {
						params.add(attrib);
					}
				}
			}
		}

		private List<RequestValue> parseAttributes(final Attributes attributes) {
			final List<RequestValue> requestParams = new ArrayList<RequestValue>(attributes.getLength());
			for (int i = 0; i < attributes.getLength(); i++) {
				final RequestValue param = new RequestValue(attributes.getQName(i), attributes.getValue(i),
						Operator.Equals);
				requestParams.add(param);
			}
			return requestParams;
		}
	}
}
