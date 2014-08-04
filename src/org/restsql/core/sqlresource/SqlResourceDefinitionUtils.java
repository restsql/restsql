/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.sqlresource;

import java.util.ArrayList;
import java.util.List;

import org.restsql.core.ColumnMetaData;
import org.restsql.core.SqlResourceException;
import org.restsql.core.TableMetaData;
import org.restsql.core.TableMetaData.TableRole;

/**
 * Contains utilities to process definitions.
 * 
 * @author Mark Sawers
 */
public class SqlResourceDefinitionUtils {

	/** Returns default database. */
	public static String getDefaultDatabase(final SqlResourceDefinition definition) {
		return definition.getMetadata().getDatabase().getDefault();
	}

	/** Returns table object for column, ignoring case. Uses qualified name first, then unqualified. */
	public static Table getTable(final SqlResourceDefinition definition, final ColumnMetaData column) {
		Table target = null;
		for (final Table table : definition.getMetadata().getTable()) {
			if (table.getName().equalsIgnoreCase(column.getQualifiedTableName())) {
				target = table;
				break;
			} else if (table.getName().equalsIgnoreCase(column.getTableName())) {
				target = table;
				break;
			}
		}
		return target;
	}

	/**
	 * Returns table object with specified table name, ignoring case. If the qualified name doesn't exist, finds table
	 * by unqualified name.
	 */
	public static Table getTable(final SqlResourceDefinition definition, final String tableName) {
		Table target = null;
		for (final Table table : definition.getMetadata().getTable()) {
			if (table.getName().equalsIgnoreCase(tableName)) {
				target = table;
			}
		}
		return target;
	}

	/** Returns table object with desired role. If there are multiple, returns first one. */
	public static Table getTable(final SqlResourceDefinition definition, final TableMetaData.TableRole role) {
		Table target = null;
		for (final Table table : definition.getMetadata().getTable()) {
			if (TableMetaData.TableRole.valueOf(table.getRole()) == role) {
				target = table;
				break;
			}
		}
		return target;
	}

	/** Returns list of table objects with specified role. */
	public static List<Table> getTableList(final SqlResourceDefinition definition,
			final TableMetaData.TableRole role) {
		final List<Table> target = new ArrayList<Table>(3);
		for (final Table table : definition.getMetadata().getTable()) {
			if (TableMetaData.TableRole.valueOf(table.getRole()) == role) {
				target.add(table);
			}
		}
		return target;
	}

	/**
	 * Throws SqlResourceException if definition meets one of the following criteria:
	 * <ol>
	 * <li>No query element</li>
	 * <li>No metadata element</li>
	 * <li>No database element with default database name</li>
	 * <li>No Parent table element</li>
	 * <li>More than one Parent, Child or Join table elements</li>
	 * </ol>
	 * 
	 * @param definition definition
	 * @throws SqlResourceException if definition is invalid
	 */
	public static void validate(final SqlResourceDefinition definition) throws SqlResourceException {
		if (definition.getQuery() == null) {
			throw new SqlResourceException("Definition requires one query element");
		} else if (definition.getMetadata() == null) {
			throw new SqlResourceException("Definition requires one metadata element");
		} else if (definition.getMetadata().getDatabase() == null
				|| definition.getMetadata().getDatabase().getDefault() == null
				|| definition.getMetadata().getDatabase().getDefault().length() == 0) {
			throw new SqlResourceException("Definition requires one database element with default name");
		} else if (getTable(definition, TableRole.Parent) == null) {
			throw new SqlResourceException("Definition requires one table element with role Parent");
		} else if (getTableList(definition, TableRole.Parent).size() > 1) {
			throw new SqlResourceException("Definition requires one table element with role Parent");
		} else if (getTableList(definition, TableRole.Child).size() > 1) {
			throw new SqlResourceException("Definition requires one table element with role Child");
		} else if (getTableList(definition, TableRole.Join).size() > 1) {
			throw new SqlResourceException("Definition requires one table element with role Join");
		}
	}
}
