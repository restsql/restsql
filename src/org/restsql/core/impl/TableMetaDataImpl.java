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
		"tableName", "qualifiedTableName", "rowAlias", "rowSetAlias", "tableRole", "columnList",
		"primaryKeyNames" })
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
	private String rowAlias;

	@XmlAttribute(required = true)
	private String rowSetAlias;

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
	public String getRowAlias() {
		return rowAlias;
	}

	@XmlTransient
	@Override
	public String getRowSetAlias() {
		return rowSetAlias;
	}

	/**
	 * Returns row alias.
	 * 
	 * @deprecated As of 0.8.11 use {@link #getRowAlias()}
	 */
	@Deprecated
	@Override
	@XmlTransient
	public String getTableAlias() {
		return rowAlias;
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
		rowAlias = tableName;
		rowSetAlias = rowAlias + "s";
		qualifiedTableName = qualifedTableName;
		this.databaseName = databaseName;
		this.tableRole = tableRole;
		primaryKeys = new ArrayList<ColumnMetaData>();
		primaryKeyNames = new ArrayList<String>();
		columnMap = new HashMap<String, ColumnMetaData>();
		columnList = columnMap.values();
	}

	@Override
	public void setAliases(final String alias, final String rowAlias, final String rowSetAlias) {
		if (tableRole == TableRole.Parent || tableRole == TableRole.Child) {
			// Set the row alias
			if (rowAlias != null) {
				this.rowAlias = rowAlias;
			} else if (alias != null) {
				this.rowAlias = alias;
			} // else default set in init() to tableName

			// Set row set alias
			if (rowSetAlias != null) {
				this.rowSetAlias = rowSetAlias;
			} else {
				this.rowSetAlias = this.rowAlias + "s";
			}
		}
	}

	/**
     * @deprecated As of 0.8.11 use {@link #setAliases(String, String, String)}
	 */
	@Deprecated
	@Override
	public void setTableAlias(final String tableAlias) {
		rowAlias = tableAlias;
	}
}