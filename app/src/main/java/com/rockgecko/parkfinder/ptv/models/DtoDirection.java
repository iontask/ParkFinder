package com.rockgecko.parkfinder.ptv.models;

import java.io.Serializable;

public class DtoDirection implements Serializable{

	/**
	 * linedir_id	numeric string
    unique identifier of a particular line and direction
        e.g. "21"
*/
	private String linedir_id;
	
	/**
direction_id	numeric string
    unique identifier of a direction (e.g. "0" signifies "city")
    e.g. 0
*/
	private String direction_id;
	/**
direction_name	string
    name of the direction of the service
    e.g. "City (Flinders Street)"
	 */
	private String direction_name;
	
	private DtoLine line;
	
	public String getLinedir_id() {
		return linedir_id;
	}
	public String getDirection_id() {
		return direction_id;
	}
	public String getDirection_name() {
		return direction_name;
	}
	
	public DtoLine getLine(){
		return line;
	}
	
	
}
