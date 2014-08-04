/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.tools;

/**
 * Creates resource definition files, one per table found in the specified database, to the specified subdirectory.
 * 
 * @author Mark Sawers
 */
public interface ResourceDefinitionGenerator {

	/**
	 * Creates resource definition files, one per table in the provided database.
	 * 
	 * @param relativeSubDir directory path relative to sql resources directory to create
	 * @param databaseName database name
	 * @param exclusionPattern table name pattern to exclude
	 * @return number of definitions created
	 * @throws GenerationException if a database access or file write error occurs
	 */
	public abstract int generate(String relativeSubDir, String databaseName, String exclusionPattern) throws GenerationException;

	/** Represents generation exception. */
	public static class GenerationException extends Exception {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs exception.
		 * 
		 * @param string message
		 */
		public GenerationException(String string) {
			super(string);
		}
	}

}