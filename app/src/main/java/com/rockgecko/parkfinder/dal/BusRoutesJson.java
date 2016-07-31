package com.rockgecko.parkfinder.dal;

import java.util.ArrayList;
import java.util.List;

public class BusRoutesJson {

	private int versionCode;
	private String versionName;
	private List<BusRouteItem> routes;
	
	public BusRoutesJson(int versionCode, String versionName,
			List<BusRouteItem> routes) {		
		this.versionCode = versionCode;
		this.versionName = versionName;
		this.routes = routes;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public String getVersionName() {
		return versionName;
	}

	public List<BusRouteItem> getRoutes(boolean removeCombinedRoutes) {
		if(removeCombinedRoutes && routes!=null){
			List<BusRouteItem> filteredRoutes = new ArrayList<BusRouteItem>(routes.size());
			for(BusRouteItem route : routes){
				if(!route.getName().contains(" combined -")){
					filteredRoutes.add(route);
				}
			}
			return filteredRoutes;
		}
		return routes;
	}
	
	
}
