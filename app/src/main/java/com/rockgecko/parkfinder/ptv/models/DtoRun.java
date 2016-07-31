package com.rockgecko.parkfinder.ptv.models;

import java.io.Serializable;

public class DtoRun implements Serializable{

	/**
	 * transport_type	string
 the mode of transport serviced by the stop
 e.g. can be either 'train', 'tram', 'bus', 'vline' or 'nightrider'
	 */
	private String transport_type;
	/**
run_id	numeric string
� the unique identifier of each run
� e.g. �1464�“
	 */
	private String run_id;
	/**
num_skipped	integer
� the number of stops skipped for the run, applicable to train; a number greater than zero indicates either a limited express or express service
� e.g. 0
	 */
	private int num_skipped;
	/**
destination_id	numeric string
� the stop_id of the destination, i.e. the last stop for the run
� e.g. �1044�
	 */
	private String destination_id;
	/**
destination_name	string
� the location_name of the destination, i.e. the last stop for the run
� e.g. �Craigieburn�
	 */
	private String destination_name;
		

	public String getTransport_type() {
		return transport_type;
	}
	public String getRun_id() {
		return run_id;
	}
	public int getNum_skipped() {
		return num_skipped;
	}
	public String getDestination_id() {
		return destination_id;
	}
	public String getDestination_name() {
		return destination_name;
	}
	
	
	
	
	
}
