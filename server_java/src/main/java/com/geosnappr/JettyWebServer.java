package com.geosnappr;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.thread.QueuedThreadPool;
import org.neo4j.rest.WebServer;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.rest.web.AllowAjaxFilter;

import java.util.Map;

public enum JettyWebServer implements WebServer
{
    INSTANCE;

    private Server server;
    private int port = WebServerFactory.DEFAULT_PORT;
    private static final String REST_MAX_JETTY_THREADS = "rest_max_jetty_threads";

    public void startServer()
    {
        startServer( WebServerFactory.DEFAULT_PORT );
    }

    public void startServer( int port )
    {

        Map<Object, Object> configParams = DatabaseLocator.getConfiguration().getParams();

        this.port = port;
        server = new Server( port );
        ServletHolder servletHolder = new ServletHolder( ServletContainer.class );
        servletHolder.setInitParameter(
                "com.sun.jersey.config.property.packages", "com.geosnappr.rest.web" );
        servletHolder.setInitParameter(
                ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
                AllowAjaxFilter.class.getName() );
        Context context = new Context( server, "/" );
        context.addServlet( servletHolder, "/*" );

        if ( configParams.containsKey( REST_MAX_JETTY_THREADS ) )
        {
            Integer threadPoolSize = Integer.valueOf( (String) configParams.get( REST_MAX_JETTY_THREADS ) );
            server.setThreadPool( new QueuedThreadPool( threadPoolSize ) );
        }

        try
        {
            server.start();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

    }

    public int getPort()
    {
        return port;
    }

    public String getBaseUri()
    {
        return WebServerFactory.getLocalhostBaseUri( port );
    }

    public void stopServer()
    {
        try
        {
            server.stop();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
