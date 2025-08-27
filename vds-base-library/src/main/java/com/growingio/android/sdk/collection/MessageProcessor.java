package com.growingio.android.sdk.collection;

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;

import com.growingio.android.sdk.base.event.RefreshPageEvent;
import com.growingio.android.sdk.models.AppCloseEvent;
import com.growingio.android.sdk.models.ConversionEvent;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.PageVariableEvent;
import com.growingio.android.sdk.models.PeopleEvent;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.models.VisitEvent;
import com.growingio.android.sdk.models.VisitorVarEvent;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.ThreadMode;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * 原本的MessageProcessor, 保留它主要为了保留git历史
 * 此类功能:
 * - 在page重构前， 保留一些需要暂存的Page状态
 * - 在page重构后， 协调无埋点与打点的page事件协调
 * - 缓存与组装vist事件
 */
@SuppressLint("NewApi")
public class MessageProcessor {
    private static final String TAG = "GIO.MessageProcessor";

    private GConfig mGConfig;
    private CoreAppState mCoreAppState;

    private PageEvent mLastPageEvent;
    private WeakReference<Object> mLastPageObjRef = Constants.NULL_REF;           // 对应于LastPageName的Page对象
    private PageEvent mPendingPageEvent;
    private WeakReference<Object> mPendingPageObjRef = Constants.NULL_REF;         // 对应于mPendingPageEvent的Page对象

    public static final String FAKE_PAGE_NAME = "GIOFakePage";

    public MessageProcessor(GConfig config, CoreAppState coreAppState) {
        mGConfig = config;
        mCoreAppState = coreAppState;
    }

    // TODO: 2018/7/14 PTM更新时间 存在问题, 需要参见master-2.x仔细涉及
    public long getPTMWithoutPending() {
        return mLastPageEvent == null ? 0 : mLastPageEvent.getTime();
    }

    public long getPTMWithPending() {
        if (mPendingPageEvent != null) {
            return mPendingPageEvent.getTime();
        }
        return getPTMWithoutPending();
    }

    public String getPageNameWithoutPending() {
        return mLastPageEvent == null ? null : mLastPageEvent.mPageName;
    }

    public String getPageNameWithPending() {
        if (mPendingPageEvent != null) {
            return mPendingPageEvent.mPageName;
        }
        return getPageNameWithoutPending();
    }

    public void saveVisit(boolean isNewVisit) {
        if (isNewVisit) {
            persistEvent(VisitEvent.makeVisitEvent());
            JSONObject appVar = mCoreAppState.getVisitorVariable();
            if (appVar != null) {
                persistEvent(new VisitorVarEvent(appVar, System.currentTimeMillis()));
            }
        } else {
            persistEvent(VisitEvent.getCachedVisitEvent());
        }

        if (GConfig.isRnMode) {
            savePage("GIOFakePage");
        }
    }

    public void persistEvent(@Nullable VPAEvent event) {
        if (event == null || !mGConfig.isEnabled())
            return;
        EventCenter.getInstance().post(event);
    }

    public void saveCustomEvent(CustomEvent customEvent) {
        if (GConfig.isRnMode) {
            savePage(FAKE_PAGE_NAME);
        }
        if (!customEvent.fromWebView()) {
            customEvent.setPageTime(getPTMWithPending());
            customEvent.mPageName = getPageNameWithPending();
        }
        persistEvent(customEvent);
    }

    public void savePage(PageEvent pageEvent, Object referenceObj) {
        Object pendingPageObj = mPendingPageObjRef.get();

        // every time savePage be called， the PendingPage Event should be send
        if (mPendingPageEvent != null) {
            persistEvent(mPendingPageEvent);

            if(pendingPageObj != null) {
                JSONObject jsonObject = mCoreAppState.getPageVariableHelper(pendingPageObj).getVariable();
                if (jsonObject != null && jsonObject.length() != 0) {
                    persistEvent(new PageVariableEvent(mPendingPageEvent, jsonObject));
                }
            }
        }

        // 该场景下需要发送pendingPage而不是后产生的Page
        // 退到后台返回时，mPendingPageEvent可能是fragment，但是此时activity中cstm关联的时pending中fragment事件
        if (mPendingPageEvent != null &&
                (mPendingPageEvent == pageEvent ||
                        (mPendingPageEvent.mPageName.equals(pageEvent.mPageName) && pendingPageObj == referenceObj))) {
            mLastPageEvent = mPendingPageEvent;
            mLastPageObjRef = new WeakReference<>(referenceObj);
        } else {
            mLastPageEvent = pageEvent;
            mLastPageObjRef = new WeakReference<>(referenceObj);
            persistEvent(pageEvent);
            JSONObject jsonObject = mCoreAppState.getPageVariableHelper(referenceObj).getVariable();
            if (jsonObject != null && jsonObject.length() != 0) {
                persistEvent(new PageVariableEvent(mLastPageEvent, jsonObject));
            }
        }

        mPendingPageEvent = null;
        mPendingPageObjRef = Constants.NULL_REF;
    }

    public void savePage(String pageName) {
        PageEvent event = new PageEvent(pageName, mLastPageEvent != null ? mLastPageEvent.mPageName : null, System.currentTimeMillis());
        savePage(event, pageName);
    }

    public void updatePendingPageEvent(PageEvent pageEvent, Object pendingObj) {
        mPendingPageEvent = pageEvent;
        mPendingPageObjRef = new WeakReference<>(pendingObj);
    }

    public PageEvent getPendingPageEvent() {
        return mPendingPageEvent;
    }

    public Object getPendingObj() {
        return mPendingPageObjRef.get();
    }

    public PageEvent getLastPageEvent() {
        return mLastPageEvent;
    }

    void setEvar(JSONObject variable) {
        persistEvent(new ConversionEvent(variable, System.currentTimeMillis()));
    }

    void setAppClose(long closeTime) {
        if (mLastPageEvent == null) {
            LogUtil.d(TAG, "appClose: lastPage is null, return");
            return;
        }
        persistEvent(new AppCloseEvent(mLastPageEvent, closeTime));
    }

    void setPeople(JSONObject variable) {
        persistEvent(new PeopleEvent(variable, System.currentTimeMillis()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN, priority = 1000)
    public void refreshPageIfNeed(RefreshPageEvent refreshPageEvent) {
        if (mLastPageEvent == null) {
            LogUtil.d(TAG, "refreshPageIfNeed: lastPage is null, return");
            return;
        }
        long ptm = refreshPageEvent.isNewPTM() ? System.currentTimeMillis() : mLastPageEvent.getTime();
        PageEvent newPage = new PageEvent(mLastPageEvent.mPageName, mLastPageEvent.mPageName, ptm);
        savePage(newPage, mLastPageObjRef.get());
    }

    void onAppVariableUpdated() {
        if (mPendingPageEvent == null && mLastPageEvent != null) {
            refreshPageIfNeed(new RefreshPageEvent(false, false));
        }
    }

    void onPageVariableUpdated(Object page) {
        LogUtil.i(TAG, "onPageVariableUpdated:" + page.toString());
        if (isLastEventPage(page)) {
            JSONObject jobj = mCoreAppState.getPageVariableHelper(page).getVariable();
            persistEvent(new PageVariableEvent(mLastPageEvent, jobj));
        }
    }

    public boolean isLastEventPage(Object page) {
        if (page instanceof String) {
            return page.equals(mLastPageObjRef.get());
        }
        return mLastPageObjRef.get() == page;
    }

    public boolean isPendingPage(Object pageRef) {
        if (pageRef instanceof String) {
            return pageRef.equals(mPendingPageObjRef.get());
        }
        return mPendingPageObjRef.get() == pageRef;
    }

}
