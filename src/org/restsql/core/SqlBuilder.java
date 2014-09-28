/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds SQL for an operation on a SQL Resource.
 * 
 * @author Mark Sawers
 */
public interface SqlBuilder {

	/** Creates select SQL. */
	public SqlStruct buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final Request request) throws InvalidRequestException;

	/** Creates update, insert or delete SQL. */
	public Map<String, SqlStruct> buildWriteSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException;

	/**
	 * Helper struct for building SQL.
	 * 
	 * @author Mark Sawers
	 */
	public static class SqlStruct {
		private final StringBuilder clause, main, preparedClause, preparedStatement, statement;
		private StringBuilder preparedMain;
		private final List<Object> preparedValues;

		public SqlStruct(final int mainSize, final int clauseSize) {
			main = new StringBuilder(mainSize);
			clause = new StringBuilder(clauseSize);
			preparedClause = new StringBuilder(clauseSize);
			preparedValues = new ArrayList<Object>(clauseSize);
			statement = new StringBuilder(mainSize + clauseSize);
			preparedStatement = new StringBuilder(mainSize + clauseSize);
		}

		public SqlStruct(final int mainSize, final int clauseSize, final boolean usePreparedMain) {
			this(mainSize, clauseSize);
			preparedMain = new StringBuilder(mainSize);
		}

		public void appendToBothClauses(final String string) {
			clause.append(string);
			preparedClause.append(string);
		}

		public void appendToBothMains(final String string) {
			main.append(string);
			preparedMain.append(string);
		}

		/**
		 * Appends clause to the main for the complete statement, and prepared clause to the main for the complete
		 * prepared statement.
		 */
		public void compileStatements() {
			statement.append(main);
			statement.append(clause);
			preparedStatement.append(preparedMain == null ? main : preparedMain);
			preparedStatement.append(preparedClause);
		}

		public StringBuilder getClause() {
			return clause;
		}

		public StringBuilder getMain() {
			return main;
		}

		public StringBuilder getPreparedClause() {
			return preparedClause;
		}

		public StringBuilder getPreparedMain() {
			return preparedMain;
		}

		public String getPreparedStatement() {
			return preparedStatement.toString();
		}

		public List<Object> getPreparedValues() {
			return preparedValues;
		}

		public String getStatement() {
			return statement.toString();
		}

		public boolean isClauseEmpty() {
			return clause.length() == 0;
		}
	}

}