package com.geosnappr.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MediaType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.rest.domain.JsonHelper;

import com.geosnappr.JettyWebServer;
import com.geosnappr.Main;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;


public class FunctionalTest {
	private static String NODE_URI_PATTERN;
	
	 @Test
	    public void shouldGetTrainsaroundMalmo() throws Exception
	    {
	        String url = "trains/13.0009/55.6093/1/" +
				System.currentTimeMillis() + "/30";
	        System.out.println(url);
			ClientResponse response = sendGetRequestToServer(url);
	        assertEquals( 200, response.getStatus() );
	        assertEquals( MediaType.APPLICATION_JSON_TYPE, response.getType() );
	        System.out.println( JsonHelper.jsonToMap( response.getEntity( String.class ) ) );
	    }
	
	
	 private ClientResponse sendGetRequestToServer( String url )
	    {
	        Client client = Client.create();
	        WebResource resource = client.resource( WebServerFactory.getDefaultWebServer().getBaseUri()
	                                                + url );
	        System.out.println(resource.getURI());
	        ClientResponse response = resource.type( MediaType.APPLICATION_JSON ).accept(
	                MediaType.APPLICATION_JSON ).get( ClientResponse.class );
	        return response;
	    }
	 
	 
	@BeforeClass
    public static void startWebServer()
    {
		System.setProperty(Main.GRAPHDB_LOCATION, "target/db");
        JettyWebServer.INSTANCE.startServer();
        NODE_URI_PATTERN = WebServerFactory.getDefaultWebServer().getBaseUri()
                           + "node/[0-9]+";
    }

    @AfterClass
    public static void stopWebServer() throws Exception
    {
        JettyWebServer.INSTANCE.stopServer();
        DatabaseLocator.shutdownGraphDatabase();
    }
}
