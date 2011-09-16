/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.security;

import java.util.List;

import org.restsql.core.Request;

/**
 * Models actions available to a role on SQL Resource. There may be multiple instances of SqlResourceRolePrivileges for
 * a particular SQL Resource.
 * 
 * @author Mark Sawers
 */
public class SqlResourceRolePrivileges {
	public static final String TOKEN_WILDCARD = "*";

	private final List<Privilege> privileges;
	private final String roleName;
	private final String sqlResource;

	public SqlResourceRolePrivileges(final String sqlResource, final List<Privilege> privileges,
			final String roleName) {
		this.sqlResource = sqlResource;
		this.privileges = privileges;
		this.roleName = roleName;
	}

	public boolean hasPrivilege(Request.Type requestType) {
		boolean authorized = false;
		for (Privilege privilege : privileges) {
			if (privilege.equals(requestType)) {
				authorized = true;
				break;
			}
		}
		return authorized;
	}

	public List<Privilege> getPrivileges() {
		return privileges;
	}

	public String getRoleName() {
		return roleName;
	}

	public String getSqlResource() {
		return sqlResource;
	}

	public String toString() {
		StringBuffer string = new StringBuffer(100);
		string.append(sqlResource);
		string.append(".");
		for (int i = 0; i < privileges.size(); i++) {
			if (i > 0) {
				string.append(",");
			}
			string.append(privileges.get(i).toString());
		}
		string.append("=");
		string.append(roleName);
		return string.toString();
	}

	public enum Privilege {
		SELECT, INSERT, UPDATE, DELETE, WILDCARD;

		public boolean equals(final Request.Type requestType) {
			if (this == WILDCARD) {
				return true;
			} else {
				return this.ordinal() == requestType.ordinal();
			}
		}
		
		@Override
		public String toString() {
			if (this == WILDCARD) {
				return TOKEN_WILDCARD;
			} else {
				return super.toString();
			}
		}

		public static Privilege fromString(String name) {
			if (name == null) {
				throw new NullPointerException("Name is null");
			}
			if (name.equals(TOKEN_WILDCARD)) {
				return WILDCARD;
			} else if (name.equalsIgnoreCase(SELECT.toString())) {
				return SELECT;
			} else if (name.equalsIgnoreCase(INSERT.toString())) {
				return INSERT;
			} else if (name.equalsIgnoreCase(UPDATE.toString())) {
				return UPDATE;
			} else if (name.equalsIgnoreCase(DELETE.toString())) {
				return DELETE;
			} else {
		        throw new IllegalArgumentException(
			            "No enum const Privilege." + name);
			}
		}
	}
}
