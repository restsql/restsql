/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.mysql;

import org.restsql.core.impl.AbstractSqlBuilder;

/**
 * Adds limit clause.
 * 
 * @author Mark Sawers
 */
public class MySqlSqlBuilder extends AbstractSqlBuilder {

	@Override
	public String buildSelectLimitSql(final int limit, final int offset) {
		StringBuilder string = new StringBuilder(25);
		string.append(" LIMIT ");
		string.append(limit);
		string.append(" OFFSET ");
		string.append(offset);
		return string.toString();
	}

}
