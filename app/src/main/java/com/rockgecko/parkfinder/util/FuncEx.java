package com.rockgecko.parkfinder.util;

import java.util.ArrayList;

/**
 * Created by bramleyt on 30/07/2016.
 */
public class FuncEx {
    /**
     * Returns true if two possibly-null objects are equal.
     */
    public static boolean equal(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
    public static ArrayList<Integer> toIntList(int... xs){
        ArrayList<Integer> to = new ArrayList<>();
        for (int x : xs) {
            to.add(x);
        }
        return to;
    }
}
