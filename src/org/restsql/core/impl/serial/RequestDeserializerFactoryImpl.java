/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.serial;

import org.restsql.core.RequestDeserializer;
import org.restsql.core.SqlResourceException;
import org.restsql.core.Factory.RequestDeserializerFactory;

/**
 * Implements factory using default org.restsql.core.impl request deserializers.
 * 
 * @author Mark Sawers
 */
public class RequestDeserializerFactoryImpl implements RequestDeserializerFactory {
	private static XmlRequestDeserializer xmlRequestDeserializer = new XmlRequestDeserializer();
	private static JsonRequestDeserializer jsonRequestDeserializer = new JsonRequestDeserializer();

	/**
	 * Returns request deserializer for media type.
	 * 
	 * @throws SqlResourceException if deserializer not found for media type
	 */
	@Override
	public RequestDeserializer getRequestDeserializer(String mediaType) throws SqlResourceException {
		if (mediaType.equals("application/xml")) {
			return xmlRequestDeserializer;
		} else if (mediaType.equals("application/json")) {
			return jsonRequestDeserializer;
		} else {
			throw new SqlResourceException("No deserializer found for media type " + mediaType);
		}
	}

}
