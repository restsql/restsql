/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restsql.tools.ResourceDefinitionGenerator;
import org.restsql.tools.ToolsFactory;

/**
 * Provides utilities to users.
 * 
 * @author Mark Sawers
 */
@Path("tools")
public class ToolsResource {
	private final static String BASE_HTML = "<!DOCTYPE html>\n<html><body style=\"font-family:sans-serif\">"
			+ "<span style=\"font-weight:bold\"><a href=\"../..\">restSQL</a> Tools: Generate Resource Definitions</span><hr/>";

	@POST
	@Path("res/generate")
	@Produces(MediaType.TEXT_HTML)
	public Response generateResourceDefinitions(@FormParam("subdir") final String subdir, @FormParam("database") final String database) {
		try {
			int defs = ToolsFactory.getResourceDefinitionGenerator().generate(subdir, database, null);
			String doc = BASE_HTML + "<p>" + defs + " definitions generated</p><p/><p><a href=\"../../res/\">See Resources</a></p></body></html>";
			return Response.ok(doc).build();
		} catch (ResourceDefinitionGenerator.GenerationException exception) {
			return Response.serverError().entity(exception.toString()).build();
		}
	}
	
}
