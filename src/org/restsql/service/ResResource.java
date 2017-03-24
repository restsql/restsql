/* Copyright (c) restSQL Project Contributors. Licensed under MIT. */
package org.restsql.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.restsql.core.Config;
import org.restsql.core.Factory;
import org.restsql.core.HttpRequestAttributes;
import org.restsql.core.Request;
import org.restsql.core.Request.Type;
import org.restsql.core.RequestLogger;
import org.restsql.core.RequestUtil;
import org.restsql.core.RequestValue;
import org.restsql.core.SqlResource;
import org.restsql.core.SqlResourceException;
import org.restsql.core.WriteResponse;
import org.restsql.security.SecurityFactory;
import org.restsql.service.monitoring.MonitoringFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;

/**
 * Contains core JAX-RS Resource of the service, processing SQL Resource CRUD requests. Also lists available resources.
 * 
 * @author Mark Sawers
 */
@Path("res")
public class ResResource {
	private final Timer allRequestTypesTimer = MonitoringFactory.getMonitoringManager().newTimer(ResResource.class, "allRequestTypes");
	private final Counter confRequestCounter = MonitoringFactory.getMonitoringManager().newCounter(ResResource.class, "conf");
	
	@DELETE
	@Path("{resName}/{resId1}")
	@Timed
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String requestBody,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1 }, null,
				requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}")
	@Timed
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1, resId2 },
				null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Timed
	public Response delete(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String requestBody,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.DELETE, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@DELETE
	@Path("{resName}")
	@Timed
	public Response delete(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.DELETE, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, contentMediaType,
				acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}")
	@Timed
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@Context final UriInfo uriInfo, @HeaderParam("Accept") String acceptMediaType,
			@Context final HttpServletRequest httpRequest, @Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1 },
				getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}")
	@Timed
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @Context final UriInfo uriInfo,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1, resId2 },
				getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType, securityContext);
	}

	@GET
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Timed
	public Response get(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			@Context final UriInfo uriInfo, @HeaderParam("Accept") String acceptMediaType,
			@Context final HttpServletRequest httpRequest, @Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.SELECT, resName, new String[] { resId1, resId2,
				resId3 }, getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType,
				securityContext);
	}

	@GET
	@Path("{resName}")
	@Timed
	public Response get(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Request.Type.SELECT, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), null, null, acceptMediaType, securityContext);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response getResources(@Context final UriInfo uriInfo) {
		confRequestCounter.inc();
		final StringBuffer requestBody = HttpRequestHelper.buildSqlResourceListing();
		return Response.ok(requestBody.toString()).build();
	}

	@POST
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Timed
	public Response post(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequest(httpRequest, Type.INSERT, resName, null, null, getNameValuePairs(formParams),
				requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}")
	@Timed
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, final String requestBody,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1 }, null,
				requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}")
	@Timed
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1, resId2 },
				null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Timed
	public Response post(@PathParam("resName") final String resName,
			@PathParam("resId1") final String resId1, @PathParam("resId2") final String resId2,
			@PathParam("resId3") final String resId3, final String requestBody,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.INSERT, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@POST
	@Path("{resName}")
	@Timed
	public Response post(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.INSERT, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, contentMediaType,
				acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Timed
	public Response put(@PathParam("resName") final String resName,
			final MultivaluedMap<String, String> formParams, @Context final UriInfo uriInfo,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequest(httpRequest, Type.UPDATE, resName, null,
				getNameValuePairs(uriInfo.getQueryParameters()), getNameValuePairs(formParams), requestBody,
				contentMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Timed
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1 },
				getNameValuePairs(formParams), requestBody, contentMediaType, acceptMediaType,
				securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}")
	@Timed
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1 }, null,
				requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Timed
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final MultivaluedMap<String, String> formParams,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		final String requestBody = HttpRequestHelper.getRequestBodyFromFormParams(formParams);
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2 },
				getNameValuePairs(formParams), requestBody, contentMediaType, acceptMediaType,
				securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}")
	@Timed
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, final String requestBody,
			@HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2 },
				null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}/{resId1}/{resId2}/{resId3}")
	@Timed
	public Response put(@PathParam("resName") final String resName, @PathParam("resId1") final String resId1,
			@PathParam("resId2") final String resId2, @PathParam("resId3") final String resId3,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequestParseResIds(httpRequest, Type.UPDATE, resName, new String[] { resId1, resId2,
				resId3 }, null, requestBody, contentMediaType, acceptMediaType, securityContext);
	}

	@PUT
	@Path("{resName}")
	@Timed
	public Response put(@PathParam("resName") final String resName, @Context final UriInfo uriInfo,
			final String requestBody, @HeaderParam("Content-Type") String contentMediaType,
			@HeaderParam("Accept") String acceptMediaType, @Context final HttpServletRequest httpRequest,
			@Context final SecurityContext securityContext) {
		return executeRequest(httpRequest, Type.UPDATE, resName, null, null,
				getNameValuePairs(uriInfo.getQueryParameters()), requestBody, contentMediaType,
				acceptMediaType, securityContext);
	}

	// Private utils

	/** Processes the request. The central method of this resource class. */
	private Response executeRequest(HttpServletRequest httpRequest, final Request.Type requestType,
			final String resName, SqlResource sqlResource, final List<RequestValue> resIds,
			final List<RequestValue> params, final String requestBody, String contentMediaType,
			String acceptMediaType, SecurityContext securityContext) {
		Timer.Context requestTimerContext = allRequestTypesTimer.time();
		
		// Determine the media types and create http attributes structure
		String requestMediaType = RequestUtil.getRequestMediaType(contentMediaType);
		String responseMediaType = RequestUtil
				.getResponseMediaType(params, requestMediaType, acceptMediaType);
		final HttpRequestAttributes httpAttributes = HttpRequestHelper.getHttpRequestAttributes(httpRequest,
				requestBody, requestMediaType, responseMediaType);

		// Create logger
		final RequestLogger requestLogger = Factory.getRequestLogger();
		requestLogger.setHttpRequestAttributes(httpAttributes);

		// Authorize
		if (!SecurityFactory.getAuthorizer().isAuthorized(new SecurityContextAdapter(securityContext),
				requestType, resName)) {
			Status status = Status.FORBIDDEN;
			requestLogger.log(status.getStatusCode());
			return Response.status(status).build();
		}

		try {
			String responseBody = null;
			if (sqlResource == null) {
				sqlResource = Factory.getSqlResource(resName);
			}

			// Execute request
			if (requestType.equals(Request.Type.SELECT)) {
				final Request request = Factory.getRequest(httpAttributes, requestType, resName, resIds,
						params, null, requestLogger);
				responseBody = sqlResource.read(request, responseMediaType);
			} else { // INSERT, UPDATE or DELETE
				final WriteResponse writeResponse;
				if (requestMediaType != null
						&& !requestMediaType.equals(MediaType.APPLICATION_FORM_URLENCODED)
						&& requestBody != null && requestBody.length() > 0) {
					// requestBody is not null, use request processor
					writeResponse = Factory.getRequestDeserializer(requestMediaType).execWrite(httpAttributes,
							requestType, resIds, sqlResource, requestBody, requestLogger);
				} else {
					final Request request = Factory.getRequest(httpAttributes, requestType, resName, resIds,
							params, null, requestLogger);
					writeResponse = sqlResource.write(request);
				}
				responseBody = Factory.getResponseSerializer(responseMediaType).serializeWrite(sqlResource, writeResponse);
			}

			// Log response and send it
			requestLogger.log(responseBody);

			// Get cache control

			String cacheControl = Config.properties.getProperty(Config.KEY_HTTP_CACHE_CONTROL,
					Config.DEFAULT_HTTP_CACHE_CONTROL);
			if (sqlResource.getDefinition().getHttp() != null
					&& sqlResource.getDefinition().getHttp().getResponse() != null) {
				cacheControl = sqlResource.getDefinition().getHttp().getResponse().getCacheControl();
			}

			// Send the response
			return Response.ok(responseBody).type(responseMediaType).header("Cache-Control", cacheControl)
					.build();

		} catch (final SqlResourceException exception) {
			return HttpRequestHelper.handleException(httpRequest, requestBody, requestMediaType, exception,
					requestLogger);
		} finally {
			requestTimerContext.stop();
		}
	}

	/**
	 * Pre-processes request, parsing resource ids into a collection, and then passing the request on to
	 * executeRequest().
	 */
	private Response executeRequestParseResIds(HttpServletRequest httpRequest,
			final Request.Type requestType, final String resName, final String[] resIdValues,
			final List<RequestValue> params, final String requestBody, String contentMediaType,
			String acceptMediaType, SecurityContext securityContext) {
		final SqlResource sqlResource;
		final List<RequestValue> resIds;
		try {
			sqlResource = Factory.getSqlResource(resName);
			resIds = RequestUtil.getResIds(sqlResource, resIdValues);
		} catch (final SqlResourceException exception) {
			return HttpRequestHelper.handleException(httpRequest, requestBody, contentMediaType, exception,
					null);
		}
		return executeRequest(httpRequest, requestType, resName, sqlResource, resIds, params, requestBody,
				contentMediaType, acceptMediaType, securityContext);
	}

	/** Converts form or query params into a list of NameValuePairs. */
	private List<RequestValue> getNameValuePairs(final MultivaluedMap<String, String> formOrQueryParams) {
		final List<RequestValue> params = new ArrayList<RequestValue>(formOrQueryParams.size());
		for (final String key : formOrQueryParams.keySet()) {
			for (final String value : formOrQueryParams.get(key)) {
				final RequestValue param = new RequestValue(key, value);
				params.add(param);
			}
		}
		return params;
	}
}
