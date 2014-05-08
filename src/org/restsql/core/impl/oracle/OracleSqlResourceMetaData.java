/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.sql.ResultSetMetaData;
import javax.sql.rowset.RowSetMetaDataImpl; 
import org.restsql.core.impl.AbstractSqlResourceMetaData;
import org.restsql.core.impl.ColumnMetaDataImpl;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.SqlResourceDefinitionUtils;    

/**
 * Metadata implementation for Oracle Database
 * 
 * @author Piotr Roznicki
 */

public class OracleSqlResourceMetaData extends AbstractSqlResourceMetaData {
	private static final String SQL_COLUMNS_QUERY = "select column_name, data_type, data_default from all_tab_columns where owner =  ? and table_name = ?";
	private static final String SQL_PK_QUERY = "SELECT cols.table_name, cols.column_name, cols.position, cons.status, cons.owner FROM all_constraints cons, all_cons_columns cols"
			+ " WHERE cons.owner = ? AND cols.table_name = ?"
			+ " AND cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name ";

	/**
	 * Retrieves database name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	@Override
	protected String getColumnDatabaseName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return SqlResourceDefinitionUtils.getDefaultDatabase(definition);
	}

	/**
	 * Retrieves actual column name from result set meta data. Hook method for buildTablesAndColumns() allows
	 * database-specific overrides.
	 */
	@Override
	protected String getColumnName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
		return ((ResultSetMetaData) resultSetMetaData).getColumnName(colNumber);
	}

	/**
	 * Retrieves table name from definition. Oracle getTableName returns null!!!
	 * overrides.
	 */
	@Override
	protected String getColumnTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
    return definition.getMetadata().getTable().get(0).getName();
		//return ((ResultSetMetaData) resultSetMetaData).getTableName(colNumber);
	}

	/**
	 * Retrieves sql for querying columns. Hook method for buildInvisibleForeignKeys() and buildJoinTableMetadata()
	 * allows database-specific overrides.
	 */
	@Override
	protected String getSqlColumnsQuery() {
		return SQL_COLUMNS_QUERY;
	}

	/**
	 * Retrieves sql for querying primary keys. Hook method for buildPrimaryKeys allows database-specific overrides.
	 */
	@Override
	protected String getSqlPkQuery() {
		return SQL_PK_QUERY;
	}

	/** Retrieves database-specific table name used in SQL statements. */
	@Override
	protected String getQualifiedTableName(final SqlResourceDefinition definition,
			final ResultSetMetaData resultSetMetaData, final int colNumber) throws SQLException {
    return definition.getMetadata().getTable().get(0).getName();
	}

	/** Retrieves database-specific table name used in SQL statements. Used to build join table meta data. */
	@Override
	protected String getQualifiedTableName(Connection connection, String databaseName, String tableName) {
			return databaseName + "." + tableName;
		}

	/**
	 * Return whether a column in the given result set is read-only. The Oracle implementation calls isReadOnly()
	 */
	@Override
	protected boolean isColumnReadOnly(ResultSetMetaData resultSetMetaData, int colNumber)
			throws SQLException {
		return (resultSetMetaData.isReadOnly(colNumber));
		// Hack access to protected member "fields" as we need to find the value
		// of Field.positionInTable which is 0 for columns based on SQL functions
		/*try {
			java.lang.reflect.Field fields_array = AbstractJdbc2ResultSetMetaData.class
					.getDeclaredField("fields");
			fields_array.setAccessible(true);
			final Field[] fields = (Field[]) fields_array.get(resultSetMetaData);
			return fields[colNumber - 1].getPositionInTable() == 0;
		} catch (Exception e) {
			throw new SQLException(e.toString());
		}*/
	}

	/**
	 * Sets sequence metadata for a column with the columns query result set. The column_default column will contain a
	 * string in the format nextval('sequence-name'::regclass), where sequence-name is the sequence name.
	 * 
	 * @throws SQLException when a database error occurs
	 */
	@Override
	protected void setSequenceMetaData(ColumnMetaDataImpl column, ResultSet resultSet) throws SQLException {
		final String columnDefault = resultSet.getString(3);
		if (columnDefault != null && columnDefault.startsWith("nextval")) {
			column.setSequence(true);
			column.setSequenceName(columnDefault.substring(9, columnDefault.indexOf('\'', 10)));
		}
	}
  
  protected String getSqlMainQuery(final SqlResourceDefinition definition) {
		return definition.getQuery().getValue() + " WHERE ROWNUM = 1 " ;
	}
   
}
