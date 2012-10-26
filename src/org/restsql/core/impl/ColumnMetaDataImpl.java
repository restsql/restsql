/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Types;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.SqlResourceMetaData;

/**
 * Represents column (field) metadata.
 * 
 * @author Mark Sawers
 */
public class ColumnMetaDataImpl implements ColumnMetaData {
	private final String columnLabel;
	private final String columnName;
	private final int columnNumber;
	private int columnType;
	private final String columnTypeName;
	private final String databaseName;
	private boolean nonqueriedForeignKey;
	private boolean primaryKey;
	private String qualifiedColumnName;
	private final String qualifiedTableName;
	private boolean readOnly;
	private SqlResourceMetaData sqlResourceMetadata;
	private final String tableName;

	ColumnMetaDataImpl(final int columnNumber, final String databaseName, final String qualifiedTableName,
			final String tableName, final String columnName, final String columnLabel,
			final String columnTypeName, final int columnType, final boolean readOnly,
			SqlResourceMetaData sqlResourceMetaData) {
		this.columnNumber = columnNumber;
		this.databaseName = databaseName;
		this.qualifiedTableName = qualifiedTableName;
		this.tableName = tableName;
		this.columnName = columnName;
		this.columnLabel = columnLabel;
		this.columnTypeName = columnTypeName;
		this.columnType = columnType;
		this.readOnly = readOnly;
		this.sqlResourceMetadata = sqlResourceMetaData;
	}

	/**
	 * Used for foreign key columns not declared in the SqlResource select columns. These are required for writes to
	 * child extensions, parent extensions and child tables.
	 */
	ColumnMetaDataImpl(final String databaseName, final String sqlQualifiedTableName, final String tableName,
			final String columnName, final String columnLabel, final String columnTypeString, SqlResourceMetaData sqlResourceMetaData) {
		this(0, databaseName, sqlQualifiedTableName, tableName, columnName, columnLabel, columnTypeString, 0,
				false, sqlResourceMetaData);
		if (columnTypeString.equalsIgnoreCase("BIT")) {
			columnType = Types.BIT;
		} else if (columnTypeString.equalsIgnoreCase("TINYINT")) {
			columnType = Types.TINYINT;
		} else if (columnTypeString.equalsIgnoreCase("SMALLINT")) {
			columnType = Types.SMALLINT;
		} else if (columnTypeString.equalsIgnoreCase("INTEGER")) {
			columnType = Types.INTEGER;
		} else if (columnTypeString.equalsIgnoreCase("BIGINT")) {
			columnType = Types.BIGINT;
		} else if (columnTypeString.equalsIgnoreCase("FLOAT")) {
			columnType = Types.FLOAT;
		} else if (columnTypeString.equalsIgnoreCase("REAL")) {
			columnType = Types.REAL;
		} else if (columnTypeString.equalsIgnoreCase("DOUBLE")) {
			columnType = Types.DOUBLE;
		} else if (columnTypeString.equalsIgnoreCase("NUMERIC")) {
			columnType = Types.NUMERIC;
		} else if (columnTypeString.equalsIgnoreCase("DECIMAL")) {
			columnType = Types.DECIMAL;
		} else if (columnTypeString.equalsIgnoreCase("CHAR")) {
			columnType = Types.CHAR;
		} else if (columnTypeString.equalsIgnoreCase("VARCHAR")) {
			columnType = Types.VARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("LONGVARCHAR")) {
			columnType = Types.LONGVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("DATE")) {
			columnType = Types.DATE;
		} else if (columnTypeString.equalsIgnoreCase("TIME")) {
			columnType = Types.TIME;
		} else if (columnTypeString.equalsIgnoreCase("TIMESTAMP")) {
			columnType = Types.TIMESTAMP;
		} else if (columnTypeString.equalsIgnoreCase("BINARY")) {
			columnType = Types.BINARY;
		} else if (columnTypeString.equalsIgnoreCase("VARBINARY")) {
			columnType = Types.VARBINARY;
		} else if (columnTypeString.equalsIgnoreCase("LONGVARBINARY")) {
			columnType = Types.LONGVARBINARY;
		} else if (columnTypeString.equalsIgnoreCase("OTHER")) {
			columnType = Types.OTHER;
		} else if (columnTypeString.equalsIgnoreCase("JAVA_OBJECT")) {
			columnType = Types.JAVA_OBJECT;
		} else if (columnTypeString.equalsIgnoreCase("DISTICT")) {
			columnType = Types.DISTINCT;
		} else if (columnTypeString.equalsIgnoreCase("STRUCT")) {
			columnType = Types.STRUCT;
		} else if (columnTypeString.equalsIgnoreCase("ARRAY")) {
			columnType = Types.ARRAY;
		} else if (columnTypeString.equalsIgnoreCase("BLOB")) {
			columnType = Types.BLOB;
		} else if (columnTypeString.equalsIgnoreCase("CLOB")) {
			columnType = Types.CLOB;
		} else if (columnTypeString.equalsIgnoreCase("REF")) {
			columnType = Types.REF;
		} else if (columnTypeString.equalsIgnoreCase("DATALINK")) {
			columnType = Types.DATALINK;
		} else if (columnTypeString.equalsIgnoreCase("BOOLEAN")) {
			columnType = Types.BOOLEAN;
		} else if (columnTypeString.equalsIgnoreCase("ROWID")) {
			columnType = Types.ROWID;
		} else if (columnTypeString.equalsIgnoreCase("NCHAR")) {
			columnType = Types.NCHAR;
		} else if (columnTypeString.equalsIgnoreCase("NVARCHAR")) {
			columnType = Types.NVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("LONGNVARCHAR")) {
			columnType = Types.LONGNVARCHAR;
		} else if (columnTypeString.equalsIgnoreCase("NCLOB")) {
			columnType = Types.NCLOB;
		} else if (columnTypeString.equalsIgnoreCase("SQLXML")) {
			columnType = Types.SQLXML;
		} else {
			columnType = Types.NULL;
		}

		nonqueriedForeignKey = true;
	}

	@Override
	public String getColumnLabel() {
		return columnLabel;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public int getColumnNumber() {
		return columnNumber;
	}

	@Override
	public int getColumnType() {
		return columnType;
	}

	@Override
	public String getColumnTypeName() {
		return columnTypeName;
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public String getQualifiedColumnName() {
		if (qualifiedColumnName == null) {
			StringBuilder name = new StringBuilder(100);
			if (sqlResourceMetadata.hasMultipleDatabases()) {
				name.append(getQualifiedTableName());
			} else {
				name.append(getTableName());
			}
			name.append('.');
			name.append(getColumnName());
			qualifiedColumnName = name.toString();
		}
		return qualifiedColumnName;
	}

	@Override
	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public boolean isCharOrDateTimeType() {
		boolean charOrDateTimeType = false;
		switch (columnType) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.DATE:
			case Types.OTHER:	// postgresql driver returns this for char-type enums
				charOrDateTimeType = true;
				break;

			default:
				// do nothing
		}
		return charOrDateTimeType;
	}

	@Override
	public boolean isNonqueriedForeignKey() {
		return nonqueriedForeignKey;
	}

	@Override
	public boolean isPrimaryKey() {
		return primaryKey;
	}

	@Override
	public boolean isReadOnly() {
		return readOnly;
	}

	void setPrimaryKey(final boolean primaryKey) {
		this.primaryKey = primaryKey;
	}
}