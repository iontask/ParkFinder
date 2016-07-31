package com.rockgecko.parkfinder.util;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;



public class StringUtils {
	public static final String localTimeZone="Australia/Melbourne";

	public static final String TICK = new String(new char[]{'\u2713'});



	public static final Calendar getCalendarInstance(long utcTime){
		Calendar cal = getCalendarInstance();
		cal.setTimeInMillis(utcTime);
		return cal;
	}
	public static final Calendar getCalendarInstance(){
		TimeZone tz = TimeZone.getTimeZone(localTimeZone);
		Calendar cal = Calendar.getInstance(tz, Locale.US);						
		cal.setFirstDayOfWeek(Calendar.MONDAY);		
		return cal;
	}

	public static String listToStringWithAmp(Object[] list){
		if(!isNullOrEmpty(list)){
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<list.length; i++){				
				sb.append(list[i]);
				if (i<list.length-2) sb.append(", ");
				else if(i==list.length-2) sb.append(" & ");
				
			}
			return sb.toString();
		}
		return "";
	}
	
	public static String arrayToString(Object[] list, String separator){
		if(!isNullOrEmpty(list)){
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<list.length; i++){				
				sb.append(list[i]);
				if (i<list.length-1) sb.append(separator);								
			}
			return sb.toString();
			
		}
		return "";
	}
    public static String listToString(List list, String separator){
        if(!isNullOrEmpty(list)){
            StringBuffer sb = new StringBuffer();
            for(int i=0; i<list.size(); i++){
                sb.append(list.get(i));
                if (i<list.size()-1) sb.append(separator);
            }
            return sb.toString();
        }
        return "";
    }
	
	public static String intArrayToString(int[] list, String separator){
		if(list!=null && list.length>0){
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<list.length; i++){				
				sb.append(list[i]);
				if (i<list.length-1) sb.append(separator);								
			}
			return sb.toString();
			
		}
		return "";
	}
	
	public static boolean intArrayContains(int[] list, int val){
		if(list!=null) for(int item : list){
			if(item==val) return true;
		}
		return false;
	}
	
	public static boolean isNullOrEmpty(Object[] in){
		return in==null || in.length==0;
	}
	
	public static boolean isNullOrEmpty(List<?> in){
		return in==null || in.size()==0;
	}
	
	public static boolean isNullOrEmpty(String in){
		return in==null || in.length()==0;
	}
	
	public static String isNullOrEmpty(String in, String replace){
		return isNullOrEmpty(in)?replace:in;
	}
	
	public static String toTitleCase(String in){
		if(isNullOrEmpty(in)) return "";
		return Character.toUpperCase(in.charAt(0))+in.substring(1);
		
	}
	
	public static String colourToHTML(int colour){
		return String.format(Locale.US, "#%06X", (0xFFFFFF & colour));
	}
    public static String colourToHex(int colour){
        return String.format(Locale.US, "0x%06X", (0xFFFFFF & colour));
    }
    public static String formatTimespan(long fromTime, long toTime){
        return null;
    }

    public static void appendStyled(SpannableStringBuilder builder, String str, Object... spans) {
        builder.append(str);
        for (Object span : spans) {
            builder.setSpan(span, builder.length() - str.length(), builder.length(), 0);
        }
    }

	public static String formatTimeRelative(long time, long now){
        //Interval interval = new Interval(now, time);
        //interval.toDuration().getStandardMinutes()
        long interval = time-now;
        if(Math.abs(interval)<DateUtils.MINUTE_IN_MILLIS) return "now";
        if(interval>=DateUtils.HOUR_IN_MILLIS) return formatTime(time, now);
        if(interval<-DateUtils.HOUR_IN_MILLIS) return "";
        int mins =(int)( interval/DateUtils.MINUTE_IN_MILLIS);
        int secs = (int) ((interval/DateUtils.SECOND_IN_MILLIS)-(mins*60));
        float minsF = interval/(float)DateUtils.MINUTE_IN_MILLIS;

        StringBuffer result=new StringBuffer();
        if(interval>0) result.append("in ");
        result.append(String.format("%.0f", Math.abs(minsF)));
        //result.append(mins);
        result.append('m');
        if(interval<0) result.append(" ago");
        /*
        if(Math.abs(mins)<5){
            result.append(mins);
            result.append(':');
            result.append(String.format("%02d",secs<30?0:30));
        }
        else{
            result.append(mins);
            result.append('m');
        }*/
        return result.toString();
    }
	public static String formatTimeRelativeOld(long time, long now){
		if(Math.abs(time-now)<DateUtils.HOUR_IN_MILLIS) return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS).toString();
		Calendar calTime = getCalendarInstance(time);
		Calendar calNow = getCalendarInstance(now);
		if(calTime.get(Calendar.DAY_OF_YEAR)==calNow.get(Calendar.DAY_OF_YEAR)){
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return "at " +fmt.format(calTime.getTime());
		}
		else if(calTime.get(Calendar.DAY_OF_YEAR)==(calNow.get(Calendar.DAY_OF_YEAR)+1)){
			//tomorrow
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return "tomorrow, "+fmt.format(calTime.getTime());
		}
		else{
			SimpleDateFormat fmt = new SimpleDateFormat("dd-MM HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return fmt.format(calTime.getTime());
		}
	}
	public static String formatTime(long time, long now){		
		Calendar calTime = getCalendarInstance(time);
		Calendar calNow = getCalendarInstance(now);
		if(calTime.get(Calendar.DAY_OF_YEAR)==calNow.get(Calendar.DAY_OF_YEAR)){
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return fmt.format(calTime.getTime());
		}
		else if(calTime.get(Calendar.DAY_OF_YEAR)==(calNow.get(Calendar.DAY_OF_YEAR)+1)){
			//tomorrow
			SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return "tomorrow, "+fmt.format(calTime.getTime());
		}
		else{
			SimpleDateFormat fmt = new SimpleDateFormat("dd-MM HH:mm", Locale.US); 
			fmt.setTimeZone(calTime.getTimeZone());
			return fmt.format(calTime.getTime());
		}
	}
	
	public static String formatDate(Date date, String dateFormat, TimeZone tz){
		if(tz==null) tz = TimeZone.getTimeZone(localTimeZone);
		SimpleDateFormat fmt = new SimpleDateFormat(dateFormat, Locale.US); 		
		fmt.setTimeZone(tz);
		return fmt.format(date);
		
	}


}

