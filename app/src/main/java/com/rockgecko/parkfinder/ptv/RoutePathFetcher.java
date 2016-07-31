package com.rockgecko.parkfinder.ptv;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.rockgecko.parkfinder.IActionComplete;
import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.dal.GsonTransformer;
import com.rockgecko.parkfinder.dal.RoutePathResult;
import com.rockgecko.parkfinder.util.MapUtil;

/**
 * Created by Bramley on 31/03/2015.
 */
public class RoutePathFetcher {
    public static final String TAG = "RoutePathFetcher";
    public static final String FILE_NIGHTRIDER = "shapes_nightrider.txt";

    public static final String FILE_ROUTEPATH_METRO_BUS = "RoutePath_Metro_Bus_4.txt";
    public static final String FILE_ROUTEPATH_REGIONAL_BUS = "RoutePath_Regional_Bus_4.txt";
    public static final String FILE_ROUTEPATH_NIGHTRIDER = "RoutePath_Nightrider_4.txt";

    public static final double START_END_TOLERANCE = 200;
    public static final int MIN_NIGHTRIDER_ROUTE = 940;
    public static final int MIN_METRO_ROUTE = 150;

    public static final double MELBOURNE_RADIUS = 65*1000;

    public RoutePathFetcher(){
    }


    public void getPathForRouteAsync(final String routeNo, final LatLng startPoint, final LatLng endPoint, final IActionComplete<RoutePathResult> callback) {
        new AsyncTask<Void, Void, RoutePathResult>() {
            @Override
            protected RoutePathResult doInBackground(Void... params) {
                return getPathForRoute(routeNo, startPoint, endPoint);
            }
            @Override
            protected void onPostExecute (RoutePathResult result){
                callback.done(result);
            }
        }.execute();
    }
    public RoutePathResult getPathForRoute(final String routeNo, final LatLng startPoint, final LatLng endPoint) {
                RoutePathResult result = new RoutePathResult();
                result.routeNo=routeNo;
                String filename = null;
                try{
                    int routeNoInt = Integer.parseInt(routeNo);
                    if(routeNoInt>=MIN_NIGHTRIDER_ROUTE) filename=FILE_ROUTEPATH_NIGHTRIDER;
                    else if (routeNoInt<MIN_METRO_ROUTE) filename=FILE_ROUTEPATH_REGIONAL_BUS;
                }catch(NumberFormatException e){
                }
                if(filename==null){
                    double startDistance = SphericalUtil.computeDistanceBetween(startPoint, MapUtil.MELBOURNE_CENTRE);
                    double endDistance = SphericalUtil.computeDistanceBetween(endPoint, MapUtil.MELBOURNE_CENTRE);
                    if(startDistance>MELBOURNE_RADIUS && endDistance>MELBOURNE_RADIUS){
                        filename=FILE_ROUTEPATH_REGIONAL_BUS;
                    }
                    else filename=FILE_ROUTEPATH_METRO_BUS;
                }
                Log.d(TAG, "Searching "+filename+" for route: "+routeNo);
                BufferedReader reader=null;
                try {
                    AssetManager assetManager = ParkFinderApplication.getContextStatic().getAssets();
                    InputStream is=assetManager.open(filename);

                    reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8192);
                    reader.readLine();//first line is headings:
                    //  0        1      2       3       4       5      6       7
                    //ShapeID,RouteNo,Length,StartLat,StartLng,EndLat,EndLng,Polyline
                    String line=null;
                    while((line=reader.readLine())!=null) try{
                        String[] split = line.split(",");
                        if(routeNo.equals(split[1])){
                            LatLng rowStartPoint = new LatLng(Double.parseDouble(split[3]), Double.parseDouble(split[4]));
                            double startDistance = SphericalUtil.computeDistanceBetween(startPoint, rowStartPoint);

                            if(startDistance<START_END_TOLERANCE){
                                LatLng rowEndPoint = new LatLng(Double.parseDouble(split[5]), Double.parseDouble(split[6]));
                                double endDistance = SphericalUtil.computeDistanceBetween(endPoint, rowEndPoint);
                                if(endDistance<START_END_TOLERANCE){
                                    int charsBeforeLine =0;
                                    for(int i=0;i<7;i++){
                                        charsBeforeLine+=split[i].length()+1;
                                    }
                                    result.shapeID=split[0];
                                    result.length=Double.parseDouble(split[2]);
                                    result.polyline=line.substring(charsBeforeLine);
                                    Log.d(TAG, "Finished route "+routeNo+". ID: "+result.shapeID+". Length: "+result.length+" Start: "+result.polyline.substring(0,5)+" "+result.polyline.substring(result.polyline.length()-5));
                                    break;
                                }
                                else{
                                    Log.d(TAG, "Shape "+split[0]+" matched start but not end: "+endDistance);
                                }
                            }
                            else if(startDistance<5000){
                                Log.d(TAG, "Shape "+split[0]+" did not match start: "+startDistance);
                            }
                        }
                    }
                    catch(Exception e){
                        Log.e(TAG, "Error reading line: "+line,e);
                    }

                }
                catch (IOException e){
                    Log.e(TAG, "couldn't read " + filename, e);
                    return result;
                }
                finally{
                    try{
                        reader.close();
                    }
                    catch(Exception e){}
                }

                return result;

    }


    //  0           1               2           3                   4
    //shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled
    private LatLng parseLocation(String[] line){
        return new LatLng(Double.parseDouble(line[1]), Double.parseDouble(line[2]));
    }


}
