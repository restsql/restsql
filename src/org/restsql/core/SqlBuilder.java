/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

/**
 * Builds SQL for an operation on a SQL Resource.
 * 
 * @author Mark Sawers
 */
public interface SqlBuilder {

	/** Creates select SQL. */
	public String buildSelectSql(final SqlResourceMetaData metaData, final String mainSql,
			final List<RequestValue> resourceIdentifiers, final List<RequestValue> params)
			throws InvalidRequestException;

	/** Creates select SQL limit clause. Returns empty string if database does not support limit feature. */
	public String buildSelectLimitSql(final int limit, final int offset);

	/** Creates update, insert or delete SQL. */
	public Map<String, SqlStruct> buildWriteSql(final SqlResourceMetaData metaData, final Request request,
			final boolean doParent) throws InvalidRequestException;

	/**
	 * Helper struct for building SQL.
	 * 
	 * @author Mark Sawers
	 */
	public static class SqlStruct {
		private StringBuffer clause;
		private int limit = -1;
		private StringBuffer main;
		private int offset = -1;

		public SqlStruct(final int mainSize, final int clauseSize) {
			main = new StringBuffer(mainSize);
			clause = new StringBuffer(clauseSize);
		}

		public void appendClauseToMain() {
			main.append(clause);
		}

		public StringBuffer getClause() {
			return clause;
		}

		public int getLimit() {
			return limit;
		}

		public StringBuffer getMain() {
			return main;
		}

		public int getOffset() {
			return offset;
		}

		public boolean isClauseEmpty() {
			return clause.length() == 0;
		}

		public void setLimit(final int limit) {
			this.limit = limit;
		}

		public void setOffset(final int offset) {
			this.offset = offset;
		}
	}

}