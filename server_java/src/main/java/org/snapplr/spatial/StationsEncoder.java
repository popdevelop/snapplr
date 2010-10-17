package org.snapplr.spatial;

import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePropertyEncoder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.util.GraphDatabaseUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class StationsEncoder extends SimplePropertyEncoder {

	@Override
	protected void encodeGeometryShape(Geometry geometry,
			PropertyContainer container) {
		container.setProperty("gtype", SpatialDatabaseService
				.convertJtsClassToGeometryType(geometry.getClass()));
		Coordinate[] coords = geometry.getCoordinates();
		float[] data = new float[coords.length * 2];
		for (int i = 0; i < coords.length; i++) {
			data[i * 2 + 0] = (float) coords[i].x;
			data[i * 2 + 1] = (float) coords[i].y;
		}
	}

	public Geometry decodeGeometry(PropertyContainer container) {
		float[] data = (float[]) container.getProperty("data");
		Coordinate[] coordinates = new Coordinate[data.length / 2];
		for (int i = 0; i < data.length / 2; i++) {
			coordinates[i] = new Coordinate(data[2 * i + 0], data[2 * i + 1]);
		}
		return getGeometryFactory().createPoint(coordinates[0]);
	}
}