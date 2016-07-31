package com.rockgecko.parkfinder.dal;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;


public class RoutePathResult {

	public String routeNo;
	public String shapeID;
	public double length;
	public List<LatLng> path;
	public String polyline;
}
