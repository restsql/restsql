/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import org.restsql.core.Factory.ResponseSerializerFactory;
import org.restsql.core.ResponseSerializer;
import org.restsql.core.SqlResourceException;

/**
 * Implements factory using default org.restsql.core.impl response serializers.
 * 
 * @author Mark Sawers
 */
public class ResponseSerializerFactoryImpl implements ResponseSerializerFactory {
	private static final ResponseSerializer xmlResponseSerializer = new XmlResponseSerializer();
	private static final ResponseSerializer jsonResponseSerializer = new JsonResponseSerializer();

	/**
	 * Returns response serializer for media type.
	 * 
	 * @throws SqlResourceException if serializer not found for media type
	 */
	@Override
	public ResponseSerializer getResponseSerializer(String mediaType) throws SqlResourceException {
		if (mediaType.equals("application/xml")) {
			return xmlResponseSerializer;
		} else if (mediaType.equals("application/json")) {
			return jsonResponseSerializer;
		} else {
			throw new SqlResourceException("No serializer found for media type " + mediaType);
		}
	}
}
