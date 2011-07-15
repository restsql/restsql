/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;

/**
 * Represents a single CRUD request on a specific SQL Resource.
 * 
 * @author Mark Sawers
 */
public interface Request {
	public static final String PARAM_NAME_LIMIT = "_limit";
	public static final String PARAM_NAME_OFFSET = "_offset";

	/** Returns children CUD requests to a single parent for a hierarchical SQL Resource. */
	public List<List<NameValuePair>> getChildrenParameters();

	/** Returns request logger. */
	public RequestLogger getLogger();

	/** Returns ordered list of paramters, for example the selection filter for update request. */
	public List<NameValuePair> getParameters();

	/**
	 * Returns ordered list of primary key values for a CRUD request on a single object (row). On a hierarchical SQL
	 * Resource, this list identifies the parent and the children are identified by the children parameters.
	 */
	public List<NameValuePair> getResourceIdentifiers();

	/** Returns SQL Resource name. */
	public String getSqlResource();

	/** Returns request type. */
	public Type getType();

	/** Sets parameters for request. Used for cloning requests on child objects. */ 
	public void setParameters(List<NameValuePair> params);

		/**
	 * Represents request types, mapping to CRUD operations.
	 * 
	 * @author Mark Sawers
	 */
	public enum Type {
		SELECT, INSERT, UPDATE, DELETE;

		public static Type fromHttpMethod(String method) {
			Type type;
			if (method.equals("DELETE")) {
				type = Type.DELETE;
			} else if (method.equals("GET")) {
				type = Type.SELECT;
			} else if (method.equals("POST")) {
				type = Type.INSERT;
			} else { // method.equals("PUT")
				type = Type.UPDATE;
			}
			return type;
		}
	}
}