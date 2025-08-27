package com.growingio.android.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class NonUiContextUtil {

    private static volatile Context sContext;
    private static final Object initLocker = new Object();

    public static Context getWindowContext(Context context) {
        if (context == null || context instanceof Activity) {
            return context;
        }
        // https://developer.android.com/reference/android/content/Context#createWindowContext(int,%20android.os.Bundle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (sContext == null) {
                    synchronized (initLocker) {
                        if (sContext == null) {
                            final DisplayManager dm = context.getSystemService(DisplayManager.class);
                            final Display primaryDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
                            sContext = context.createDisplayContext(primaryDisplay)
                                    .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
                        }
                    }
                }
                if (sContext != null) {
                    return sContext;
                }
            } catch (Exception ignored) {
            }
        }
        return context;
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        DisplayManager displayManager = ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE));
        if (displayManager != null) {
            Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
            if (display != null) display.getRealMetrics(metrics);
            return metrics;
        }
        return context.getResources().getDisplayMetrics();
    }
}
