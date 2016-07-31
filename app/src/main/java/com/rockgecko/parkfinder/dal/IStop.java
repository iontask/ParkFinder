package com.rockgecko.parkfinder.dal;

import com.rockgecko.parkfinder.ptv.PtvApi;

import java.io.Serializable;
import java.util.List;


public interface IStop extends Serializable{
	
	/**
	 * 	Train (metropolitan)
	 */
	public static final int MODE_TRAIN = PtvApi.POI_TRAIN;
	/**
	* 1	Tram
	*/
	public static final int MODE_TRAM = PtvApi.POI_TRAM;
	/**
	 * 2	Bus (metropolitan and regional, but not V/Line)
	 */
	public static final int MODE_BUS = PtvApi.POI_BUS;
	/**
	*3	V/Line regional train and coach
	*/
	public static final int MODE_VLINE = PtvApi.POI_VLINE;

	public static final int MODE_NIGHTRIDER = PtvApi.POI_NIGHTRIDER;

	public double getLatitude();
	
	public double getLongitude();
	
	public String getName();
	
	public String getDetails();
	
	public String getId();
	
	/**
	 * Transport mode
	 * @return
	 */
	public int getMode();
	
	public List<StopService> getServices();
	public void setServices(List<StopService> services);
	public StopService findServiceByRunId(String runId);
	
	public boolean hasCompleteServices(boolean fullDay);
	

}
