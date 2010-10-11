package org.snapplr.spatial;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.ShapefileExporter;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialTopologyUtils;
import org.neo4j.gis.spatial.SpatialTopologyUtils.PointAndGeom;
import org.neo4j.gis.spatial.geotools.data.StyledImageExporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;

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
		EditableLayer layer = importSegments();
		EditableLayer stationLayer = importStations();

		ShapefileExporter exporter = new ShapefileExporter(database);
		exporter.setExportDir("target/export");
		StyledImageExporter imageExporter = new StyledImageExporter(database);
		imageExporter.setExportDir("target/export");
		imageExporter.setZoom(3.0);
		imageExporter.setSize(1024, 768);
		imageExporter.saveLayerImage(layer.getName(), "neo.sld.xml");

		exporter.exportLayer(layer.getName());
		exporter.exportLayer(stationLayer.getName());
		ArrayList<PointAndGeom> edges = new SpatialTopologyUtils()
				.findClosestEdges(
						layer.getGeometryFactory().createPoint(
								new Coordinate(60, 16)), layer, 0.1);
		System.out.println("test");
		Iterator<PointAndGeom> iterator = edges.iterator();
		while (iterator.hasNext()) {
			PointAndGeom next = iterator.next();
			System.out.println("found " + next.getKey() + next.getValue());

		}
	}

	private EditableLayer importSegments() throws FileNotFoundException,
			IOException {
		EditableLayer layer = (EditableLayer) db
				.getOrCreateEditableLayer("railway");
		assertNotNull(layer);
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
				//System.out.println(line);
				StringTokenizer tokens = new StringTokenizer(line, ",");
				int currentTrainId = Integer.parseInt(tokens.nextToken());
				if (!(currentTrainId == trainId)) {
					Coordinate[] coords = new Coordinate[coordinates.size()];
					coordinates.toArray(coords);
					//System.out.println(coords);
					if (coords.length > 1) {

						String[] names = { "name", "railway" };

						String[] values = { "train " + trainId, "true" };
						SpatialDatabaseRecord record = layer.add(layer
								.getGeometryFactory().createLineString(coords),
								names, values);
					}
					trainId = currentTrainId;
					System.out.println("inserted train" + trainId);
					coordinates.clear();
				} else {
					double lat = Double.parseDouble(tokens
							.nextToken());
					double lon = Double.parseDouble(tokens
							.nextToken());
					coordinates.add(new Coordinate(lat, lon, 0));

				}
			}
			in.close();
		} finally {
		}
		return layer;
	}

	private EditableLayer importStations() throws FileNotFoundException,
			IOException {
		EditableLayer layer = (EditableLayer) db
				.getOrCreateEditableLayer("stations");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			dom = db.parse("Transportverkets_koordinater.kml");

			NodeList placemarks = dom.getElementsByTagName("Placemark");
			// for(int i = 0; i < placemarks.getLength(); i++) {
			// Node pm = placemarks.item(i);
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
				//System.out.println(lon);
				String[] names = { "name" };

				String[] values = { name };
				SpatialDatabaseRecord record = layer.add(layer
						.getGeometryFactory().createPoint(
								new Coordinate(lat, lon)));

			}

			// }

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (Exception ioe) {
			ioe.printStackTrace();
		}
		// Coordinate[] coords = new Coordinate[coordinates.size()];
		// coordinates.toArray(coords);
		return layer;
	}
	
	
}
