/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restsql.core.ColumnMetaData;
import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.RequestValue;
import org.restsql.core.Request;
import org.restsql.core.RequestDeserializer;
import org.restsql.core.RequestLogger;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.RequestValue.Operator;
import org.restsql.core.Request.Type;
import org.restsql.core.WriteResponse;

/**
 * Processes requests represented in a JSON string. It expects a top-level array of objects, as in:
 * 
 * <pre>
 * { "actors": [
 * 	{ "id": "1000", "first_name": "_Jack", "surname": "Daniels" },
 * 		{ "id": "1001", "first_name": "_Jack", "surname": "Smith" }
 * 	 ]
 * }
 * </pre>
 * 
 * With hierarchical resources, these top-level objects could either be parents or children. They are children when the
 * request URI points to one parent, using its primary key(s), as in <code>res/HierOneToMany/100</code>. The body
 * contains JSON like:
 * 
 * <pre>
 * { "movies": [
 * 	{ "year": "2011", "title": "ESCAPE FROM TOMORROW", "film_id": "5000" },
 * 	{ "year": "2012", "title": "BLOOD PURPLE", "film_id": "5001" }
 * 	 ]
 * }
 * </pre>
 * 
 * If the URI is generic, as in <code>res/HierOneToMany</code>, the top-level objects will be parents, and may contain
 * an array of child objects, for example:
 * 
 * <pre>
 * { "languages": [
 * 		{ "language_id": "100", "langName": "New Esperanto",
 * 			"movies": [
 * 				{ "year": "2011", "title": "ESCAPE FROM TOMORROW", "film_id": "5000" },
 * 				{ "year": "2012", "title": "BLOOD PURPLE", "film_id": "5001" }
 * 			]
 * 		},
 * 		{ "language_id": "101", "langName": "New Greek",
 * 			"movies": [
 * 				{ "year": "2012", "title": "THE DARKENING", "film_id": "5002" },
 * 				{ "year": "2012", "title": "THE LIGHTENING", "film_id": "5003" }
 * 			]
 * 		}
 * 	 ]
 * }
 * </pre>
 * 
 * Each top-level object is sent off as one request to the SQL resource. If there are two-levels, then the second level
 * array is parsed in full, and then included in the request with the parent attributes. Note that the framework will
 * only operate on children if they are included, not parents and children simultaneously.
 * 
 * @author Mark Sawers
 */
public class JsonRequestDeserializer implements RequestDeserializer {

	@Override
	public WriteResponse execWrite(HttpRequestAttributes httpAttributes, Type requestType,
			List<RequestValue> resIds, SqlResource sqlResource, String requestBody,
			RequestLogger requestLogger) throws SqlResourceException {
		final Handler handler = new Handler(httpAttributes, requestType, resIds, sqlResource, requestLogger);
		try {
			final JSONParser parser = new JSONParser();
			parser.parse(requestBody, handler);
		} catch (final ParseException exception) {
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
		return "application/json";
	}
	
	class Handler implements ContentHandler {
		private static final int DEFAULT_CHILDREN_SIZE = 10;

		private final int childColumnCount;
		private String childrenKey, currentKey;
		private List<List<RequestValue>> childrenParams;
		private SqlResourceException handlerException;
		private final HttpRequestAttributes httpAttributes;
		private List<RequestValue> params;
		private List<RequestValue> parentAttributes;
		private final int parentColumnCount;
		private final List<RequestValue> parentRequestResIds;
		private ParserState parserState = ParserState.Initial;
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

			parentColumnCount = sqlResource.getMetaData().getParentReadColumns().size();
			if (sqlResource.getMetaData().isHierarchical()) {
				childColumnCount = sqlResource.getMetaData().getChildReadColumns().size();
				childrenKey = sqlResource.getMetaData().getChild().getRowSetAlias();
			} else {
				childColumnCount = 0;
			}
		}

		@Override
		public boolean endArray() throws ParseException, IOException {
			switch (parserState) {
				case EndLevel1Object:
					parserState = ParserState.EndLevel1Array;
					break;
				case EndLevel2Object:
					parserState = ParserState.EndLevel2Array;
					break;
				default:
					throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION,
							"reached endArray() in unexpected parser state: " + parserState);
			}

			if (parentRequestResIds != null && parserState == ParserState.EndLevel1Array) {
				resIds = parentRequestResIds;
				executeRequest();
			}
			return true;
		}

		/** No-op. */
		@Override
		public void endJSON() throws ParseException, IOException {
		}

		/**
		 * Executes request if parentRequestIds are null and at level 1, otherwise continues to collect level 2 objects.
		 * If parentRequestIds are not null, this is an operation on children at level 1. The request is executed in
		 * endArray().
		 */
		@Override
		@SuppressWarnings("fallthrough")
		public boolean endObject() throws ParseException, IOException {
			switch (parserState) {
				case EndLevel2Array:
				case AtLevel1Object:
					parserState = ParserState.EndLevel1Object;
					break;
				case AtLevel2Object:
					parserState = ParserState.EndLevel2Object;
					break;
				case EndLevel1Array:
					break;		// we're done
				default:
					throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION,
							"reached endObject() in unexpected parser state: " + parserState);
			}

			// Execute request if at end of level 1 object
			if (parentRequestResIds == null && parserState == ParserState.EndLevel1Object) {
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
			}
			// else ignore

			return true;
		}

		/** No-op. */
		@Override
		public boolean endObjectEntry() throws ParseException, IOException {
			return true;
		}

		public SqlResourceException getHandlerException() {
			return handlerException;
		}

		public WriteResponse getWriteResponse() {
			return response;
		}

		/** Sets string, number or boolean value to current parameter. */
		@Override
		public boolean primitive(final Object value) throws ParseException, IOException {
			RequestValue pair;
			if (value != null) {
				pair = new RequestValue(currentKey, value.toString(), Operator.Equals);
			} else {
				pair = new RequestValue(currentKey, null, Operator.Equals);
			}
			params.add(pair);
			return true;
		}

		@Override
		public boolean startArray() throws ParseException, IOException {
			switch (parserState) {
				case AtLevel1Name:
					parserState = ParserState.AtLevel1Array;
					break;
				case AtLevel2Name:
					parserState = ParserState.AtLevel2Array;
					break;
				default:
					throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION,
							"reached startArray() in unexpected parser state: " + parserState);
			}
			return true;
		}

		/** No-op. */
		@Override
		public void startJSON() throws ParseException, IOException {
		}

		/**
		 * Reset currentKey and params. If at parent, set params to parentAttributes. child add the params to the
		 * childrenParams list.
		 */
		@Override
		@SuppressWarnings("fallthrough")
		public boolean startObject() throws ParseException, IOException {
			switch (parserState) {
				case Initial:
					parserState = ParserState.AtLevel1Name;
					break;
				case EndLevel1Object:
				case AtLevel1Array:
					parserState = ParserState.AtLevel1Object;
					break;
				case EndLevel2Object:
				case AtLevel2Array:
					parserState = ParserState.AtLevel2Object;
					break;
				default:
					throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION,
							"reached startObject() in unexpected parser state: " + parserState);
			}

			currentKey = null;
			if (parserState == ParserState.AtLevel1Object && parentRequestResIds == null) {
				params = new ArrayList<RequestValue>(parentColumnCount);
				parentAttributes = params;
			} else if (parserState == ParserState.AtLevel1Object || parserState == ParserState.AtLevel2Object) {
				if (childrenParams == null) {
					childrenParams = new ArrayList<List<RequestValue>>(DEFAULT_CHILDREN_SIZE);
				}
				params = new ArrayList<RequestValue>(childColumnCount);
				childrenParams.add(params);
			}
			return true;
		}

		/** New object found. Set the current key. */
		@Override
		public boolean startObjectEntry(final String key) throws ParseException, IOException {
			currentKey = key;
			if (parserState == ParserState.AtLevel1Object && sqlResource.getMetaData().isHierarchical()
					&& key.equals(childrenKey)) {
				parserState = ParserState.AtLevel2Name;
			}
			return true;
		}

		// Private util methods

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
	}

	static enum ParserState {
		AtLevel1Array, AtLevel2Array, AtLevel1Name, AtLevel2Name, AtLevel1Object, AtLevel2Object,
		EndLevel2Object, EndLevel2Array, EndLevel1Object, EndLevel1Array, Initial;
	}
}