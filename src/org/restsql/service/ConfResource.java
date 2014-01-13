/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.Factory.SqlResourceFactoryException;
import org.restsql.core.SqlResourceException;
import org.restsql.security.SecurityFactory;

/**
 * Provides access to framework, resource, logging and security configuration.
 * 
 * @author Mark Sawers
 */
@Path("conf")
public class ConfResource {
	private final static String MENU_HTML = "<html><body style=\"font-family:sans-serif\">"
		+ "<span style=\"font-weight:bold\">restSQL Configuration</span><hr/>"
		+ "<a href=\"system\">System</a><br/>" + "<a href=\"log\">Logging</a><br/>"
		+ "<a href=\"security\">Security</a><br/>" + "<a href=\"res\">Resources</a><br/>"
		+ "</body></html>";

	@GET
	@Path("system")
	@Produces(MediaType.TEXT_PLAIN)
	public Response dumpConfig() {
		return Response.ok(Config.dumpConfig(true)).build();
	}

	@GET
	@Path("log")
	@Produces(MediaType.TEXT_PLAIN)
	public Response dumpLoggingConfig() {
		return Response.ok(Config.dumpLoggingConfig()).build();
	}

	@GET
	@Path("security")
	@Produces(MediaType.TEXT_PLAIN)
	public Response dumpSecurityConfig() {
		return Response.ok(SecurityFactory.getAuthorizer().dumpConfig()).build();
	}

	@GET
	@Path("definition/{resName}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getDefinition(@PathParam("resName") final String resName,
			@Context final HttpServletRequest httpRequest) {
		try {
			return Response.ok(Factory.getSqlResourceDefinition(resName))
					.type(MediaType.APPLICATION_XML_TYPE).build();
		} catch (final SqlResourceFactoryException exception) {
			return HttpRequestHelper.handleException(httpRequest, null, null, exception, null);
		}
	}

	@GET
	@Path("metadata/{resName}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getMetadata(@PathParam("resName") final String resName,
			@Context final HttpServletRequest httpRequest) {
		try {
			return Response.ok(Factory.getSqlResource(resName).getMetaData().toXml())
					.type(MediaType.APPLICATION_XML_TYPE).build();
		} catch (final SqlResourceException exception) {
			return HttpRequestHelper.handleException(httpRequest, null, null, exception, null);
		}
	}

	@GET
	@Path("res")
	@Produces(MediaType.TEXT_HTML)
	public Response getResources(@Context final UriInfo uriInfo) {
		final StringBuffer requestBody = HttpRequestHelper.buildSqlResourceListing(uriInfo);
		return Response.ok(requestBody.toString()).build();
	}

	@GET
	@Path("reload/{resName}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response reloadDefinition(@PathParam("resName") final String resName,
			@Context final HttpServletRequest httpRequest) {
		try {
			Factory.reloadSqlResource(resName);
			return Response.ok("Reload of " + resName + " succeeded").build();
		} catch (final SqlResourceException exception) {
			return HttpRequestHelper.handleException(httpRequest, null, null, exception, null);
		}
	}

	@GET
	@Path("")
	@Produces(MediaType.TEXT_HTML)
	public Response getMenu() {
		return Response.ok(MENU_HTML).build();
	}
}