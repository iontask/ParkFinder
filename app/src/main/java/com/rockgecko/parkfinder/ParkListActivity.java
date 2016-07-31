package com.rockgecko.parkfinder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class ParkListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty);
        Bundle args = getIntent().getExtras();
        if(savedInstanceState==null) {
            Fragment fragment = new ParkListFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_main, fragment, ParkListFragment.TAG).commit();
        }
    }
}
