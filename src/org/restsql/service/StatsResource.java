/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/stats")
public class StatsResource {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getStatus() {
		return Response.ok("Work in progress").build();
	}
}
