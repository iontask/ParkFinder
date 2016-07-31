package com.rockgecko.parkfinder;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rockgecko.parkfinder.dal.ParkAchievements;

import net.servicestack.client.JsonSerializers;

import java.util.Date;

/**
 * Created by bramleyt on 31/07/2016.
 */
public class AppConfig {

    public static final String PREFS_FILE_PARK_ACHIEVEMENTS = "achievements";

    public static ParkAchievements getAchievements(Context c, String parkPrefName){
        String json = c.getSharedPreferences(PREFS_FILE_PARK_ACHIEVEMENTS, 0).getString(parkPrefName, null);
        if(json==null) return null;
        return getAppConfigGson().fromJson(json, ParkAchievements.class);
    }
    public static void writeAchievements(Context c, String parkPrefName, ParkAchievements achievements){
        String json = getAppConfigGson().toJson(achievements);
        c.getSharedPreferences(PREFS_FILE_PARK_ACHIEVEMENTS,0).edit().putString(parkPrefName, json).commit();
    }

    public static Gson getAppConfigGson(){
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, JsonSerializers.getDateDeserializer())
                .registerTypeAdapter(Date.class, JsonSerializers.getDateSerializer())
                .create();
    }
}
