package com.growingio.android.sdk.models;

import android.text.TextUtils;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.utils.GJSONStringer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class Tag {
    public boolean archived;
    public String id;
    public String name;
    public String eventType;
    public String platform;
    public ViewAttrs attrs;
    public ViewAttrs filter;
    String comment;
    public String source;
    Screenshot screenshot;

    public Tag() {
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Tag(JSONObject jsonObject) {
        try {
            id = jsonObject.getString("id");
            name = jsonObject.getString("name");
            eventType = jsonObject.getString("eventType");
            platform = jsonObject.getString("platform");
            source = jsonObject.optString("source");
            attrs = ViewAttrs.parse(jsonObject.getJSONObject("attrs"));
            filter = ViewAttrs.parse(jsonObject.getJSONObject("filter"));
            screenshot = Screenshot.parse(jsonObject.getJSONObject("screenshot"));
            archived = TextUtils.equals(jsonObject.optString("status"), "archived");
        } catch (JSONException ignored) {
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    // only the eventType = elem can change content
    public void setFilterContent(String content) {
        filter.content = content;
    }

    public void setFilterIndex(String index) {
        filter.index = index;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    void setPageMode(String page) {
        this.filter.path = page;
    }

    public void setScreenshot(Screenshot screenshot) {
        this.screenshot = screenshot;
    }

    public Tag copyWithoutScreenShot() {
        Tag copy = new Tag();
        copy.id = id;
        copy.platform = platform;
        copy.eventType = eventType;
        copy.name = name;
        copy.comment = comment;
        copy.source = source;
        if (attrs != null){
            copy.attrs = attrs.copy();
        }
        if (filter != null){
            copy.filter = filter.copy();
        }
        return copy;
    }

    @Override
    public String toString() {
        return new GJSONStringer().convertToString(toJson());
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("eventType", eventType);
            jsonObject.put("platform", platform);
            jsonObject.put("attrs", attrs.toJSON());
            jsonObject.put("filter", filter.toJSON());
            jsonObject.put("comment", comment);
            jsonObject.put("appVersion", CoreInitialize.config().getAppVersion());
            jsonObject.put("sdkVersion", GConfig.GROWING_VERSION);
            if (!TextUtils.isEmpty(source)) {
                jsonObject.put("source", source);
            }
            JSONObject screenshotObj = new JSONObject();
            if (screenshot != null) {
                screenshotObj = screenshot.toJSON();
            }
            jsonObject.put("screenshot", screenshotObj);

        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    // TODO: 15/9/16 fixed this
    public String toStringWithoutScreenshot() {
        return toJsonWithoutScreenshot().toString();
    }

    public JSONObject toJsonWithoutScreenshot() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("name", name);
            jsonObject.put("eventType", eventType);
            jsonObject.put("platform", platform);
            if (attrs != null) {
                jsonObject.put("attrs", attrs.toJSON());
            }
            if (filter != null) {
                jsonObject.put("filter", filter.toJSON());
            }
            jsonObject.put("comment", comment);
//            JSONObject screenshotObj = new JSONObject();
//            if (screenshot != null) {
//                screenshotObj = screenshot.toJSON();
//            }
//            jsonObject.put("screenshot", screenshotObj);

        } catch (JSONException ignored) {
        }
        return jsonObject;
    }


    public boolean match(Tag tag) {
        return Constants.PLATFORM_ANDROID.equalsIgnoreCase(tag.platform)
                && eventType.equals(tag.eventType)
                && matchStr(attrs.domain, tag.filter.domain)
                && matchStr(attrs.path, tag.filter.path)
                && matchStr(attrs.query, tag.filter.query);
    }

    private boolean matchStr(String str, String filterStr) {
        return TextUtils.equals(str, filterStr) || TextUtils.isEmpty(filterStr) || filterStr.contains("*") && str != null && Pattern.matches(filterStr.replace("*", ".*"), str);
    }
}