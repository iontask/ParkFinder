package com.rockgecko.parkfinder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.rockgecko.parkfinder.util.StringUtils;


public class GenericDetailActivity extends AppCompatActivity {

	public static final String ARG_FRAGMENT_CLASS = "GenericDetailActivity.FragmentClass";
	public static final String ARG_FRAGMENT_TAG = "GenericDetailActivity.FragmentTag";
	public static final String ARG_TITLE = "GenericDetailActivity.Title";
	public static final String ARG_SUBTITLE = "GenericDetailActivity.Subtitle";
	
	
	
	@Override
	public void onCreate(Bundle savedState){
		super.onCreate(savedState);
		FrameLayout view = new FrameLayout(this);		
		view.setId(R.id.fragment_container_main);
		setContentView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		Bundle args = getIntent().getExtras();
		if(savedState==null) {
			Fragment fragment = Fragment.instantiate(this, args.getString(ARG_FRAGMENT_CLASS), args);
			fragment.setArguments(args);
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_main, fragment, args.getString(ARG_FRAGMENT_TAG)).commit();
		}
		String title = args.getString(ARG_TITLE);
		if (!StringUtils.isNullOrEmpty(title)) setTitle(title);
		String subTitle = args.getString(ARG_SUBTITLE);
		if(!StringUtils.isNullOrEmpty(subTitle) && getSupportActionBar()!=null ){
			getSupportActionBar().setSubtitle(subTitle);
		}
	}
}
