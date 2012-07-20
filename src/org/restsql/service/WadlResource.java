/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Redirects wadl request to the wadl file.
 * 
 * @author Mark Sawers
 */
@Path("wadl")
public class WadlResource {

	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response getWadl() {
		try {
			return Response.seeOther(new URI("wadl/restsql-wadl.xml")).build();
		} catch (URISyntaxException shouldNeverHappen) {
			return Response.serverError().build();
		}
	}
}
