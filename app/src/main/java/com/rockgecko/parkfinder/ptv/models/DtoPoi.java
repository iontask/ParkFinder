package com.rockgecko.parkfinder.ptv.models;

import java.util.ArrayList;
import java.util.List;

import com.rockgecko.parkfinder.util.StringUtils;

/**
 * A list of DtoStopOutlets near coordinates
 * requested by /poi/
 * @author Bramley
 *
 */
public class DtoPoi {


	private double minLat;
	private double minLong;
	private double maxLat;
	private double maxLong;
	private double weightedLat;
	private double weightedLong;
	private int totalLocations;
	private List<DtoStopOutlet> locations;
	private List<DtoPoi> clusters;
	
	
	
	public DtoPoi(double weightedLat, double weightedLong, List<DtoStopOutlet> locations) {		
		this.weightedLat = weightedLat;
		this.weightedLong = weightedLong;		
		this.locations = locations;
		totalLocations = locations!=null?locations.size():0;
	}
	public double getMinLat() {
		return minLat;
	}
	public double getMinLong() {
		return minLong;
	}
	public double getMaxLat() {
		return maxLat;
	}
	public double getMaxLong() {
		return maxLong;
	}
	public double getWeightedLat() {
		return weightedLat;
	}
	public double getWeightedLong() {
		return weightedLong;
	}
	public int getTotalLocations() {
		return totalLocations;
	}
	public List<DtoStopOutlet> getLocations() {
		return locations;
	}
	public List<DtoPoi> getClusters() {
		return clusters;
	}
	
	public List<DtoStopOutlet> getLocationsOrClusterConcat(){
		if(!StringUtils.isNullOrEmpty(locations)) return locations;
		List<DtoStopOutlet> result = new ArrayList<DtoStopOutlet>();
		for(DtoPoi cluster : clusters){
			result.addAll(cluster.locations);
		}
		return result;
	}
	
}
