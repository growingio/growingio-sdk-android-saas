package com.growingio.android.sdk.base.event;

import android.view.View;

/**
 * author CliffLeopard
 * time   2018/7/3:下午3:56
 * email  gaoguanling@growingio.com
 */
public class ViewTreeStatusChangeEvent {

    private StatusType statusType;
    private View oldFocus;
    private View newFocus;

    public ViewTreeStatusChangeEvent(StatusType statusType) {
        this.statusType = statusType;
    }

    public ViewTreeStatusChangeEvent(StatusType statusType, View oldFocus, View newFocus) {
        this.statusType = statusType;
        this.oldFocus = oldFocus;
        this.newFocus = newFocus;
    }
    public enum StatusType {
        FocusChanged,
        LayoutChanged,
        ScrollChanged
    }

    public StatusType getStatusType() {
        return statusType;
    }

    public View getOldFocus() {
        return oldFocus;
    }

    public View getNewFocus() {
        return newFocus;
    }
}
