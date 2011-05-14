/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.sql.Types;

import org.restsql.core.ColumnMetaData;

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
	private boolean primaryKey;
	private boolean nonqueriedForeignKey;
	private final String qualifiedTableName;
	private final String tableName;

	ColumnMetaDataImpl(final int columnNumber, final String databaseName, final String tableName,
			final String columnName, final String columnLabel, final String columnTypeName,
			final int columnType) {
		this.columnNumber = columnNumber;
		this.databaseName = databaseName;
		this.tableName = tableName;
		this.columnName = columnName;
		this.columnLabel = columnLabel;
		this.columnTypeName = columnTypeName;
		this.columnType = columnType;
		this.qualifiedTableName = databaseName + "." + tableName;
	}

	/**
	 * Used for foreign key columns not declared in the SqlResource query. These are required for writes to child
	 * extensions, parent extensions and and one-to-many child tables.
	 * 
	 * @todo columnTypeString mappping to types may be MySQL-specific
	 */
	ColumnMetaDataImpl(final String databaseName, final String tableName, final String columnName,
			final String columnTypeString) {
		this(0, databaseName, tableName, columnName, columnName, columnTypeString, 0);
		if (columnTypeString.equalsIgnoreCase("smallint")) {
			columnType = Types.SMALLINT;
		} else if (columnTypeString.equalsIgnoreCase("bigint")) {
			columnType = Types.BIGINT;
		} else if (columnTypeString.equalsIgnoreCase("integer")) {
			columnType = Types.INTEGER;
		}
		nonqueriedForeignKey = true;
	}

	public String getColumnLabel() {
		return columnLabel;
	}

	public String getColumnName() {
		return columnName;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public int getColumnType() {
		return columnType;
	}

	public String getColumnTypeName() {
		return columnTypeName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	public String getTableName() {
		return tableName;
	}

	public boolean isCharType() {
		boolean charType = false;
		switch (columnType) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
				charType = true;
				break;

			default:
				// do nothing
		}
		return charType;
	}

	public boolean isNonqueriedForeignKey() {
		return nonqueriedForeignKey;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}

	void setPrimaryKey(final boolean primaryKey) {
		this.primaryKey = primaryKey;
	}
}
