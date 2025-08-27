package com.growingio.android.sdk.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SystemUtil {
    private static final String TAG = "GIO.SystemUtil";

    private SystemUtil() {
    }

    public static String getProcessName() {
        if (Build.VERSION.SDK_INT >= 28) {
            return Application.getProcessName();
        }

        String processName = null;
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            // Before API 18, the method was incorrectly named "currentPackageName", but it still returned the process name
            // See https://github.com/aosp-mirror/platform_frameworks_base/commit/b57a50bd16ce25db441da5c1b63d48721bb90687
            String methodName = Build.VERSION.SDK_INT >= 18 ? "currentProcessName" : "currentPackageName";

            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            processName = (String) getProcessName.invoke(null);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        if (processName == null && Build.VERSION.SDK_INT >= 19) {
            try {
                try (BufferedReader mBufferedReader = new BufferedReader(new FileReader(new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline")))) {
                    processName = mBufferedReader.readLine().trim();
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return processName;
    }

    public static boolean isMainProcess(Context context) {
        try {
            String processName = getProcessName();
            return !TextUtils.isEmpty(processName) && processName.equals(context.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    public static Set<Integer> getRunningProcess(Context context) {
        LogUtil.d(TAG, "getRunningProcess:" + Process.myPid());
        Set<Integer> myRunningProcess = new HashSet<>();
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
            int myUid = Process.myUid();
            for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
                if (myUid == info.uid) {
                    myRunningProcess.add(info.pid);
                }
            }
        } catch (Throwable e) {
            // for System Service Died exception
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return myRunningProcess;
    }

}
