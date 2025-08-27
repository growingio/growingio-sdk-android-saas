package com.growingio.android.sdk.collection;

import android.view.View;

import com.growingio.android.sdk.models.Screenshot;

/**
 * Created by xyz on 15/12/15.
 */
public class HitView {
    private String mXPath;
    private String mIndex;
    private View mView;
    private boolean mInFullScreenWindow;
    private Screenshot mScreenshot;

    public String getXPath() {
        return mXPath;
    }

    public void setXPath(String XPath) {
        mXPath = XPath;
    }

    public String getIndex() {
        return mIndex;
    }

    public void setIndex(String index) {
        mIndex = index;
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        mView = view;
    }

    public boolean isInFullScreenWindow() {
        return mInFullScreenWindow;
    }

    public void setInFullScreenWindow(boolean mInFullScreenWindow) {
        this.mInFullScreenWindow = mInFullScreenWindow;
    }

    public void setScreenshot(Screenshot info) {
        this.mScreenshot = info;
    }

    public Screenshot getScreenshot() {
        return mScreenshot;
    }
}
