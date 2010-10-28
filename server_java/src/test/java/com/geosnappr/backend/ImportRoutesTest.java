package com.geosnappr.backend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.rest.domain.DatabaseLocator;

import com.geosnappr.Main;
import com.geosnappr.TaginfoImporter;

public class ImportRoutesTest {

	@Before
	public void setUp() {
		System.setProperty(Main.GRAPHDB_LOCATION, "target/db");
	}

	@Test
	public void testImport() throws Exception {
		TaginfoImporter importer = new TaginfoImporter();
		importer.importRoutes();
		// search around Malmo
//		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd H:mm");
//		Map result = importer.getStations(13.0009, 55.6093, 1,
//				System.currentTimeMillis(), 30);
//		System.out.println(result);
		// String stations = importer.getStations(11.4709, 58.61, 1,
		// df.parse("20101028 8:38").getTime(), 30);
		// System.out.println(stations);
		// importer.exportToPng("stations");
		// StyledImageExporter imageExporter = new
		// StyledImageExporter(database);
		// imageExporter.setExportDir("target/export");
		// imageExporter.setOffset(-0.4, -0.4);
		// imageExporter.setZoom(2);
		//
		// imageExporter.setSize(1024, 768);
		// imageExporter.saveLayerImage("stations", "geosnappr.sld");
	}

	@After
	public void shutdown() {
		DatabaseLocator.getGraphDatabase().shutdown();
	}

}
