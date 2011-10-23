/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.TableMetaData;

/**
 * Encapsulates table information for an SQL Resource.
 * 
 * @author Mark Sawers
 * @see ColumnMetaData
 */
public class TableMetaDataImpl implements TableMetaData {
	private final Map<String, ColumnMetaData> columns;
	private final String databaseName;
	private final List<ColumnMetaData> primaryKeys;
	private final String qualifiedTableName;
	private String tableAlias;
	private final String tableName;
	private final TableRole tableRole;

	public TableMetaDataImpl(final String tableName, final String qualifedTableName,
			final String databaseName, final TableRole tableRole) {
		this.tableName = tableName;
		this.tableAlias = tableName;
		qualifiedTableName = qualifedTableName;
		this.databaseName = databaseName;
		this.tableRole = tableRole;
		primaryKeys = new ArrayList<ColumnMetaData>();
		columns = new HashMap<String, ColumnMetaData>();
	}

	public void addColumn(final ColumnMetaData column) {
		columns.put(column.getColumnLabel(), column);
	}

	public void addPrimaryKey(final ColumnMetaData column) {
		primaryKeys.add(column);
	}

	public Map<String, ColumnMetaData> getColumns() {
		return columns;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public List<ColumnMetaData> getPrimaryKeys() {
		return primaryKeys;
	}

	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public String getTableName() {
		return tableName;
	}

	public TableRole getTableRole() {
		return tableRole;
	}

	public boolean isChild() {
		return tableRole == TableRole.Child;
	}

	public boolean isParent() {
		return tableRole == TableRole.Parent;
	}

	void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
}
