package com.growingio.android.sdk.collection;

import android.content.Context;

/**
 * <p>
 * 支持由用户提供oaid的获取方式
 *
 * @author cpacm 2022/1/18
 */
public class OaidProvideConfig {

    private OnProvideCertCallback provideCertCallback;
    private OnProvideOaidCallback provideOaidCallback;

    protected OaidProvideConfig(OnProvideCertCallback provideCertCallback) {
        this.provideCertCallback = provideCertCallback;
    }

    protected OaidProvideConfig(OnProvideOaidCallback provideOaidCallback) {
        this.provideOaidCallback = provideOaidCallback;
    }

    protected OnProvideCertCallback getProvideCertCallback() {
        return provideCertCallback;
    }

    protected OnProvideOaidCallback getProvideOaidCallback() {
        return provideOaidCallback;
    }

    public interface OnProvideOaidCallback {
        String provideOaidJob(Context context);
    }

    public interface OnProvideCertCallback {
        String provideCertJob(Context context);
    }

    public static OaidProvideConfig provideOaid(OnProvideOaidCallback provideOaidCallback){
        return new OaidProvideConfig(provideOaidCallback);
    }

    public static OaidProvideConfig provideCert(OnProvideCertCallback provideCertCallback){
        return new OaidProvideConfig(provideCertCallback);
    }
}
