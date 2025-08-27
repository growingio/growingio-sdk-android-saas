package com.growingio.android.sdk.collection;

import java.lang.ref.WeakReference;

/**
 * Created by xyz on 15/8/29.
 */
public class Constants {

    public static final String PLATFORM_ANDROID = "Android";
    public static final String WEB_PART_SEPARATOR = "::";
    public static final String HTTP_PROTOCOL_PREFIX = "http://";
    public static final String HTTPS_PROTOCOL_PREFIX = "https://";
    public static final String ENDPOINT_TAIL = "/oauth2/token";
    public static final String HEAT_MAP_TAIL = "/mobile/heatmap/data";
    public static final String EVENT_TAIL = "/mobile/events";
    public static final String REALTIME_TAIL = "/mobile/realtime";
    public static final String XRANK_TAIL =  "/mobile/xrank";
    public static final String WEB_CIRCLE_TAIL = "/mobile/link";         // Web圈选时
    public static final char ID_PREFIX = '#';
    public static final int GROWINGIO_COLOR_RED = 0XE5FF4824;
    public static final int GROWINGIO_COLOR_LIGHT_RED = 0X4CFF4824;
    public static final int GROWINGIO_COLOR_YELLOW = 0XFFFFDD24;
    public static final int GROWINGIO_COLOR_LIGHT_YELLOW = 0X4CFFDD24;
    public static final String SIGN_FIELD_NAME = "GROPVAL";
    public static final String SIGN_FLAG ="giopid";

    public static final WeakReference<Object> NULL_REF = new WeakReference<>(null);
}
