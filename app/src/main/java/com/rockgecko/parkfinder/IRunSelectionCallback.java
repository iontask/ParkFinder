package com.rockgecko.parkfinder;

import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;

import java.util.List;

public interface IRunSelectionCallback {

	public void setRunSelected(StopService stopService, boolean selected);
	public boolean isRunSelected(StopService stopService);	
	public void fetchCompleteRun(StopService stopService, IActionComplete<List<IStop>> callback);
	public void getStopAndServices(String stopId, int mode, int nextDeparturesLimit, boolean forceRefresh, IActionComplete<IStop> callback);
	
	
	
}