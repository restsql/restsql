/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.security.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.restsql.core.Config;
import org.restsql.core.Request;
import org.restsql.security.Authorizer;
import org.restsql.security.SecurityContext;
import org.restsql.security.SqlResourceRolePrivileges;
import org.restsql.security.SqlResourceRolePrivileges.Privilege;

/**
 * Authorizes restSQL requests using privileges properties file, which maps roles to request types on SQL Resources. If
 * privileges properties are not defined or not able to load, authorization is disabled. In that case,
 * <code>isAuthorized()</code> always returns <code>true</code>.
 * 
 * @author Mark Sawers
 */
public class AuthorizerImpl implements Authorizer {
	private boolean authEnabled = false;
	private String authStatusMessage;
	private String configFileName;
	// Map<sqlResource, List<SqlResourceRolePrivileges>>
	private Map<String, List<SqlResourceRolePrivileges>> privilegeMap;

	/** For use by the Factory. Uses properties file specified in standard restSQL properties. */
	public AuthorizerImpl() {
		configFileName = Config.properties.getProperty(Config.KEY_SECURITY_PRIVILEGES, null);
		loadPrivileges();
	}

	/** Creates object in test environment with specified properties file. */
	AuthorizerImpl(String configFileName) {
		this.configFileName = configFileName;
		loadPrivileges();
	}

	/** Returns true if authorization is enabled. */
	public boolean isAuthorizationEnabled() {
		return authEnabled;
	}
	
	/** Returns status message and string representation of all roles and associated privileges. */
	@Override
	public String dumpConfig() {
		final StringBuilder string = new StringBuilder(1000);
		string.append(authStatusMessage);
		if (authEnabled && privilegeMap.size() > 0) {
			string.append("\n\n[SqlResource,*].[requestType,*]=role\n");
			string.append("---------------------------------------------------------------------------\n");
			for (final List<SqlResourceRolePrivileges> privObjList : privilegeMap.values()) {
				for (final SqlResourceRolePrivileges privileges : privObjList) {
					string.append(privileges.toString());
					string.append("\n");
				}
			}
		}
		return string.toString();
	}

	// ** Returns true if user is authorized (or authorization is disabled), false otherwise. */
	@Override
	public boolean isAuthorized(final SecurityContext context, final Request request) {
		return isAuthorized(context, request.getType(), request.getSqlResource());
	}

	/** Returns true if user is authorized (or authorization is disabled), false otherwise. */
	@Override
	public boolean isAuthorized(SecurityContext context, Request.Type requestType, String sqlResource) {
		boolean authorized = false;
		if (authEnabled) {
			authorized = isAuthorized(context, requestType, privilegeMap.get(sqlResource));
			if (!authorized) {
				authorized = isAuthorized(context, requestType, privilegeMap.get(SqlResourceRolePrivileges.TOKEN_WILDCARD));
			}
		} else {
			authorized = true;
		}
		return authorized;
	}

	// Private utils

	private boolean isAuthorized(final SecurityContext context, final Request.Type requestType,
			final List<SqlResourceRolePrivileges> privilegeList) {
		boolean authorized = false;
		if (privilegeList != null) {
			for (final SqlResourceRolePrivileges privileges : privilegeList) {
				if (context.isUserInRole(privileges.getRoleName())
						&& privileges.hasPrivilege(requestType)) {
					authorized = true;
					break;
				}
			}
		}
		return authorized;
	}

	/** Loads authorization config from configured properties file. */
	private void loadPrivileges() {
		if (configFileName == null) {
			// File not configured, log warning
			authStatusMessage = "Authorization disabled -- No privileges defined. Use "
					+ Config.KEY_SECURITY_PRIVILEGES + " in restsql properties.";
			Config.logger.warn(authStatusMessage);

		} else {
			authEnabled = true;
			privilegeMap = new TreeMap<String, List<SqlResourceRolePrivileges>>();
			InputStream inputStream = null;
			try {
				final File file = new File(configFileName);
				if (!file.exists()) {
					// File not found, log error
					authStatusMessage = "Authorization enabled -- But privileges properties file "
							+ configFileName + " not found";
					Config.logger.error(authStatusMessage);
				} else {
					if (Config.logger.isDebugEnabled()) {
						Config.logger.debug("Loading privileges from " + configFileName);
					}

					// Load file into properties object
					final Properties properties = new Properties();
					inputStream = new FileInputStream(file);
					properties.load(inputStream);

					// Parse file contents
					// Format is [SqlResource,*].[requestType,*]=[*,role]
					for (final String name : properties.stringPropertyNames()) {
						// Parse name/value pair contents
						final String definition = name + "=" + properties.getProperty(name);
						final StringTokenizer nameTokenizer = new StringTokenizer(name, ".");
						if (nameTokenizer.hasMoreTokens()) {
							final String sqlResource = nameTokenizer.nextToken();
							if (nameTokenizer.hasMoreTokens()) {
								// Parse privileges string
								final String privilegesString = nameTokenizer.nextToken();
								final StringTokenizer privilegeTokenizer = new StringTokenizer(
										privilegesString, ",");
								final List<Privilege> privilegesList = new ArrayList<SqlResourceRolePrivileges.Privilege>(
										4);
								while (privilegeTokenizer.hasMoreTokens()) {
									final String token = privilegeTokenizer.nextToken();
									try {
										final Privilege privilege = Privilege.fromString(token);
										privilegesList.add(privilege);
									} catch (final IllegalArgumentException exception) {
										// privilege not found
										Config.logger
												.warn("Privilege definition '"
														+ definition
														+ "' contains invalid request type '"
														+ token
														+ "' -- privilege must be a request type (select, insert, update, delete) or wildcard (*)");
									}
								}

								if (privilegesList.size() > 0) {
									// Create collection of privileges
									final List<SqlResourceRolePrivileges> privObjectsList = new ArrayList<SqlResourceRolePrivileges>(
											5);

									// Parse value string
									final StringTokenizer roleTokenizer = new StringTokenizer(
											properties.getProperty(name), ",");
									while (roleTokenizer.hasMoreTokens()) {
										final String roleName = roleTokenizer.nextToken();
										if (roleName != null) {
											// Create priv object and add to object list
											final SqlResourceRolePrivileges privileges = new SqlResourceRolePrivileges(
													sqlResource, privilegesList, roleName);
											privObjectsList.add(privileges);
										}
									}

									if (privObjectsList.size() > 0) {
										// Initialize priv map if necessary, add object list to map
										if (privilegeMap.containsKey(sqlResource)) {
											privilegeMap.get(sqlResource).addAll(privObjectsList);
										} else {
											privilegeMap.put(sqlResource, privObjectsList);
										}
										authEnabled = true;
									}
								}

							} else {
								// definition is missing dot
								Config.logger.warn("Privilege definition '" + definition + "' ignored "
										+ " -- Use format [SqlResource,*].[requestType,*]=[*,role]");
							}
						}
						// else line is blank, move on
					}

					// Log results
					if (privilegeMap.size() == 0) {
						// No valid privs found
						authStatusMessage = "Authorization enabled -- But no privileges valid in "
								+ configFileName;
						Config.logger.error(authStatusMessage);
					} else {
						// We're good!
						authStatusMessage = "Authorization enabled -- Loaded privileges from "
								+ configFileName;
						if (Config.logger.isInfoEnabled()) {
							Config.logger.info(authStatusMessage);
						}
					}
				}
			} catch (final IOException exception) {
				// Error reading file or parsing contents, log error
				authStatusMessage = "Authorization enabled -- Failed to load privileges properties from "
						+ configFileName;
				Config.logger.error(authStatusMessage, exception);
			} finally {
				// Close stream
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch (final IOException exception) {
						// ignore
					}
				}
			}
		}
	}
}