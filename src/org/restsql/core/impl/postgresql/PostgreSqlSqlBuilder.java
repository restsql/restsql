/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.postgresql;

import java.sql.Types;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.impl.AbstractSqlBuilder;

/**
 * Adds limit clause and special handling for type casting parameters in prepared statements (apparently only needed for
 * IN operator).
 * 
 * @author Mark Sawers
 */
public class PostgreSqlSqlBuilder extends AbstractSqlBuilder {

	@Override
	public String buildSelectLimitSql(final int limit, final int offset) {
		StringBuilder string = new StringBuilder(25);
		string.append(" LIMIT ");
		string.append(limit);
		string.append(" OFFSET ");
		string.append(offset);
		return string.toString();
	}

	@Override
	protected String buildPreparedParameterSql(final ColumnMetaData column) {
		switch (column.getColumnType()) {
			case Types.BOOLEAN:
				return "?::boolean";

			case Types.DATE:
				return "?::date";

			case Types.TIME:
				return "?::time";

			case Types.TIMESTAMP:
				return "?::timestamp";

			case Types.BIT:
			case Types.SMALLINT:
			case Types.TINYINT:
				return "?::smallint";

			case Types.INTEGER:
				return "?::integer";

			case Types.BIGINT:
				return "?::bigint";

			case Types.NUMERIC:
				return "?::numeric";

			case Types.DECIMAL:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
				return "?::decimal";

			case Types.OTHER:
				return "?::" + column.getColumnTypeName();

			default:
				return "?";
		}
	}

}
