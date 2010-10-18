package com.geosnappr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.neo4j.gis.spatial.Layer;
import org.neo4j.gis.spatial.Search;
import org.neo4j.gis.spatial.ShapefileExporter;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialIndexReader;
import org.neo4j.gis.spatial.query.SearchPointsWithinOrthodromicDistance;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.util.GraphDatabaseUtil;
import org.snapplr.spatial.RelTypes;
import org.snapplr.spatial.StationsEncoder;
import org.snapplr.spatial.StationsLayer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class TaginfoImporter {
	private static final String STATIONS2 = "stations";
	private SpatialDatabaseService db;
	private GraphDatabaseService database;
	private Transaction tx;
	private LuceneIndexService index;
	SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd H:mm");

	public TaginfoImporter(EmbeddedGraphDatabase database2,
			LuceneIndexService index2) {
		database = database2;
		// TODO Auto-generated constructor stub
		index = index2;

		db = new SpatialDatabaseService(database);
	}

	public void importRoutes() throws Exception {
		// DynamicLayer rails = (DynamicLayer) db
		// .createLayer("railway",SimpleGraphEncoder.class, DynamicLayer.class
		// );
		// DynamicLayer stations = (DynamicLayer) db
		// .createLayer("stations", StationsEncoder.class, DynamicLayer.class);
		// String[] names = new String[] { "name", "railway", "description" };
		// ((DefaultLayer) stations).setExtraPropertyNames(names);
		// ((DefaultLayer) rails).setExtraPropertyNames(names);
		//
		// EditableLayerImpl results = (EditableLayerImpl) db
		// .getOrCreateEditableLayer("testSnapping_results");
		// String[] fieldsNames = new String[] { "name", "description",
		// "distance", "railway" };
		// results.setExtraPropertyNames(fieldsNames);
		tx = database.beginTx();
		importStations();
		tx.success();
		tx.finish();
		tx = database.beginTx();
		importSegments();
		tx.success();
		tx.finish();
		// snap(rails, results, fieldsNames);
		// ShapefileExporter exporter = new ShapefileExporter(database);
		// exporter.setExportDir("target/export");
		// exporter.exportLayer(rails.getName());
		// // exporter.exportLayer(stations.getName());
		// StyledImageExporter imageExporter = new
		// StyledImageExporter(database);
		// imageExporter.setExportDir("target/export");
		// //imageExporter.setZoom(3.0);
		// //imageExporter.setOffset(0.1, 0.1);
		// imageExporter.setSize(3000, 3000);
		// String[] layerNames = new String[] { rails.getName(),
		// stations.getName(), results.getName() };
		// imageExporter.saveLayerImage(layerNames, "geosnappr.sld.xml", new
		// File(
		// "all.png"), null);
	}

	private void importSegments() throws FileNotFoundException, IOException {
		URL remote = new URL("http://xn--tg-yia.info/tag.xml");
		System.out.println("Getting trains from " + remote);
		database.beginTx();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file

			dom = db.parse(remote.openStream());
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xpath.evaluate("//tagLista/tag", dom,
					XPathConstants.NODESET);
			GraphDatabaseUtil util = new GraphDatabaseUtil(database);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node station = nodes.item(i);

				String number = (String) xpath.evaluate("nr", station,
						XPathConstants.STRING);
				String url = (String) xpath.evaluate("url", station,
						XPathConstants.STRING);
				org.neo4j.graphdb.Node trainNode = database.createNode();
				org.neo4j.graphdb.Node trainsNode = util
						.getOrCreateSubReferenceNode(RelTypes.TRAINS);
				trainsNode.createRelationshipTo(trainNode, RelTypes.TRAIN);
				trainNode.setProperty("number", number);
				parseTrain(url, trainNode);
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

	private void parseTrain(String url, org.neo4j.graphdb.Node trainNode)
			throws Exception {

		URL remote = new URL(url);
		System.out.println("Getting train from " + remote);
		Document dom = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// Using factory get an instance of document builder
		DocumentBuilder db = dbf.newDocumentBuilder();

		// parse using builder to get DOM representation of the XML file
		dom = db.parse(remote.openStream());
		XPath xpath = XPathFactory.newInstance().newXPath();
		trainNode.setProperty("till", (String) xpath.evaluate("//tag/till",
				dom, XPathConstants.STRING));
		NodeList stations = (NodeList) xpath.evaluate("//stationer/station",
				dom, XPathConstants.NODESET);
		org.neo4j.graphdb.Node previous = trainNode;
		long ordDeparture = 0;
		DynamicRelationshipType relType = DynamicRelationshipType
				.withName("TRAIN-"
						+ (String) trainNode.getProperty(Const.NUMBER));

		for (int i = 0; i < stations.getLength(); i++) {
			Node sn = stations.item(i);
			String stationName = (String) xpath.evaluate("namn", sn,
					XPathConstants.STRING);
			String ordArrival = (String) xpath.evaluate("ordAnkomst", sn,
					XPathConstants.STRING);
			String ordArrivalDate = (String) xpath.evaluate("ordAnkomstDatum",
					sn, XPathConstants.STRING);

			org.neo4j.graphdb.Node station = index.getSingleNode("name",
					stationName);
			if (station == null) {
				System.out.println("Could not find node for staion "
						+ stationName);
			} else {
				Relationship to = previous.createRelationshipTo(station,
						relType);

				if (ordDeparture != 0) {
					to.setProperty(Const.ORD_DEPARTURE, ordDeparture);
				}
				if (!ordArrival.equals("")) {
					to.setProperty(Const.ORD_ARRIVAL,
							df.parse(ordArrivalDate + " " + ordArrival)
									.getTime());
				}
				String ordDepartureTime = (String) xpath.evaluate("ordAvgang",
						sn, XPathConstants.STRING);
				String ordDepartureDate = (String) xpath.evaluate(
						"ordAvgangDatum", sn, XPathConstants.STRING);
				if (!ordDepartureTime.equals("")) {
					ordDeparture = df.parse(
							ordDepartureDate + " " + ordDepartureTime)
							.getTime();
				} else {
					ordDeparture = 0;
				}
				previous = station;
			}
		}
		// previous.createRelationshipTo(trainNode, relType);

	}

	private void importStations() throws FileNotFoundException, IOException {
		URL remote = new URL("http://xn--tg-yia.info/stationer.xml");

		System.out.println("Getting stations from " + remote);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {

			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			dom = db.parse(remote.openStream());

			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList nodes = (NodeList) xpath.evaluate("//stationer/station",
					dom, XPathConstants.NODESET);
			GraphDatabaseUtil util = new GraphDatabaseUtil(database);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node station = nodes.item(i);

				String name = (String) xpath.evaluate("namn", station,
						XPathConstants.STRING);
				Double lon = Double.parseDouble((String) xpath.evaluate("lon",
						station, XPathConstants.STRING));
				Double lat = Double.parseDouble((String) xpath.evaluate("lat",
						station, XPathConstants.STRING));
				// String[] keys = new String[] { "name" };
				// Object[] values = new String[] { name };
				// layer.add(
				// layer.getGeometryFactory().createPoint(
				// new Coordinate(lon, lat)), keys, values);
				org.neo4j.graphdb.Node stat = database.createNode();
				org.neo4j.graphdb.Node stationsNode = util
						.getOrCreateSubReferenceNode(RelTypes.STATIONS);
				stationsNode.createRelationshipTo(stat, RelTypes.STATION);
				stat.setProperty("lon", lon);
				stat.setProperty("lat", lat);
				stat.setProperty("name", name);
				index.index(stat, "name", name);
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

	public void buildStationLayer() {
		tx = database.beginTx();
		StationsLayer layer = (StationsLayer) db.getOrCreateLayer(STATIONS2,
				StationsEncoder.class, StationsLayer.class);
		layer.clear();
		String[] keys = new String[] { "name" };
		layer.setExtraPropertyNames(keys);
		Traverser stations = Traversal
				.description()
				.expand(Traversal.expanderForTypes(RelTypes.STATIONS,
						Direction.OUTGOING, RelTypes.STATION,
						Direction.OUTGOING)).filter(new Predicate<Path>() {

					@Override
					public boolean accept(Path path) {
						return path.length() == 2;
					}
				}).traverse(database.getReferenceNode());
		for (org.neo4j.graphdb.Node station : stations.nodes()) {
			System.out.println("indexing " + station.getProperty(Const.NAME));
			layer.addStation(station);
		}
		System.out.println("finished");
		tx.success();
		tx.finish();
	}

	public void getStations(double lon, double lat, int km, long from, int minutesForward) {
		Layer layer = db.getLayer(STATIONS2);
		SpatialIndexReader spatialIndex = layer.getIndex();
		Point startingPoint = layer.getGeometryFactory().createPoint(
				new Coordinate(lon, lat));
		Search searchQuery = new SearchPointsWithinOrthodromicDistance(
				startingPoint, km);
		spatialIndex.executeSearch(searchQuery);
		List<SpatialDatabaseRecord> results = searchQuery.getResults();
		for (Iterator iterator = results.iterator(); iterator.hasNext();) {
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(new Date(from));
			calendar.add(Calendar.MINUTE, minutesForward);
			System.out.println("requested: " + df.format(from) + " + " + minutesForward + "min");
			SpatialDatabaseRecord spatialDatabaseRecord = (SpatialDatabaseRecord) iterator
					.next();
			org.neo4j.graphdb.Node stationNode = spatialDatabaseRecord
					.getGeomNode();
			System.out.println("Found " + stationNode.getProperty(Const.NAME));

			for (Relationship rel : stationNode
					.getRelationships(Direction.OUTGOING)) {
				if (rel.hasProperty(Const.ORD_DEPARTURE)) {
					long departure = new Date(
							(Long) rel.getProperty(Const.ORD_DEPARTURE))
							.getTime();
					Date dep = new Date(departure);
					if (calendar.getTimeInMillis() > departure && departure > from) {
						System.out.println(rel.getType() + " departing at "
								+ df.format(departure) + " to " + rel.getEndNode().getProperty(Const.NAME));
						System.out.println(" - valid.");
					}
				}
			}

		}

	}

	public void exportToPng(String layername) {
		ShapefileExporter shpExporter = new ShapefileExporter(database);
		shpExporter.setExportDir("target/export");
		try {
			shpExporter.exportLayer(layername);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
