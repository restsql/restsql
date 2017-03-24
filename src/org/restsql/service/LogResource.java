/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.restsql.core.Config;
import org.restsql.service.monitoring.MonitoringFactory;

import com.codahale.metrics.Counter;

/**
 * Provides access to logs.
 * 
 * @author Mark Sawers
 */
@Path("log")
public class LogResource {
	private static final String LOG_NAME_ACCESS = "access.log";
	private static final String LOG_NAME_ERROR = "error.log";
	private static final String LOG_NAME_INTERNAL = "internal.log";
	private static final String LOG_NAME_TRACE = "trace.log";
	
	private final Counter logAccessCounter = MonitoringFactory.getMonitoringManager().newCounter(LogResource.class, "logAccess");
	private final Counter logListCounter = MonitoringFactory.getMonitoringManager().newCounter(LogResource.class, "logList");

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getLogList() {
		logListCounter.inc();
		final StringBuilder body = new StringBuilder(500);
		body.append("<!DOCTYPE html>\n<html><head><link rel=\"icon\" type=\"image/png\" href=\"../assets/favicon.ico\"/></head>\n<body style=\"font-family:sans-serif\">\n");
		body.append("<span style=\"font-weight:bold\"><a href=\"..\">restSQL</a> Logs</span><hr/>");
		body.append("<span style=\"font-weight:bold\">Current Logs</span><br/>");
		appendCurrentLogAnchor(body, "access");
		appendCurrentLogAnchor(body, "error");
		appendCurrentLogAnchor(body, "trace");
		appendCurrentLogAnchor(body, "internal");
		body.append("<p/><p/><span style=\"font-weight:bold\">Historical Logs</span><br/>");
		final File dir = new File(getLogDir());
		for (final File file : dir.listFiles()) {
			if (file.getName().contains(".log")) {
				if (!file.getName().equals(LOG_NAME_ACCESS) && !file.getName().equals(LOG_NAME_ERROR)
						&& !file.getName().equals(LOG_NAME_TRACE)
						&& !file.getName().equals(LOG_NAME_INTERNAL)) {
					body.append("<a href=\"");
					body.append(file.getName());
					body.append("\">");
					body.append(file.getName());
					body.append("</a><br/>");
				}
			}
		}
		body.append("</body>\n</html>");
		return Response.ok(body.toString()).build();
	}
	
	@GET
	@Path("access")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCurrentAccessLog() {
		logAccessCounter.inc();
		return getFileContents(LOG_NAME_ACCESS);
	}

	@GET
	@Path("error")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCurrentErrorLog() {
		logAccessCounter.inc();
		return getFileContents(LOG_NAME_ERROR);
	}

	@GET
	@Path("internal")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCurrentInternalLog() {
		logAccessCounter.inc();
		return getFileContents(LOG_NAME_INTERNAL);
	}

	@GET
	@Path("trace")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getCurrentTraceLog() {
		logAccessCounter.inc();
		return getFileContents(LOG_NAME_TRACE);
	}

	@GET
	@Path("{fileName}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLog(@PathParam("fileName") final String fileName) {
		logAccessCounter.inc();
		return getFileContents(fileName);
	}

	// Private utils

	private void appendCurrentLogAnchor(StringBuilder body, String log) {
		body.append("<a href=\"");
		body.append(log);
		body.append("\">");
		body.append(log);
		body.append("</a><br/>");
	}

	private Response getFileContents(final String fileName) {
		try {
			final FileInputStream inputStream = new FileInputStream(getLogDir() + "/" + fileName);
			return Response.ok(inputStream).build();
		} catch (final FileNotFoundException exception) {
			return Response.status(Status.NOT_FOUND).entity(fileName + " not found").build();
		}
	}

	private String getLogDir() {
		return Config.properties.getProperty(Config.KEY_LOGGING_DIR, Config.DEFAULT_LOGGING_DIR);
	}
}
