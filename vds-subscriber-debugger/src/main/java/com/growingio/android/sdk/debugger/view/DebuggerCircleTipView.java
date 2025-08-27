package com.growingio.android.sdk.debugger.view;

import android.content.Context;

/**
 * author CliffLeopard
 * time   2017/9/6:下午4:55
 * email  gaoguanling@growingio.com
 */

public class DebuggerCircleTipView extends CircleTipView {

    public DebuggerCircleTipView(Context context) {
        super(context);
    }

    @Override
    public String getStrDialogTittle() {
        return "正在进行Debugger";
    }

    @Override
    public String getStrDialogCancel() {
        return "继续Debugger";
    }

    @Override
    public String getStrDialogOk() {
        return "退出Debugger";
    }

    @Override
    public void doing() {
        setContent("正在进行Debugger...");
    }
}
