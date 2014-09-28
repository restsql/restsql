/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import com.sun.jersey.core.util.Base64;

/**
 * Wraps a byte array for input/output to a binary type column. String representation is expected in base64.
 * 
 * @author Mark Sawers
 */
public class BinaryObject {
	private byte[] bytes;

	public BinaryObject(byte[] bytes) {
		this.bytes = bytes;
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	/** Generates base64 string representation. */
	public String toString() {
		return new String(Base64.encode(bytes));
	}
	
	/** Returns true if string is base64 encoded. */
	public static boolean isStringBase64(String string) {
		return Base64.isBase64(string);
	}
	
	/** Parses bytes from base64 string representation. */
	public static BinaryObject fromString(String string) {
		return new BinaryObject(Base64.decode(string));
	}
}
