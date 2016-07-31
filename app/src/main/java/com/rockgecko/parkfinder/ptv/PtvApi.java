package com.rockgecko.parkfinder.ptv;

import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List;
import java.util.ArrayList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.rockgecko.parkfinder.ParkFinderApplication;
import com.rockgecko.parkfinder.R;
import com.rockgecko.parkfinder.util.StringUtils;

public class PtvApi {
	
	/**
	 * 	Train (metropolitan)
	 */
	public static final int POI_TRAIN = 0;
	/**
	* 1	Tram
	*/
	public static final int POI_TRAM = 1;
	/**
	 * 2	Bus (metropolitan and regional, but not V/Line)
	 */
	public static final int POI_BUS = 2;
	/**
	*3	V/Line regional train and coach
	*/
	public static final int POI_VLINE = 3;
	/**
	*4	NightRider
	*/
	public static final int POI_NIGHTRIDER = 4;
	/**
	 * 100	Ticket outlet
	 */
	public static final int POI_TICKET_OUTLET = 100;

    public static final String DATE_FORMAT_DISRUPTIONS = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	static final SimpleDateFormat DATE_ISO;

    public static String URL_API;
    public static String URL_WEB;

	static{
		DATE_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); 
		DATE_ISO.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

    public static void setUrls(String urlApi,String urlWeb) {
        URL_API = urlApi;
        URL_WEB=urlWeb;
    }

    /**
	 * nearestStops returns up to 30 stops nearest to a specified coordinate.
	 * "Stops" includes train stations as well as tram and bus stops.
	 * @param latitude
	 * @param longitude
	 * @return
	 */
	public static String nearestStops(double latitude, double longitude){
		String uri = String.format("nearme/latitude/%f/longitude/%f", latitude, longitude);
		return generateCompleteURLWithSignature(uri);
	}


	
	/**
	 * 
	 * @param pois
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @param griddepth the number of cells per block of cluster grid (between 0-20 inclusive).
	 * @param limit the minimum number of POIs (stops or outlets) required to create a cluster, 
	 * as well as the maximum number of POIs returned as part of a cluster in the JSON response 
	 * (for example, if the limit is �4�, at least 4 POIs are required to form a cluster; and in 
	 * the JSON response, if there are 7 total locations in a cluster, only 4 will be listed in
	 *  the response)
	 * @return
	 */
	public static String poi(int[] pois, double lat1, double lon1, double lat2, double lon2, int griddepth, int limit){
		String uri = String.format("poi/%s/lat1/%f/long1/%f/lat2/%f/long2/%f/griddepth/%d/limit/%d",
				StringUtils.intArrayToString(pois, ","),
				lat1, lon1, lat2, lon2, griddepth, limit
				);
		return generateCompleteURLWithSignature(uri);
	}
	
	/**
	 * 
	 * @param mode
	 * @param stopId
	 * @param limit
	 * @return
	 */
	public static String nextDeparturesForStop(int mode, String stopId, int limit){
		String uri = String.format("mode/%d/stop/%s/departures/by-destination/limit/%d", mode, stopId, limit);
		return generateCompleteURLWithSignature(uri);
	}
    public static String specificNextDeparturesForStop(int mode, String lineId, String stopId, String directionId, int limit, long time){
        String uri = String.format("mode/%d/line/%s/stop/%s/directionid/%s/departures/all/limit/%d?for_utc=%s",
                mode, lineId, stopId, directionId, limit, DATE_ISO.format(new Date(time)));
        return generateCompleteURLWithSignature(uri);
    }
	/**
	 * run	=	the run_id of the requested run
e.g. �1464�
stop	=	the stop_id of the stop
e.g. �1108�
for_utc	=	the date and time of the request in ISO 8601 UTC format
e.g. 2013-11-13T05:24:25Z

	 * @return
	 */
	public static String stoppingPattern(int mode, String runId, String stopId, long time){
		
		String uri = String.format("mode/%d/run/%s/stop/%s/stopping-pattern?for_utc=%s", mode, runId, stopId, DATE_ISO.format(new Date(time)));
		return generateCompleteURLWithSignature(uri);
	}
	

    private static String boolToInt(boolean in){
        return in?"1":"0";
    }

	
	/**
     * Generates a signature using the HMAC-SHA1 algorithm 
     * 
     * @param privateKey - Developer Key supplied by PTV
     * @param uri - request uri (Example :/v2/HealthCheck) 
     * @param developerId - Developer ID supplied by PTV
     * @return Unique Signature Value  
     */
    private static String generateSignature(final String privateKey, final String uri, final int developerId)
    {
        String encoding = "UTF-8";
        String HMAC_SHA1_ALGORITHM = "HmacSHA1";
        String signature;
        StringBuffer uriWithDeveloperID = new StringBuffer();
        uriWithDeveloperID.append(uri).append(uri.contains("?") ? "&" : "?").append("devid="+developerId);     
        try
        {
            byte[] keyBytes = privateKey.getBytes(encoding);
            byte[] uriBytes = uriWithDeveloperID.toString().getBytes(encoding);
            Key signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);
            byte[] signatureBytes = mac.doFinal(uriBytes);
            StringBuffer buf = new StringBuffer(signatureBytes.length * 2);
            for (byte signatureByte : signatureBytes)
            {
                int intVal = signatureByte & 0xff;
                if (intVal < 0x10)
                {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(intVal));
            }
            signature = buf.toString();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvalidKeyException e)
        {
            throw new RuntimeException(e);
        }
        return signature.toUpperCase();
    }
    
    /**
     * Generate full URL using generateSignature() method
     *      
     * @param methodUri - request method uri (Example :"mode/2/line/787/stops-for-line)  
     * @return - Full URL with Signature
     */
    public static String generateCompleteURLWithSignature(final String methodUri)
    {
        final String uri = "/v2/"+methodUri;
        final int DEVELOPER_ID = Integer.parseInt(ParkFinderApplication.getContextStatic().getString(R.string.ptv_developer_id));
        final String PRIVATE_KEY = ParkFinderApplication.getContextStatic().getString(R.string.ptv_api_key);
        StringBuffer url = new StringBuffer(URL_API).append(uri).append(uri.contains("?") ? "&" : "?")
        		.append("devid="+DEVELOPER_ID)
        		.append("&signature="+generateSignature(PRIVATE_KEY, uri, DEVELOPER_ID));
        return url.toString();
      
    }


}
