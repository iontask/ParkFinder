package com.rockgecko.parkfinder.dal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.rockgecko.parkfinder.ptv.models.DtoDirection;
import com.rockgecko.parkfinder.ptv.models.DtoRun;
import com.rockgecko.parkfinder.util.UIUtil;

public class StopService implements Serializable {

	private String stopId;
	private DtoDirection direction;
	private DtoRun run;
	private Date timetabled;
	//private Date correctedTime;
	private Date realtime;
	
	/**
	 * Run sort order
	 */
	private int order;
	
	public DtoDirection getDirection() {
		return direction;
	}
	public void setDirection(DtoDirection direction) {
		this.direction = direction;
	}
	public DtoRun getRun() {
		return run;
	}
	public void setRun(DtoRun run) {
		this.run = run;
	}
	public Date getTime() {
		return timetabled;
	}
	public void setTime(Date time) {
		this.timetabled = time;
	}
	/*
	public Date getCorrectedTime() {
		if(correctedTime==null) return new Date(0);
		return correctedTime;
	}
	public void setCorrectedTime(Date correctedTime) {
		this.correctedTime = correctedTime;
	}*/
	public Date getRealtime() {
		return realtime;
	}
	public void setRealtime(Date realtime) {
		this.realtime = realtime;
	}
	public String getStopId() {
		return stopId;
	}
	public void setStopId(String stopId) {
		this.stopId = stopId;
	}
	
	
	
	@Override
	public boolean equals(Object other){
		if(other instanceof StopService){
			return stopId.equals(((StopService) other).stopId) 
					&& run.getRun_id().equals(((StopService) other).getRun().getRun_id());
		}
		return super.equals(other);
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	
	public int getColourForLineAndDirection(){
		return UIUtil.getColour(direction.getLine().getLine_number_formatted() + "_" + direction.getDirection_id());
	}


	public static final List<String> getRouteNumberList(List<StopService> in){
		List<String> routeNumbers = new ArrayList<String>();
		List<StopService> services = new ArrayList<StopService>();
		services.addAll(in);
		Collections.sort(services, StopService.RouteNumberComparator);
		for(StopService service : services){
			String lineNumber =service.getDirection().getLine().getLine_number_formatted();
			if(!routeNumbers.contains(lineNumber)) routeNumbers.add(lineNumber);
		}
		return routeNumbers;
	}



	public static final Comparator<StopService> TimeComparator = new Comparator<StopService>() {
		@Override
		public int compare(StopService lhs, StopService rhs) {						
			return lhs.getTime().compareTo(rhs.getTime());
		}
	};
	
	public static final Comparator<StopService> RouteNumberComparator = new Comparator<StopService>() {
		@Override
		public int compare(StopService lhs, StopService rhs) {
			int lhsRoute = lhs.getDirection().getLine().getLine_number_int();
			int rhsRoute = rhs.getDirection().getLine().getLine_number_int();
			int compare = lhsRoute-rhsRoute;
			if(compare==0) return TimeComparator.compare(lhs, rhs);
			return compare;
		}
	};
}
