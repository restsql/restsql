/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.tools.impl.mysql;

import org.restsql.tools.impl.AbstractResourceDefinitionGenerator;

/**
 * Provides MySQL specific columns query.
 * 
 * @author Mark Sawers
 */
public class MySqlResourceDefinitionGenerator extends AbstractResourceDefinitionGenerator {
	private static final String SQL_COLUMNS_QUERY = "select column_name, table_name from information_schema.columns where table_schema = ? and table_name not in (select table_name from information_schema.views)";

	@Override
	public String getColumnsQuery() {
		return SQL_COLUMNS_QUERY;
	}

}
