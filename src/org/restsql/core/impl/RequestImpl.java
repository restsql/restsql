/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.List;

import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.InvalidRequestException;
import org.restsql.core.Request;
import org.restsql.core.RequestLogger;
import org.restsql.core.RequestValue;

/**
 * Represents a restSQL request.
 * 
 * @author Mark Sawers
 */
public class RequestImpl implements Request {
	private final List<List<RequestValue>> childrenParams;
	private HttpRequestAttributes httpAttributes;
	private List<RequestValue> params;
	private Request parent;
	private final RequestLogger requestLogger;
	private final List<RequestValue> resourceIdentifiers;
	private Integer selectLimit, selectOffset;
	private final String sqlResource;
	private final Request.Type type;

	/** Constructs object. */
	public RequestImpl(final HttpRequestAttributes httpAttributes, final Request.Type type,
			final String sqlResource, final List<RequestValue> resourceIdentifiers,
			final List<RequestValue> params, final List<List<RequestValue>> childrenParams,
			final RequestLogger requestLogger) {
		this.type = type;
		this.resourceIdentifiers = resourceIdentifiers;
		this.sqlResource = sqlResource;
		this.childrenParams = childrenParams;
		this.requestLogger = requestLogger;
		this.params = params;
		if (httpAttributes != null) {
			this.httpAttributes = httpAttributes;
		} else {
			this.httpAttributes = Factory.getHttpRequestAttributes("?", "?", "?", null, null, null);
		}
		if (requestLogger != null) {
			requestLogger.setRequest(this);
		}
	}

	@Override
	public List<List<RequestValue>> getChildrenParameters() {
		return childrenParams;
	}

	@Override
	public HttpRequestAttributes getHttpRequestAttributes() {
		return httpAttributes;
	}

	@Override
	public RequestLogger getLogger() {
		return requestLogger;
	}

	@Override
	public List<RequestValue> getParameters() {
		return params;
	}

	@Override
	public Request getParent() {
		return parent;
	}

	@Override
	public List<RequestValue> getResourceIdentifiers() {
		return resourceIdentifiers;
	}

	@Override
	public Integer getSelectLimit() {
		return selectLimit;
	}

	@Override
	public Integer getSelectOffset() {
		return selectOffset;
	}

	@Override
	public String getSqlResource() {
		return sqlResource;
	}

	@Override
	public Request.Type getType() {
		return type;
	}

	@Override
	public boolean hasParameter(final String name) {
		for (final RequestValue param : params) {
			if (name.equals(param.getName())) {
				return true;
			}
		}
		return false;
	}

	/** Sets parameters for request. Used for cloning requests on child objects. Does not scan for output param. */
	@Override
	public void setParameters(final List<RequestValue> params) {
		this.params = params;
	}

	@Override
	public void setParent(final Request parent) {
		this.parent = parent;
	}

	@Override
	public void setSelectLimit(final Integer selectLimit) {
		this.selectLimit = selectLimit;
	}

	@Override
	public void setSelectOffset(final Integer selectOffset) {
		this.selectOffset = selectOffset;
	}

	/**
	 * Returns string representation, using HttpRequestAttributes string if present. of resource identifiers and params
	 */
	@Override
	public String toString() {
		if (httpAttributes != null) {
			return httpAttributes.toString();
		} else {
			return type + " " + sqlResource;
		}
	}

	@Override
	public void extractParameters() throws InvalidRequestException {
		if (params != null && params.size() > 0) {
			RequestValue selectLimitRequestValue = null, selectOffsetRequestValue = null;
			for (final RequestValue requestValue : params) {
				// Extract limit and offset
				if (requestValue.getName().equalsIgnoreCase(Request.PARAM_NAME_LIMIT)) {
					selectLimitRequestValue = setSelectLimitOrOffset(Request.PARAM_NAME_LIMIT, requestValue);
				} else if (requestValue.getName().equalsIgnoreCase(Request.PARAM_NAME_OFFSET)) {
					selectOffsetRequestValue = setSelectLimitOrOffset(Request.PARAM_NAME_OFFSET, requestValue);
				}
			}

			// Validate both limit and offset provided
			if (type == Type.SELECT) {
				if (selectLimit != null && selectOffset == null) {
					throw new InvalidRequestException(InvalidRequestException.MESSAGE_OFFSET_REQUIRED);
				} else if (selectOffset != null && selectLimit == null) {
					throw new InvalidRequestException(InvalidRequestException.MESSAGE_LIMIT_REQUIRED);
				} else if (selectLimit != null && selectOffset != null) {
					params.remove(selectLimitRequestValue);
					params.remove(selectOffsetRequestValue);
				}
			}
		}
	}

	private RequestValue setSelectLimitOrOffset(final String paramName, final RequestValue requestValue)
			throws InvalidRequestException {
		if (requestValue.getValue() instanceof Integer) {
			if (paramName.equals(Request.PARAM_NAME_LIMIT)) {
				selectLimit = (Integer) requestValue.getValue();
			} else {
				selectOffset = (Integer) requestValue.getValue();
			}
		} else if (requestValue.getValue() instanceof String) {
			try {
				if (paramName.equals(Request.PARAM_NAME_LIMIT)) {
					selectLimit = Integer.valueOf((String) requestValue.getValue());
				} else {
					selectOffset = Integer.valueOf((String) requestValue.getValue());
				}
			} catch (final NumberFormatException exception) {
				throw new InvalidRequestException(paramName + " value " + requestValue.getValue()
						+ " is not a number");
			}
		} else {
			throw new InvalidRequestException(paramName + " value " + requestValue.getValue()
					+ " is not an Integer");
		}
		return requestValue;
	}
}
