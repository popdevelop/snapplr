package org.snapplr.spatial;

import org.neo4j.gis.spatial.AbstractGeometryEncoder;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class StationsEncoder extends AbstractGeometryEncoder {

	protected GeometryFactory geometryFactory;
	@Override
	protected void encodeGeometryShape(Geometry geometry,
			PropertyContainer container) {
		container.setProperty("gtype", SpatialDatabaseService
				.convertJtsClassToGeometryType(geometry.getClass()));

	}
	protected GeometryFactory getGeometryFactory() {
		if(geometryFactory==null) geometryFactory = new GeometryFactory();
		return geometryFactory;
	}

	public Geometry decodeGeometry(PropertyContainer container) {
		Coordinate coordinate = new Coordinate(
				(Double) container.getProperty("lon"),
				(Double) container.getProperty("lat"));

		return getGeometryFactory().createPoint(coordinate);
	}

	public Envelope decodeEnvelope(PropertyContainer container) {
		if (!isStation(container)) {
			return super.decodeEnvelope(container);
		} else {
			Double lon = (Double) container.getProperty("lon");
			Double lat = (Double) container.getProperty("lat");

			// Envelope parameters: xmin, xmax, ymin, ymax
			return new Envelope(lon, lon, lat, lat);
		}
	}

	private String print(PropertyContainer container) {
		String result = container.toString();
		for (String key : container.getPropertyKeys()) {
			result += key + container.getProperty(key) + ", ";
		}
		return result ;
	}

	public void encodeGeometry(Geometry geometry, PropertyContainer container) {
		if (!isStation(container)) {
			super.encodeGeometry(geometry, container);
		} else {
			container.setProperty(PROP_TYPE,
					encodeGeometryType(geometry.getGeometryType()));
		}
	}

	private boolean isStation(PropertyContainer container) {
		return ((Node) container).hasRelationship(RelTypes.STATION);
	}
	
}