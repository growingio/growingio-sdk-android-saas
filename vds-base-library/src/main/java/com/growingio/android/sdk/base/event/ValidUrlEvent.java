package com.growingio.android.sdk.base.event;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 使用UrlSchema跳转， 并且为GrowingIO Valid数据
 * Created by liangdengke on 2018/10/15.
 */
public class ValidUrlEvent {
    public Uri data;
    public Activity activity;
    public @ValidURLType
    int type;
    public static final int DEEPLINK = 1;
    public static final int APPLINK = 2;


    @IntDef({DEEPLINK, APPLINK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ValidURLType {
    }

    public ValidUrlEvent(Uri data, Activity activity, @ValidURLType int type) {
        this.data = data;
        this.activity = activity;
        this.type = type;
    }
}
