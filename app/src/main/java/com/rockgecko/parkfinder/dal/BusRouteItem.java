package com.rockgecko.parkfinder.dal;

public class BusRouteItem {

	private String id;
	private String name;
	
	
	public BusRouteItem(String id, String name) {		
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString(){
		return name;
	}
}
