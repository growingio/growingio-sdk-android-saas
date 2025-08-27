package com.growingio.android.sdk.collection;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * GrowingIO内部API
 */
public class GInternal {
    private final String TAG = "GIO.InternalAPI";

    private String featuresVersionJson = null;

    public static GInternal getInstance(){
        return Internal.instance;
    }


    /**
     * 添加特性与其对应的版本
     * @param feature2Version 已key, value, key, value的形式传参
     * 示例:
     * GInternal.getInstance().addFeaturesVersion("gtouch", "0.5.60", "web-circle", "2")
     */
    public synchronized GInternal addFeaturesVersion(String... feature2Version){
        if (feature2Version.length % 2 != 0){
            String errorMsg = "GInternal addFeaturesVersion the num of arguments must be even";
            if (GConfig.DEBUG){
                throw new IllegalArgumentException(errorMsg);
            }else{
                LogUtil.e(TAG, errorMsg);
            }
            return this;
        }

        try {
            JSONObject jsonObject = featuresVersionJson == null ? new JSONObject() : new JSONObject(featuresVersionJson);
            int current = 0;
            while (current < feature2Version.length){
                String key = feature2Version[current];
                String value = feature2Version[current + 1];
                if (key == null || value == null){
                    String errorMsg = "key or value is null";
                    if (GConfig.DEBUG){
                        throw new IllegalArgumentException(errorMsg);
                    }else{
                        LogUtil.e(TAG, errorMsg);
                    }
                    return this;
                }
                current += 2;
                if (jsonObject.has(key)){
                    LogUtil.d(TAG, "addFeaturesVersion key: ", key,
                            " has exist on featuresVersionJson, oops");
                }
                jsonObject.put(key, value);
            }
            if (jsonObject.length() == 0){
                featuresVersionJson = null;
            }else{
                featuresVersionJson = jsonObject.toString();
            }
        } catch (JSONException e) {
            LogUtil.e(TAG, e.getMessage(), e);
        }
        return this;
    }

    public String getFeaturesVersionJson() {
        return featuresVersionJson;
    }

    private static class Internal{
        static GInternal instance = new GInternal();
    }
}
