/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.tools.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.sqlresource.Database;
import org.restsql.core.sqlresource.MetaData;
import org.restsql.core.sqlresource.ObjectFactory;
import org.restsql.core.sqlresource.Query;
import org.restsql.core.sqlresource.SqlResourceDefinition;
import org.restsql.core.sqlresource.Table;
import org.restsql.tools.ResourceDefinitionGenerator;

/**
 * Base implemenation for generator.
 * 
 * @author Mark Sawers
 */
public abstract class AbstractResourceDefinitionGenerator implements ResourceDefinitionGenerator {

	@Override
	public int generate(final String relativeSubDir, final String databaseName, final String exclusionPattern)
			throws GenerationException {
		if (databaseName == null || databaseName.length() == 0) {
			throw new GenerationException("databaseName required");
		}
		final String sqlResourcesDir = Config.properties.getProperty(Config.KEY_SQLRESOURCES_DIR,
				Config.DEFAULT_SQLRESOURCES_DIR);
		Config.logger.info("Attempting to generate resource definitions in subdirectory " + relativeSubDir
				+ " for database " + databaseName);

		try {
			final File subdirObj = createSubDir(relativeSubDir, sqlResourcesDir);
			return createDefs(subdirObj, databaseName, exclusionPattern);
		} catch (final GenerationException exception) {
			Config.logger.error(exception.toString());
			throw exception;
		}
	}

	/**
	 * Creates resource definitions.
	 * 
	 * @param subDirObj subdir file object
	 * @param databaseName database name
	 * @return number of definitions created
	 * @throws GenerationException if a database access or file write error occurs
	 */
	protected int createDefs(final File subDirObj, final String databaseName, final String exclusionPattern)
			throws GenerationException {
		// Create definition object
		final ObjectFactory objectFactory = new ObjectFactory();
		final SqlResourceDefinition def = objectFactory.createSqlResourceDefinition();
		final Query query = objectFactory.createQuery();
		def.setQuery(query);
		final MetaData metaData = objectFactory.createMetaData();
		final Database database = objectFactory.createDatabase();
		database.setDefault(databaseName);
		metaData.setDatabase(database);
		final Table table = objectFactory.createTable();
		table.setRole("Parent");
		metaData.getTable().add(table);
		def.setMetadata(metaData);

		StringBuilder queryString = null;
		int defsCreated = 0;

		// Now inspect the information schema for columns and tables, build definition and write the files
		Connection connection = null;
		try {
			connection = Factory.getConnection(databaseName);

			// Build SQL query, prepare statement and execute
			String sql = getColumnsQuery();
			if (exclusionPattern != null) {
				sql += getTableExclusionQueryClause();
			}
			final PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, databaseName);
			if (exclusionPattern != null) {
				statement.setString(2, exclusionPattern);
			}
			Config.logger.info(sql);
			final ResultSet resultSet = statement.executeQuery();

			// Iterate through results, create build def and write the files
			while (resultSet.next()) {
				final String columnName = resultSet.getString(1);
				final String tableName = resultSet.getString(2);

				if (!tableName.equals(table.getName())) {
					if (defsCreated > 0) {
						// Complete previous def and write it
						queryString.append("\n\t\tFROM ");
						queryString.append(table.getName());
						queryString.append("\n\t");
						query.setValue(queryString.toString());
						writeDef(subDirObj, def, table.getName());
						table.setName(tableName);
					} else {
						table.setName(tableName);
					}

					// Start new def
					defsCreated++;
					queryString = new StringBuilder();
					queryString.append("\n\t\tSELECT ");
					queryString.append(columnName);
				} else {
					queryString.append(", ");
					queryString.append(columnName);
				}
			}
			// Finish up the last one
			if (defsCreated > 0) {
				queryString.append(" FROM ");
				queryString.append(table.getName());
				query.setValue(queryString.toString());
				writeDef(subDirObj, def, table.getName());
			}
		} catch (final SQLException exception) {
			throw new GenerationException(exception.toString());
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException e) {
				}
			}
		}

		Config.logger.info("Generated " + defsCreated + " resource definitions in "
				+ subDirObj.getAbsolutePath());
		return defsCreated;
	}

	/**
	 * Hook method for implementation by db-specific classes. Must be preparable statement with single query param of
	 * the database name. Columns returned must be column name and then table name.
	 */
	protected abstract String getColumnsQuery();

	/** Returns query clause for a table name exclusion pattern. */
	protected String getTableExclusionQueryClause() {
		return " AND table_name NOT LIKE ?";
	}

	/**
	 * Creates sub directory if it does not exist.
	 * 
	 * @param relativeSubDir directory path relative to sql resources directory to create
	 * @param sqlResourcesDir absolute sql resources directory
	 * @throws GenerationException if subdir could not be created, or it exists and is not writable or empty
	 */
	protected File createSubDir(final String relativeSubDir, final String sqlResourcesDir)
			throws GenerationException {
		File dir;
		if (relativeSubDir == null || relativeSubDir.length() == 0) {
			dir = new File(sqlResourcesDir);
		} else {
			dir = new File(sqlResourcesDir + "/" + relativeSubDir);
			if (!dir.exists()) {
				if (!dir.mkdir()) {
					throw new GenerationException("Could not create subdir " + dir.getAbsolutePath());
				}
			} else {
				if (!dir.canWrite()) {
					throw new GenerationException("Cannot write to subdir " + dir.getAbsolutePath());
				} else if (dir.list().length > 0) {
					throw new GenerationException("Subdir " + dir.getAbsolutePath()
							+ " exists and is not empty");
				}
			}
		}
		return dir;
	}

	/**
	 * Writes definition to file in provided subdirectory.
	 * 
	 * @param subDirObj subdir file object
	 * @param def sql resource definition
	 * @param resourceName table name
	 * @throws GenerationException if a serialization or write error occurs
	 */
	private void writeDef(final File subDirObj, final SqlResourceDefinition def, final String resourceName)
			throws GenerationException {
		final String defFileName = subDirObj + "/" + resourceName + ".xml";
		try {
			final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
			final Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			final FileWriter writer = new FileWriter(defFileName);
			marshaller.marshal(def, writer);
		} catch (final JAXBException exception) {
			throw new GenerationException(exception.toString());
		} catch (final IOException exception) {
			throw new GenerationException(exception.toString());
		}

		Config.logger.info("Wrote resource definition " + defFileName);
	}
}
