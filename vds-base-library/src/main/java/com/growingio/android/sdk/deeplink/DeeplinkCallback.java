package com.growingio.android.sdk.deeplink;

import java.util.Map;

/**
 * Created by denghuaxin on 2018/4/9.
 */

public interface DeeplinkCallback {
    public final static int SUCCESS = 0x0;
    public final static int PARSE_ERROR = 0x1;
    public final static int ILLEGAL_URI = 0X2;
    public final static int NO_QUERY = 0X3;

    int ERROR_NET_FAIL =  0x5;
    int ERROR_EXCEPTION = 0x6;

    int ERROR_UNKNOWN = 400;
    int ERROR_LINK_NOT_EXIST = 404;
    int ERROR_TIMEOUT = 408;
    int ERROR_APP_NOT_ACCEPT = 406;
    int ERROR_URL_FORMAT_ERROR = 412;

    public void onReceive(Map<String, String> params, int error , long appAwakePassedTime);
}
