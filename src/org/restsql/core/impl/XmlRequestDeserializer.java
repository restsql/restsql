/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.RequestDeserializer;
import org.restsql.core.RequestLogger;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.NameValuePair.Operator;
import org.restsql.core.Request.Type;
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
	public int execWrite(HttpRequestAttributes httpAttributes, final Request.Type requestType,
			final List<NameValuePair> resIds, final SqlResource sqlResource, final String requestBody,
			RequestLogger requestLogger) throws SqlResourceException {
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
		SqlResourceException handlerException = handler.getHandlerException();
		if (handlerException != null) {
			throw handlerException;
		}
		return handler.getRowsAffected();
	}

	@Override
	public String getSupportedMediaType() {
		return "application/xml";
	}
	
	static class Handler extends DefaultHandler {
		private static final String TAG_REQUEST = "request";

		private static final int DEFAULT_CHILDREN_SIZE = 10;

		int rowsAffected = 0;
		private Request.Type requestType;
		private List<NameValuePair> parentRequestResIds;
		private List<List<NameValuePair>> childrenParams;
		private SqlResourceException handlerException;
		private List<NameValuePair> params;
		private List<NameValuePair> parentAttributes;
		private List<NameValuePair> resIds;
		private final SqlResource sqlResource;
		private RequestLogger requestLogger;
		private HttpRequestAttributes httpAttributes;

		Handler(HttpRequestAttributes httpAttributes, final Request.Type requestType,
				final List<NameValuePair> parentRequestResIds, final SqlResource sqlResource,
				RequestLogger requestLogger) {
			this.httpAttributes = httpAttributes;
			this.requestType = requestType;
			this.parentRequestResIds = parentRequestResIds;
			this.sqlResource = sqlResource;
			this.requestLogger = requestLogger;
		}

		@Override
		public void endElement(final String uri, final String localName, final String qName)
				throws SAXException {
			if (qName.equals(sqlResource.getMetaData().getParent().getTableAlias())) {
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

		public int getRowsAffected() {
			return rowsAffected;
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName,
				final Attributes attributes) throws SAXException {
			if (!qName.equals(TAG_REQUEST)) {
				if (qName.equals(sqlResource.getMetaData().getParent().getTableAlias())) {
					parentAttributes = parseAttributes(attributes);
				} else { // child element
					if (childrenParams == null) {
						childrenParams = new ArrayList<List<NameValuePair>>(DEFAULT_CHILDREN_SIZE);
					}
					final List<NameValuePair> childParams = parseAttributes(attributes);
					childrenParams.add(childParams);
				}
			}
		}

		private void executeRequest() {
			try {
				Request request = Factory.getRequest(httpAttributes, requestType, sqlResource.getName(),
						resIds, params, childrenParams, requestLogger);
				rowsAffected += sqlResource.write(request);
			} catch (SqlResourceException exception) {
				handlerException = exception;
			}
		}

		private void extractResIdsParamsFromParentAttributes(final boolean extractParams) {
			resIds = new ArrayList<NameValuePair>(sqlResource.getMetaData().getParent().getPrimaryKeys()
					.size());
			if (extractParams) {
				params = new ArrayList<NameValuePair>(parentAttributes.size() - resIds.size());
			}
			for (final NameValuePair attrib : parentAttributes) {
				for (final ColumnMetaData column : sqlResource.getMetaData().getParent().getPrimaryKeys()) {
					if (column.getColumnLabel().equals(attrib.getName())) {
						resIds.add(attrib);
					} else if (extractParams) {
						params.add(attrib);
					}
				}
			}
		}

		private List<NameValuePair> parseAttributes(final Attributes attributes) {
			final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(attributes.getLength());
			for (int i = 0; i < attributes.getLength(); i++) {
				final NameValuePair param = new NameValuePair(attributes.getQName(i), attributes.getValue(i), Operator.Equals);
				nameValuePairs.add(param);
			}
			return nameValuePairs;
		}
	}
}
