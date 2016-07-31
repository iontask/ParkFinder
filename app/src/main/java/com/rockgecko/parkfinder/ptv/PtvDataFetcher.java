package com.rockgecko.parkfinder.ptv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.Transformer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.rockgecko.parkfinder.dal.GsonTransformer;
import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;
import com.rockgecko.parkfinder.dal.TZDateTypeAdapter;
import com.rockgecko.parkfinder.ptv.models.DtoDeparturesResponse;
import com.rockgecko.parkfinder.ptv.models.DtoDeparturesValues;
import com.rockgecko.parkfinder.ptv.models.DtoNearestStopsResponse;
import com.rockgecko.parkfinder.ptv.models.DtoPoi;
import com.rockgecko.parkfinder.ptv.models.DtoStopOutlet;
import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.util.StringUtils;

import net.servicestack.func.Func;
import net.servicestack.func.Predicate;

public class PtvDataFetcher {
	public static final String TAG = "PTV";

	private AQuery aq;
	private GsonTransformer gsonTransformer;
	private StopServiceTransformer stopTransformer;
	public PtvDataFetcher(Activity act){
		aq = new AQuery(act);
		gsonTransformer = new GsonTransformer();
		stopTransformer = new StopServiceTransformer(gsonTransformer);
	}
    public PtvDataFetcher(Context context){
        aq = new AQuery(context);
        gsonTransformer = new GsonTransformer();
        stopTransformer = new StopServiceTransformer(gsonTransformer);
    }
	
	public void getNearestBusStops(double latitude, double longitude, final AjaxCallback<List<IStop>> callback){
		String url = PtvApi.nearestStops(latitude, longitude);
		log(url);		
		aq.transformer(new NearestStopsTransformer(gsonTransformer, new int[]{PtvApi.POI_BUS})).ajax(url, DtoStopOutletList.class, new AjaxCallback<DtoStopOutletList>(){
			@Override
			public void callback(String url, DtoStopOutletList result, AjaxStatus status) {	
				callback.callback(url, result, status);
			}
		}
				);
	}


	
	public void getBusStopsWithinBounds(int[] modes, double lat1, double lon1, double lat2, double lon2, AjaxCallback<DtoPoi> callback){
		String url = PtvApi.poi(modes, lat1, lon1, lat2, lon2, 1, 15);
		log(url);
		aq.transformer(gsonTransformer).ajax(url, DtoPoi.class, callback);
	}
	


	public void getServicesForStop(int mode, final String stopId, AjaxCallback<IStop> callback, int limit){
        String nextDeparturesUrl = PtvApi.nextDeparturesForStop(mode, stopId, limit);
        log(nextDeparturesUrl);
        //Put IStop in as the return class type is ok, stopTransformer ignores it anyway.
        Transformer transformer = new FullDayDeparturesForStopTransformer(gsonTransformer, limit==0);
        aq.transformer(transformer).ajax(nextDeparturesUrl, IStop.class, callback);
    }

    public void getSpecificServicesForStop(int mode, final String lineId, final String stopId, final String directionId, AjaxCallback<IStop> callback, int limit, long forTime){
        String nextDeparturesUrl = PtvApi.specificNextDeparturesForStop(mode, lineId, stopId, directionId, limit, forTime);
        log(nextDeparturesUrl);
        //Put IStop in as the return class type is ok, stopTransformer ignores it anyway.
        aq.transformer(stopTransformer).ajax(nextDeparturesUrl, IStop.class, callback);
    }


	
	/**
	 * Get an entire run for a run id.
	 * @param stopId
	 * @param runId
	 * @param callback
	 * @param forTime the time this run stops at this stop, or 0. Used to correct the date of returned stop times
	 */
	public void getSingleRunForStop(int mode, final String stopId, final String runId, final AjaxCallback<List<IStop>> callback, final long forTime){
		MultiStopServiceTransformer multiTransformer = new MultiStopServiceTransformer(gsonTransformer);
		String url = PtvApi.stoppingPattern(mode, runId, stopId, forTime);
		log(url);
		aq.transformer(multiTransformer).ajax(url, DtoStopOutletList.class, new AjaxCallback<DtoStopOutletList>() {
			@Override
			public void callback(String url, DtoStopOutletList result, AjaxStatus status) {
				if (result != null && forTime > 0) {
					//correct the Date of returned stop times
					long timeDifference = 0;
					for (IStop stop : result) {
						if (stopId.equals(stop.getId())) {
							//Find the wrong time, compare with forTime for stopId.
							//There's only 1 service per stop here.
							timeDifference = forTime - stop.getServices().get(0).getTime().getTime();
						}
					}
					for (IStop stop : result) {
						stop.getServices().get(0).setTime(new Date(stop.getServices().get(0).getTime().getTime() + timeDifference));
					}
				}
				callback.callback(url, result, status);
			}
		});
	}
	

	public static void log(String msg){
		if(ParkFinderApplication.DEBUG) Log.d(TAG, msg);
	}
	
	
	/**
	 * 
	 * Transformer for results whose stop id is always the same. Returns one DtoStopOutlet with a list of services.
	 *
	 */
	class StopServiceTransformer implements Transformer{
		private GsonTransformer _gsonTransformer;
		public StopServiceTransformer(GsonTransformer gsonTransformer) {
			_gsonTransformer = gsonTransformer;
		}
		@Override
		public <T> T transform(String url, Class<T> type, String encoding, byte[] data, AjaxStatus status){
			//if(type!=List<DtoStopOutlet>.class) throw new RuntimeException("StopServiceTransformer converts List<DtoDeparturesResponse> to List<DtoStopOutlet> only");
			DtoDeparturesResponse ptvResponse = _gsonTransformer.transform(url, DtoDeparturesResponse.class, encoding, data, status);
			if(ptvResponse==null || StringUtils.isNullOrEmpty(ptvResponse.values)) return null;
			DtoStopOutlet result = ptvResponse.values.get(0).getPlatform().getStop();
			result.setHasCompleteServices(true);
			List<StopService> services = new ArrayList<StopService>();
			result.setServices(services);
			
			for(int i=0;i<ptvResponse.values.size();i++){
				DtoDeparturesValues value = ptvResponse.values.get(i);												
			
				StopService stopService = new StopService();
				stopService.setStopId(value.getPlatform().getStop().getId());
				stopService.setDirection(value.getPlatform().getDirection());
				stopService.setRun(value.getRun());
				stopService.setTime(value.getTime_timetable_utc());
				stopService.setRealtime(value.getTime_realtime_utc());
				services.add(stopService);
			//}
			
			}
			
			return (T) result;
		}
		
	}
	class FullDayDeparturesForStopTransformer extends StopServiceTransformer{
		boolean fullDayCompleteDepartures;
		public FullDayDeparturesForStopTransformer(
				GsonTransformer gsonTransformer, boolean fullDayCompleteDepartures) {
			super(gsonTransformer);
			this.fullDayCompleteDepartures=fullDayCompleteDepartures;
		}
		@Override
		public <T> T transform(String url, Class<T> type, String encoding, byte[] data, AjaxStatus status){
            DtoStopOutlet result = (DtoStopOutlet) super.transform(url, type, encoding, data, status);
			if(result!=null) {
				result.setHasFullDayCompleteServices(fullDayCompleteDepartures);
			}
			return (T)result;
		}
	}
	
	/**
	 * Transformer for a list of different stops. Returns a DtoStopOutletList of stops, with one service per stop. 
	 *
	 */
	class MultiStopServiceTransformer implements Transformer{
		private GsonTransformer _gsonTransformer;
		public MultiStopServiceTransformer(GsonTransformer gsonTransformer) {
			_gsonTransformer = gsonTransformer;
		}
		@Override
		public <T> T transform(String url, Class<T> type, String encoding, byte[] data, AjaxStatus status){
			DtoDeparturesResponse ptvResponse = _gsonTransformer.transform(url, DtoDeparturesResponse.class, encoding, data, status);
			if(ptvResponse==null || StringUtils.isNullOrEmpty(ptvResponse.values)) return null;
			DtoStopOutletList results = new DtoStopOutletList();
			for(int i=0;i<ptvResponse.values.size();i++){
				DtoDeparturesValues value = ptvResponse.values.get(i);

				DtoStopOutlet stop = value.getPlatform().getStop();
				List<StopService> services= new ArrayList<StopService>();
				stop.setServices(services);			
			
				StopService stopService = new StopService();
				stopService.setStopId(value.getPlatform().getStop().getId());
				stopService.setDirection(value.getPlatform().getDirection());
				stopService.setRun(value.getRun());
				stopService.setTime(value.getTime_timetable_utc());
				stopService.setRealtime(value.getTime_realtime_utc());
				//For keeping the run in order. Dont start at 0.
				stopService.setOrder(i+1);
				services.add(stopService);
			//}
			results.add(stop);
			}
						

			return (T) results;
		}

		
	}
	
	class NearestStopsTransformer implements Transformer{
		private GsonTransformer _gsonTransformer;
		private int[] _modeFilter;
		public NearestStopsTransformer(GsonTransformer gsonTransformer, int[] modeFilter) {
			_gsonTransformer = gsonTransformer;
			_modeFilter=modeFilter;			
		}
		@Override
		public <T> T transform(String url, Class<T> type, String encoding, byte[] data, AjaxStatus status){
			DtoNearestStopsResponseList response = _gsonTransformer.transform(url, DtoNearestStopsResponseList.class, encoding, data, status);
			if(response!=null){
				DtoStopOutletList results = new DtoStopOutletList();
				for(DtoNearestStopsResponse item : response){
					if(item.isStop() && StringUtils.intArrayContains(_modeFilter, item.result.getMode())){
						results.add(item.result);
					}
				}
				return (T) results;
			}
			return null;			
		}
	}
	
	static class DtoNearestStopsResponseList extends ArrayList<DtoNearestStopsResponse>{
		
	}
	
	static class DtoStopOutletList extends ArrayList<IStop> {
		
	}
}
