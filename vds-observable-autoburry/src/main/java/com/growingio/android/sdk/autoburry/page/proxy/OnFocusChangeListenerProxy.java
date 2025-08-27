package com.growingio.android.sdk.autoburry.page.proxy;

import android.view.View;
import android.widget.EditText;

import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ViewHelper;

public class OnFocusChangeListenerProxy implements View.OnFocusChangeListener{
    private static final String TAG = "GIO.OnFocusChangeListenerProxy";
    private View.OnFocusChangeListener object;

    public OnFocusChangeListenerProxy(View.OnFocusChangeListener object){
        this.object = object;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        LogUtil.i(TAG, v.toString());
        if (v instanceof EditText) {
            ViewHelper.changeOn(v);
        }
        if(object != null)
            object.onFocusChange(v, hasFocus);
    }
}
