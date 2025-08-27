package com.growingio.android.sdk.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Author: tongyuzheng
 * <br/>
 * Date: 2016/9/05
 * <br/>
 * Description: 用于控制数据量较少且变更不频繁的数据的存储和读取
 */

public class PersistUtil {
    private static final String DEVICE_ID_KEY = "device_id";
    private static final String HOST_INFORMATION_KEY = "host_info";
    private static final String PERSIST_FILE_NAME = "growing_persist_data";
    private static final String LAST_VERSION = "host_app_last_version";

    private static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        if (sharedPreferences == null){
            sharedPreferences = context.getSharedPreferences(PERSIST_FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    public static String fetchDeviceId() {
        return sharedPreferences.getString(DEVICE_ID_KEY, null);
    }

    public static void saveDeviceId(String deviceId) {
        sharedPreferences.edit().putString(DEVICE_ID_KEY, deviceId).commit();
    }

    /**
     * 获取存储在本地的HttpDNS后的数据
     */
    public static String fetchHostInformationData() {
        return sharedPreferences.getString(HOST_INFORMATION_KEY, null);
    }

    /**
     * 存储HttpDNS后的数据
     */
    public static void saveHostInformationData(String hostInformationData) {
        sharedPreferences.edit().putString(HOST_INFORMATION_KEY, hostInformationData).commit();
    }

    public static int fetchLastAppVersion(){
        return sharedPreferences.getInt(LAST_VERSION, 0);
    }

    public static void saveLastAppVersion(int versionCode){
        sharedPreferences.edit().putInt(LAST_VERSION, versionCode).commit();
    }
}
