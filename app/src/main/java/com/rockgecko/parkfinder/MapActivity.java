package com.rockgecko.parkfinder;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.rockgecko.parkfinder.dal.FavouriteStop;
import com.rockgecko.parkfinder.dal.GsonTransformer;
import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;

import com.rockgecko.parkfinder.ptv.PtvApi;
import com.rockgecko.parkfinder.ptv.PtvDataFetcher;

import com.rockgecko.parkfinder.util.StringUtils;
import com.rockgecko.parkfinder.util.UIUtil;
import com.rockgecko.parkfinder.views.SlidingDrawer;
import com.rockgecko.parkfinder.views.SlidingDrawer.OnDrawerCloseListener;
import com.rockgecko.parkfinder.views.SlidingDrawer.OnDrawerOpenListener;
import com.rockgecko.parkfinder.views.SlidingDrawer.OnDrawerScrollListener;
import com.rockgecko.parkfinder.vision.MachineVision;


import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import net.servicestack.func.Func;

import org.json.JSONException;
import org.json.JSONObject;


public class MapActivity extends AppCompatActivity implements
OnDrawerOpenListener,  OnDrawerCloseListener, OnDrawerScrollListener, DrawerListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final int REQUEST_CAMERA = 109;
	private AQuery aq;
	private ViewGroup mStopSummaryFragmentContainer;
	private SlidingDrawer mStopDrawer;
	private DrawerLayout mLRDrawer;
	public static final String TAG = "MapActivity";
	public static final String ACTION_SHOW_STOP = "com.rockgecko.dips.parkfinder.MapActivity.action_show_stop";
	public static final String EXTRA_STOP_ID = "MapActivity.stopid";
	public static final String EXTRA_STOP_MODE = "MapActivity.stopmode";
	public static final String ARG_KML_FILENAME = "filename";

	//obfs db favourite id
	//public static final String EXTRA_FAVOURITE_ID_E = "MapActivity.favouriteid";
	public static final String EXTRA_FAVOURITE_ID = "MapActivity.favouriteid";

	private static final String TAG_SELECTED_STOP_ID = "MapActivity.SelectedStopID";
	private static final String TAG_SELECTED_STOP_MODE = "MapActivity.SelectedStopMode";

	private static final int GRAVITY_FAVOURITES_DRAWER = Gravity.RIGHT;
    //private static final int ACTION_BAR_TINT_COLOUR = 0x222222; action_bar_background

	private Intent pendingIntent;
	private String pendingSelectedStopID;
    private int pendingSelectedStopMode;

	private int mSavedVersionCode;
	private int mCurrentVersionCode;
	private String mCurrentVersionName;
	
	private View mMapContainer;

    private boolean mHasDonate1, mHasUpgrade1;

    private GoogleApiClient mApiClient;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mSavedVersionCode = BuildConfig.VERSION_CODE;//prefs.getInt(ParkFinderPreferenceActivity.KEY_LAST_VERSION_CODE, 0);
		
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			mCurrentVersionCode = pi.versionCode;
			mCurrentVersionName = pi.versionName;
		} catch (NameNotFoundException e) {			
		}


		setContentView(R.layout.map_activity);
		mMapContainer = findViewById(R.id.mapContainer);



		pendingIntent = getIntent();
		aq = new AQuery(this);
		mPark=new ParkListFragment.Park(getIntent().getStringExtra(ARG_KML_FILENAME));
		mPark.init(this);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setElevation(0);
		String title = getIntent().getStringExtra(GenericDetailActivity.ARG_TITLE);
		if(title!=null)
			actionBar.setTitle(title);
		
		mStopSummaryFragmentContainer = (ViewGroup) findViewById(R.id.fragment_stop_summary_container);
		
		mLRDrawer = (DrawerLayout) findViewById(R.id.drawer_container_lr);
		mLRDrawer.setDrawerListener(this);
		
		mStopDrawer = (SlidingDrawer) findViewById(R.id.drawer_container_bottom);
		mStopDrawer.setVisibility(View.GONE);
		mStopDrawer.setOnDrawerOpenListener(this);
		mStopDrawer.setOnDrawerCloseListener(this);
		mStopDrawer.setOnDrawerScrollListener(this);

        View favouritesFragmentContainer = findViewById(R.id.fragment_favourites);
        ViewGroup.MarginLayoutParams favouritesLp=null;
        if(favouritesFragmentContainer!=null && favouritesFragmentContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){
            favouritesLp= (ViewGroup.MarginLayoutParams) favouritesFragmentContainer.getLayoutParams();
        }


		
		
		GMapFragment mapFragment = getMapFragment();
	        String tag;
	        if(mapFragment==null){
	        	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();      

	        	mapFragment = new GMapFragment();
	        	tag = GMapFragment.TAG;
	        	Bundle extras = new Bundle();
	        	//extras.putInt(GMapFragment.MAP_MODE_BUNDLE_NAME, 0);
	        	mapFragment.setArguments(extras);
	        	ft.replace(R.id.mapContainer, mapFragment, tag);        
	        	ft.commit();	        
	        }
	        StopSummaryFragment stopSummaryFragment = getStopSummaryFragment();
	        if(stopSummaryFragment!=null) stopSummaryFragment.setMenuVisibility(false);
	   if(savedInstanceState!=null){
		   pendingSelectedStopID = savedInstanceState.getString(TAG_SELECTED_STOP_ID);
		   pendingSelectedStopMode = savedInstanceState.getInt(TAG_SELECTED_STOP_MODE);
		   mFilename=savedInstanceState.getString("mFilename");

	   }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

	}
	private ParkListFragment.Park mPark;
	public ParkListFragment.Park getPark(){
		return mPark;
	}
	
	

    

    private void fixSearchViewTextColor(View view) {
        if (view != null) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(Color.WHITE);
                ((TextView) view).setHintTextColor(Color.LTGRAY);
                return;
            } else if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    fixSearchViewTextColor(viewGroup.getChildAt(i));
                }
            }
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    
	private final boolean sHideMapWhenOverlaying=Build.VERSION.SDK_INT<Build.VERSION_CODES.HONEYCOMB;
	private boolean mOpaqueStopListBg=sHideMapWhenOverlaying;
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onResume(){
		super.onResume();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		
		int alpha = mOpaqueStopListBg?0xff:0xcc;
		int colour = alpha<<24|0xffffff;
		mStopSummaryFragmentContainer.setBackgroundColor(colour);
		//mLRDrawer.setDrawerLockMode(mStopDrawer.isOpened()
		//		?DrawerLayout.LOCK_MODE_LOCKED_CLOSED:DrawerLayout.LOCK_MODE_UNLOCKED, GRAVITY_FAVOURITES_DRAWER);
		if(getMapFragment()!=null && getMapFragment().getMap()!=null){
			//int actionBarHeight =mActionBarBottom;
            /*
			TypedValue tv = new TypedValue();
			   if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
			      if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
			        actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
			   }else if(getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, tv, true)){
			        actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
			   }
			*/
			int mapPaddingBottom=0;
			if(pendingSelectedStopID!=null){
				getMapFragment().fetchStop(pendingSelectedStopID, pendingSelectedStopMode, GMapFragment.NEXT_DEPARTURES_LIMIT, false, false, false, true, null);
				mapPaddingBottom=getResources().getDimensionPixelOffset(R.dimen.stop_summary_handle_height);
			}
			//Log.d("MapActivity", "ActionBarHeight: "+actionBarHeight + "px, "+UIUtil.unScale(actionBarHeight)
			//		+"dp. map padding: "+mapPaddingBottom+"px, "+UIUtil.unScale(mapPaddingBottom)+"dp.");
		//	getMapFragment().getMap().setPadding(0, actionBarHeight, 0, mapPaddingBottom);
		}

		pendingSelectedStopID=null;
        pendingSelectedStopMode=-1;
		if(pendingIntent!=null){
			handleIntent(pendingIntent);
			pendingIntent=null;
		}
		if(mCurrentVersionCode>mSavedVersionCode && mDialog==null){

		}


	}
	private LatLng getCurrentLocation(){
		try {
			LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (lastLoc == null)
				lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (lastLoc == null)
				lastLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			if(lastLoc!=null){
				return new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
			}
		}catch (SecurityException | NullPointerException e){

		}
		return null;
	}

	@Override
	protected void onPause() {
		LatLng currentLocation = getCurrentLocation();
		if(currentLocation!=null&&mPark.getAchievements()!=null){
			if(mPark.bounds.contains(currentLocation) ){
				mPark.getAchievements().hasVisited=true;
				AppConfig.writeAchievements(this, mPark.getPreferencesName(), mPark.getAchievements());
			}
		}
		super.onPause();
	}


	@Override
	public void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		pendingIntent=intent;
	}
	
	private void handleIntent(Intent intent){
		if(ACTION_SHOW_STOP.equals(intent.getAction())){
			if(getMapFragment()!=null){
				getMapFragment().fetchStop(intent.getStringExtra(EXTRA_STOP_ID), intent.getIntExtra(EXTRA_STOP_MODE, -1), GMapFragment.NEXT_DEPARTURES_LIMIT, false, true, true, true, null);
			}
			
		}

	}
	
	public void onFavouriteSelected(FavouriteStop favourite){
		if(getMapFragment()!=null){
			getMapFragment().fetchStop(favourite, GMapFragment.NEXT_DEPARTURES_LIMIT, false, true, true, true, null);				
		}
		mLRDrawer.closeDrawer(GRAVITY_FAVOURITES_DRAWER);
		mStopDrawer.close();
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
		outState.putString("mFilename", mFilename);
        StopSummaryFragment stopSummaryFragment = getStopSummaryFragment();
		if(stopSummaryFragment!=null){
        	IStop selectedStop =stopSummaryFragment.getStop();
        	if(selectedStop!=null){
                outState.putString(TAG_SELECTED_STOP_ID, selectedStop.getId());
                outState.putInt(TAG_SELECTED_STOP_MODE, selectedStop.getMode());
            }
        	
        }
	}

	public IRunSelectionCallback getRunSelectionCallback(){
		return getMapFragment();
	}
	
	private GMapFragment getMapFragment() {
		return (GMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapContainer);
	}
	
	private StopSummaryFragment getStopSummaryFragment(){
		return (StopSummaryFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_stop_summary);
	}
	
	private FavouritesFragment getFavouritesFragment(){
		return (FavouritesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_favourites);
	}

    private SearchView mSearchView;
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

   
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		Intent intent=null;
		switch(item.getItemId()){
		
		case R.id.menu_info_drawer:
			//show favourites list
			if (mLRDrawer.isDrawerOpen(GRAVITY_FAVOURITES_DRAWER)) mLRDrawer.closeDrawer(GRAVITY_FAVOURITES_DRAWER);
			else mLRDrawer.openDrawer(GRAVITY_FAVOURITES_DRAWER);
			break;
		case R.id.menu_camera:
			launchCamera();
			break;
		
		default:
			return super.onOptionsItemSelected(item);
		}
		
		if(item.getItemId()!=R.id.menu_info_drawer){
			mLRDrawer.closeDrawer(GRAVITY_FAVOURITES_DRAWER);
		}
		if(intent!=null){
			startActivity(intent);			
		}
		return true;
	}

	String mFilename;
	private void launchCamera() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(String.format("Your score: %d", mPark.getAchievements().getScore()));
		String msg ="Have a look for:\n";
		if(mPark.getFeatures()!=null&&mPark.getFeatures().fauna.size()>0){
			//msg+=StringUtils.listToString(mPark.getFeatures().fauna,"\n-");
			for(String fauna : mPark.getFeatures().fauna){
				boolean found = mPark.getAchievements().faunaFound.contains(fauna);
				msg+=String.format("\n%s %s %s", found ? StringUtils.TICK : "-", fauna, found ? "(found)" : "");
			}
			if(mPark.getAchievements().landmarksFound.size()>0){
				msg+="Landmarks found:";
			}
			for(String landmark : mPark.getAchievements().landmarksFound){
				msg+=String.format("\n%s %s", StringUtils.TICK, landmark);
			}
		}
		builder.setMessage(msg);
		builder.setNegativeButton(R.string.ok, null);
		builder.setPositiveButton("Take photo", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
				File temDir = getCacheDir();
				mFilename = System.currentTimeMillis() + ".jpg";
				File imgFile = new File(temDir, mFilename);
				//intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imgFile));
				startActivityForResult(intent, REQUEST_CAMERA);
			}
		});
		builder.show();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode==REQUEST_CAMERA){
			if(resultCode==RESULT_OK){
				processImage(data.getData());
				//File resultFile = new File(getCacheDir(), mFilename);
				//File resultFile = new File(data.getData().getPath());
				//if(resultFile.exists()){
				//	processImage(resultFile);
				//	mFilename=null;
				//}
				//else Log.d(TAG, data.getDataString()+" file not found");
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	private void processImage(final Uri uri){
		showLoadingDialog("Processing image...");
		//noinspection unchecked
		new AsyncTask(){

			@Override
			protected Object doInBackground(Object[] objects) {
				Bitmap orig=null;
				try {
					orig=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return null;
				}
				//Bitmap orig = BitmapFactory.decodeFile(file.getPath());
				Bitmap scaled = UIUtil.getResizedBitmap(orig, 800);
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				scaled.compress(Bitmap.CompressFormat.JPEG, 85, os);
				String b64 = Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
				Log.d(TAG, String.format("Scaled from %sx%s to %dx%d", orig.getWidth(), orig.getHeight(), scaled.getWidth(), scaled.getHeight()));
				Log.d(TAG, "Base64 head "+b64.substring(0,10)+"... len "+b64.length());
				return b64;
			}

			@Override
			protected void onPostExecute(Object o) {
				String base64= (String) o;
				if(base64==null){
					Log.d(TAG, "no image");
					return;
				}
				String json = String.format("{\"requests\":[{\"features\":[{\"type\":\"LABEL_DETECTION\"},{\"type\":\"LANDMARK_DETECTION\"}],\"image\":{\"content\":\"%s\"}}]}",base64);
				JSONObject jsonObj = null;
				try {
					jsonObj = new JSONObject(json);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				//Log.d("requestDump",jsonObj.toString());
				String url = "https://vision.clients6.google.com/v1/images:annotate?key="+getString(R.string.google_vision_browser_key);
				AQuery aq = new AQuery(MapActivity.this);
				aq.transformer(new GsonTransformer()).post(url, jsonObj, MachineVision.Response.class, new AjaxCallback<MachineVision.Response>(){
					@Override
					public void callback(String url, MachineVision.Response result, AjaxStatus status) {
							Log.d(TAG, "Result: "+status.getCode()+" "+result);
						tryDismissDialog();
						if(result!=null){


							AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
							String msg="";
							ArrayList<String> allFound=new ArrayList<String>();
							HashSet<String> faunaFound = new HashSet<String>();
							HashSet<String> landmarksFound = new HashSet<String>();
							List<String> fauna = mPark.getFeatures()==null?new ArrayList<String>():mPark.getFeatures().fauna;
							//List<String> landmarks = mPark.getFeatures()==null?new ArrayList<String>(): mPark.getFeatures().landmarks;
							for(MachineVision.AnnotateImageResponse response:result.responses){
								if(response.labelAnnotations!=null)for(MachineVision.EntityAnnotation label : response.labelAnnotations){
									allFound.add(label.description);
									for(String f : fauna){
										if(f.equalsIgnoreCase(label.description)){
											faunaFound.add(f);
										}
									}
								}
								if(response.landmarkAnnotations!=null)for(MachineVision.EntityAnnotation landmark : response.landmarkAnnotations){
									if(!StringUtils.isNullOrEmpty(landmark.locations)) {
										allFound.add(landmark.description);
										for(MachineVision.LocationInfo loc : landmark.locations) {
											if (mPark.getBounds().contains(loc.latLng.getLatLng()) || ParkFinderApplication.DEBUG){
												landmarksFound.add(landmark.description);
												break;
											}
										}
									}
								}
								if(allFound.size()>0){
									msg+="What we found in your photo:\n"+StringUtils.listToString(allFound, "\n-");
									msg+="\n";
								}
								if(faunaFound.size()>0){
									msg+="Congratulations, this fauna matches this park:\n-"+StringUtils.listToString(Func.toList(faunaFound),"\n-");
									msg+="\n";
								}
								if(landmarksFound.size()>0){
									msg+="Congratulations, this landmark is part of this park:\n-"+StringUtils.listToString(Func.toList(landmarksFound),"\n-");
									msg+="\n";
								}
								int oldScore = mPark.getAchievements().getScore();
								mPark.getAchievements().faunaFound.addAll(faunaFound);
								mPark.getAchievements().landmarksFound.addAll(landmarksFound);
								int newScore = mPark.getAchievements().getScore();
								int gain = newScore-oldScore;
								if(gain>0){
									msg+=String.format("You have gained %d point%s", gain, gain==1?"":"s");
									msg+="\n";
								}
								else{
									msg+="Sorry, nothing matches this park. Keep looking!\n";
								}
								msg+="Your score: "+newScore;
								if(!ParkFinderApplication.DEBUG)
									AppConfig.writeAchievements(MapActivity.this, mPark.getPreferencesName(), mPark.getAchievements());
								View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_scrollview_msg, null);
								((TextView)view.findViewById(R.id.text1)).setText(msg);
								builder.setView(view);
								builder.setTitle("Image processed");
								builder.setPositiveButton(R.string.ok, null).show();
							}
						}
					}
				});
			}
		}.execute();
	}
	public void onBackPressed(){
		if(mStopDrawer.isOpened()) mStopDrawer.animateClose();
		else super.onBackPressed();
	}

    
	public void selectStop(IStop stop){
		StopSummaryFragment stopSummaryFragment = getStopSummaryFragment();
		stopSummaryFragment.setStop(stop);
		boolean hasDisruptions=false;
		if(stop==null){
			stopSummaryFragment.setMenuVisibility(false);
			mStopDrawer.setVisibility(View.GONE);
			getMapFragment().getMap().setPadding(0, 0, 0, 0);
		}
		else{
			mStopDrawer.setVisibility(View.VISIBLE);
			getMapFragment().getMap().setPadding(0, 0, 0, mStopDrawer.findViewById(R.id.handle).getHeight());
			aq.id(R.id.handle_text1).text(stop.getName());
			String subText;
			if(stop.hasCompleteServices(false)){		
				List<String> routeNumberList = StopService.getRouteNumberList(stop.getServices());
				subText= String.format("Route%s %s", routeNumberList.size()==1?"":"s", 
						StringUtils.listToStringWithAmp(routeNumberList.toArray()));

               
			}
			else{
				subText = getString(R.string.loading_services);
			}
			Drawable disruptionIcon=null;
			
			aq.id(R.id.handle_text2).text(subText).getTextView().setCompoundDrawablesWithIntrinsicBounds(disruptionIcon, null, null, null);
			
			//zone
		}
	}
	




    @Override
    public void onStop(){
        super.onStop();
        if(mApiClient.isConnected())
            mApiClient.disconnect();
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	




	
	/**
	 * Bottom drawer
	 */
	@Override
	public void onDrawerOpened() {
        Log.d(TAG, "Bottom drawer opened");
		//mLRDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GRAVITY_FAVOURITES_DRAWER); //can't open favourites while stop summary is up
		StopSummaryFragment stopSummaryFragment = getStopSummaryFragment();
		stopSummaryFragment.setMenuVisibility(true);
		IStop stop = stopSummaryFragment.getStop();
		if(stop!=null && getMapFragment()!=null) getMapFragment().fetchStop(stop, GMapFragment.NEXT_DEPARTURES_LIMIT, false, true, false, false, null);
		if(sHideMapWhenOverlaying ){
			mMapContainer.setVisibility(View.INVISIBLE);
			mLRDrawer.requestLayout();
			mLRDrawer.invalidate();
		}


        //Window window = getWindow();
        // Disable status bar translucency (requires API 19)
        //window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

	}
	/**
	 * Bottom drawer
	 */
	@Override
	public void onDrawerClosed() {
        Log.d(TAG, "Bottom drawer closed");
		//mLRDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GRAVITY_FAVOURITES_DRAWER);
		StopSummaryFragment stopSummaryFragment = getStopSummaryFragment();
		stopSummaryFragment.setMenuVisibility(false);
		if(sHideMapWhenOverlaying ){
			mMapContainer.setVisibility(View.VISIBLE);
			mLRDrawer.requestLayout();
			mLRDrawer.invalidate();
		}

        //Window window = getWindow();
        // Enable status bar translucency (requires API 19)
        //window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
        //        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);


	}
	/**
	 * Bottom drawer
	 */
	@Override
	public void onDrawerScrollStarted() {
		Log.d("MapActivity", "OnScroll: Start");
		
	}
	/**
	 * Bottom drawer
	 */
	@Override
	public void onDrawerScrollEnded() {
		Log.d("MapActivity", "OnScroll: End");
		
	}
	//static final int base = 0xaabbcc;
	//static final int color = (255<<24)|base;
	/**
	 * Bottom drawer
	 */
	@Override
	public void onDrawerScroll(float scrollOffset) {
		int maxAlpha = 255;
		int alpha;
        if(mSearchView!=null && !mSearchView.isIconified()) alpha = maxAlpha; // !iconified=expanded
        else alpha= scrollOffset>=0.99f?maxAlpha:scrollOffset<=0.01f?0:(int)(scrollOffset*maxAlpha);
		Log.d(TAG, "OnScroll: "+scrollOffset+" alpha: "+alpha);
		//mStopSummaryFragmentContainer.setBackgroundColor(Color.argb(alpha, 255, 255, 255));
		//mStopSummaryFragmentContainer.invalidate();

		//getSupportActionBar().setBackgroundDrawable(new ColorDrawable((alpha<<24)|mActionBarBackgroundColour));

        float snapped;
        if(mSearchView!=null && !mSearchView.isIconified()) snapped = 1; // !iconified=expanded
        else snapped= scrollOffset>=0.9999f?1:scrollOffset<=0.0001f?0:scrollOffset;


		if(sHideMapWhenOverlaying ) mMapContainer.setVisibility(View.INVISIBLE);
	}

	/**
	 * LR Drawer
	 */
	@Override
	public void onDrawerSlide(View drawerView, float slideOffset) {
		getFavouritesFragment().setUserVisibleHint(true);
		if(sHideMapWhenOverlaying ){
			boolean req = mMapContainer.getVisibility()!=View.INVISIBLE;
			mMapContainer.setVisibility(View.INVISIBLE);
			if(req){
				mLRDrawer.requestLayout();
				mLRDrawer.invalidate();
			}
		}
	}
	/**
	 * LR Drawer
	 */
	@Override
	public void onDrawerOpened(View drawerView) {
		if(sHideMapWhenOverlaying ){
			mMapContainer.setVisibility(View.INVISIBLE);
			mLRDrawer.requestLayout();
			mLRDrawer.invalidate();
		}
	}
	/**
	 * LR Drawer
	 */
	@Override
	public void onDrawerClosed(View drawerView) {
		getFavouritesFragment().setUserVisibleHint(false);
		if(sHideMapWhenOverlaying ){
			mMapContainer.setVisibility(View.VISIBLE);
			mLRDrawer.requestLayout();
			mLRDrawer.invalidate();
		}
		
	}
	/**
	 * LR Drawer
	 */
	@Override
	public void onDrawerStateChanged(int newState) {
		
	}

	@Override
	public void onConnected(Bundle bundle) {

	}

	@Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

	private Dialog mDialog;
	private void showLoadingDialog(String msg){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.dialog_loading, null);
		((TextView) dialogView.findViewById(R.id.text1)).setText(msg);
		builder.setView(dialogView).setCancelable(false);
		mDialog= builder.show();
	}
	private void tryDismissDialog(){
		if(mDialog!=null){
			mDialog.dismiss();
			mDialog=null;
		}
	}
	
}
