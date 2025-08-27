package com.growingio.android.sdk.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by 郑童宇 on 2016/09/12.
 */

public class NetworkUtil {
    public static final String NETWORK_WIFI = "WIFI";
    public static final String NETWORK_UNKNOWN = "UNKNOWN";
    public static final String NETWORK_2G = "2G";
    public static final String NETWORK_3G = "3G";
    public static final String NETWORK_4G = "4G";
    public static final String NETWORK_5G = "5G";

    public static String getMobileNetworkTypeName(int subType, String subtypeName) {
        switch (subType) {
            //如果是2g类型
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_2G;
            //如果是3g类型
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_3G;
            //如果是4g类型
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return NETWORK_4G;
            case TelephonyManager.NETWORK_TYPE_NR:
                return NETWORK_5G;
            default:
                //中国移动 联通 电信 三种3G制式
                if (TextUtils.isEmpty(subtypeName)) {
                    break;
                }

                if (subtypeName.equalsIgnoreCase("TD-SCDMA") || subtypeName.equalsIgnoreCase("WCDMA") || subtypeName.equalsIgnoreCase("CDMA2000")) {
                    return NETWORK_3G;
                }
        }

        return NETWORK_UNKNOWN;
    }

    /**
     * 获取当前连接Wifi的Ip地址, 必须确保已经连接了Wifi
     *
     * @return ipv4地址
     */
    public static String getWifiIp(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null)
            return null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null || TextUtils.isEmpty(wifiInfo.getSSID()))
            return null;
        InetAddress inetAddress = ReflectUtil.findFieldRecur(wifiInfo, "mIpAddress");
        if (inetAddress instanceof Inet4Address) {
            return inetAddress.getHostAddress();
        } else if (inetAddress instanceof Inet6Address) {
            return inetAddress.getHostAddress();
        } else {
            return null;
        }
    }


    public static String encode(@Nullable String original) {
        if (original != null) {
            try {
                return URLEncoder.encode(original, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogUtil.d("NetWorkUtil", e);
            }
        }
        return "";
    }


    public static String decode(@Nullable String original){
        if (original != null) {
            try {
                return URLDecoder.decode(original, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LogUtil.d("NetWorkUtil", e);
            }
        }
        return "";
    }
}