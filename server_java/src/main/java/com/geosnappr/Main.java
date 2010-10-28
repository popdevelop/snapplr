package com.geosnappr;

import org.neo4j.helpers.Args;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;

public class Main {
	public static final String GRAPHDB_LOCATION = "org.neo4j.graphdb.location";

	public static void main(String[] argsArray) throws Exception {
		Args args = new Args(argsArray);
		System.setProperty(GRAPHDB_LOCATION, args.get("path", "target/db"));
		int port = args.getNumber("port", WebServerFactory.DEFAULT_PORT)
				.intValue();
		final String baseUri = WebServerFactory.getLocalhostBaseUri(port);
		System.out.println(String.format("Using database at [%s]",
				System.getProperty(GRAPHDB_LOCATION)));
		System.out.println(String.format("Running server at [%s]", baseUri));
		JettyWebServer.INSTANCE.startServer(port);
		System.out.println("Press Ctrl-C to kill the server");

		// TODO We couldn't have a System.in.read() here since Java Service
		// Wrapper
		// couldn't keep the service running if we had :)
		// System.in.read();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Shutting down the server");
				WebServerFactory.getDefaultWebServer().stopServer();
				DatabaseLocator.shutdownAndBlockGraphDatabase();
			}
		});
	}
}
