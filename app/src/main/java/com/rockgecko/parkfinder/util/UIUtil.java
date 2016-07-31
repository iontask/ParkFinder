package com.rockgecko.parkfinder.util;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.text.format.DateUtils;

import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.R;

public class UIUtil {
	
	public static final float DENSITY;
	
	public static final int[] colors = new int[]{
		0xFF008000, 0xFF007BA7, 0xFF0047AB, 0xFF00bfff, 0xFF1560BD, 0xFF446CCF, 0xFF7DF9FF, //blue
		0xFF004626, 0xFF8DB600, 0xFF80FF00, 0xFF00FF6F, 0xFF009900, 0xFF4CBB17, 0xFF57D200, //green
		0xFF6C256F, 0xFFA115A6, 0xFF87208A, 0xFF522553, 0xFF371E38, 0xFF6C256F, 0xFF471249, // purple http://paletton.com/#uid=14V0u0klllls1vWoGqDh+g2eEaK
		0xFFA4C639, 0xFF915C83, 0xFFFF2052, 0xFF98777B, 0xFFBF94E4, 0xFFF4BBFF, 0xFFff5865, //random
		0xFFC41E3A, 0xFF007BA7, 0xFF36454F, 0xFFff868f, 0xFF220523, 0xFF008B8B, 0xFF779ECB, //random
		0xFF85BB65, 0xFF6F00FF, 0xFF924095, 0xFF3F00FF, 0xFF86608E, 0xFF808000, 0xFF5A4FCF //random to "iris"
		
	};

	//removed
	// 0xFFFDEE00, 0xFFffd700, 0xFFF7F75E, 0xFFFBEC5D, 0xFFFADA5E, 0xFFEEE600, 0xFFffff00, //yellow - crap

    static{
        float d;
        try{
            d = ParkFinderApplication.getContextStatic().getResources().getDisplayMetrics().density;
        }
        catch(Exception e){
            d =1;
        }
        DENSITY=d;
    }

	public static int getColour(String in){
		Random rand = new Random(in.hashCode());
		return colors[rand.nextInt(colors.length)];
	}

	public static int colourForDelay(Date timetable, Date realtime){
		if(realtime==null) return Color.BLACK;
		long delay = realtime.getTime()-timetable.getTime();
		//more than 1 min early
		if(delay<-DateUtils.MINUTE_IN_MILLIS) return ParkFinderApplication.getContextStatic().getResources().getColor(R.color.holo_blue_dark);
		if(delay<4*DateUtils.MINUTE_IN_MILLIS) return ParkFinderApplication.getContextStatic().getResources().getColor(R.color.holo_green_dark);
		if(delay<6*DateUtils.MINUTE_IN_MILLIS) return ParkFinderApplication.getContextStatic().getResources().getColor(R.color.nightrider);
		return Color.RED;
	}
	
	public static float scale(float in){
		return in*DENSITY;
	}
	public static float unScale(float in){
		return in/DENSITY;
	}
	public static int scaleLayoutParam(int in){
		return (int) (in*DENSITY+0.5f);
	}
	public static Intent getSupportIntent(Context context){
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("plain/text");
		sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {context.getString(R.string.support_email)});
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_contact_msg_subject));
        String msg = getSupportInfo(context);
		sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        return sendIntent;
	}


	
	public static String getSupportInfo(Context context){
		String versionName ="0";
		int versionCode=0;
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionName=info.versionName;
			versionCode=info.versionCode;
		} catch (NameNotFoundException e) {

		}
		String version;
		String applicationName = "Application name: " + context.getString(R.string.app_name);
		String packageName= "Package name: " + context.getPackageName();
		
		String platformInfo = "Device name: "
			+ android.os.Build.MANUFACTURER + " "
			+ android.os.Build.MODEL + "\n"
			+ "API version: "			
			+ android.os.Build.VERSION.SDK_INT +" - "
			+ android.os.Build.VERSION.RELEASE;
		
		
			
			version="Version: " +versionName + " build " + versionCode;
			if(ParkFinderApplication.DEBUG) version+=" DEBUG";
		//JsonData jsonData = JsonData.getInstance(context);
		//String ttVersion = "Timetable version: " + " Main: " + jsonData.getParkFinderData().getVersionName()
		//		+" Bus: "+jsonData.getParkFinderData().getVersionName();
		String msg = "-------------\nBasic information on your device is included below.\n-------------\n"
			+ applicationName + "\n"
			+ packageName + "\n"
			+ version + "\n"	
			//+ ttVersion + "\n"
			+ platformInfo  + "\n-------------\n";	
		return msg;
	}
	public static Bitmap getResizedBitmap(Bitmap in, int newWidth){
		float aspectRatio = in.getWidth() /
				(float) in.getHeight();

		int height = Math.round(newWidth / aspectRatio);

		return getResizedBitmap(in, newWidth, height);
	}

	public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// CREATE A MATRIX FOR THE MANIPULATION
		Matrix matrix = new Matrix();
		// RESIZE THE BIT MAP
		matrix.postScale(scaleWidth, scaleHeight);

		// "RECREATE" THE NEW BITMAP
		Bitmap resizedBitmap = Bitmap.createBitmap(
				bm, 0, 0, width, height, matrix, false);
		bm.recycle();
		return resizedBitmap;
	}
}
