package com.rockgecko.parkfinder;

import android.app.Application;
import android.content.Context;

import com.androidquery.util.AQUtility;
import com.google.maps.android.kml.KmlLayer;
import com.rockgecko.parkfinder.ptv.PtvApi;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class ParkFinderApplication extends Application {
    public static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean DEV_PREVIEW = false;

    private static ParkFinderApplication instance;
    @Override
    public void onCreate(){
        super.onCreate();
        instance=this;
        KmlLayer.init(this);
        AQUtility.setDebug(DEBUG);
        PtvApi.setUrls(getString(R.string.server_ptv_api), getString(R.string.server_ptv_web));
    }


    public static Context getContextStatic(){
        if(instance==null){
            throw new RuntimeException("ParkFinderApplication not yet instantiated");
        }
        return instance.getApplicationContext();
    }
}
