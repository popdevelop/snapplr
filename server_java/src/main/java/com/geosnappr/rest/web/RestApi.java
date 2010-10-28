package com.geosnappr.rest.web;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.map.ObjectMapper;
import org.mortbay.util.ajax.JSON;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.rest.domain.DatabaseBlockedException;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.rest.domain.JsonHelper;
import org.neo4j.rest.domain.JsonRenderers;
import org.neo4j.rest.domain.Renderer;
import org.neo4j.rest.domain.StorageActions;

import com.geosnappr.TaginfoImporter;

/* (non-javadoc)
 * I'd really like to split up the JSON and HTML parts in two different
 * classes, but Jersey can't handle that (2010-03-30).
 * 
 * Instead we end up with json/html prefixed methods... URGH!
 * But it's... ok, kind of, since it doesn't really matter what the
 * methods are called. They only ones who could get upset are the tests.
 */
@Path("/")
public class RestApi {
	private static final String PATH_NODE = "/trains/{lon}/{lat}/{radius}/{time}/{forward}";
	private GraphDatabaseService db;
	private URI baseUri;
	private StorageActions actions;

	public static final String UTF8 = "UTF-8";

	public RestApi(@Context UriInfo uriInfo) {
		System.out.println("webservice: " + uriInfo);
		db = DatabaseLocator.getGraphDatabase();

		this.baseUri = uriInfo.getBaseUri();
		try {
			this.actions = new StorageActions(baseUri);
		} catch (DatabaseBlockedException e) {
			throw new RuntimeException(
					"Unable to create GenericWebService, database access is blocked.",
					e);
		}
	}

	// ===== JSON =====

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(PATH_NODE)
	public Response jsonGetNode(@PathParam("lon") double lon,
			@PathParam("lat") double lat, @PathParam("radius") double radius,
			@PathParam("time") long time, @PathParam("forward") int forward) {
		JsonRenderers renderer = JsonRenderers.DEFAULT;
		Map stations = new TaginfoImporter().getStations(lon, lat, radius, time, forward);
		JsonFactory f = new JsonFactory();
		try {
			JsonGenerator g = f.createJsonGenerator(System.out, JsonEncoding.UTF8 );
			ObjectCodec om = new ObjectMapper();
			g.setCodec(om );
			g.writeObject(stations);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return addHeaders(
				Response.ok(JsonHelper.createJsonFrom(stations), renderer.getMediaType()))
				.build();
	}


	private static ResponseBuilder addHeaders(ResponseBuilder builder) {
		String entity = (String) builder.clone().build().getEntity();
		byte[] entityAsBytes;
		try {
			entityAsBytes = entity.getBytes(UTF8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Could not encode string as UTF-8", e);
		}
		builder = builder.entity(entityAsBytes);
		builder = builder.header(HttpHeaders.CONTENT_LENGTH,
				String.valueOf(entityAsBytes.length));
		builder = builder.header(HttpHeaders.CONTENT_ENCODING, UTF8);
		return builder;
	}
}
