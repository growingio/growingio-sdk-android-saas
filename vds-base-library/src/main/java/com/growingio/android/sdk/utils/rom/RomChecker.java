package com.growingio.android.sdk.utils.rom;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import com.growingio.android.sdk.utils.LogUtil;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:27
 * email  gaoguanling@growingio.com
 * <p>
 * 类说明：
 */

public class RomChecker {
    private static String TAG = "GIO.RomChecker";

    public static boolean isHuaweiRom() {
        return getEmuiVersion() > 0;
    }

    public static boolean isMiuiRom() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    public static boolean isMeizuRom() {
        String meizuFlymeOSFlag = getSystemProperty("ro.build.display.id");
        if (TextUtils.isEmpty(meizuFlymeOSFlag)) {
            return false;
        } else if (meizuFlymeOSFlag.contains("flyme") || meizuFlymeOSFlag.toLowerCase(Locale.getDefault()).contains("flyme")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean is360Rom() {
        return Build.MANUFACTURER.contains("QiKU")
                || Build.MANUFACTURER.contains("360");
    }

    /**
     * 获取小米 rom 版本号，获取失败返回 -1
     *
     * @return miui rom version code, if fail , return -1
     */
    public static int getMiuiVersion() {
        String version = getSystemProperty("ro.miui.ui.version.name");
        if (version != null) {
            try {
                return Integer.parseInt(version.substring(1));
            } catch (Exception e) {
                LogUtil.i(TAG, "get miui version code error, version : " + version);
            }
        }
        return -1;
    }


    /**
     * 获取 emui 版本号
     * @return -1.0 if not emui
     */
    public static double getEmuiVersion() {
        try {
            String emuiVersion = getSystemProperty("ro.build.version.emui");
            if (TextUtils.isEmpty(emuiVersion)){
                return -1.0;
            }
            Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+");
            Matcher matcher = pattern.matcher(emuiVersion);
            if (matcher.find()){
                return Double.parseDouble(matcher.group());
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return 4.0;
    }

    /**
     * 获取系统属性
     *
     * @param propName
     * @return
     */
    @SuppressLint("PrivateApi")
    @SuppressWarnings("unchecked")
    public static String getSystemProperty(String propName) {
        try {
            Class clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, propName, null);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return null;
    }

}

