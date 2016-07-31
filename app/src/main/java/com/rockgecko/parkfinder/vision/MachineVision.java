package com.rockgecko.parkfinder.vision;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by bramleyt on 31/07/2016.
 */
public class MachineVision {
    public static class Response{
        public ArrayList<AnnotateImageResponse> responses;

    }
    public static class AnnotateImageResponse{
        public ArrayList<EntityAnnotation> labelAnnotations;
        public ArrayList<EntityAnnotation> landmarkAnnotations;
    }
    public static class EntityAnnotation{
        public String mid;
        public String description;
        public double score;
        public ArrayList<LocationInfo> locations;
    }
    public static class LocationInfo{
        public LatLng latLng;
    }
    public static class LatLng{
        public double latitude, longitude;
        public com.google.android.gms.maps.model.LatLng getLatLng(){
            return new com.google.android.gms.maps.model.LatLng(latitude, longitude);
        }
    }
}
