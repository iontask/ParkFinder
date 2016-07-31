package com.rockgecko.parkfinder.ptv.models;

public class DtoPlatform {
	/**
	 * a place holder for the stop's real-time feed system ID
	 * (for potential future implementation; as no real-time feeds
	 *  are provided at this time, this returns '0')
	 */
	private String realtime_id;
	
	private DtoStopOutlet stop;
	
	private DtoDirection direction;

	public String getRealtime_id() {
		return realtime_id;
	}

	public DtoStopOutlet getStop() {
		return stop;
	}

	public DtoDirection getDirection() {
		return direction;
	} 
	
	
}
