package com.rockgecko.parkfinder.ptv.models;

import com.rockgecko.parkfinder.ptv.PtvApi;
import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.R;
import com.rockgecko.parkfinder.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DtoLine implements Serializable{

	/**
	 * For regional centres eg Bendigo, where line number is just Bendigo. Eg:
	 * Bendigo - Strathfieldsaye (Route 16)
	 */
	private static final Pattern LINE_NAME_NUMBER = Pattern.compile("(.*) \\([Rr]oute ([0-9]+)\\)");
	private static final Pattern LINE_NUMBER_NAME = Pattern.compile("([0-9]+) - (.*)");
	private static final Pattern LINE_NUMBER_NAME_SMARTBUS = Pattern.compile("([0-9]+) - (.*)(\\(SMARTBUS [Ss]ervice\\)?)");
	private static final String SMARTBUS_TAIL = "(SMARTBUS Service)";
	/**
	 * transport_type	string
 the mode of transport serviced by the line
 e.g. can be either 'train', 'tram', 'bus', 'V/Line' or 'NightRider'
*/
	private String transport_type;
	/**
line_id	numeric string
 the unique identifier of each line
 e.g. '1818'
*/
	private String line_id;
	
	/**
line_name	string
 the name of the line
 e.g. "970 - City - Frankston - Mornington - Rosebud via Nepean Highway & Frankston Station "
*/
	private String line_name;
	/**
line_number	string
 the line number that is presented to the public (i.e. not the �line_id�)
 e.g. '970'
	 */
	private String line_number;

	/**
	 * eg City - Frankston - Mornington - Rosebud via Nepean Highway & Frankston Station
	 */
	private String line_name_short;
	/**
	 * eg 970
	 */
	private String line_number_long;

	/**
	 * new field not needed, use getTransportType_int
	 */
	//private int route_type;

	
	public String getTransport_type() {
		return transport_type;
	}
	public int getTransport_type_int() {
        if(transport_type!=null)
		switch (transport_type.toLowerCase()){
            case "bus":
                return PtvApi.POI_BUS;
            case "nightrider":
            case "night bus":
            case "nightbus":
                return PtvApi.POI_NIGHTRIDER;
            case "train":
                return PtvApi.POI_TRAIN;
            case "tram":
                return PtvApi.POI_TRAM;
            case "v/line":
            case "vline":
                return PtvApi.POI_VLINE;
        }
        return -1;
	}
	public String getLine_id() {
		return line_id;
	}	
	public String getLine_name() {
		return line_name;
	}
	public String getLine_name_formatted() {
		if(line_name!=null){
			Matcher match = LINE_NAME_NUMBER.matcher(line_name);
			if(match.matches()){
				return match.group(1).trim();
			}
			match = LINE_NUMBER_NAME_SMARTBUS.matcher(line_name);
			if(match.matches()){
				return match.group(2).trim();
			}
		}
		return line_name;
	}
    public String getRouteNumberSubText(){
        if(getTransport_type_int()==PtvApi.POI_NIGHTRIDER) return ParkFinderApplication.getContextStatic().getString(R.string.nightrider);
        if(line_name!=null){
            Matcher match = LINE_NUMBER_NAME_SMARTBUS.matcher(line_name);
            if(match.matches() && !StringUtils.isNullOrEmpty(match.group(3))){
                return ParkFinderApplication.getContextStatic().getString(R.string.smartbus);
            }
        }
        return null;
    }
	public boolean isCombinedLine(){
		return line_name!=null && line_name.contains(" combined ");
	}
	public String getLine_number() {
		return line_number;
	}
	public String getLine_number_formatted() {
		/*try{
			Integer.parseInt(line_number);
			return line_number;
		}
		catch(Exception e){}*/
		if(line_name!=null){
			Matcher match = LINE_NAME_NUMBER.matcher(line_name);
			if(match.matches()){
				return match.group(2);
			}
		}
		return line_number;
		
	}
	public int getLine_number_int() {
		try{
			return Integer.parseInt(getLine_number_formatted());
		}
		catch(Exception e){
			return 0;
		}
	}

	public String toString(){
		return line_name;
	}
	
}
