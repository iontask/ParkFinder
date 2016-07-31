package com.rockgecko.parkfinder.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.GMapFragment;
import com.rockgecko.parkfinder.R;
import com.rockgecko.parkfinder.util.UIUtil;

import net.servicestack.func.Func;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bramley on 18/03/2015.
 */
public class HighResTileProvider implements TileProvider {
    public static final int MIN_ZOOM = 14;
    public static final int TILE_SIZE_BASE = 256;
    public static final String TAG = "HighResTileProvider";
    private int mDensityDPI;
    private Context mContext;
    private SphericalMercatorProjection mProjection;


    private static final Boolean LOG = ParkFinderApplication.DEBUG;


    public HighResTileProvider(Context c, int densityDPI){
        mDensityDPI=densityDPI;
        mContext=c;
        mProjection=new SphericalMercatorProjection(WORLD_WIDTH);


    }
    @Override
    public Tile getTile(int x, int y, int zoom) {
        if(mDensityDPI>= DisplayMetrics.DENSITY_XHIGH)
            return get4Tiles(x,y,zoom);
        return getSingleTile(x,y,zoom);
    }

    private Tile getSingleTile(int x, int y, int zoom) {
        if(zoom<MIN_ZOOM) return NO_TILE;
        AjaxCallback<byte[]> cb= new AjaxCallback<byte[]>().type(byte[].class).memCache(true).fileCache(true).url(getTileUrl(x, y, zoom));
        AQuery aq = new AQuery(mContext);
        aq.sync(cb);
        if(cb.getResult()==null){
            if(LOG)Log.d(TAG, "Failed: " + cb.getStatus().getCode()+" " + cb.getUrl());
            return cb.getStatus().getCode()==404 ? NO_TILE : null;
        }
        return new Tile(TILE_SIZE_BASE, TILE_SIZE_BASE, cb.getResult());
        //return convertBitmap(cb.getResult());
    }
    private Tile get4Tiles(int x, int y, int zoom) {

        if(LOG)Log.d(TAG, "Orig: " + getTileUrl(x, y, zoom));
        ArrayList<AjaxCallback<Bitmap>> cbs = Func.toList(
                new AjaxCallback<Bitmap>().type(Bitmap.class).memCache(true).fileCache(true).url(getTileUrl(x * 2, y * 2, zoom + 1)),
                new AjaxCallback<Bitmap>().type(Bitmap.class).memCache(true).fileCache(true).url(getTileUrl(x * 2 + 1, y * 2, zoom + 1)),
                new AjaxCallback<Bitmap>().type(Bitmap.class).memCache(true).fileCache(true).url(getTileUrl(x * 2, y * 2 + 1, zoom + 1)),
                new AjaxCallback<Bitmap>().type(Bitmap.class).memCache(true).fileCache(true).url(getTileUrl(x * 2 + 1, y * 2 + 1, zoom + 1))
        );
        /*
        for(BitmapAjaxCallback cb : cbs) {
            Log.d(TAG, "Get: " + cb.getUrl());
            cb.async(mContext);
        }*/
        AQuery aq = new AQuery(mContext);
        for(AjaxCallback<Bitmap> cb : cbs) {
            if(cb.getUrl()==null) return NO_TILE;
            if(LOG)Log.d(TAG, "Get: " + cb.getUrl());
            aq.sync(cb);
//            cb.block();
        }
        int cacheHits=0;
        for(AjaxCallback<Bitmap> cb : cbs) {
            if(cb.getResult()==null){
                if(LOG)Log.d(TAG, "Failed: " + cb.getStatus().getCode()+" " + cb.getUrl());
                return cb.getStatus().getCode()==404 ? NO_TILE : null;
            }
            if(cb.getStatus().getSource()== AjaxStatus.MEMORY
                    || cb.getStatus().getSource()== AjaxStatus.FILE){
                cacheHits++;
            }
        }
        if(LOG)Log.d(TAG, "Get: " + getTileUrl(x, y, zoom) +" x4 OK. Hits: "+cacheHits);
        int width = 2*TILE_SIZE_BASE;
        int height = 2*TILE_SIZE_BASE;
        Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);


        comboImage.drawBitmap(cbs.get(0).getResult(), 0f, 0f, null);


        comboImage.drawBitmap(cbs.get(1).getResult(), TILE_SIZE_BASE, 0f, null);


        comboImage.drawBitmap(cbs.get(2).getResult(), 0f, TILE_SIZE_BASE, null);


        comboImage.drawBitmap(cbs.get(3).getResult(), TILE_SIZE_BASE, TILE_SIZE_BASE, null);


        return convertBitmap(cs);
    }



    private String getTileUrl(int x, int y, int zoom) {
        if(zoom<MIN_ZOOM) return null;
        String baseUrl = mContext
                .getString(R.string.server_overlay_url);


            return baseUrl
                    .replace("{z}", "" + zoom)
                    .replace("{x}",""+x)
                    .replace("{y}",""+y);

    }

    public boolean hasTiles(LatLng target, float cameraZoom){
        Point p = mProjection.toPoint(target);
        int zoom;
        if(mDensityDPI>= DisplayMetrics.DENSITY_XHIGH) zoom = (int)(cameraZoom+1); //floor
        else zoom = (int)(cameraZoom); //floor
        double tileWidth = WORLD_WIDTH / Math.pow(2, zoom);
        int x = (int) Math.round(p.x/tileWidth);
        int y = (int) Math.round(p.y/tileWidth);
        return getTileUrl(x,y,zoom)!=null;
    }

    /**
     * helper function - convert a bitmap into a tile
     *
     * @param bitmap bitmap to convert into a tile
     * @return the tile
     */
    private static Tile convertBitmap(Bitmap bitmap) {
        // Convert it into byte array (required for tile creation)
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bitmapdata = stream.toByteArray();

        //int bytes = bitmap.getByteCount();


        //ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        //bitmap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer

        //byte[] bitmapdata = buffer.array();
        //bitmap.copyPixelsToBuffer(buffer);
        return new Tile(bitmap.getWidth(), bitmap.getHeight(), bitmapdata);
    }
    @SuppressLint("NewApi")
    protected static int byteSizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        }
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return data.getByteCount();
        }
        else {
            return data.getAllocationByteCount();
        }
    }

    static final double WORLD_WIDTH = 1;
    private Bounds getBounds(int x, int y, int zoom){
        // Convert tile coordinates and zoom into Point/Bounds format
        // Know that at zoom level 0, there is one tile: (0, 0) (arbitrary width 512)
        // Each zoom level multiplies number of tiles by 2
        // Width of the world = WORLD_WIDTH = 1
        // x = [0, 1) corresponds to [-180, 180)

        // calculate width of one tile, given there are 2 ^ zoom tiles in that zoom level
        // In terms of world width units
        double tileWidth = WORLD_WIDTH / Math.pow(2, zoom);


        // Make bounds: minX, maxX, minY, maxY
        double minX = x * tileWidth;
        double maxX = (x + 1) * tileWidth;
        double minY = y * tileWidth;
        double maxY = (y + 1) * tileWidth;
        Bounds b = new Bounds(minX,maxX,minY,maxY);
        return b;
    }
    private LatLng getCentre(Bounds b){
        double cX = b.minX+0.5*(b.maxX-b.minX);
        double cY = b.minY+0.5*(b.maxY-b.minY);
        return mProjection.toLatLng(new Point(cX, cY));
    }
    private LatLng getBottomRight(Bounds b){
        return mProjection.toLatLng(new Point(b.maxX, b.maxY));
    }
}
