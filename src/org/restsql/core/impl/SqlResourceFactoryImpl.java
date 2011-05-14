/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.core.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.restsql.core.Config;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.Trigger;
import org.restsql.core.Factory.SqlResourceFactory;
import org.restsql.core.Factory.SqlResourceFactoryException;
import org.restsql.core.sqlresource.ObjectFactory;
import org.restsql.core.sqlresource.SqlResourceDefinition;

/**
 * Manages SQL Resource construction.
 * 
 * @author Mark Sawers
 */
public class SqlResourceFactoryImpl implements SqlResourceFactory {
	private String sqlResourcesDir;
	private Map<String, SqlResource> sqlResources = new HashMap<String, SqlResource>();

	@SuppressWarnings("unchecked")
	public SqlResource getSqlResource(String resName) throws SqlResourceFactoryException,
			SqlResourceException {
		SqlResource sqlResource = sqlResources.get(resName);
		if (sqlResource == null) {
			InputStream inputStream = getInputStream(resName);
			JAXBContext context;
			try {
				context = JAXBContext.newInstance(ObjectFactory.class);
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				unmarshaller.setSchema(null);
				SqlResourceDefinition definition = ((JAXBElement<SqlResourceDefinition>) unmarshaller
						.unmarshal(inputStream)).getValue();
				sqlResource = new SqlResourceImpl(definition, new ArrayList<Trigger>());
				sqlResources.put(resName, sqlResource);
			} catch (JAXBException exception) {
				throw new SqlResourceFactoryException("Error unmarshalling SQL Resource "
						+ getSqlResourceFileName(resName) + " -- " + exception.getMessage());
			}
		}
		return sqlResource;
	}

	public InputStream getSqlResourceDefinition(String resName) throws SqlResourceFactoryException {
		return getInputStream(resName);
	}

	/**
	 * Returns available SQL Resource names.
	 */
	public List<String> getSqlResourceNames() {
		List<String> resNames = new ArrayList<String>();
		File dir = new File(getSqlResourcesDir());
		for (File file : dir.listFiles()) {
			int extIndex = file.getName().indexOf(".xml");
			if (extIndex > 0) {
				resNames.add(file.getName().substring(0, extIndex));
			}
		}
		return resNames;
	}

	public boolean isSqlResourceLoaded(String name) {
		return sqlResources.containsKey(name);
	}

	private String getSqlResourceFileName(String resName) {
		return getSqlResourcesDir() + "/" + resName + ".xml";
	}

	private InputStream getInputStream(String resName) throws SqlResourceFactoryException {
		String fileName = getSqlResourceFileName(resName);
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(fileName);
		} catch (FileNotFoundException exception) {
			inputStream = this.getClass().getResourceAsStream(fileName);
		}
		if (inputStream == null) {
			throw new SqlResourceFactoryException("SQL Resource " + resName + " not found - expected "
					+ fileName);
		}
		return inputStream;
	}

	private String getSqlResourcesDir() {
		if (sqlResourcesDir == null) {
			sqlResourcesDir = Config.properties.getProperty(Config.KEY_SQLRESOURCES_DIR,
					Config.DEFAULT_SQLRESOURCES_DIR);
			Config.logger.info("SqlResources dir is " + sqlResourcesDir);
		}
		return sqlResourcesDir;
	}
}
