package com.growingio.android.sdk.utils;

import android.util.Log;

import com.growingio.android.sdk.collection.ErrorLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * VarUpdateObserver
 * Created by lishaojie on 2017/4/14.
 */

public abstract class SimpleJSONVariableUpdateHelper implements Runnable {

    private final String TAG = "GIO.SimpleJSONVariableUpdateHelper";
    public static final int MAX_JSON_SIZE = 100;
    private JSONObject mVariable;

    public JSONObject getVariable() {
        return mVariable;
    }

    public void setVariable(JSONObject variable) {
        mVariable = variable;
    }

    public SimpleJSONVariableUpdateHelper() {
        mVariable = new JSONObject();
    }

    public SimpleJSONVariableUpdateHelper(JSONObject variable) {
        if (variable == null) {
            variable = new JSONObject();
        }
        mVariable = variable;
    }


    private boolean isValueChanged(Object from, Object to) {
        return (from != null && !from.equals(to)) || (from == null && to != null);
    }

    private int mergeJson(JSONObject dst, JSONObject src) {
        int affectedKeyCount = 0;
        try {
            JSONObject copyDst = JsonUtil.copyJson(dst, false);
            if (mergeOverMaxJsonSize(copyDst, src) > 0){
                affectedKeyCount = mergeOverMaxJsonSize(dst, src);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return affectedKeyCount;
    }

    private int mergeOverMaxJsonSize(JSONObject dst, JSONObject src) throws JSONException {
        int affectedKeyCount = 0;
        Iterator<String> keys = src.keys();
        int topIndex = SimpleJSONVariableUpdateHelper.MAX_JSON_SIZE;
        while (keys.hasNext() && topIndex-- > 0) {
            String key = keys.next();
            Object newValue = src.get(key);
            Object oldValue = dst.opt(key);
            if ("".equals(newValue)){
                newValue = null;
            }
            if (isValueChanged(oldValue, newValue)) {
                affectedKeyCount++;
            }
            dst.put(key, newValue);
            if (dst.length() > MAX_JSON_SIZE) {
                Log.e("GrowingIO", ErrorLog.JSON_TOO_LONG);
                return -1;
            }
        }
        return affectedKeyCount;
    }

    // 此处传参全部为GrowingIO方法调用， 参数判断应该在最外层，
    // 此处假设参数有效
    public void update(String key, Object value) {
        if (isValueChanged(mVariable.opt(key), value)) {
            try {
                mVariable.put(key, value);
                ThreadUtils.cancelTaskOnUiThread(this);
                ThreadUtils.postOnUiThread(this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // 此处参数有效, 不做参数校验判断
    public void update(JSONObject variable) {
        // 这里variable 为null 或者为 empty 时应该是pvar事件
        if (variable == null || variable.length() == 0){
            LogUtil.d(TAG, "update JSONObject, and variable is null");
            mVariable = new JSONObject();
            return;
        }
        int affected = mergeJson(mVariable, variable);
        LogUtil.d(TAG,"数据变更量："+affected);
        if (affected > 0) {
            ThreadUtils.cancelTaskOnUiThread(this);
            ThreadUtils.postOnUiThread(this);
        }
    }

    public abstract void afterUpdated();

    @Override
    public void run() {
        afterUpdated();
    }
}
