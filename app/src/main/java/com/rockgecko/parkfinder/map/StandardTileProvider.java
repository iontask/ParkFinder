package com.rockgecko.parkfinder.map;

import com.google.android.gms.maps.model.UrlTileProvider;
import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.R;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Bramley on 16/03/2015.
 */
public class StandardTileProvider extends UrlTileProvider{
    public static final int MIN_ZOOM = 14;
    public static final int TILE_SIZE = 256;

    public StandardTileProvider(){
        super(TILE_SIZE, TILE_SIZE);
    }
    @Override
    public URL getTileUrl(int x, int y, int zoom) {
        if(zoom<MIN_ZOOM) return null;
        try {
            return new URL(ParkFinderApplication.getContextStatic()
                    .getString(R.string.server_overlay_url)
                    .replace("{z}",""+zoom)
                    .replace("{x}",""+x)
                    .replace("{y}",""+y));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
