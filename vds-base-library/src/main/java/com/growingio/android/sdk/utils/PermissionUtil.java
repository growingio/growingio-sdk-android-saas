package com.growingio.android.sdk.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.VisibleForTesting;

/**
 * Created by zyl on 15/4/26.
 */
public class PermissionUtil {
    private static final String TAG = "GIO.permission";

    private static final int FLAG_INTERNET = 1;
    private static final int FLAG_ACCESS_NETWORK_STATE = 1 << 1;
    private static final int FLAG_EXTERNAL_STORAGE = 1 << 2;
    private static final int FLAG_READ_PHONE_STATE = 1 << 3;

    private int mPermissionFlags = 0;
    private final PackageManager mPackageManager;
    private final String mPackageName;

    private static PermissionUtil s_Instance;

    @VisibleForTesting
    PermissionUtil(PackageManager packageManager, String packageName){
        mPackageManager = packageManager;
        mPackageName = packageName;
    }

    public static boolean hasInternetPermission() {
        return s_Instance.checkPermission(Manifest.permission.INTERNET, FLAG_INTERNET);
    }

    public static boolean hasAccessNetworkStatePermission() {
        return s_Instance.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, FLAG_ACCESS_NETWORK_STATE);
    }

    public static boolean hasWriteExternalPermission() {
        return s_Instance.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, FLAG_EXTERNAL_STORAGE);
    }

    public static boolean checkReadPhoneStatePermission() {
        return s_Instance.checkPermission(Manifest.permission.READ_PHONE_STATE, FLAG_READ_PHONE_STATE);
    }

    public static void init(Context context) {
        s_Instance = new PermissionUtil(context.getPackageManager(), context.getPackageName());
    }

    @VisibleForTesting
    boolean checkPermission(String permissionName, int flag){
        if ((mPermissionFlags & flag) != 0){
            return true;
        }
        boolean hasPermission;
        try{
            hasPermission = PackageManager.PERMISSION_GRANTED == mPackageManager.checkPermission(permissionName, mPackageName);
        }catch (Throwable e){
            hasPermission = false;
            LogUtil.d(TAG, "checkPermission failed: ", e);
        }
        if (hasPermission){
            mPermissionFlags |= flag;
            return true;
        }
        return false;
    }
}
