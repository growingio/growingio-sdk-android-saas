package com.growingio.android.sdk.circle;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Base64;
import android.view.View;

import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.circle.utils.CircleUtil;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by xyz on 15/7/19.
 */
public class ScreenshotInfo {
    private final String TAG = "GIO.ScreenshotInfo";
    private WeakReference<Activity> activityWeakReference = null;
    private JSONArray mViewLayouts = new JSONArray();
    private ViewNode mTarget;
    private JSONArray mTargetImp;
    private String mPageName;
    private String mSPN;
    private String mEncodeScreenshot;

    private int currentZIndex;

    public ScreenshotInfo(Activity activity, List<ViewNode> nodes, ViewNode target) {
        activityWeakReference = new WeakReference<Activity>(activity);
        mTarget = target;
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public JSONObject getScreenShotInfo() {
        Activity activity = activityWeakReference.get();
        JSONObject jsonObject = new JSONObject();
        if (activity != null) {
            View[] allViews = WindowHelper.getSortedWindowViews();
            boolean isPortrait = activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
            byte[] screenshotData = ScreenshotHelper.captureAllWindows(allViews, null);
            try {
                mPageName = AutoBuryObservableInitialize.autoBuryAppState().getPageName(activity);
                mSPN = CoreInitialize.coreAppState().getSPN();
                jsonObject.put(PageEvent.TYPE_NAME, mPageName);
                jsonObject.put("screenshotWidth", isPortrait ? ScreenshotHelper.getScaledShort(): ScreenshotHelper.getScaledLong());
                jsonObject.put("screenshotHeight", isPortrait ? ScreenshotHelper.getScaledLong(): ScreenshotHelper.getScaledShort());
                jsonObject.put("scaled", ScreenshotHelper.getScaledFactor());
                jsonObject.put("title", activity.getTitle());
                mEncodeScreenshot = "data:image/jpeg;base64," + Base64.encodeToString(screenshotData, Base64.NO_WRAP);
                jsonObject.put("impress", getImpress(allViews));
                if (mTarget != null) {
                    mTargetImp = new JSONArray();
                    ViewTraveler traveler = new ViewTraveler() {
                        @Override
                        public void traverseCallBack(ViewNode viewNode) {
                            JSONObject impressObj = CircleUtil.getImpressObj(viewNode);
                            if (impressObj != null) {
                                mTargetImp.put(impressObj);
                            }
                        }
                    };
                    traveler.traverseCallBack(mTarget);
                    mTarget.setViewTraveler(traveler);
                    mTarget.traverseChildren();
                    jsonObject.put("targets", mTargetImp);
                }
                jsonObject.put("screenshot", mEncodeScreenshot);
            } catch (JSONException ex) {
                LogUtil.d(TAG, "generate screenshot data error", ex);
            }
        }
        return jsonObject;
    }

    private JSONArray getImpress(View[] rootViews) {
        mViewLayouts = new JSONArray();
        currentZIndex = 0;
        ViewHelper.traverseWindows(rootViews, mTraverseCallBack);
        return mViewLayouts;
    }

    private ViewTraveler mTraverseCallBack = new ViewTraveler() {
        @Override
        public boolean needTraverse(ViewNode viewNode) {
            return super.needTraverse(viewNode) || Util.isIgnoredView(viewNode.mView);
        }

        @Override
        public void traverseCallBack(ViewNode viewNode) {
            JSONObject impressObj = CircleUtil.getImpressObj(viewNode);
            addDomainAndPageAndZIndex(impressObj, viewNode);
            if (impressObj != null) {
                mViewLayouts.put(impressObj);
            }
        }
    };

    private void addDomainAndPageAndZIndex(JSONObject jsonObject, ViewNode viewNode) {
        if (jsonObject == null) return;
        String page = mPageName;
        String domain = mSPN;
        if (viewNode.mWebElementInfo != null) {
            page = mPageName + Constants.WEB_PART_SEPARATOR + viewNode.mWebElementInfo.mPath;
            domain = mSPN + Constants.WEB_PART_SEPARATOR + viewNode.mWebElementInfo.mHost;
        }
        try {
            jsonObject.put("domain", domain);
            /*
             * 此Z仅仅是Hint， 同位置下的不同View, z大的View一般在上边
             * 如果View不在统一位置此Z没有对比的意义
             *
             * 相信能够适配常见的ViewGroup, 并不能从技术上保证其层级关系
             */
            jsonObject.put("zLevel", currentZIndex++);
            jsonObject.put(PageEvent.TYPE_NAME, page);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
