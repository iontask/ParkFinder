package com.rockgecko.parkfinder.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.kml.KmlContainer;
import com.google.maps.android.kml.KmlGeometry;
import com.google.maps.android.kml.KmlLayer;
import com.google.maps.android.kml.KmlLineString;
import com.google.maps.android.kml.KmlMultiGeometry;
import com.google.maps.android.kml.KmlPlacemark;
import com.google.maps.android.kml.KmlPoint;
import com.google.maps.android.kml.KmlPolygon;
import com.rockgecko.parkfinder.ParkListFragment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class MapUtil {
    public static final LatLng MELBOURNE_CENTRE = new LatLng(-37.813611,144.963056);
    public static final LatLng BALLARAT_CENTRE = new LatLng(-37.5592153,143.8626583);
    public static final LatLng BENDIGO_CENTRE = new LatLng(-36.75618,144.2801258);
    public static final LatLng GEELONG_CENTRE = new LatLng(-38.1482595,144.3629658);
    public static final LatLng VICTORIA_CENTRE = new LatLng(-36.8541666,144.281111);

    public static LatLngBounds getWindow(KmlLayer layer){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for(KmlPlacemark pm : layer.getPlacemarks()){
            List<LatLng> list = flattenGeometry(pm.getGeometry());
            for(LatLng latLng : list){
                builder.include(latLng);
            }
        }
        for(KmlContainer container : layer.getContainers()){
            for(LatLng latLng : flattenContainer(container)){
                builder.include(latLng);
            }
        }
        try {
            return builder.build();
        }catch (Exception e){
            //no points
            return null;
        }
    }
    public static List<LatLng> flattenContainer(KmlContainer container){
        ArrayList<LatLng> result = new ArrayList<>();
        for(KmlContainer childContainer : container.getContainers()){
            result.addAll(flattenContainer(childContainer));
        }
        for(KmlPlacemark pm : container.getPlacemarks()){
            List<LatLng> list = flattenGeometry(pm.getGeometry());
            result.addAll(list);
        }
        return result;
    }

    public static List<LatLng> flattenGeometry(KmlGeometry<?> geometry){
        ArrayList<LatLng> result = new ArrayList<>();
        switch (geometry.getGeometryType()){
            case KmlMultiGeometry.GEOMETRY_TYPE:
                ArrayList<KmlGeometry> multi = ((KmlMultiGeometry) geometry).getGeometryObject();
                for(KmlGeometry<?> g : multi){
                    result.addAll(flattenGeometry(g));
                }
                break;
            case KmlLineString.GEOMETRY_TYPE:
                ArrayList<LatLng> line = ((KmlLineString) geometry).getGeometryObject();
                result.addAll(line);
                break;
            case KmlPolygon.GEOMETRY_TYPE:
                ArrayList<ArrayList<LatLng>> poly = ((KmlPolygon) geometry).getGeometryObject();
                //nah
                break;
            case KmlPoint.GEOMETRY_TYPE:
                result.add(((KmlPoint)geometry).getGeometryObject());
                break;

        }
        return result;
    }

    public static Comparator<ParkListFragment.Park> getAirportDistanceComparator(final double latitude, final double longitude){
        return new Comparator<ParkListFragment.Park>() {

            @Override
            public int compare(ParkListFragment.Park lhs, ParkListFragment.Park rhs) {
                float[] lhsDistance = new float[]{-1};
                float[] rhsDistance = new float[]{-1};
                LatLng lhsLocation = lhs.getBounds().getCenter();
                LatLng rhsLocation = rhs.getBounds().getCenter();
                if(lhsLocation!=null){
                    Location.distanceBetween(lhsLocation.latitude, lhsLocation.longitude, latitude, longitude, lhsDistance);
                }
                if(rhsLocation!=null){
                    Location.distanceBetween(rhsLocation.latitude, rhsLocation.longitude, latitude, longitude, rhsDistance);
                }
                return Float.compare(lhsDistance[0], rhsDistance[0]);

            }
        };
    }
}
