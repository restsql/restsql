/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core;

import java.util.List;
import java.util.Map;

import org.restsql.core.Request.Type;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Represents meta data for sql resource. Queries database for table and column meta data and primary and foreign keys.
 * 
 * @author Mark Sawers
 */
public interface SqlResourceMetaData {

	public List<ColumnMetaData> getAllReadColumns();

	public TableMetaData getChild();

	public List<TableMetaData> getChildPlusExtTables();

	public List<ColumnMetaData> getChildReadColumns();

	public TableMetaData getJoin();

	public List<TableMetaData> getJoinList();

	public int getNumberTables();

	public TableMetaData getParent();

	public List<TableMetaData> getParentPlusExtTables();

	public List<ColumnMetaData> getParentReadColumns();

	public Map<String, TableMetaData> getTableMap();

	public List<TableMetaData> getTables();

	public List<TableMetaData> getWriteTables(final Type requestType, final boolean doParent);

	public boolean hasJoinTable();

	public boolean hasMultipleDatabases();

	public boolean isHierarchical();

	public void init(final String sqlResourceName, final SqlResourceDefinition definition, final SqlBuilder sqlBuilder)
			throws SqlResourceException;

	public String toHtml();

	public String toXml();
}