package com.rockgecko.parkfinder.dal;

import android.util.Log;

import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.Transformer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rockgecko.parkfinder.ParkFinderApplication;

public class GsonTransformer implements Transformer{
	
	private Gson gson;
    public GsonTransformer(Gson gson){
        this.gson=gson;
    }
	public GsonTransformer(){
			gson = new GsonBuilder().create();
	}

    public <T> T transform(String url, Class<T> type, String encoding, byte[] data, AjaxStatus status) {
        try {
            String json = new String(data);
            if(ParkFinderApplication.DEBUG)Log.d("responseDump", json);
            return gson.fromJson(json, type);
        }catch(Exception e){
            Log.e("Gson", "ex in URL: " + url, e);
            throw e;
        }
    }
}