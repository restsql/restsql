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

	public List<Privilege> getPrivileges() {
		return privileges;
	}

	public String getRoleName() {
		return roleName;
	}

	public String getSqlResource() {
		return sqlResource;
	}

	public boolean hasPrivilege(final Request.Type requestType) {
		boolean authorized = false;
		for (final Privilege privilege : privileges) {
			if (privilege.equals(requestType)) {
				authorized = true;
				break;
			}
		}
		return authorized;
	}

	@Override
	public String toString() {
		final StringBuffer string = new StringBuffer(100);
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
		DELETE, INSERT, SELECT, UPDATE, WILDCARD;

		public static Privilege fromString(final String name) {
			if (name == null) {
				throw new NullPointerException("Name is null");
			} else if (name.equalsIgnoreCase(DELETE.toString())) {
				return DELETE;
			} else if (name.equalsIgnoreCase(SELECT.toString())) {
				return SELECT;
			} else if (name.equalsIgnoreCase(INSERT.toString())) {
				return INSERT;
			} else if (name.equalsIgnoreCase(UPDATE.toString())) {
				return UPDATE;
			} else if (name.equals(TOKEN_WILDCARD)) {
				return WILDCARD;
			} else {
				throw new IllegalArgumentException("No enum const Privilege." + name);
			}
		}

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
	}
}
