package com.rockgecko.parkfinder.dal;

import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;


/**
 * A favourite stop from local db
 * @author Bramley
 *
 */
public class FavouriteStop implements IStop, BaseColumns{
		
	
	public static final int FAVOURITE_TYPE_BUS_STOP = 1;
	public static final int FAVOURITE_TYPE_NIGHTRIDER_STOP = 2;

	private long favouriteId;			
	private String stopId;
	private int favouriteType;
	
	private String name;
	private String customName;	
	private String details;
	/**
	 * eg "Jolimont Rd/Wellington Pde #10 " for tram stop,
	 * or street address for shop.
	 */
	
	private double latitude;
	private double longitude;
	
	FavouriteStop(){		
	}
	
	
	public FavouriteStop(IStop s, String customName){
		stopId=s.getId();
		name=s.getName();
		this.customName=customName;
		details=s.getDetails();
		latitude=s.getLatitude();
		longitude=s.getLongitude();
		switch(s.getMode()){
		case MODE_BUS:
			favouriteType=FAVOURITE_TYPE_BUS_STOP;
			break;
        case MODE_NIGHTRIDER:
            favouriteType=FAVOURITE_TYPE_NIGHTRIDER_STOP;
            break;
		default:
			favouriteType=-1;
		}
	}
	
	public long getFavouriteId(){
		return favouriteId;
	}
	public int getFavouriteType(){
		return favouriteType;
	}
	
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}

	@Override
	public String getName() {		
		return name;
	}

	public String getCustomName(){
		return customName;
	}
	
	@Override
	public String getDetails() {
		return details;
	}

	@Override
	public String getId() {
		return stopId;
	}

	@Override
	public int getMode() {
		if(favouriteType==FAVOURITE_TYPE_BUS_STOP) return MODE_BUS;
		//if("tram".equalsIgnoreCase(transport_type)) return MODE_TRAM;
		//if("train".equalsIgnoreCase(transport_type)) return MODE_TRAIN;
		//if("vline".equalsIgnoreCase(transport_type)) return MODE_VLINE;
		if(favouriteType==FAVOURITE_TYPE_NIGHTRIDER_STOP) return MODE_NIGHTRIDER;
		return -1;
	}

	public void setFavouriteId(long favouriteId){
		this.favouriteId=favouriteId;
	}
	public void setCustomName(String customName){
		this.customName=customName;
	}

	@Override
	public List<StopService> getServices() {
		return null;
	}

	@Override
	public void setServices(List<StopService> services) {		
		
	}

	@Override
	public StopService findServiceByRunId(String runId) {
		return null;
	}

	@Override
	public boolean hasCompleteServices(boolean fullDay) {
		return false;
	}

	public static FavouriteStop getFavouriteByStopId(List<FavouriteStop> in, String stopId){
		for(FavouriteStop stop : in){
			if(stopId.equals(stop.getId())) return stop;
		}
		return null;
	}

	public static final String TABLE_NAME = "favourite_stop";
	public static final String COL_STOP_ID = "stop_id";
	public static final String COL_FAVOURITE_TYPE = "favourite_type";
	public static final String COL_NAME = "name";
	public static final String COL_CUSTOM_NAME = "custom_name";
	public static final String COL_DETAILS = "details";
	public static final String COL_LATITUDE = "latitude";
	public static final String COL_LONGITUDE = "longitude";
	
	static final String TABLE_CREATE =
			"create table "
			+ TABLE_NAME + " ("
			+ _ID + " integer primary key autoincrement,"
			+ COL_STOP_ID + " text not null unique,"
			+ COL_FAVOURITE_TYPE + " int not null,"
			+COL_NAME + " text,"
			+COL_CUSTOM_NAME + " text,"
			+COL_DETAILS + " text,"
			+COL_LATITUDE + " real not null,"
			+COL_LONGITUDE + " real not null"			
			+");";
	
	static FavouriteStop loadFromCursor(Cursor cursor){
		FavouriteStop stop = new FavouriteStop();
		stop.favouriteId = cursor.getInt(cursor.getColumnIndex(_ID));
		stop.stopId = cursor.getString(cursor.getColumnIndex(COL_STOP_ID));
		stop.favouriteType = cursor.getInt(cursor.getColumnIndex(COL_FAVOURITE_TYPE));
		stop.name = cursor.getString(cursor.getColumnIndex(COL_NAME));
		stop.customName = cursor.getString(cursor.getColumnIndex(COL_CUSTOM_NAME));
		stop.details = cursor.getString(cursor.getColumnIndex(COL_DETAILS));
		stop.latitude = cursor.getDouble(cursor.getColumnIndex(COL_LATITUDE));
		stop.longitude = cursor.getDouble(cursor.getColumnIndex(COL_LONGITUDE));
		
		return stop;
	}
	
	ContentValues toContentValues(){
		ContentValues cv = new ContentValues();
        //cv.put(_ID, favouriteId);
        cv.put(COL_STOP_ID, stopId);
        cv.put(COL_FAVOURITE_TYPE, favouriteType);
        cv.put(COL_NAME, name);
        cv.put(COL_CUSTOM_NAME, customName);
        cv.put(COL_DETAILS, details);
        cv.put(COL_LATITUDE, latitude);
        cv.put(COL_LONGITUDE, longitude);
        
        return cv;
	}
}
