package org.snapplr.spatial;

import org.neo4j.gis.spatial.DynamicLayer;
import org.neo4j.gis.spatial.NullListener;

public class StationsLayer extends DynamicLayer{

	public void clear() {
		index.clear(new NullListener());
	}

	public void addStation(org.neo4j.graphdb.Node station) {
		index.add(station);
	}

	@Override
	public Integer getGeometryType() {
			return GTYPE_POINT;
	}
	
	
}
