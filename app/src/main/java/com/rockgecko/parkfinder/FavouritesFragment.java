package com.rockgecko.parkfinder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class FavouritesFragment extends Fragment {
    public static final String TAG = "FavouritesFragment";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cell_empty, container, false);
        return view;
    }
}
