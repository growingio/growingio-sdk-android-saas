package com.growingio.android.sdk.utils;

/**
 * Created by liangdengke on 2018/12/20.
 */
public class ThreadUtils {

    private static Runnable nextRunnable;

    public static void cancelTaskOnUiThread(Runnable task) {
        if (nextRunnable != null && nextRunnable == task){
            nextRunnable = null;
        }
    }

    public static void run(){
        if (nextRunnable != null){
            nextRunnable.run();
            nextRunnable = null;
        }
    }

    public static void runOnUiThread(Runnable runnable){
        nextRunnable = runnable;
    }

    public static void postOnUiThread(Runnable task){
        nextRunnable = task;
    }

    public static boolean runningOnUiThread(){
        return false;
    }
}
