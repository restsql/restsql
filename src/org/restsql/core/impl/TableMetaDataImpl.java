/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.TableMetaData;

/**
 * Encapsulates table information for an SQL Resource.
 * 
 * @author Mark Sawers
 * @see ColumnMetaData
 */

@XmlType(name = "TableMetaData", namespace = "http://restsql.org/schema", propOrder = { "databaseName",
		"tableName", "tableAlias", "qualifiedTableName", "tableRole", "columnList", "primaryKeyNames" })
public class TableMetaDataImpl implements TableMetaData {
	@XmlElementWrapper(name = "columns", required = true)
	@XmlElement(name = "column", type = ColumnMetaDataImpl.class, required = true)
	private Collection<ColumnMetaData> columnList;

	@XmlTransient
	private Map<String, ColumnMetaData> columnMap;

	@XmlAttribute(required = true)
	private String databaseName;

	@XmlElementWrapper(name = "primaryKeys")
	@XmlElement(name = "column")
	private List<String> primaryKeyNames;

	@XmlTransient
	private List<ColumnMetaData> primaryKeys;

	@XmlAttribute(required = true)
	private String qualifiedTableName;

	@XmlAttribute(required = true)
	private String tableAlias;

	@XmlAttribute(required = true)
	private String tableName;

	@XmlAttribute(required = true)
	private TableRole tableRole;

	// No-arg ctor required for JAXB
	public TableMetaDataImpl() {
	}

	@Override
	public void addColumn(final ColumnMetaData column) {
		columnMap.put(column.getColumnLabel(), column);
	}

	@Override
	public void addPrimaryKey(final ColumnMetaData column) {
		primaryKeys.add(column);
		primaryKeyNames.add(column.getQualifiedColumnName());
	}

	@Override
	public Map<String, ColumnMetaData> getColumns() {
		return columnMap;
	}

	@Override
	public String getDatabaseName() {
		return databaseName;
	}

	@Override
	public List<ColumnMetaData> getPrimaryKeys() {
		return primaryKeys;
	}

	@Override
	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	@XmlTransient
	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public String getTableName() {
		return tableName;
	}

	@Override
	public TableRole getTableRole() {
		return tableRole;
	}

	@Override
	public boolean isChild() {
		return tableRole == TableRole.Child;
	}

	@Override
	public boolean isParent() {
		return tableRole == TableRole.Parent;
	}

	@Override
	public void setAttributes(final String tableName, final String qualifedTableName,
			final String databaseName, final TableRole tableRole) {
		this.tableName = tableName;
		tableAlias = tableName;
		qualifiedTableName = qualifedTableName;
		this.databaseName = databaseName;
		this.tableRole = tableRole;
		primaryKeys = new ArrayList<ColumnMetaData>();
		primaryKeyNames = new ArrayList<String>();
		columnMap = new HashMap<String, ColumnMetaData>();
		columnList = columnMap.values();
	}

	@Override
	public void setTableAlias(final String tableAlias) {
		this.tableAlias = tableAlias;
	}
}