package com.growingio.android.sdk.base.event;

import com.growingio.android.sdk.models.ViewNode;

/**
 * author CliffLeopard
 * time   2018/7/3:下午5:54
 * email  gaoguanling@growingio.com
 */
public class CircleEvent {
    public String type;
    public ViewNode viewNode;

    public CircleEvent(String type) {
        this.type = type;
    }

    public CircleEvent(String type, ViewNode viewNode) {
        this.type = type;
        this.viewNode = viewNode;
    }
}
