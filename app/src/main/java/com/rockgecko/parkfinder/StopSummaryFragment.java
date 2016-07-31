package com.rockgecko.parkfinder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.TreeMap;
import java.util.Date;
import java.util.List;


import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.rockgecko.parkfinder.dal.FavouriteStop;
import com.rockgecko.parkfinder.dal.IStop;
import com.rockgecko.parkfinder.dal.StopService;
import com.rockgecko.parkfinder.ptv.PtvDataFetcher;
import com.rockgecko.parkfinder.util.CellAdapterWithEverything;
import com.rockgecko.parkfinder.util.StringUtils;
import com.rockgecko.parkfinder.util.UIUtil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ListView;

import android.widget.Toast;


public class StopSummaryFragment extends Fragment implements OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

	private AQuery aq;
	private IStop mStop;
	private boolean mHideCombinedRoutes=true;
	private boolean mIsTomorrows=false;
	//private ServiceListAdapter mAdapter;
	private SwipeRefreshLayout mRefreshLayout;
	private ListView mList;
	private boolean mGetAllForToday;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){		
		mList = new ListView(inflater.getContext());
		mList.setOnItemClickListener(this);
		
		mRefreshLayout = new SwipeRefreshLayout(inflater.getContext());		
		mRefreshLayout.addView(mList, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mRefreshLayout.setOnRefreshListener(this);
		mRefreshLayout.setColorSchemeResources(R.color.holo_orange_light, R.color.holo_orange_dark, R.color.holo_blue_light, R.color.holo_green_light);
		return mRefreshLayout;
	}
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);		
		aq=new AQuery(view);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

	}
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		//inflater.inflate(R.menu.fragment_stopsummary,menu);
		super.onCreateOptionsMenu(menu, inflater);

	}
    private Drawable getFavouriteDrawable(boolean favourited){
        Drawable d = getResources().getDrawable(favourited?R.drawable.abc_btn_rating_star_on_mtrl_alpha:R.drawable.abc_btn_rating_star_off_mtrl_alpha);
        Drawable wrapped = DrawableCompat.wrap(d);
        DrawableCompat.setTint(wrapped, getResources().getColor(R.color.theme_highlight));
        return wrapped;
    }
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.menu_refresh:
			refreshServices(false, true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void setStop(IStop stop){
        setStop(stop, false);
    }
	private void setStop(IStop stop, boolean isTomorrows){
		this.mStop=stop;
        this.mIsTomorrows=isTomorrows;
		ServiceListAdapter adapter=null;
		List<StopService> adapterData = mStop!=null ? mStop.getServices():null;
		if(getListAdapter()!=null){
			adapter = getListAdapter();
			adapter.updateData(adapterData);
		}
		else if(mList!=null){
			adapter = new ServiceListAdapter(adapterData);
			mList.setAdapter(adapter);
		}
		if(mGetAllForToday && mStop!=null && mStop.hasCompleteServices(true) && mList!=null){
			int position=-1;
			Date justNow = new Date(System.currentTimeMillis()-2*DateUtils.MINUTE_IN_MILLIS);
			//iterate over filtered and sorted adapter items
			//to find the first service after now.
			for(int i=0;i<adapter.getCount();i++){
				Object item = adapter.getItem(i);
				if(!(item instanceof StopService)) continue;
				StopService stopService = (StopService) item;
				if(stopService!=null && stopService.getTime().after(justNow)){
					position = i==0?0:i-1;
					break;
				}
			}
			if(position<0) position = adapter.getCount()-1; //means all services are Before now, go to end.
			mList.setSelectionFromTop(position, 0);
			mGetAllForToday=false;
		}
		getActivity().supportInvalidateOptionsMenu();
	}
	
	private ServiceListAdapter getListAdapter() {
		if(mList!=null) 
			return (ServiceListAdapter) mList.getAdapter();
		return null;
	}
	public IStop getStop(){
		return mStop;
	}
	
	private IRunSelectionCallback getRunSelectionCallback(){
		return ((MapActivity) getActivity()).getRunSelectionCallback();
	}
	
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position,
			long id) {
		Object item = l.getItemAtPosition(position);
		if(item instanceof StopService){
			Checkable checkbox = (Checkable) v.findViewById(R.id.run_checked);
			checkbox.toggle();
			getRunSelectionCallback().setRunSelected((StopService)item, checkbox.isChecked());
		}

		else if (item instanceof SeparatorItem){
            //footer
            int footerId = ((SeparatorItem) item).id;
            switch (footerId) {
                case R.string.load_services_tomorrow:
                    TreeMap<String, StopService> uniqueRoutesMap = new TreeMap<>();
                    for (StopService service : getListAdapter().mStopData) {
                        String directionName = service.getDirection().getDirection_name();
                        if(directionName.contains("City")) directionName="zzz"+directionName;//make city directions after down directions
                        uniqueRoutesMap.put(service.getDirection().getLine().getLine_number_formatted() + "_" + directionName,
                                service);
                    }
                    final List<StopService> uniqueRoutes = new ArrayList<>(uniqueRoutesMap.values());
                    CharSequence[] labels = new CharSequence[uniqueRoutes.size()];
                    for (int i = 0; i < uniqueRoutes.size(); i++) {
                        StopService service = uniqueRoutes.get(i);
                        SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
                        StringUtils.appendStyled(spanBuilder, service.getDirection().getLine().getLine_number_formatted(),
                                new ForegroundColorSpan(service.getColourForLineAndDirection()),
                                new StyleSpan(Typeface.BOLD));
                        spanBuilder.append(' ');
                        spanBuilder.append(service.getDirection().getDirection_name());
                        labels[i] = spanBuilder;
                    }

                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Calendar cal = StringUtils.getCalendarInstance();
                            cal.add(Calendar.DAY_OF_YEAR, 1);
                            final StopService service = uniqueRoutes.get(which);
                            mRefreshLayout.setRefreshing(true);
                            new PtvDataFetcher(getActivity()).getSpecificServicesForStop(
                                    service.getDirection().getLine().getTransport_type_int(),
                                    service.getDirection().getLine().getLine_id(),
                                    mStop.getId(),
                                    service.getDirection().getDirection_id(), new AjaxCallback<IStop>() {
                                        @Override
                                        public void callback(String url, IStop result, AjaxStatus status) {
                                            if (result != null) {
                                                setStop(result, true);
                                            }
                                            else if (status.getCode()==200 || status.getError()==null){
                                                Toast.makeText(getActivity(), getString(R.string.no_services_for_tomorrow,
                                                        service.getDirection().getLine().getLine_number_formatted(),
                                                        service.getDirection().getDirection_name()), Toast.LENGTH_SHORT).show();
                                            }
                                            mRefreshLayout.setRefreshing(false);
                                        }

                                    }, 0, cal.getTimeInMillis());
                        }
                    };


                    if (uniqueRoutes.size()==1){
                        listener.onClick(null, 0);
                    }
                    else if(uniqueRoutes.size()>1){
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.load_services_tomorrow_dialog);
                        builder.setItems(labels, listener);
                        builder.show();
                    }
                    break;
                case R.string.load_all_services:
                    if(!mStop.hasCompleteServices(true)) refreshServices(true, true);
                    break;
                case R.string.reset:
                    refreshServices(false, true);
                    break;
            }
            }


		
	}

	class SeparatorItem{
		public String title;
		public int id;
		public int icon;
		public Drawable iconDrawable;

		SeparatorItem(int id, String title, int icon) {
			this.id = id;
			this.title = title;
			this.icon = icon;
		}
		public String toString(){return title;}
	}
	
	class ServiceListAdapter extends CellAdapterWithEverything{

		private List<StopService> mStopData;
		private List<SeparatorItem> mSeparatorData;
		public ServiceListAdapter(List<StopService> stopData) {
			super(0, 3, 3, 0);
			updateData(stopData);
		}
		

		void updateData(List<StopService> stopData){
			mStopData = new ArrayList<>();
			mSeparatorData= new ArrayList<>();
			if(stopData!=null){
				for(StopService service : stopData){
					if(!(mHideCombinedRoutes && service.getDirection().getLine().isCombinedLine())){									
						mStopData.add(service);
					}
				}
				//sorted by next departure time
				Collections.sort(mStopData, StopService.TimeComparator);
				
				if(mStop!=null && !mStop.hasCompleteServices(true) && !mIsTomorrows){
					//setNumHeadersAndFooters(0, 2);
					mSeparatorData.add(new SeparatorItem(R.string.load_all_services, getString(R.string.load_all_services), R.drawable.ic_expand));
					mSeparatorData.add(new SeparatorItem(R.string.load_services_tomorrow, getString(R.string.load_services_tomorrow), R.drawable.ic_expand));

				}
				else{
				//	setNumHeadersAndFooters(0, 1);
					mSeparatorData.add(new SeparatorItem(R.string.reset, getString(R.string.reset), R.drawable.ic_reset));
				}
			}

			
			notifyDataSetChanged();
		}
		final float DEFAULT_ROUTE_TEXT_SIZE = 20;
		final int ROUTE_SUB_TEXT_SIZE = 9;
		final long REALTIME_HIDE_AFTER = 10*DateUtils.MINUTE_IN_MILLIS;

		@SuppressWarnings("ConstantConditions")
		public View getStopServiceView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_stop_service, parent, false);
				View checkbox = convertView.findViewById(R.id.run_checked);
				checkbox.setClickable(false);
				checkbox.setFocusable(false);
			}
			aq.recycle(convertView);
			final StopService stopService = (StopService) getItem(position);

            aq.id(R.id.text1).text(stopService.getDirection().getDirection_name());
			Date timetabled = stopService.getTime();
			Date realtime = stopService.getRealtime();
			long now = System.currentTimeMillis();
			if(realtime!=null && (now-timetabled.getTime()<REALTIME_HIDE_AFTER)){
				//Realtime
				aq.id(R.id.time_realtime)
						.text(StringUtils.formatTimeRelative(realtime.getTime(), now))
							//+"\n"+StringUtils.formatDate(realtime, "HH:mm ss",null))
						.textColor(UIUtil.colourForDelay(stopService.getTime(), realtime))
						.visible();
			}
            else{
                aq.id(R.id.time_realtime).gone();
            }
			String subText = String.format("%s - %s", StringUtils.formatTime(timetabled.getTime(),
							now),
					stopService.getDirection().getLine().getLine_name_formatted());
			aq.id(R.id.text2).text(subText);

            String routeNumberSub=stopService.getDirection().getLine().getRouteNumberSubText();
			String lineNumberFormatted =StringUtils.isNullOrEmpty(stopService.getDirection().getLine().getLine_number_formatted(), "");
			int lineColour = stopService.getColourForLineAndDirection();
			Drawable disruptionIcon=null;

            if(routeNumberSub!=null){
                SpannableStringBuilder routeNumber = new SpannableStringBuilder(lineNumberFormatted);
                StringUtils.appendStyled(routeNumber, "\n"+routeNumberSub,
                        new AbsoluteSizeSpan(ROUTE_SUB_TEXT_SIZE, true), new ForegroundColorSpan(getResources().getColor(R.color.nightrider)));

                aq.id(R.id.route_number).textColor(lineColour).textSize(DEFAULT_ROUTE_TEXT_SIZE).text(routeNumber).getView().setBackgroundDrawable(disruptionIcon);
            }
			else {
                float size = lineNumberFormatted.length() > 3 ? 10 : DEFAULT_ROUTE_TEXT_SIZE;
                aq.id(R.id.route_number).text(lineNumberFormatted).textColor(lineColour).textSize(size).getView().setBackgroundDrawable(disruptionIcon);
            }

			aq.id(R.id.run_checked).checked(getRunSelectionCallback().isRunSelected(stopService));
			aq.id(R.id.chink).clicked(new OnClickListener() {
				
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse(getString(R.string.route_link_format, stopService.getDirection().getLine().getLine_id())));
					startActivity(intent);
					/*PopupMenu menu = new PopupMenu(v.getContext(), v);
					menu.inflate(R.menu.ctx_stop_summary);
					menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							Intent intent=null;
							switch(item.getItemId()){

							case R.id.menu_route_details:
								intent = new Intent(Intent.ACTION_VIEW,
										Uri.parse(getString(R.string.route_link_format, stopService.getDirection().getLine().getLine_id())));								
								break;
							}
							if(intent!=null){
								startActivity(intent);
								return true;
							}
							return false;
						}
					});
					menu.show();*/
				}
			});
			
			return convertView;
		}




		public View getSeparatorView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_stop_service_loadall, parent, false);
			}
			aq.recycle(convertView);
			SeparatorItem item = (SeparatorItem) getItem(position);
			aq.id(R.id.text1).text(item.title);
			if(item.iconDrawable!=null)aq.id(R.id.imageView1).image(item.iconDrawable);
			else aq.id(R.id.imageView1).image(item.icon);
			return convertView;
		}


		static final int VIEW_TYPE_STOP=0;
		static final int VIEW_TYPE_SEPARATOR=1;
		public int getItemViewTypeForDataBetweenHeadersAndFooters(int position){
			int dataSetIndex = getIntermediateDataSetIndex(position);
			if(dataSetIndex==0) return VIEW_TYPE_STOP;
			if(dataSetIndex==1) return VIEW_TYPE_SEPARATOR;
			return IGNORE_ITEM_VIEW_TYPE;
		}

		@Override
		public List<?> getIntermediateDataSet(int dataSetIndex) {
			if(dataSetIndex==0) return mStopData;
			if(dataSetIndex==1) return mSeparatorData;
			return null;

		}

		@Override
	    public boolean isEnabled(int position) {
			if(isFooter(position)) return true;
			Object item = getItem(position);
			if(item instanceof SeparatorItem && ((SeparatorItem)item).id==R.string.disruptions_title) return false;
			return super.isEnabled(position);			
	    }

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int viewType = getItemViewType(position);
			if(viewType==VIEW_TYPE_STOP) return getStopServiceView(position, convertView, parent);
			if(viewType==VIEW_TYPE_SEPARATOR) return getSeparatorView(position, convertView, parent);
			return getHeaderOrFooterView(position, convertView, parent);
		}

		@Override
		public View getHeaderView(int headerIndex, ViewGroup parent) {
			return null;
		}
		@Override
		public View getFooterView(int footerIndex, ViewGroup parent) {
			return null;
		}
		@Override
		public boolean isHeaderVisible(int headerIndex) {
			return false;
		}
		@Override
		public boolean isFooterVisible(int footerIndex) {
			return false;
		}
	}



	@Override
	public void onRefresh() {
		//from refreshLayout, no need to tell it to refresh as it already is
		refreshServices(false, false);
	}
	
	private void refreshServices(boolean getAllForDay, boolean tellRefreshLayout){
		mGetAllForToday=getAllForDay;
		if(tellRefreshLayout)mRefreshLayout.setRefreshing(true);
		getRunSelectionCallback().getStopAndServices(mStop.getId(), mStop.getMode(), getAllForDay?0:GMapFragment.NEXT_DEPARTURES_LIMIT, true, new IActionComplete<IStop>() {
			@Override
			public void done(IStop result) {
				mRefreshLayout.setRefreshing(false);
				if(result!=null){
					setStop(result);
				}
			}
		});
	}
	

	
	
	
	
}
