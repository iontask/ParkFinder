package com.rockgecko.parkfinder.ptv.models;

public class DtoNearestStopsResponse {

	public DtoStopOutlet result;
	
	/**
	 * "stop"
	 */
	public String type;
	
	public boolean isStop(){
		return type!=null && type.equals("stop") && result!=null;
	}
}
