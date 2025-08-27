package com.growingio.android.sdk.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by xyz on 15/9/8.
 */

public class ViewAttrs {
    public String xpath;
    public String path;
    public String content;
    public String domain;
    public String index;
    public String query;
    public String href;
    public String nodeType;
    public boolean webElem = false;
    //不收集这些单字符
    private static  final String illegalStr = "_!@#$%^&*()-=+|\\[]{},.<>/?";

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("domain", domain);
            jsonObject.put("path", path);
            if (!TextUtils.isEmpty(xpath))
                jsonObject.put("xpath", xpath);
            if (isLegal(content))
                jsonObject.put("content", content);
            if (!TextUtils.isEmpty(index))
                jsonObject.put("index", index);
            if (!TextUtils.isEmpty(query))
                jsonObject.put("query", query);
            if (!TextUtils.isEmpty("href"))
                jsonObject.put("href", href);
            if (!TextUtils.isEmpty(nodeType)) {
                jsonObject.put("nodeType", nodeType);
            }
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    /**
     * 忽悠对个位数字/英文字母的采集
     *
     * @param content
     * @return
     */
    private static boolean isLegal(String content) {
        if (TextUtils.isEmpty(content))
            return false;
        if (content.length() > 1)
            return true;
        char character = content.charAt(0);
        boolean isNum = character >= '0' && character <= '9';
        boolean isChar = (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z');
        return !isNum && !isChar && !illegalStr.contains(character+"");
    }

    public static ViewAttrs parse(JSONObject jsonObject) {
        ViewAttrs viewAttrs = new ViewAttrs();
        try {
            viewAttrs.domain = jsonObject.getString("domain");
            viewAttrs.xpath = jsonObject.optString("xpath");
            viewAttrs.path = jsonObject.optString("path");
            viewAttrs.content = jsonObject.optString("content");
            viewAttrs.index = jsonObject.optString("index");
            viewAttrs.query = jsonObject.optString("query");
            viewAttrs.href = jsonObject.optString("href");
            viewAttrs.nodeType = jsonObject.optString("nodeType");
        } catch (JSONException ignored) {
        }
        return viewAttrs;
    }

    public ViewAttrs copy() {
        ViewAttrs copy = new ViewAttrs();
        copy.xpath = xpath;
        copy.path = path;
        copy.content = content;
        copy.domain = domain;
        copy.index = index;
        copy.query = query;
        copy.href = href;
        copy.nodeType = nodeType;
        return copy;
    }
}
