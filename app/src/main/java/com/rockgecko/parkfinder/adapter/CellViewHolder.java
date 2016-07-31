package com.rockgecko.parkfinder.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;
import com.rockgecko.parkfinder.util.AQueryEx;


/**
 * Created by Bramley on 4/08/2015.
 */
public class CellViewHolder extends RecyclerView.ViewHolder{
    protected AQueryEx aq;
    public CellViewHolder(View itemView) {
        super(itemView);
    }
    public CellViewHolder(ViewGroup parent, int layoutId) {
        super(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
    }
    public AQueryEx aq(){
        if(aq==null) aq=new AQueryEx(itemView);
        return aq;
    }
    protected Context getContext(){
        return itemView.getContext();
    }
    protected Resources getResources(){
        return itemView.getResources();
    }
    protected String getString(int resID){
        return itemView.getContext().getString(resID);
    }
    protected String getString(int resID, Object... formatArgs){
        return itemView.getContext().getString(resID, formatArgs);
    }
}
