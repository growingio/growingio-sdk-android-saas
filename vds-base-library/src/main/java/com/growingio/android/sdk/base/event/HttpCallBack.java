package com.growingio.android.sdk.base.event;

import java.util.List;
import java.util.Map;

/**
 * author CliffLeopard
 * time   2018/7/5:下午6:59
 * email  gaoguanling@growingio.com
 */
public interface HttpCallBack {
    void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders);
}
