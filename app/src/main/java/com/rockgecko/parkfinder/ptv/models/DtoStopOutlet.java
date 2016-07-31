package com.rockgecko.parkfinder.ptv.models;

import java.util.List;
import java.util.HashSet;

import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;
import com.rockgecko.parkfinder.util.StringUtils;

/**
 * A stop or ticket outlet
 * @author Bramley
 *
 */
public class DtoStopOutlet implements IStop{
	
	

	/*
	 *       "outlet_type": "Stop",
      "suburb": "East Melbourne",
      "business_name": "Jolimont Station",
      "location_name": "Wellington Cres",
      "lat": -37.81653,
      "lon": 144.9841,
      "distance": 0.0
    },
    {
      "suburb": "East Melbourne",
      "transport_type": "tram",
      "stop_id": 2824,
      "location_name": "Powlett St/Wellington Pde #12 ",
      "lat": -37.8163261,
      "lon": 144.985016,
      "distance": 0.0
    },

	 */
	
	/**
	 * Retail, Stop, or null for stops [sic]
	 * Stop means its a ticket machine at a stop/train station.
	 */	
	private String outlet_type;
	/**
	 * 
	 */
	private String business_name;
	
	/**
	 * stop only
     * the mode of transport serviced by the stop
     – e.g. can be either “train”, “tram”, “bus”, “V/Line” or “NightRider”

     */
	private String transport_type;

	/**
	 * new field, not needed, use getMode instead
	 */
	private int route_type;
	/**
	 * stop only
	 */
	private String stop_id;
	
	private String suburb;
	/**
	 * eg "Jolimont Rd/Wellington Pde #10 " for tram stop,
	 * or street address for shop.
	 */
	private String location_name;
	private double lat;
	private double lon;
	//private double distance;
	
	private List<StopService> _services;
	private boolean _completeServices=false;
	private boolean _fullDayCompleteServices=false;
	private HashSet<Integer> _modes;
	private String _zone;
	
	public boolean isStop(){
		return !StringUtils.isNullOrEmpty(stop_id);
	}
	
	public String getOutlet_type() {
		return outlet_type;
	}
	public String getBusiness_name() {
		return business_name;
	}
	public String getTransport_type() {
		return transport_type;
	}
	public String getId() {
		return stop_id;
	}
	public String getSuburb() {
		return suburb;
	}
	public String getLocation_name() {
		return location_name;
	}
	public double getLatitude() {
		return lat;
	}
	public double getLongitude() {
		return lon;
	}

	public List<StopService> getServices(){
		return _services;	
	}
	
	@Override
	public void setServices(List<StopService> services) {		
		_services=services;
	}
	
	public String getZone(){
		return _zone;
	}
	public void setZone(String zone){
		this._zone=zone;
	}
	
	public StopService findServiceByRunId(String runId){
		if(getServices()!=null) for(StopService service : getServices()){
			 if(runId.equals(service.getRun().getRun_id())){
				 return service;
			 }
		 }
		return null;
	}
	
	@Override
	public boolean hasCompleteServices(boolean fullDay){
		return fullDay?_fullDayCompleteServices:_completeServices;
	}
	public void setHasCompleteServices(boolean hasCompleteServices){
		_completeServices=hasCompleteServices;
	}
	
	public void setHasFullDayCompleteServices(boolean fullDayCompleteServices){
		_fullDayCompleteServices=fullDayCompleteServices;
	}


	/*
	public void addService(StopService service) {
		if(_services==null) _services = new ArrayList<StopService>();
		getServices().add(service);
		
	}*/

	@Override
	public String getName() {		
		return location_name;
	}
	
	public String getDetails(){
		return suburb;
	}


	@Override
	public int getMode() {
		/*if("tram".equalsIgnoreCase(transport_type)) return MODE_TRAM;
		if("train".equalsIgnoreCase(transport_type)) return MODE_TRAIN;
		if("bus".equalsIgnoreCase(transport_type)) return MODE_BUS;
		if("vline".equalsIgnoreCase(transport_type)) return MODE_VLINE;
		if("nightrider".equalsIgnoreCase(transport_type)) return MODE_NIGHTRIDER;*/
		return route_type;
	}
    public void setMode(int mode){
		route_type=mode;
        /*switch(mode){
            case MODE_TRAM:
                transport_type="tram";
                break;
            case MODE_TRAIN:
                transport_type="train";
                break;
            case MODE_BUS:
                transport_type="bus";
                break;
            case MODE_VLINE:
                transport_type="vline";
                break;
            case MODE_NIGHTRIDER:
                transport_type="nightrider";
                break;
        }*/
    }

	public HashSet<Integer> getModes(){
		if(_modes==null){
			HashSet<Integer> result= new HashSet<>();
			result.add(getMode());
			return result;
		}
		return _modes;
	}
	public void addMode(int mode){
		if(_modes==null) _modes=getModes();
		_modes.add(mode);
	}

	

	


	
	
	
	
}
