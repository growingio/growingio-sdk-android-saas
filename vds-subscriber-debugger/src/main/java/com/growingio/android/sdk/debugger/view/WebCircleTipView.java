package com.growingio.android.sdk.debugger.view;


import android.content.Context;


/**
 * Created by lishaojie on 2/22/16.
 */
public class WebCircleTipView extends CircleTipView {

    public WebCircleTipView(Context context) {
        super(context);
    }

    @Override
    public String getStrDialogTittle() {
        return "正在进行圈选";
    }

    @Override
    public String getStrDialogCancel() {
        return "继续圈选";
    }

    @Override
    public String getStrDialogOk() {
        return "退出圈选";
    }

    @Override
    public void doing() {
        setError(false);
        setContent("正在进行GrowingIO移动端圈选");
    }

    @Override
    public void show() {
        super.show();
    }
}
