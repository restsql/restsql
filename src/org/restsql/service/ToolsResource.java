/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.restsql.service.monitoring.MonitoringFactory;
import org.restsql.tools.ResourceDefinitionGenerator;
import org.restsql.tools.ToolsFactory;

import com.codahale.metrics.Counter;

/**
 * Provides utilities to users.
 * 
 * @author Mark Sawers
 */
@Path("tools")
public class ToolsResource {
	private final static String BASE_HTML = "<!DOCTYPE html>\n<html><head><link rel=\"icon\" type=\"image/png\" href=\"../assets/favicon.ico\"/></head><body style=\"font-family:sans-serif\">"
			+ "<span style=\"font-weight:bold\"><a href=\"../..\">restSQL</a> Tools: Generate Resource Definitions</span><hr/>";
	private final Counter requestCounter = MonitoringFactory.getMonitoringManager().newCounter(ToolsResource.class, "tools");

	@POST
	@Path("res/generate")
	@Produces(MediaType.TEXT_HTML)
	public Response generateResourceDefinitions(@FormParam("subdir") final String subdir, @FormParam("database") final String database) {
		requestCounter.inc();
		try {
			int defs = ToolsFactory.getResourceDefinitionGenerator().generate(subdir, database, null);
			String doc = BASE_HTML + "<p>" + defs + " definitions generated</p><p/><p><a href=\"../../res/\">See Resources</a></p></body></html>";
			return Response.ok(doc).build();
		} catch (ResourceDefinitionGenerator.GenerationException exception) {
			return Response.serverError().entity(exception.toString()).build();
		}
	}
	
}
