package com.growingio.android.sdk.utils;

/**
 * just like Objects
 * Created by liangdengke on 2018/7/18.
 */
public class ObjectUtils {

    public static boolean equals(Object objL, Object objR){
        return objL == objR || (objL != null && objL.equals(objR));
    }

    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }

    public static String toString(Object o) {
        return String.valueOf(o);
    }
}
