package com.growingio.android.sdk.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class Screenshot implements Parcelable {

    public String x;
    public String y;
    public String w;
    public String h;
    public String target;
    public String viewport;

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("x", x);
            jsonObject.put("y", y);
            jsonObject.put("w", w);
            jsonObject.put("h", h);
            jsonObject.put("target", target);
            jsonObject.put("viewport", viewport);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    public static Screenshot parse(JSONObject jsonObject) {
        Screenshot screenshot = new Screenshot();
        try {
            screenshot.x = jsonObject.getString("x");
            screenshot.y = jsonObject.getString("y");
            screenshot.w = jsonObject.getString("w");
            screenshot.h = jsonObject.getString("h");
            screenshot.target = jsonObject.getString("target");
            screenshot.viewport = jsonObject.getString("viewport");
        } catch (JSONException ignored) {
        }
        return screenshot;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.x);
        dest.writeString(this.y);
        dest.writeString(this.w);
        dest.writeString(this.h);
        dest.writeString(this.target);
        dest.writeString(this.viewport);
    }

    public Screenshot() {
    }

    protected Screenshot(Parcel in) {
        this.x = in.readString();
        this.y = in.readString();
        this.w = in.readString();
        this.h = in.readString();
        this.target = in.readString();
        this.viewport = in.readString();
    }

    public static final Creator<Screenshot> CREATOR = new Creator<Screenshot>() {
        public Screenshot createFromParcel(Parcel source) {
            return new Screenshot(source);
        }

        public Screenshot[] newArray(int size) {
            return new Screenshot[size];
        }
    };
}
