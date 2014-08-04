/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.tools.impl.postgresql;

import org.restsql.tools.impl.AbstractResourceDefinitionGenerator;

/**
 * Provides PostgreSQL specific columns query.
 * 
 * @author Mark Sawers
 */
public class PostgreSqlResourceDefinitionGenerator extends AbstractResourceDefinitionGenerator {
	private static final String SQL_COLUMNS_QUERY = "select column_name, table_name from information_schema.columns where table_catalog = ? and table_schema = 'public' and table_name not in (select table_name from information_schema.views)";

	@Override
	public String getColumnsQuery() {
		return SQL_COLUMNS_QUERY;
	}

}
