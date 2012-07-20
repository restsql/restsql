/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.List;

import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.RequestLogger;

/**
 * Represents a restSQL request.
 * 
 * @author Mark Sawers
 */
public class RequestImpl implements Request {
	private HttpRequestAttributes httpAttributes;
	private final List<List<NameValuePair>> childrenParams;
	private List<NameValuePair> params;
	private Request parent;
	private final RequestLogger requestLogger;
	private final List<NameValuePair> resourceIdentifiers;
	private final String sqlResource;
	private final Request.Type type;

	/** Constructs object. */
	public RequestImpl(HttpRequestAttributes httpAttributes, final Request.Type type,
			final String sqlResource, final List<NameValuePair> resourceIdentifiers,
			final List<NameValuePair> params, final List<List<NameValuePair>> childrenParams,
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
		requestLogger.setRequest(this);
	}

	@Override
	public List<List<NameValuePair>> getChildrenParameters() {
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
	public List<NameValuePair> getParameters() {
		return params;
	}

	@Override
	public Request getParent() {
		return parent;
	}

	@Override
	public List<NameValuePair> getResourceIdentifiers() {
		return resourceIdentifiers;
	}

	@Override
	public String getSqlResource() {
		return sqlResource;
	}

	@Override
	public Request.Type getType() {
		return type;
	}

	/** Sets parameters for request. Used for cloning requests on child objects. Does not scan for output param. */
	@Override
	public void setParameters(final List<NameValuePair> params) {
		this.params = params;
	}

	@Override
	public void setParent(final Request parent) {
		this.parent = parent;
	}

	/**
	 * Returns string representation, using HttpRequestAttributes string if present.
	 * 
	 * @todo build string representation of resource identifiers and params
	 */
	@Override
	public String toString() {
		if (httpAttributes != null) {
			return httpAttributes.toString();
		} else {
			return type + " " + sqlResource;
		}
	}
}
