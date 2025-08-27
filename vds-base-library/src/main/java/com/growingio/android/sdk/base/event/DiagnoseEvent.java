package com.growingio.android.sdk.base.event;

/**
 * author CliffLeopard
 * time   2018/7/3:上午11:40
 * email  gaoguanling@growingio.com
 */
public class DiagnoseEvent {
    public String type;
    public int count = -1;

    public DiagnoseEvent(String type, int count) {
        this.type = type;
        this.count = count;
    }

    public DiagnoseEvent(String type) {
        this.type = type;
    }
}
