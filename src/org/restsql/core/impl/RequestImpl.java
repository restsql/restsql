/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.List;

import org.restsql.core.NameValuePair;
import org.restsql.core.Request;
import org.restsql.core.RequestLogger;

/**
 * Represents a restSQL request.
 * 
 * @author Mark Sawers
 */
public class RequestImpl implements Request {
	private List<NameValuePair> params;
	private final List<NameValuePair> resourceIdentifiers;
	private String sqlResource;
	private final Request.Type type;
	private List<List<NameValuePair>> childrenParams;
	private RequestLogger requestLogger;

	public RequestImpl(final Request.Type type, final String sqlResource,
			final List<NameValuePair> resourceIdentifiers, final List<NameValuePair> params, final List<List<NameValuePair>> childrenParams, RequestLogger requestLogger) {
		this.type = type;
		this.resourceIdentifiers = resourceIdentifiers;
		this.sqlResource = sqlResource;
		this.params = params;
		this.childrenParams = childrenParams;
		this.requestLogger = requestLogger;
	}

	public List<List<NameValuePair>> getChildrenParameters() {
		return childrenParams;
	}

	public RequestLogger getLogger() {
		return requestLogger;
	}
	
	public List<NameValuePair> getParameters() {
		return params;
	}

	public List<NameValuePair> getResourceIdentifiers() {
		return resourceIdentifiers;
	}

	public String getSqlResource() {
		return sqlResource;
	}

	public Request.Type getType() {
		return type;
	}
	
	public void setParameters(List<NameValuePair> params) {
		this.params = params;
	}
}
