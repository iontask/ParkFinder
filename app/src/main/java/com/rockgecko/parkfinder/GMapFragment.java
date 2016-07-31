package com.rockgecko.parkfinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import com.google.maps.android.kml.KmlLayer;
import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;
import com.rockgecko.parkfinder.map.HighResTileProvider;
import com.rockgecko.parkfinder.ptv.PtvApi;
import com.rockgecko.parkfinder.ptv.PtvDataFetcher;
import com.rockgecko.parkfinder.ptv.RoutePathFetcher;
import com.rockgecko.parkfinder.ptv.models.DtoPoi;
import com.rockgecko.parkfinder.ptv.models.DtoStopOutlet;
import com.rockgecko.parkfinder.dal.RoutePathResult;
import com.rockgecko.parkfinder.util.FuncEx;
import com.rockgecko.parkfinder.util.MapUtil;
import com.rockgecko.parkfinder.util.StringUtils;
import com.rockgecko.parkfinder.util.UIUtil;


public class GMapFragment extends SupportMapFragment implements IRunSelectionCallback,
	 OnMarkerClickListener, OnMapClickListener,
  //InfoWindowAdapter, OnInfoWindowClickListener,
    //LocationListener,
        OnCameraChangeListener{
	public static final int NEXT_DEPARTURES_LIMIT = 2;
	public static final int INITIAL_ZOOM = 15;
	public static final String MAP_MODE_BUNDLE_NAME = "GMapFragment.MapMode";
	public static final String TAG = "GMapFragment";
	
	private static final String TAG_OPTIONS_BUNDLE = "GMapFramgent.Options";
	private static final String TAG_PLOTTED_STOPS = "GMapFramgent.Stops";
	private static final String TAG_PLOTTED_RUNS = "GMapFramgent.Runs";
	private static final String KEY_SHOW_TILE_OVERLAY = "ShowTileOverlay";

	private GoogleMap mMapView;

	// These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    public static final LatLng MELBOURNE_CENTRE = new LatLng(-37.813611,144.963056);
    public static final LatLng BALLARAT_CENTRE = new LatLng(-37.5592153,143.8626583);
    public static final LatLng BENDIGO_CENTRE = new LatLng(-36.75618,144.2801258);
    public static final LatLng GEELONG_CENTRE = new LatLng(-38.1482595,144.3629658);
    public static final LatLng VICTORIA_CENTRE = new LatLng(-36.8541666,144.281111);
    /**
     * Max distance from the centre of Victoria to a PTV bus service
     */
    private static final float VICTORIA_MAX_DISTANCE_TO_SERVICES = 400*1000;
	private PtvDataFetcher mFetcher;
	
	private boolean shouldMoveToLocationOnStart=true;
	
	private LinkedHashMap<Marker, IStop> markerHashMap = new LinkedHashMap<Marker, IStop>();
	private LinkedHashMap<String, Polyline> runLineHashMap = new LinkedHashMap<String, Polyline>();
	private LinkedHashMap<String, RoutePathResult> routePathHashMap = new LinkedHashMap<>();
	private LinkedHashMap<String, List<Marker>> runLineArrowsHashMap = new LinkedHashMap<>();
	private HighResTileProvider mTileOverlayProvider;
	private KmlLayer mLayer;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		setRetainInstance(true);
		mFetcher = new PtvDataFetcher(getActivity());
		shouldMoveToLocationOnStart=savedInstanceState==null;
		if(savedInstanceState!=null){
			
		}
	}

	public GoogleMap getMap() {
		return mMapView;
	}

	@Override
	public void onViewCreated(View view, final Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(mMapView==null) {
			getMapAsync(new OnMapReadyCallback() {
				@Override
				public void onMapReady(GoogleMap googleMap) {

					mMapView = googleMap;
					int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
					if (googlePlayServicesAvailable != ConnectionResult.SUCCESS
							|| mMapView == null) {
						GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, getActivity(), -1, new DialogInterface.OnCancelListener() {

							@Override
							public void onCancel(DialogInterface dialog) {
								getActivity().finish();

							}
						}).show();
					} else {
						//mMapView.setPadding(0, ((ActionBarActivity) getActivity()).getSupportActionBar().getHeight(), 0, 0);
						//mMapView.setOnInfoWindowClickListener(this);
						//mMapView.setInfoWindowAdapter(this);
						mMapView.setOnMarkerClickListener(GMapFragment.this);
						mMapView.setOnMapClickListener(GMapFragment.this);
						mMapView.setMyLocationEnabled(true);
						// mMapView.setOnMyLocationButtonClickListener(this);
						mMapView.setOnCameraChangeListener(GMapFragment.this);
						UiSettings uiSettings = mMapView.getUiSettings();
						uiSettings.setRotateGesturesEnabled(false);
						uiSettings.setMapToolbarEnabled(false);

					}


					int mapMode;
					if (savedInstanceState != null) {
						mapMode = savedInstanceState.getInt(MAP_MODE_BUNDLE_NAME, GoogleMap.MAP_TYPE_NORMAL);
						Bundle options = savedInstanceState.getBundle(TAG_OPTIONS_BUNDLE);
						ArrayList<IStop> stops = (ArrayList<IStop>) options.getSerializable(TAG_PLOTTED_STOPS);
						for (IStop stop : stops) {
							plotStop(stop);
						}
						ArrayList<String> runs = options.getStringArrayList(TAG_PLOTTED_RUNS);
						for (String runId : runs) {
							plotRun(runId);
						}
					} else {
						mapMode = getArguments().getInt(MAP_MODE_BUNDLE_NAME, GoogleMap.MAP_TYPE_NORMAL);
					}
					mMapView.setMapType(mapMode);
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
					boolean showTileOverlay = sp.getBoolean(KEY_SHOW_TILE_OVERLAY, false);
					showTileOverlay(showTileOverlay);
					try {
						//mLayer = new KmlLayer(mMapView, getActivity().getAssets().open(fileName), getActivity().getApplicationContext());
						mLayer = ((MapActivity)getActivity()).getPark().getLayer();
						mLayer.setMap(mMapView,false);
						mLayer.addLayerToMap();
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					if (isResumed()) {
						getView().post(new Runnable() {
							@Override
							public void run() {
								if (shouldMoveToLocationOnStart) moveToLocationOnStart();
							}
						});
					}
				}

			});
		}
	}
	
	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		
	}

    @Override
    public void onResume(){
    	super.onResume();
    	//setUpMapIfNeeded();
    	setUpLocationClientIfNeeded();
		getView().post(new Runnable() {
			@Override
			public void run() {
				if (shouldMoveToLocationOnStart) moveToLocationOnStart();
			}
		});

	}
    private ParkListFragment.Park getPark(){
		return getActivity()==null?null:((MapActivity)getActivity()).getPark();
	}
    @Override
    public void onPause() {
        super.onPause();

    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        ArrayList<IStop> stops = new ArrayList<IStop>(markerHashMap.values());
        bundle.putSerializable(TAG_PLOTTED_STOPS, stops);
        bundle.putStringArrayList(TAG_PLOTTED_RUNS, new ArrayList<String>(runLineHashMap.keySet()));
        outState.putBundle(TAG_OPTIONS_BUNDLE, bundle);
    }

    
    private void setUpMapIfNeeded(){

    }
    @Deprecated
    private void setUpLocationClientIfNeeded() {
        /*
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(
                    getActivity().getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener
        }*/
    }
    
    @Override
    public void onDestroyView(){
    	super.onDestroyView();
    	//remove any existing tiles first for now
    	mMapView=null;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
    	super.onCreateOptionsMenu(menu, inflater);
    	inflater.inflate(R.menu.fragment_gmap, menu);

    }
    @Override
    public void onPrepareOptionsMenu(Menu menu){
		menu.findItem(R.id.menu_overlay_map).setChecked(mTileOverlay != null);
		super.onPrepareOptionsMenu(menu);
    }
    private TileOverlay mTileOverlay;
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
		Intent intent;
    	switch(item.getItemId()){    	  
    	case R.id.menu_nearest_stop:
    		if(mMapView!=null){
    			LatLng target = mMapView.getCameraPosition().target;
    			fetchNearestStops(target.latitude, target.longitude);
    		}
    		return true;
			case R.id.menu_overlay_map:
				boolean show = !item.isChecked();
				showTileOverlay(show);
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
				sp.edit().putBoolean(KEY_SHOW_TILE_OVERLAY, show).commit();
				return true;
    	}
    	return super.onOptionsItemSelected(item);
    }

	private void showTileOverlay(boolean show){
		if(mMapView==null) return;
		if(mTileOverlay==null && show){
			mTileOverlayProvider = new HighResTileProvider(getActivity(), getActivity().getResources().getDisplayMetrics().densityDpi);
			mTileOverlay = mMapView.addTileOverlay(new TileOverlayOptions().tileProvider(mTileOverlayProvider).zIndex(-1));
			mMapView.setMapType(GoogleMap.MAP_TYPE_NONE);
		}
		else if (!show && mTileOverlay!=null){
			mTileOverlay.remove();
			mTileOverlay=null;
			mTileOverlayProvider=null;
			mMapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			//item.setChecked(false);
		}
	}
    
    private void resetMarkerIcons(){

    	for(Entry<Marker, IStop> entry : markerHashMap.entrySet()){
			entry.getKey().setIcon(BitmapDescriptorFactory.fromResource(getIconForStop(entry.getValue(), false)));
			entry.getKey().setAnchor(111f/120f, 1f);
		}
    }
    
    private int getIconForStop(IStop stop, boolean isSelected){

    	return isSelected?R.drawable.busstop_lg:R.drawable.busstop_sm;
    }
    
    private void clearMarkers(){
    	for(Marker marker : markerHashMap.keySet()){
			marker.remove();
		}
		markerHashMap.clear();
    }
    
    private void clearLines(){
    	for(Polyline line : runLineHashMap.values()){
    		line.remove();
    	}
    	runLineHashMap.clear();
        for(List<Marker> arrows:runLineArrowsHashMap.values()){
            for(Marker arrow:arrows){
                arrow.remove();
            }
        }
        runLineArrowsHashMap.clear();
    }
    private LatLng mLastNearbyCentre=new LatLng(0,0);
    private void fetchNearbyStops(){
    	VisibleRegion visibleRegion = mMapView.getProjection().getVisibleRegion();
    	LatLng centre = visibleRegion.latLngBounds.getCenter();
    	if(mLastNearbyCentre==null){
    		mLastNearbyCentre=centre;
    		fetchStopsWithinBounds(visibleRegion.farLeft, visibleRegion.farRight,
        			visibleRegion.nearLeft, visibleRegion.nearRight);
    	}
    	else{
	    	float[] screenWidth = new float[1];
	    	Location.distanceBetween(visibleRegion.farLeft.latitude, visibleRegion.farLeft.longitude,
	    			visibleRegion.farRight.latitude, visibleRegion.farRight.longitude, screenWidth);
	    	float[] screenHeight = new float[1];
	    	Location.distanceBetween(visibleRegion.farLeft.latitude, visibleRegion.farLeft.longitude,
	    			visibleRegion.nearLeft.latitude, visibleRegion.nearLeft.longitude, screenHeight);
	    	float minXY = Math.min(screenWidth[0], screenHeight[0]);
	    	float[] distanceFromLast = new float[1];
	    	Location.distanceBetween(mLastNearbyCentre.latitude, mLastNearbyCentre.longitude,
	    			centre.latitude, centre.longitude, distanceFromLast);
	    	if(distanceFromLast[0]>(minXY/2f)){
	    		mLastNearbyCentre=centre;
	    		fetchStopsWithinBounds(visibleRegion.farLeft, visibleRegion.farRight,
	    			visibleRegion.nearLeft, visibleRegion.nearRight);
	    	}
    	}
    }
    private void fetchStopsWithinBounds(final LatLng farLeft, final LatLng farRight,
    		final LatLng nearLeft, final LatLng nearRight){
        int[] modes=new int[]{PtvApi.POI_BUS};
        //if(mShowNightrider) modes= new int[]{PtvApi.POI_BUS, PtvApi.POI_NIGHTRIDER};
        //else modes = new int[]{PtvApi.POI_BUS};
    	mFetcher.getBusStopsWithinBounds(modes, farLeft.latitude, farLeft.longitude,
    			nearRight.latitude, nearRight.longitude, 
    			new AjaxCallback<DtoPoi>(){
    		@Override
    		 public void callback(String url, DtoPoi result, AjaxStatus status) {     			 
    			if(result==null){
    				Toast.makeText(getActivity(), getString(R.string.request_failed_format, "stops"), Toast.LENGTH_SHORT).show();
    				mLastNearbyCentre=null;
    			}
    			else{
    				if(result.getTotalLocations()==0){
    					boolean minZoom = mMapView!=null && mMapView.getCameraPosition().zoom<=INITIAL_ZOOM;
    					//Toast.makeText(getActivity(), minZoom?R.string.no_stops_nearby_min_zoom:R.string.no_stops_nearby, Toast.LENGTH_SHORT).show();
    				}
    				else for(IStop stop : result.getLocationsOrClusterConcat()){    		
    		    		plotStop(stop);
    		    	}
    				if(ParkFinderApplication.DEBUG && false){
    					PolylineOptions line = new PolylineOptions().add(farLeft)
    						.add(farRight)    						
    						.add(nearRight)
    						.add(nearLeft)
    						.add(farLeft)
    						.width(UIUtil.scale(2f))
    						.color(UIUtil.getColour(url));
    					mMapView.addPolyline(line);
    						
    				}
    			}
    			
    		 }
    		
    	});
		
    }
    
    private void fetchNearestStops(final double latitude, final double longitude){
    	mFetcher.getNearestBusStops(latitude, longitude, new AjaxCallback<List<IStop>>(){
    		@Override
    		public void callback(String url, List<IStop> result, AjaxStatus status) {	
    			if(result==null){
    				Toast.makeText(getActivity(), getString(R.string.request_failed_format, "nearest stop"), Toast.LENGTH_SHORT).show();
    			}
    			else{
    				if(result.size()==0){
    					Toast.makeText(getActivity(), R.string.no_nearest_stop, Toast.LENGTH_SHORT).show();
    				}
    				else{
    					for (IStop stop : result){    				
    						plotStop(stop);    					
    					}
    					if(mMapView!=null){
    						//go to the nearest stop.
    						CameraUpdate update = CameraUpdateFactory.newLatLng(new LatLng(result.get(0).getLatitude(), result.get(0).getLongitude()));
    						mMapView.animateCamera(update);
    						IStop existingOrNewStop = findStopOnMap(result.get(0).getId());
    						Marker marker = findMarker(existingOrNewStop);
    						onMarkerClick(marker);
    					}
    				}
    			}
    		}
    	});
    }
    

    /**
     * Add new services into a possibly existing stop
     * @param stop with services
     * @param overwriteServices
     * @return The stop that is in the markerhashmap
     */
    private IStop updateStopServices(IStop stop, boolean overwriteServices){
    	IStop existingStop=findStopOnMap(stop.getId());
    	
    	if(existingStop==null){
    		plotStop(stop);
    		existingStop = stop;
    	}
    	else{
            if(existingStop instanceof DtoStopOutlet) {
                if (!existingStop.hasCompleteServices(false) && stop.hasCompleteServices(false)) {
                    ((DtoStopOutlet) existingStop).setHasCompleteServices(true);
                }
                if (overwriteServices) {
                    ((DtoStopOutlet) existingStop).setHasFullDayCompleteServices(stop.hasCompleteServices(true));
                }
                if(existingStop.getMode()==IStop.MODE_NIGHTRIDER && stop.getMode()==IStop.MODE_BUS){
                    ((DtoStopOutlet) existingStop).setMode(IStop.MODE_BUS); //BUS has priority over Nightrider
                }
            }
    	}
    	//foundStop.setServices(services);    	
    	if(existingStop.getServices()==null) existingStop.setServices(new ArrayList<StopService>());
    	else if(existingStop.getServices()!=stop.getServices() && overwriteServices){
    		//check its not the same object first
    		ListIterator<StopService> serviceIterator = existingStop.getServices().listIterator();
    		while(serviceIterator.hasNext()){
    			StopService service = serviceIterator.next();
    			//Services where an entire run has been fetched should remain in the list,
    			//so that they can still be un-checked
    			//if(service.getOrder()==0) serviceIterator.remove();
    			if(!isRunSelected(service)) serviceIterator.remove();
    		}
    			
    		
    		//existingStop.getServices().clear();
    	}
    	for(StopService service : stop.getServices()){
    		StopService existingService = existingStop.findServiceByRunId(service.getRun().getRun_id());
			if(existingService==null){
    			existingStop.getServices().add(service);
    		}
			else{
				if(existingService.getOrder()==0) existingService.setOrder(service.getOrder());
				
			}
    	}
    	return existingStop;
    }
    
   private IStop findStopOnMap(String stopId){
	   for(IStop markerStop : markerHashMap.values()){
	   		if(markerStop.getId().equals(stopId)){
	   			return markerStop;
	   		}
	   }
   	return null;
   	
   }
   
   private Marker findMarker(IStop stop){
	   for(Entry<Marker,IStop> entry : markerHashMap.entrySet()){
		   if(entry.getValue()==stop){
			   return entry.getKey();			   
		   }
	   }
	   return null;
   }
   
   public void fetchStop(IStop stop, int nextDeparturesLimit, final boolean forceRefresh, final boolean thenShow, final boolean thenZoomIn, 
		   final boolean thenSelect, final IActionComplete<IStop> callback){
	   if(thenShow){
		   float zoom = thenZoomIn?INITIAL_ZOOM:mMapView.getCameraPosition().zoom;
		   CameraUpdate update= CameraUpdateFactory.newLatLngZoom(new LatLng(stop.getLatitude(), stop.getLongitude()), zoom);
		   mMapView.animateCamera(update);
	   }
	   fetchStop(stop.getId(), stop.getMode(), nextDeparturesLimit, forceRefresh, thenShow, thenZoomIn, thenSelect, callback);
   }

   @Override
   public void getStopAndServices(String stopId, int mode, int nextDeparturesLimit, boolean forceRefresh, IActionComplete<IStop> callback){
	   fetchStop(stopId, mode, nextDeparturesLimit, forceRefresh, false, false, false, callback);
   }
	private static final DtoStopOutlet EMPTY_STOP = new DtoStopOutlet();
   public void fetchStop(String stopId, int mode, int nextDeparturesLimit, final boolean forceRefresh, final boolean thenShow, final boolean thenZoomIn,
		   final boolean thenSelect, final IActionComplete<IStop> callback){
        if(mode==-1) mode=IStop.MODE_BUS;
	   final IActionComplete<IStop> action = new IActionComplete<IStop>() {
		   @Override
		   public void done(IStop result) {
			   if(result==null){
				   if(callback!=null) callback.done(null);
				   return;
			   }
			   if(thenShow){
				   float zoom = thenZoomIn?INITIAL_ZOOM:mMapView.getCameraPosition().zoom;
				   CameraUpdate update= CameraUpdateFactory.newLatLngZoom(new LatLng(result.getLatitude(), result.getLongitude()), zoom);
				   mMapView.animateCamera(update);
			   }
			   if(thenSelect){						   
				   Marker marker = findMarker(result);
				   onMarkerClick(marker);
			   }		
			   if(callback!=null){
				   callback.done(result);
			   }
		   }
	   };
	   IStop stop=findStopOnMap(stopId);

	   if(stop!=null &&!forceRefresh){ //&& stop.hasCompleteServices()
		   action.done(stop);
	   }	   
	   else {
		   //do we already have the stop and need services, or do we need the stop?
		   final String requestingFormat = stop == null ? "stop" : "services";
		   final HashSet<Integer> modes = new HashSet<>(FuncEx.toIntList(mode));
		   final LinkedHashMap<Integer, IStop> completedModes = new LinkedHashMap<>();
		   if (stop instanceof DtoStopOutlet) modes.addAll(((DtoStopOutlet) stop).getModes());
		   final boolean mightHaveRegularBus = modes.contains(IStop.MODE_NIGHTRIDER) && !modes.contains(IStop.MODE_BUS);
		   if (mightHaveRegularBus) {
			   modes.add(IStop.MODE_BUS);
		   }
		   for (final int lMode : modes) {
			   mFetcher.getServicesForStop(lMode, stopId, new AjaxCallback<IStop>() {
				   @Override
				   public void callback(String url, IStop result, AjaxStatus status) {
					   if (result == null) {
						   if(mightHaveRegularBus && lMode== IStop.MODE_BUS){
							   //was a guess
						   }
						   else{
							   Toast.makeText(getActivity(), getString(R.string.request_failed_format, requestingFormat), Toast.LENGTH_SHORT).show();
						   }
						   completedModes.put(lMode, EMPTY_STOP);
					   } else {
						   IStop mapStop = updateStopServices(result, forceRefresh && (completedModes.size()==0 || (completedModes.size()==1 && completedModes.containsValue(EMPTY_STOP))));
						   completedModes.put(lMode, mapStop);
					   }
					   if(completedModes.size()==modes.size()){
						   IStop cbResult = completedModes.get(lMode);
						   if(cbResult==EMPTY_STOP){
							   cbResult=null;
							   //Log.d(TAG, "map can contain null");
							   for(Entry<Integer, IStop> entry : completedModes.entrySet()){
								   if(entry.getValue()!=null && entry.getValue()!=EMPTY_STOP){
									   cbResult=entry.getValue();
									   break;
								   }
							   }
						   }
						   action.done(cbResult);
					   }
				   }
			   }, nextDeparturesLimit);
		   }
	   }
   }

	private void plotStop(IStop stop) {
        IStop existingStop = findStopOnMap(stop.getId());
        if(existingStop instanceof DtoStopOutlet){
            if(existingStop.getMode()!=stop.getMode()){
				if(!((DtoStopOutlet) existingStop).getModes().contains(stop.getMode())) {
					((DtoStopOutlet) existingStop).setMode(IStop.MODE_BUS); //BUS has priority over Nightrider
					((DtoStopOutlet) existingStop).addMode(IStop.MODE_NIGHTRIDER);
				}
            }
        }
        else{
			if(ParkFinderApplication.DEBUG) Log.d("Plot", "plotting stop "+stop.getId());
			MarkerOptions markerOpts = new MarkerOptions();
			//markerOpts.title(stop.getName()).snippet(stop.getDetails()).position(new LatLng(stop.getLatitude(), stop.getLongitude()));
			markerOpts.position(new LatLng(stop.getLatitude(), stop.getLongitude()));
			markerOpts.icon(BitmapDescriptorFactory.fromResource(getIconForStop(stop, false)))
					.anchor(111f/120f, 1f).infoWindowAnchor(10f/120f, 1f);
			Marker marker = mMapView.addMarker(markerOpts);
			markerHashMap.put(marker, stop);
		}
	}
	static final int ARROW_INTERVAL_STOPS = 4;
	static final double ARROW_INTERVAL_METERS = 1000;
	static final double ARROW_MIN_DISTANCE_FROM_STOP_METERS = 100;
	private void plotRun(final String runId){
		Polyline polyLine = runLineHashMap.get(runId);
		if(polyLine!=null){
			//remove existing from map
			polyLine.remove();			
		}
        List<IStop> stops = getStopsForRun(runId);
        if (stops.size() > 0) {
            RoutePathResult routePathResult = routePathHashMap.get(runId);

            if(routePathResult==null){
                getRoutePathForRun(runId);
            }
            else if (routePathResult!=ROUTE_PATH_RESULT_PENDING && routePathResult.polyline!=null){
                plotRoutePathResultPolyLine(runId, stops, routePathResult);
            }

            else { //Pending or empty result
                int color = stops.get(0).findServiceByRunId(runId).getColourForLineAndDirection();
                //use the one bitmap for the run. Colour it to the run by setting a colorFilter and drawing to a canvas.
                Bitmap bmp = getArrowBitmap(color);

                PolylineOptions line = new PolylineOptions();
                List<Marker> arrowMarkers = new ArrayList<>();
                for (int i=0; i<stops.size();i++) {
                    IStop stop = stops.get(i);
                    LatLng thisLatLng = new LatLng(stop.getLatitude(), stop.getLongitude());
                    line.add(thisLatLng);
                    if(i>0 && i% ARROW_INTERVAL_STOPS ==0 && i<stops.size()-1 ){
                        IStop next = stops.get(i+1);
                        LatLng nextLatLng = new LatLng(next.getLatitude(), next.getLongitude());
                        double heading = SphericalUtil.computeHeading(thisLatLng, nextLatLng);
                        LatLng halfWay = SphericalUtil.interpolate(thisLatLng, nextLatLng, 0.5d);
                        MarkerOptions markerOpts = new MarkerOptions();
                        markerOpts.position(halfWay);

                        markerOpts.icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                .anchor(0.5f, 0.5f)
                                .rotation((float) heading)
                                .flat(true);
                        Marker marker = mMapView.addMarker(markerOpts);
                        arrowMarkers.add(marker);
                    }
                }


                line.color(color);
                polyLine = mMapView.addPolyline(line);
                runLineHashMap.put(runId, polyLine);
                runLineArrowsHashMap.put(runId, arrowMarkers);

            }
        }
        else {
            runLineHashMap.remove(runId);
            runLineArrowsHashMap.remove(runId);
        }

	}

    private Bitmap getArrowBitmap(int colour){
        //use the one bitmap for the run. Colour it to the run by setting a colorFilter and drawing to a canvas.
        BitmapDrawable icon = (BitmapDrawable) getResources().getDrawable(R.drawable.arrow_logo).mutate();
        icon.setColorFilter(colour, PorterDuff.Mode.SRC_IN);

        Bitmap bmp = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);
        return bmp;
    }



    static final double MAX_DIST_FROM_PATH = 50;
    private boolean acceptPathProd(List<IStop> legStops, List<LatLng> legLine){
        for(IStop stop : legStops){
            if(!PolyUtil.isLocationOnPath(new LatLng(stop.getLatitude(), stop.getLongitude()), legLine, true, MAX_DIST_FROM_PATH)) {
                return false;
            }
        }
        return true;
    }




    private void plotRoutePathResultPolyLine(String runId, List<IStop> stops, RoutePathResult routePathResult){
        int colour=stops.get(0).findServiceByRunId(runId).getColourForLineAndDirection();
        Bitmap bmp = getArrowBitmap(colour);
        PolylineOptions line = new PolylineOptions();
        List<Marker> arrowMarkers = runLineArrowsHashMap.get(runId);
        if(arrowMarkers!=null) for (Marker marker : arrowMarkers){
            marker.remove();
        }
        else arrowMarkers=new ArrayList<>();
        double segmentDistance=0;
        List<LatLng> points = PolyUtil.decode(routePathResult.polyline);
        for (int i=0;i<points.size();i++) {
            LatLng point = points.get(i);
            line.add(point);
            if(i>0) {
                double intervalDistance = SphericalUtil.computeDistanceBetween(points.get(i - 1), point);
                segmentDistance+=intervalDistance;
                if (i > 0 && segmentDistance > ARROW_INTERVAL_METERS && intervalDistance>2d && i < points.size() - 1) {
                    LatLng halfWay = SphericalUtil.interpolate(points.get(i - 1), point, 0.5d);
                    double minDistance=Double.MAX_VALUE;
                    for(IStop stop : stops){
                        double pointToStop = SphericalUtil.computeDistanceBetween(halfWay, new LatLng(stop.getLatitude(), stop.getLongitude()));
                        if(pointToStop<minDistance){
                            minDistance=pointToStop;
                        }
                        else if ( pointToStop>ARROW_INTERVAL_METERS) break;
                        if(minDistance<=ARROW_MIN_DISTANCE_FROM_STOP_METERS) break;
                    }
                    if(minDistance>ARROW_MIN_DISTANCE_FROM_STOP_METERS) {
                        double heading = SphericalUtil.computeHeading(points.get(i - 1), point);

                        MarkerOptions markerOpts = new MarkerOptions();
                        markerOpts.position(halfWay);

                        markerOpts.icon(BitmapDescriptorFactory.fromBitmap(bmp))
                                .anchor(0.5f, 0.5f)
                                .rotation((float) heading)
                                .flat(true);
                        Marker marker = mMapView.addMarker(markerOpts);
                        arrowMarkers.add(marker);
                        segmentDistance = 0;
                    }
                    else{
                        segmentDistance-=ARROW_MIN_DISTANCE_FROM_STOP_METERS;
                    }
                }
            }
        }

        line.color(colour);
        Polyline polyLine = mMapView.addPolyline(line);
        runLineHashMap.put(runId, polyLine);
        runLineArrowsHashMap.put(runId, arrowMarkers);
    }


    private static final RoutePathResult ROUTE_PATH_RESULT_PENDING = new RoutePathResult();
    private void getRoutePathForRun(final String runId) {
        routePathHashMap.put(runId, ROUTE_PATH_RESULT_PENDING);
        List<IStop> stops = getStopsForRun(runId);

        StopService service = stops.get(0).findServiceByRunId(runId);
        String routeNo = service.getDirection().getLine().getLine_number_formatted();

        new RoutePathFetcher().getPathForRouteAsync(routeNo, new LatLng(stops.get(0).getLatitude(), stops.get(0).getLongitude()),
                new LatLng(stops.get(stops.size() - 1).getLatitude(), stops.get(stops.size() - 1).getLongitude()),
                new IActionComplete<RoutePathResult>() {
                    @Override
                    public void done(RoutePathResult result) {
                        if (getActivity() != null && mMapView != null) {
                            routePathHashMap.put(runId, result);
                            plotRun(runId);
                        }
                    }
                });
    }

	private boolean hasCompleteRun(String runId){
		List<IStop> stopsForRun = getStopsForRun(runId);
		return stopsForRun.size()>0 && stopsForRun.get(0).findServiceByRunId(runId).getOrder()>0;
	}
	
	@Override
	public void fetchCompleteRun(StopService stopService, IActionComplete<List<IStop>> callback){
		String runId = stopService.getRun().getRun_id();
		if(hasCompleteRun(runId)) callback.done(getStopsForRun(runId));
		else{
			fetchSingleRunForStop(stopService, callback);
		}
	}
	public List<IStop> getStopsForRun(final String runId){
		List<IStop> result = new ArrayList<IStop>();
		 for(IStop markerStop : markerHashMap.values()){
			 StopService service = markerStop.findServiceByRunId(runId);
			 if(service!=null) result.add(markerStop);
		 }
		 Collections.sort(result, new Comparator<IStop>() {

			 @Override
			 public int compare(IStop lhs, IStop rhs) {
				 //StopService leftService = null, rightService = null;
				 StopService leftService = lhs.findServiceByRunId(runId);
				 StopService rightService = rhs.findServiceByRunId(runId);
				 return leftService.getOrder() - rightService.getOrder();
				/*				
				int compare = leftService.getTime().compareTo(rightService.getTime());
				if(compare==0){					
					return leftService.getOrder()-rightService.getOrder();
				}
				return compare;*/
			 }
		 });
		 return result;		 
	}
	
	private void fetchSingleRunForStop(StopService stopService, final IActionComplete<List<IStop>> callback){
		String stopId = stopService.getStopId();
		final String runId = stopService.getRun().getRun_id();
        int mode = stopService.getDirection().getLine().getTransport_type_int();
		mFetcher.getSingleRunForStop(mode, stopId, runId, new AjaxCallback<List<IStop>>() {
			@Override
			public void callback(String url, List<IStop> result, AjaxStatus status) {
				if (StringUtils.isNullOrEmpty(result)) {
					if (result == null)
						Toast.makeText(getActivity(), getString(R.string.request_failed_format, "run"), Toast.LENGTH_SHORT).show();
					if (callback != null) callback.done(result);
					return;
				}
				for (IStop stop : result) {
					updateStopServices(stop, false);
				}
				if (callback != null) callback.done(getStopsForRun(runId));
			}
		}, stopService.getTime().getTime());
	}
	
	
/*
	@Override
	public View getInfoContents(Marker marker) {
		
		return null;

	}

	@Override
	public View getInfoWindow(Marker marker) {				
			
		return null;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {

	}*/
	
	private void selectStop(IStop stop){
		((MapActivity) getActivity()).selectStop(stop);
		if(stop==null){
			resetMarkerIcons();
		}
	}


	@Override
	public void setRunSelected(final StopService stopService, boolean selected) {
		if(selected && !isRunSelected(stopService)){
			if(hasCompleteRun(stopService.getRun().getRun_id()))
				plotRun(stopService.getRun().getRun_id());
			else{
				//fetch run
				fetchSingleRunForStop(stopService, 
						new IActionComplete<List<IStop>>() {						
							@Override
							public void done(List<IStop> result) {								
								plotRun(stopService.getRun().getRun_id());
							}
						});				
			}
		}
		else{
			Polyline plottedLine = runLineHashMap.get(stopService.getRun().getRun_id());
			if(plottedLine!=null){
				plottedLine.remove();
				runLineHashMap.remove(stopService.getRun().getRun_id());
				List<Marker> arrows = runLineArrowsHashMap.remove(stopService.getRun().getRun_id());
                if(arrows!=null) for(Marker arrow: arrows){
                    arrow.remove();
                }
			}
		}
		
	}
	
	

	@Override
	public boolean isRunSelected(StopService stopService) {		
		return runLineHashMap.containsKey(stopService.getRun().getRun_id());		
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		final IStop clickedStop=markerHashMap.get(marker);
        if(clickedStop==null){
			if(!StringUtils.isNullOrEmpty(marker.getTitle())) return false;
			return true; //Clicked an arrow instead, do nothing.
		}

		if(!clickedStop.hasCompleteServices(false)) {
			HashSet<Integer> modes = new HashSet<>(FuncEx.toIntList(clickedStop.getMode()));
			if (clickedStop instanceof DtoStopOutlet) modes.addAll (((DtoStopOutlet) clickedStop).getModes());
			final boolean mightHaveRegularBus =modes.contains(IStop.MODE_NIGHTRIDER) && !modes.contains(IStop.MODE_BUS);
			if(mightHaveRegularBus){
				modes.add(IStop.MODE_BUS);
			}
			for (final int mode : modes) {
				mFetcher.getServicesForStop(mode, clickedStop.getId(), new AjaxCallback<IStop>() {
					@Override
					public void callback(String url, IStop result, AjaxStatus status) {
						if (result == null) {
							if(mightHaveRegularBus && mode== IStop.MODE_BUS){
								//was a guess
							}
							else {
								Toast.makeText(getActivity(), getString(R.string.request_failed_format, "services"), Toast.LENGTH_SHORT).show();
							}
						} else {
							updateStopServices(result, false);
							selectStop(clickedStop);
						}
					}

				}, NEXT_DEPARTURES_LIMIT);
			}
		}
		selectStop(clickedStop);
		resetMarkerIcons();
		marker.setIcon(BitmapDescriptorFactory.fromResource(getIconForStop(clickedStop, true)));//.anchor(111f/120f, 1f)));
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {		
		selectStop(null);
	}




	private void moveToLocationOnStart() {
        // mLocationClient.requestLocationUpdates(REQUEST, this);

        if (shouldMoveToLocationOnStart) try {
			if(mLayer!=null&&mMapView!=null) {
				LatLngBounds window = MapUtil.getWindow(mLayer);
				if (window != null)
					mMapView.moveCamera(CameraUpdateFactory.newLatLngBounds(window, UIUtil.scaleLayoutParam(20)));
				shouldMoveToLocationOnStart = false;
			}
            mLastNearbyCentre = null;
            //fetchNearbyStops();
        } catch (IllegalStateException | NullPointerException e) { //NullPointerException: CameraUpdateFactory is not initialized
            e.printStackTrace();
        }
    }



	@Override
	public void onCameraChange(CameraPosition position) {
		if(position.zoom>=INITIAL_ZOOM){
			fetchNearbyStops();
		}
        if(mTileOverlay!=null){
			boolean hasTiles = mTileOverlayProvider.hasTiles(position.target, position.zoom);
			Log.d(TAG, "Has tiles for zoom "+position.zoom+": "+hasTiles);
            mMapView.setMapType(hasTiles
					? GoogleMap.MAP_TYPE_NONE : GoogleMap.MAP_TYPE_NORMAL);

        }
		
	}

	
}
