package org.snapplr.spatial;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.gis.spatial.DefaultLayer;
import org.neo4j.gis.spatial.DynamicLayer;
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.EditableLayerImpl;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.ShapefileExporter;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialTopologyUtils;
import org.neo4j.gis.spatial.SpatialTopologyUtils.PointResult;
import org.neo4j.gis.spatial.encoders.SimpleGraphEncoder;
import org.neo4j.gis.spatial.geotools.data.StyledImageExporter;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.util.GraphDatabaseUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.geosnappr.TaginfoImporter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class ImportRoutesTest {

	private String directoryName = "target/db";
	private EmbeddedGraphDatabase database;
	private LuceneIndexService index;


	@Before
	public void setUp() {
		try {
			FileUtils.deleteDirectory(new File(directoryName));
			database = new EmbeddedGraphDatabase(directoryName);
			index = new LuceneIndexService(database);
			// db = new SpatialDatabaseService(database);
			// tx = database.beginTx();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}
	
	@Test
	public void testImport() throws Exception {
		TaginfoImporter importer = new TaginfoImporter(database, index);
		importer.importRoutes();
	}

	@After
	public void shutdown() {
		// tx.success();
		// tx.finish();
	}

	// @Ignore
//	@Test
//	
//
//	private void snap(EditableLayer layer2, EditableLayer results,
//			String[] fieldsNames) {
//		// Now test snapping to a layer
//		GeometryFactory factory = layer2.getGeometryFactory();
//
//		Coordinate coordinate_malmo_c = new Coordinate(13.0029899, 55.6095057);
//		Point malmo_c = factory.createPoint(coordinate_malmo_c);
//		results.add(malmo_c, fieldsNames, new Object[] { 0L, "Point to snap",
//				0L, "" });
//		String layerName = layer2.getName();
//		Layer layer = db.getLayer(layerName);
//		assertNotNull("Missing layer: " + layerName, layer);
//		System.out.println("Closest features in " + layerName + " to point "
//				+ malmo_c + ":");
//		// Coordinate[] dummy = new Coordinate[]{coordinate_malmo_c,
//		// coordinate_malmo_c};
//		for (PointResult result : SpatialTopologyUtils.findClosestEdges(
//				malmo_c, layer)) {
//			System.out.println("\t" + result);
//			long dist = (long) (1000000 * result.getDistance());
//			results.add(result.getKey(), fieldsNames, new Object[] {
//					"Snap, " + "dist=" + dist + ", Geo="
//							+ result.getValue().getGeomNode().getId(),
//					"Snapped point to layer " + layerName + ": "
//							+ result.getValue().getGeometry().toString(), dist,
//					"" });
//		}
//		ShapefileExporter shpExporter = new ShapefileExporter(database);
//		shpExporter.setExportDir("target/export");
//		try {
//			shpExporter.exportLayer(results.getName());
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	//}


}
