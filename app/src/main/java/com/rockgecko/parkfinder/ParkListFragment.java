package com.rockgecko.parkfinder;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlLineString;
import com.google.maps.android.kml.KmlMultiGeometry;
import com.google.maps.android.kml.KmlPlacemark;
import com.rockgecko.parkfinder.adapter.CellRecyclerAdapter;
import com.rockgecko.parkfinder.adapter.CellViewHolder;
import com.rockgecko.parkfinder.dal.ParkAchievements;
import com.rockgecko.parkfinder.dal.ParkFeatures;
import com.rockgecko.parkfinder.util.MapUtil;
import com.rockgecko.parkfinder.util.StringUtils;
import com.rockgecko.parkfinder.util.UIUtil;

import net.servicestack.func.Func;
import net.servicestack.func.Function;
import net.servicestack.func.Predicate;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class ParkListFragment extends Fragment {
    public static final String TAG = "ParkListFragment";
    private RecyclerView mRecyclerView;

    private List<Park> mParks;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recyclerview, container, false);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new ParkListAdapter());
        if(mParks==null)loadParks();
        return root;
    }
    private LatLng getCurrentLocation(){
        try {
            LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
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
    public void onResume() {
        super.onResume();
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void loadParks(){
        //noinspection unchecked
        new AsyncTask(){
            @Override
            protected Object doInBackground(Object[] objects) {
                ArrayList<Park> result = new ArrayList<Park>();
                try {
                    String path = "kml";
                    for(String filename: getActivity().getAssets().list(path)){
                        Park item = new Park(path+"/"+filename);
                        item.init(ParkFinderApplication.getContextStatic());
                        result.add(item);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LatLng lastLoc = getCurrentLocation();
                if(lastLoc!=null) {
                    result = Func.orderBy(result, MapUtil.getAirportDistanceComparator(lastLoc.latitude, lastLoc.longitude));
                }
                return result;
            }

            @Override
            protected void onPostExecute(Object o) {
                mParks= (List<Park>) o;
                if(mRecyclerView.getAdapter()==null)mRecyclerView.setAdapter(new ParkListAdapter());
                else ((ParkListAdapter)mRecyclerView.getAdapter()).updateData();
            }
        }.execute();
    }


    private class ParkListAdapter extends CellRecyclerAdapter<CellViewHolder, Object> {
        private ArrayList<Object> aItems;
        ParkListAdapter(){
            updateData();
        }

        private void updateData() {
            aItems=new ArrayList<>();
            if(mParks!=null)
                aItems.addAll(mParks);
            notifyDataSetChanged();
        }

        @Override
        public List<Object> getItems() {
            return aItems;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);
            return R.layout.cell_simple_2_line_lr_text;
        }

        @Override
        public CellViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CellViewHolder vh = new CellViewHolder(parent, viewType);
          ///  if(viewType==R.layout.cell_simple_2_line){
                vh.itemView.setTag(vh);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CellViewHolder vh = (CellViewHolder) view.getTag();
                        Object item = getItem(vh.getAdapterPosition());
                        if(item instanceof Park){
                            Intent intent = new Intent(getActivity(), MapActivity.class);
                            intent.putExtra(MapActivity.ARG_KML_FILENAME, ((Park) item).filename);
                            intent.putExtra(GenericDetailActivity.ARG_TITLE, ((Park) item).getName());
                            startActivity(intent);
                        }
                    }
                });
           // }
            return vh;
        }

        @Override
        public void onBindViewHolder(CellViewHolder vh, int position) {
            Object item = getItem(position);
            if(item instanceof Park){
                Park park = (Park) item;
                park.refreshAchievements(getActivity());
                String name = park.getName();
                String text2="",text3="";
                if(park.layer!=null&&park.layer.hasContainers()) {
                    List<KmlPlacemark> pathPlacemarks = park.getPathPlacemarks();
                    List<Double> lengths = new ArrayList<>();
                    for(KmlPlacemark pm : pathPlacemarks){
                        List<LatLng> path = MapUtil.flattenGeometry(pm.getGeometry());
                        double distance =0;
                        for (int i = 0; i < path.size(); i++) {
                            if(i>0) {
                                distance += SphericalUtil.computeDistanceBetween(path.get(i-1), path.get(i));
                            }

                        }
                        lengths.add(distance);
                    }
                    if(pathPlacemarks.size()==1){
                        text2 = String.format("%d path, %.1fkm", pathPlacemarks.size(), lengths.get(0)/1000d);
                    }else {
                        text2 = String.format("%d paths, %.1f-%.1fkm", pathPlacemarks.size(), Func.minDouble(lengths) / 1000d, Func.maxDouble(lengths) / 1000d);
                    }
                    if(park.getAchievements()!=null){
                        text2+=String.format("\nVisited: %s.", park.getAchievements().hasVisited?"yes":"not yet");
                        //if(park.getAchievements().maxScore>0)
                            text2+=String.format(" Achievements: %d", park.getAchievements().getScore());

                    }
              //      String address=park.layer.getContainers().iterator().next().getProperty("address");
            //        if(!StringUtils.isNullOrEmpty(address))text2+=" "+address;
                    LatLng loc = getCurrentLocation();
                    if(loc!=null){
                        double distance = SphericalUtil.computeDistanceBetween(loc, park.getBounds().getCenter());
                        text3 = String.format("%.1fkm\naway", distance/1000d);
                    }
                }
                vh.aq().id(R.id.text1).text(name);
                vh.aq().id(R.id.text2).text(text2);
                vh.aq().id(R.id.textRight).text(text3);

            }
            else{
                vh.aq().id(R.id.text1).text(item.toString());
            }
        }
    }
    public static class Park{
        public final String filename;
        KmlLayer layer;
        String error;
        LatLngBounds bounds;
        ParkAchievements achievements;
        ParkFeatures features;
        public Park(String filename){
            this.filename=filename;
        }

        public LatLngBounds getBounds() {
            return bounds;
        }

        public KmlLayer getLayer() {
            return layer;
        }
        public String getPreferencesName(){
            return filename.toLowerCase().replace(" ","_").replace("kml/","").replace(".kml","").trim();
        }

        public ParkFeatures getFeatures() {
            return features;
        }

        public ParkAchievements getAchievements() {
            return achievements;
        }

        public String getName(){
            if(layer!=null&&layer.hasContainers()) return layer.getContainers().iterator().next().getProperty("name");
            return filename;
        }
        public void init(Context c){
            if(layer==null && error==null){
                try {
                    layer = new KmlLayer(null, c.getAssets().open(filename), c);
                    bounds = MapUtil.getWindow(layer);
                    String json = layer.getContainers().iterator().next().getProperty("description");
                    if(json!=null && json.startsWith("{")){
                        features = AppConfig.getAppConfigGson().fromJson(json, ParkFeatures.class);
                    }
                    refreshAchievements(c);
                } catch (Exception e) {
                    e.printStackTrace();
                    error=e.getMessage();
                }
            }

        }
        public void refreshAchievements(Context c){
            achievements = AppConfig.getAchievements(c, getPreferencesName());
            if(achievements==null){
                achievements = new ParkAchievements();
                achievements.faunaFound=new HashSet<>();
                achievements.landmarksFound=new HashSet<>();
            }
            if(features!=null){
                achievements.maxScore=features.fauna.size()+features.landmarks.size()+1;
            }
            else{
                achievements.maxScore=1;
            }
        }
        public List<KmlPlacemark> getPathPlacemarks(){
            ArrayList<KmlPlacemark> result = new ArrayList<>();

            result.addAll(Func.filter(layer.getPlacemarks(), pathPred));
            for(KmlContainer container : layer.getContainers()) {
                result.addAll(getPathPlacemarksInternal(container));
            }
            return result;
        }
        private final Predicate<KmlPlacemark> pathPred = new Predicate<KmlPlacemark>() {
            @Override
            public boolean apply(KmlPlacemark pm) {
                return pm.getGeometry().getGeometryType().equals(KmlMultiGeometry.GEOMETRY_TYPE) || pm.getGeometry().getGeometryType().equals(KmlLineString.GEOMETRY_TYPE);
            }
        };
        private ArrayList<KmlPlacemark> getPathPlacemarksInternal(KmlContainer container){
            ArrayList<KmlPlacemark> result=new ArrayList<>();
            result.addAll(Func.filter(container.getPlacemarks(), pathPred));
            if(container.hasContainers())for(KmlContainer container1: container.getContainers()){
                result.addAll(getPathPlacemarksInternal(container1));
            }
            return result;
        }

    }
}
