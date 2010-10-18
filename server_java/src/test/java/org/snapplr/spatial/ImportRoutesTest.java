package org.snapplr.spatial;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.gis.spatial.geotools.data.StyledImageExporter;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import com.geosnappr.TaginfoImporter;

public class ImportRoutesTest {

	private String directoryName = "target/db";
	private EmbeddedGraphDatabase database;
	private LuceneIndexService index;

	@Before
	public void setUp() {
		// try {
		// FileUtils.deleteDirectory(new File(directoryName));
		database = new EmbeddedGraphDatabase(directoryName);
		index = new LuceneIndexService(database);
		// db = new SpatialDatabaseService(database);
		// tx = database.beginTx();
		// } catch (IOException ioe) {
		// ioe.printStackTrace();
		// }

	}

	@Test
	public void testImport() throws Exception {
		TaginfoImporter importer = new TaginfoImporter(database, index);
		importer.importRoutes();
		importer.buildStationLayer();
		//search around Malmo
		importer.getStations(13.0009, 55.6093, 10);
		//importer.exportToPng("stations");
		StyledImageExporter imageExporter = new StyledImageExporter(database);
		imageExporter.setExportDir("target/export");
		//imageExporter.setZoom(3.0);
		//imageExporter.setOffset(-0.05, -0.05);
		imageExporter.setSize(1024, 768);
		imageExporter.saveLayerImage("stations", "geosnappr.sld.xml");
	}

	@After
	public void shutdown() {
		System.out.println("test");
		//tx.success();
		// tx.finish();
	}

	// @Ignore
	// @Test
	//
	//
	// private void snap(EditableLayer layer2, EditableLayer results,
	// String[] fieldsNames) {
	// // Now test snapping to a layer
	// GeometryFactory factory = layer2.getGeometryFactory();
	//
	// Coordinate coordinate_malmo_c = new Coordinate(13.0029899, 55.6095057);
	// Point malmo_c = factory.createPoint(coordinate_malmo_c);
	// results.add(malmo_c, fieldsNames, new Object[] { 0L, "Point to snap",
	// 0L, "" });
	// String layerName = layer2.getName();
	// Layer layer = db.getLayer(layerName);
	// assertNotNull("Missing layer: " + layerName, layer);
	// System.out.println("Closest features in " + layerName + " to point "
	// + malmo_c + ":");
	// // Coordinate[] dummy = new Coordinate[]{coordinate_malmo_c,
	// // coordinate_malmo_c};
	// for (PointResult result : SpatialTopologyUtils.findClosestEdges(
	// malmo_c, layer)) {
	// System.out.println("\t" + result);
	// long dist = (long) (1000000 * result.getDistance());
	// results.add(result.getKey(), fieldsNames, new Object[] {
	// "Snap, " + "dist=" + dist + ", Geo="
	// + result.getValue().getGeomNode().getId(),
	// "Snapped point to layer " + layerName + ": "
	// + result.getValue().getGeometry().toString(), dist,
	// "" });
	// }
	// ShapefileExporter shpExporter = new ShapefileExporter(database);
	// shpExporter.setExportDir("target/export");
	// try {
	// shpExporter.exportLayer(results.getName());
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }

	// }

}
