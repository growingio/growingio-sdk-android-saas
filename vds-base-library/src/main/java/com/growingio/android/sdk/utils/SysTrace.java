package com.growingio.android.sdk.utils;

import android.os.Build;
import android.os.Trace;

/**
 * Just Delegate to Trace
 * Created by liangdengke on 2018/9/27.
 */
public final class SysTrace {

    public static void beginSection(String sectionName){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.beginSection(sectionName);
        }
    }

    public static void endSection(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }
    }
}
