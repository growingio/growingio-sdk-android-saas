package com.growingio.android.sdk.api;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.HttpEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.NetworkConfig;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.eventcenter.EventCenter;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by lishaojie on 16/8/4.
 */
public class FetchTagListTask implements HttpCallBack{

    public void run() {
        try{
            CoreAppState state = CoreInitialize.coreAppState();
            GConfig config = CoreInitialize.config();
            if (state == null) return;
            String path = String.format(Locale.US, "/products/%s/android/%s/settings", state.getProjectId(), state.getSPN());
            String timestampKey = "timestamp=" + System.currentTimeMillis();
            String packageVersion = "";
            try {
                PackageInfo info = state.getGlobalContext().getPackageManager().getPackageInfo(state.getGlobalContext().getPackageName(), 0);
                packageVersion = info.versionName;
            } catch (Throwable e) {
                LogUtil.d(e);
            }
            String avKey = "av=" + GConfig.GROWING_VERSION;
            String cvKey = "cv=" + packageVersion;
            String sign = getSignature(path, timestampKey, avKey, cvKey);
            if (sign == null) return;
            HashMap<String, String> requestHeaders = new HashMap<String, String>(1);
            requestHeaders.put("If-None-Match", config.getSettingsETag());
            String url = String.format(Locale.US, "%s%s?%s&%s&%s&sign=%s", NetworkConfig.getInstance().tagsHost(), path, avKey, cvKey, timestampKey, sign);
            HttpEvent httpEvent = new HttpEvent();
            httpEvent.setUrl(url);
            httpEvent.setRequestMethod(HttpEvent.REQUEST_METHOD.GET);
            httpEvent.setHeaders(requestHeaders);
            httpEvent.setCallBack(this);
            EventCenter.getInstance().post(httpEvent);
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    /**
     * 签名运算时除path以外所有参数的排序需要按照字典序来,从小到大
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private String getSignature(String path, String timestampKey, String avKey, String cvKey) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
            digest.update(String.format(Locale.US, "api=%s&%s&%s&%s", path, avKey, cvKey, timestampKey).getBytes(Charset.forName("UTF-8")));
            byte[] result = digest.digest();
            String hexString = new BigInteger(1, result).toString(16);
            final int SHA1_RESULT_LENGTH = 40;
            if (hexString.length() >= SHA1_RESULT_LENGTH) {
                return hexString;
            } else {
                StringBuilder sb = new StringBuilder(SHA1_RESULT_LENGTH);
                int prefixZeorLength = SHA1_RESULT_LENGTH - hexString.length();
                while (prefixZeorLength-- > 0) {
                    sb.append("0");
                }
                return sb.append(hexString).toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
        GConfig config = CoreInitialize.config();
        if (responseCode == HttpURLConnection.HTTP_OK){
            config.saveServerSettings(new String(data));
            if (mResponseHeaders != null && mResponseHeaders.containsKey("ETag")) {
                List<String> tags = mResponseHeaders.get("ETag");
                if (tags.size() > 0) {
                    config.saveETagForSettings(tags.get(0));
                }
            }
        }
    }
}
