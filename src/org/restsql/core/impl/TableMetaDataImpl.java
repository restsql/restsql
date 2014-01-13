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
//TODO: remove dependency on this class, probably by moving setters to the interface

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

	@XmlTransient
	private List<ColumnMetaData> primaryKeys;

	@XmlElementWrapper(name = "primaryKeys")
	@XmlElement(name = "column")
	private List<String> primaryKeyNames;

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

	public TableMetaDataImpl(final String tableName, final String qualifedTableName,
			final String databaseName, final TableRole tableRole) {
		this.tableName = tableName;
		this.tableAlias = tableName;
		qualifiedTableName = qualifedTableName;
		this.databaseName = databaseName;
		this.tableRole = tableRole;
		primaryKeys = new ArrayList<ColumnMetaData>();
		primaryKeyNames = new ArrayList<String>();
		columnMap = new HashMap<String, ColumnMetaData>();
		columnList = columnMap.values();
	}

	public void addColumn(final ColumnMetaData column) {
		columnMap.put(column.getColumnLabel(), column);
	}

	public void addPrimaryKey(final ColumnMetaData column) {
		primaryKeys.add(column);
		primaryKeyNames.add(column.getQualifiedColumnName());
	}

	public Map<String, ColumnMetaData> getColumns() {
		return columnMap;
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