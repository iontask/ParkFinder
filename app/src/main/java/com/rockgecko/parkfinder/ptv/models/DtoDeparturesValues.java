package com.rockgecko.parkfinder.ptv.models;

import java.util.Date;

import com.rockgecko.parkfinder.util.StringUtils;

public class DtoDeparturesValues {

	private DtoPlatform  platform;
	
	private DtoRun run;
	private Date time_timetable_utc;
	private Date time_realtime_utc;
	/**
	 * Dash - separated list:
	 * RR = Reservations Required
		GC = Guaranteed Connection
		DOO = Drop Off Only
		PUO = Pick Up Only
		MO = Mondays only
		TU = Tuesdays only
		WE = Wednesdays only
		TH = Thursdays only
		FR = Fridays only
		SS = School days only
		
		note: ignore "E" flag

	 */
	private String flags;

	//private ArrayList<DtoDisruption> disruptions;
	
	public String[] getFlags(){
		if(StringUtils.isNullOrEmpty(flags)) return new String[0];
		return flags.split("-");
	}

	public DtoPlatform getPlatform() {
		return platform;
	}

	public DtoRun getRun() {
		return run;
	}

	public Date getTime_timetable_utc() {
		return time_timetable_utc;
	}
	/**
	 * TODO NYI
	 * @return
	 */
	public Date getTime_realtime_utc() {
		return time_realtime_utc;
	}
	
}
