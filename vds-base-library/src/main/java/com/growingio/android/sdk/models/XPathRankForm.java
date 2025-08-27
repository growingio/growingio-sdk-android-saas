package com.growingio.android.sdk.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by xyz on 16/3/24.
 */
public class XPathRankForm {

    String domain;
    String path;
    ArrayList<String> xpath;
    String range;

    public XPathRankForm(String domain, String path, ArrayList<String> xpath, String range) {
        this.domain = domain;
        this.path = path;
        this.xpath = xpath;
        this.range = range;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("domain", domain);
            jsonObject.put("path", path);
            jsonObject.put("xpath", new JSONArray(xpath));
            jsonObject.put("range", range);
        } catch (JSONException ignored) {
        }
        return jsonObject.toString();
    }
}
