package com.growingio.android.sdk.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by liangdengke on 2018/6/26.
 */
public final class JsonUtil {
    private JsonUtil(){}

    public static JSONObject copyJson(JSONObject jsonObject, boolean containJSONObject){
        if (jsonObject == null) return null;
        JSONObject copy = new JSONObject();
        try {
            for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext();){
                String key = iterator.next();
                Object value = jsonObject.opt(key);
                if (!containJSONObject && (value instanceof JSONObject || value instanceof JSONArray)){
                    throw new IllegalArgumentException("containJSONObject is false, but jsonObject contain JSONObject and JSONArray");
                }
                copy.put(key, value);
            }
        } catch (JSONException e) {
            // ignore
        }
        return copy;
    }

    /**
     * @return true if left and right key value equal,
     */
    public static boolean equal(JSONObject left, JSONObject right){
        if (left == null || right == null){
            return left == right;
        }
        if (left.length() != right.length()){
            return false;
        }
        try {
            for (Iterator<String> iterator = left.keys(); iterator.hasNext();){
                String key = iterator.next();
                if (!right.has(key))
                    return false;
                Object leftValue = left.get(key);
                Object rightValue = right.get(key);
                // leftValue and rightValue all not null
                if (!jsonEqual(leftValue, rightValue)){
                    return false;
                }
            }
        }catch (JSONException e){
            // ignore
        }
        return true;
    }

    private static boolean jsonEqual(Object left, Object right){
        if (ObjectUtils.equals(left, right)){
            return true;
        }else if (left instanceof JSONObject && right instanceof JSONObject){
            return equal((JSONObject)left, (JSONObject)right);
        }else if(left instanceof JSONArray && right instanceof JSONArray){
            return equal((JSONArray)left, (JSONArray)right);
        }
        return false;
    }

    /**
     * @return true if left and right key value equal,
     */
    public static boolean equal(JSONArray left, JSONArray right){
        if (left == null || right == null){
            return left == right;
        }
        if (left.length() != right.length()){
            return false;
        }
        try {
            for (int i = 0; i < left.length(); i++){
                Object leftValue = left.get(i);
                Object rightValue = right.get(i);
                if (!jsonEqual(leftValue, rightValue)){
                    return false;
                }
            }
        }catch (JSONException e){
            // ignore
        }
        return true;
    }

    public static JSONObject fromString(String json){
        if (json == null) return null;
        try{
            return new JSONObject(json);
        }catch (JSONException e){
            return null;
        }
    }
}
