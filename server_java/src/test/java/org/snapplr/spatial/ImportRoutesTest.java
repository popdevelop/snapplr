package org.snapplr.spatial;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.gis.spatial.DefaultLayer;
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.EditableLayerImpl;
import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.ShapefileExporter;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialTopologyUtils;
import org.neo4j.gis.spatial.SpatialTopologyUtils.PointResult;
import org.neo4j.gis.spatial.geotools.data.StyledImageExporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class ImportRoutesTest {

	private String directoryName = "target/db";
	private SpatialDatabaseService db;
	private GraphDatabaseService database;

	@Before
	public void setUp() {
		try {
			FileUtils.deleteDirectory(new File(directoryName));
			database = new EmbeddedGraphDatabase(directoryName);
			db = new SpatialDatabaseService(database);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	@Test
	public void importRoutes() throws Exception {
		database.beginTx();
		EditableLayer rails = (EditableLayer) db
				.getOrCreateEditableLayer("railway");
		EditableLayer stations = (EditableLayer) db
				.getOrCreateEditableLayer("stations");
		String[] names = new String[] { "name", "railway" };
		((DefaultLayer) stations).setExtraPropertyNames(names);
		((DefaultLayer) rails).setExtraPropertyNames(names);

		importSegments(rails);
		importStations(stations);
		snap(rails);
		ShapefileExporter exporter = new ShapefileExporter(database);
		exporter.setExportDir("target/export");
		exporter.exportLayer(rails.getName());
		exporter.exportLayer(stations.getName());
		StyledImageExporter imageExporter = new StyledImageExporter(database);
		imageExporter.setExportDir("target/export");
		imageExporter.setZoom(1.0);
		imageExporter.setSize(1024, 768);
		imageExporter.saveLayerImage(rails.getName(), "geosnappr.sld.xml");

		// ArrayList<PointAndGeom> edges = new SpatialTopologyUtils()
		// .findClosestEdges(
		// layer.getGeometryFactory().createPoint(
		// new Coordinate(60, 16)), layer, 0.1);
		// System.out.println("test");
		// Iterator<PointAndGeom> iterator = edges.iterator();
		// while (iterator.hasNext()) {
		// PointAndGeom next = iterator.next();
		// System.out.println("found " + next.getValue()
		// + next.getValue().getClass());
		//
		// }
	}

	private void snap(EditableLayer layer2) {
		// Now test snapping to a layer
		GeometryFactory factory = layer2.getGeometryFactory();
		EditableLayerImpl results = (EditableLayerImpl) db
				.getOrCreateEditableLayer("testSnapping_results");
		String[] fieldsNames = new String[] { "snap-id", "description",
				"distance" };
		results.setExtraPropertyNames(fieldsNames);
		Coordinate coordinate_malmo_c = new Coordinate(13.0029899,55.6095057);
		Point malmo_c = factory.createPoint(coordinate_malmo_c);
		results.add(malmo_c, fieldsNames,
				new Object[] { 0L, "Point to snap", 0L });
		for (String layerName : new String[] { "railway" }) {
			Layer layer = db.getLayer(layerName);
			assertNotNull("Missing layer: " + layerName, layer);
			System.out.println("Closest features in " + layerName
					+ " to point " + malmo_c + ":");
			//Coordinate[] dummy = new Coordinate[]{coordinate_malmo_c, coordinate_malmo_c};
			for (PointResult result : SpatialTopologyUtils.findClosestEdges(
					malmo_c, layer)) {
				System.out.println("\t" + result);
				results.add(result.getKey(), fieldsNames, new Object[] {
						result.getValue().getGeomNode().getId(),
						"Snapped point to layer " + layerName + ": "
								+ result.getValue().getGeometry().toString(),
						(long) (1000000 * result.getDistance()) });
			}
		}
		ShapefileExporter shpExporter = new ShapefileExporter(database);
		shpExporter.setExportDir("target/export");
		try {
			shpExporter.exportLayer(results.getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void importSegments(EditableLayer layer)
			throws FileNotFoundException, IOException {
		database.beginTx();
		try {
			InputStreamReader in = new InputStreamReader(new FileInputStream(
					"rail-points.csv"));
			BufferedReader buf = new BufferedReader(in);
			String line;
			int trainId = 0;
			// skip first line
			buf.readLine();

			ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
			while ((line = buf.readLine()) != null) {
				// System.out.println(line);
				StringTokenizer tokens = new StringTokenizer(line, ",");
				int currentTrainId = Integer.parseInt(tokens.nextToken());
				if (!(currentTrainId == trainId)) {
					Coordinate[] coords = new Coordinate[coordinates.size()];
					coordinates.toArray(coords);
					// System.out.println(coords);
					if (coords.length > 1) {

						String[] keys = { "name", "railway" };

						String[] values = { "train " + trainId, "true" };
						SpatialDatabaseRecord record = layer.add(layer
								.getGeometryFactory().createLineString(coords),
								keys, values);
					}
					trainId = currentTrainId;
					System.out.println("inserted train" + trainId);
					coordinates.clear();
				} else {
					double lat = Double.parseDouble(tokens.nextToken());
					double lon = Double.parseDouble(tokens.nextToken());
					coordinates.add(new Coordinate(lon, lat, 0));

				}
			}
			in.close();
		} finally {
		}
	}

	private void importStations(EditableLayer layer)
			throws FileNotFoundException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			dom = db.parse("Transportverkets_koordinater.kml");

			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xpath.evaluate("//Placemark", dom,
					XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node station = nodes.item(i);

				String name = (String) xpath.evaluate("name", station,
						XPathConstants.STRING);
				StringTokenizer coord = new StringTokenizer(
						(String) xpath.evaluate("Point/coordinates", station,
								XPathConstants.STRING), ",");
				Double lon = Double.parseDouble(coord.nextToken());
				Double lat = Double.parseDouble(coord.nextToken());
				String[] keys = new String[] { "name" };
				Object[] values = new String[] { name };
				layer.add(
						layer.getGeometryFactory().createPoint(
								new Coordinate(lon, lat)), keys, values);
			}

			// }

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
	}

}
