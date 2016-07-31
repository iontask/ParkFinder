package com.rockgecko.parkfinder.dal;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by bramleyt on 31/07/2016.
 */
public class ParkAchievements {
    public String parkName;
    public HashSet<String> faunaFound;
    public HashSet<String> landmarksFound;
    public int maxScore;
    public boolean hasVisited;

    public int getScore() {
        int score= faunaFound.size()+landmarksFound.size();
        if(hasVisited)score++;
        return score;
    }
}
