package org.snapplr.spatial;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.ShapefileExporter;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.SpatialTopologyUtils;
import org.neo4j.gis.spatial.SpatialTopologyUtils.PointAndGeom;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import com.vividsolutions.jts.geom.Coordinate;

public class ImportRoutesTest {

	private String directoryName = "target/db";
	private SpatialDatabaseService db;
	private GraphDatabaseService database;

	@Before
	public void setUp() {
		try {
			FileUtils.deleteDirectory(new File(directoryName));
			database = new EmbeddedGraphDatabase(
					directoryName);
			db = new SpatialDatabaseService(database);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

	}

	@Test
	public void importRoutes() throws Exception {
		EditableLayer layer = (EditableLayer) db
				.getOrCreateEditableLayer("trains");
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
				System.out.println(line);
				StringTokenizer tokens = new StringTokenizer(line, ",");
				int currentTrainId = Integer.parseInt(tokens.nextToken());
				if (!(currentTrainId == trainId)) {
					Coordinate[] coords = new Coordinate[coordinates.size()];
					coordinates.toArray(coords);
					System.out.println(coords);
					if(coords.length > 1) {
						
						String[] names = {"name"};
						
						String[] values = {"train "+ trainId};
						SpatialDatabaseRecord record = layer.add(layer
								.getGeometryFactory().createLineString(
										coords), names , values );
					}
					trainId = currentTrainId;
					System.out.println("inserted train" + trainId);
					coordinates.clear();
				} else {
					coordinates.add(new Coordinate(Double.parseDouble(tokens
							.nextToken()), Double.parseDouble(tokens
							.nextToken()), 0));

				}
			}
			in.close();
		} finally {
		}
		ShapefileExporter exporter = new ShapefileExporter( database );
        exporter.setExportDir( "target/export" );

        exporter.exportLayer( layer.getName() );
        ArrayList<PointAndGeom> edges = new SpatialTopologyUtils().findClosestEdges(layer.getGeometryFactory().createPoint(new Coordinate(60, 16)), layer, 0.1);
        System.out.println("test");
        Iterator<PointAndGeom> iterator = edges.iterator();
        while (iterator.hasNext() ) {
        	PointAndGeom next = iterator.next();
        	System.out.println("found " + next.getKey() + next.getValue());
        	
        }
//        edges = new SpatialTopologyUtils().findClosestEdges(layer.getGeometryFactory().createPoint(new Coordinate(60, 15)), layer, 100);
//        System.out.println("test");
	}

}
