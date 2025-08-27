package com.growingio.android.sdk.autoburry;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.webkit.WebView;

import com.growingio.android.sdk.api.FetchTagListTask;
import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.CircleEvent;
import com.growingio.android.sdk.base.event.RefreshPageEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.base.event.message.MessageEvent;
import com.growingio.android.sdk.collection.ActionCalculator;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.MessageProcessor;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;
import com.growingio.eventcenter.bus.ThreadMode;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * author CliffLeopard
 * time   2018/7/3:下午2:06
 * email  gaoguanling@growingio.com
 */
public class AutoBuryMessageProcessor {
    private static final String TAG = "GIO.AutoBuryMessageProcessor";

    private final GConfig mConfig;
    private final CoreAppState mCoreAppState;
    private final AutoBuryAppState mAutoAppState;
    private final MessageProcessor mCoreMessageProcessor;

    private Map<WeakReference<View>, ActionCalculator> mActionCalculatorMap = new LinkedHashMap<WeakReference<View>, ActionCalculator>();
    private static final int MAX_RETRY_CHECK_SETTINGS_COUNT = 1;
    private static final int SAVE_ALL_IMPRESSION_DELAY = 250;
    private static final int SAVE_NEW_WINDOW_IMPRESSION_DELAY = 500; // 新的对话框窗口创建一般比较慢，等500ms再扫描
    private static final int ONREUME_TIME_INTERVAL = 500;

    private Runnable mResendPageEventTask;
    private boolean mFullRefreshingPage = false;


    private FetchTagListTask mCheckSettingsTask;
    private static int sSettingsRetryCount = 0;
    private long mLastSettingsUpdateTime = -1;
    private long mNextForceSaveAllImpressionTime = -1;
    private long mViewTreeChangeDownTime = -1;
    private boolean mIsInFirstImpressionTime;

    private ExecutorService mBgExecutorService;
    private volatile boolean mIsInObtainImpressing = false;

    // 仅针对activity进行处理, fragment不做处理， 即不使用mLastPageObjRef
    private WeakReference<Activity> mLastActivity;
    private long mLastPauseTime = -1;

    private final ProcessorHandler mHandler;

    public AutoBuryMessageProcessor(GConfig config, CoreAppState coreAppState,
                                    AutoBuryAppState autoBuryAppState, MessageProcessor messageProcessor) {
        mConfig = config;
        mCoreAppState = coreAppState;
        mAutoAppState = autoBuryAppState;
        mCoreMessageProcessor = messageProcessor;
        mBgExecutorService = EventBus.getDefault().getExecutorService();
        mHandler = new ProcessorHandler();
    }

    @Subscribe
    public void onActivityLifeCycleChanged(ActivityLifecycleEvent event){
        Activity activity =  event.getActivity();
        switch (event.event_type){
            case ON_RESUMED:
                savePageForPureActivity(activity, true);
                updateSettingsIfNeeded();
                break;
            case ON_PAUSED:
                flushPendingActivityPageEvent();
                cancelSaveImpAndClearImpRecord();
                mIsInFirstImpressionTime = true;
                mLastPauseTime = System.currentTimeMillis();
                break;
        }
    }

    @Subscribe
    public void onScrollChanged(ViewTreeStatusChangeEvent event){
        saveAllWindowImpressionDelayedForViewTreeChange();
    }

    private void cancelSaveImpAndClearImpRecord(){
        clearActionCalculatorMap();
        ThreadUtils.cancelTaskOnUiThread(mSaveAllWindowImpression);
    }

    /**
     * 产生一个Activity的Page事件
     * 目前只有Activity的page事件允许有pending延迟, 其余Page事件， 产生即发送
     */
    public void savePageForPureActivity(Activity activity) {
        savePageForPureActivity(activity, false);
    }

    private void savePageForPureActivity(Activity activity, boolean forceRefresh){
        if (activity == null || !isLegalPageEvent()) return;
        // 同一个activity退到后台超过500ms，更新pendingEvent，viewTree变化不设置activity
        if (mLastActivity != null && activity == mLastActivity.get()
                && (!forceRefresh || System.currentTimeMillis() - mLastPauseTime < ONREUME_TIME_INTERVAL)) {
            return;
        }

        // 判断该Activity 是否被用户要求忽略
        if (mAutoAppState.isIgnoredActivity(activity)) {
            return;
        }

        mLastActivity = new WeakReference<>(activity);
        // 后台返回前台时，pendingEvent和pendingObj对应fragment
        Object fragment = mAutoAppState.getForegroundFragment(activity);
        Object referenceObj;
        String pageName;
        if (fragment != null) {
            referenceObj = fragment;
            pageName = mAutoAppState.getFragmentPageName(fragment);
        } else {
            referenceObj = activity;
            pageName = mAutoAppState.getPageName(activity);
        }
        PageEvent pageEvent = new PageEvent(pageName,
                mCoreMessageProcessor.getPageNameWithPending(),
                System.currentTimeMillis());
        if (mAutoAppState.isPageManualModel(activity)){
            mCoreMessageProcessor.savePage(pageEvent, referenceObj);
        }else{
            mCoreMessageProcessor.updatePendingPageEvent(pageEvent, referenceObj);
        }
        mIsInFirstImpressionTime = true;
        if (!mHandler.hasMessages(ProcessorHandler.MSG_SEND_ACTIVITY_PAGE)){
            // 对于Activity的Page事件而言， 自产生，延迟1000ms发送(200 + 200 < 1000ms)
            mHandler.sendEmptyMessageDelayed(ProcessorHandler.MSG_SEND_ACTIVITY_PAGE, 1000);
        }
    }

    public void onFragmentPage(SuperFragment superFragment){
        Activity activity = superFragment.getActivity();
        if (activity == null) return;
        mIsInFirstImpressionTime = true;
        PageEvent pageEvent = new PageEvent(mAutoAppState.getPageName(activity),
                mCoreMessageProcessor.getPageNameWithPending(),
                System.currentTimeMillis());
        mCoreMessageProcessor.savePage(pageEvent, superFragment.getFragment());
        clearActionCalculatorMapAndSaveImpressDelay();
    }

    public void savePageForManualModel(Activity activity){
        ThreadUtils.cancelTaskOnUiThread(mResendPageEventTask);
        PageEvent pageEvent = new PageEvent(mAutoAppState.getPageName(activity), mCoreMessageProcessor.getPageNameWithoutPending(), System.currentTimeMillis());
        mCoreMessageProcessor.savePage(pageEvent, activity);
        clearActionCalculatorMapAndSaveImpressDelay();
    }

    public void saveRNPage(String pageName, Object pageObj){
        ThreadUtils.cancelTaskOnUiThread(mResendPageEventTask);
        PageEvent pageEvent = new PageEvent(pageName,
                mCoreMessageProcessor.getPageNameWithoutPending(),
                System.currentTimeMillis());
        mCoreMessageProcessor.savePage(pageEvent, pageObj);
        clearActionCalculatorMapAndSaveImpressDelay();
    }

    private boolean isLegalPageEvent() {
        return mCoreAppState.isScreenOn();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refreshPageIfNeeded(RefreshPageEvent event) {
        final boolean refreshImpression = event.isWithImpression();
        final boolean refreshPtm = event.isNewPTM();
        if (mFullRefreshingPage && !refreshImpression) {
            return;
        }
        mFullRefreshingPage = refreshImpression && refreshPtm;
        ThreadUtils.cancelTaskOnUiThread(mResendPageEventTask);
        mResendPageEventTask = new Runnable() {
            @Override
            public void run() {
                mFullRefreshingPage = false;
                forceRefresh(refreshImpression);
            }
        };
        ThreadUtils.postOnUiThreadDelayed(mResendPageEventTask, SAVE_ALL_IMPRESSION_DELAY);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event){
        if (event.messageType == MessageEvent.MessageType.IMP){
            if (mActionCalculatorMap.size() > 0)
                saveAllWindowImpressionDelayedForViewTreeChange();
        }
    }


    private void forceRefresh(final boolean withImpression) {
        Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
        if (activity != null) {
            ThreadUtils.cancelTaskOnUiThread(mResendPageEventTask);
            if (withImpression) {
                clearActionCalculatorMap();
                ThreadUtils.cancelTaskOnUiThread(mSaveAllWindowImpression);
                LogUtil.d(TAG, "forceRefresh: saveAllWindowImpression");
                mSaveAllWindowImpression.run();
            } else {
                ViewHelper.traverseWindow(activity.getWindow().getDecorView(), "", new ViewTraveler() {
                    @Override
                    public void traverseCallBack(ViewNode viewNode) {
                        if (viewNode.mView instanceof WebView || ClassExistHelper.instanceOfX5WebView(viewNode.mView)) {
                            LogUtil.d(TAG, "resend page event for ", viewNode.mView);
                            if (VdsJsBridgeManager.isWebViewHooked(viewNode.mView)) {
                                Util.callJavaScript(viewNode.mView, "_vds_hybrid.resendPage", false);
                            }
                        }
                    }
                });
            }
        }
    }

    private synchronized void clearActionCalculatorMapAndSaveImpressDelay(){
        mHandler.removeMessages(ProcessorHandler.MSG_SEND_ACTIVITY_PAGE);
        clearActionCalculatorMap();
        mIsInFirstImpressionTime = true;
        ThreadUtils.cancelTaskOnUiThread(mSaveAllWindowImpression);
        ThreadUtils.postOnUiThreadDelayed(mSaveAllWindowImpression, SAVE_NEW_WINDOW_IMPRESSION_DELAY);
    }

    private synchronized void clearActionCalculatorMap() {
        try {
            mActionCalculatorMap.clear();
        } catch (Exception ignore) {
            ignore.printStackTrace();
            LogUtil.i(TAG, "mActionCalculatorMap clear failed");
        }
        updateNextForceSaveAllImpressionTime();
    }

    private void updateNextForceSaveAllImpressionTime(){
        mNextForceSaveAllImpressionTime = System.currentTimeMillis() + 3000;
    }

    private Runnable mSaveAllWindowImpression = new Runnable() {
        @Override
        public void run() {
            mIsInFirstImpressionTime = false;
            saveAllWindowImpress(false);
        }
    };

    private void flushPendingActivityPageEvent() {
        PageEvent pendingPageEvent = mCoreMessageProcessor.getPendingPageEvent();
        if (pendingPageEvent != null) {
            mCoreMessageProcessor.savePage(pendingPageEvent, mCoreMessageProcessor.getPendingObj());
            mCoreMessageProcessor.updatePendingPageEvent(null, null);
        }
        mHandler.removeMessages(ProcessorHandler.MSG_SEND_ACTIVITY_PAGE);
        clearActionCalculatorMapAndSaveImpressDelay();
    }

    private void saveAllWindowImpressionDelayedForViewTreeChange() {
        if (mIsInFirstImpressionTime){
            // In first impression
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - mViewTreeChangeDownTime > 4000){
            // 此时可以认为是一次崭新的Scroll事件了
            updateNextForceSaveAllImpressionTime();
            mViewTreeChangeDownTime = currentTime;
        }
        if (mNextForceSaveAllImpressionTime < currentTime){
            LogUtil.d(TAG, "saveAllWindowImpression, and last saveAllImpressionTime over three seconds, force refresh");
            mSaveAllWindowImpression.run();
        }else{
            ThreadUtils.cancelTaskOnUiThread(mSaveAllWindowImpression);
            ThreadUtils.postOnUiThreadDelayed(mSaveAllWindowImpression, SAVE_ALL_IMPRESSION_DELAY);
        }
    }

    public void saveAllWindowImpress(boolean onlyNewWindow) {
        SysTrace.beginSection("gio.saveAllWindowImpress");
        updateNextForceSaveAllImpressionTime();
        EventCenter.getInstance().post(new CircleEvent("updateTagsIfNeeded"));
        if (mConfig == null || !mConfig.shouldSendImp()){
            SysTrace.endSection();
            return;
        }
        Activity activity = CoreInitialize.coreAppState().getResumedActivity();
        if (activity == null){
            SysTrace.endSection();
            return;
        }
        WindowHelper.init();
        View[] windowRootViews = WindowHelper.getWindowViews();
        ArrayList<ActionCalculator> newWindowCalculators = new ArrayList<ActionCalculator>();
        boolean skipOtherActivity = ViewHelper.getMainWindowCount(windowRootViews) > 1;
        WindowHelper.init();
        for (View root : windowRootViews) {
            if (root == null) continue;
            String prefix = WindowHelper.getWindowPrefix(root);
            if (WindowHelper.sIgnoredWindowPrefix.equals(prefix))
                continue;
            if (ViewHelper.isWindowNeedTraverse(root, prefix, skipOtherActivity) && findCalculatorByWindow(root) == null) {
                ActionCalculator actionCalculator = new ActionCalculator(
                        mAutoAppState.getPageName(activity), mCoreMessageProcessor.getPTMWithPending(), root, prefix);
                mActionCalculatorMap.put(new WeakReference<View>(root), actionCalculator);
                newWindowCalculators.add(actionCalculator);
            }
        }
        Collection<ActionCalculator> calculators;
        if (onlyNewWindow) {
            calculators = newWindowCalculators;
        } else {
            calculators = mActionCalculatorMap.values();
        }
        saveImpInBg(calculators);
        if (newWindowCalculators.size() > 0) {
            EventCenter.getInstance().post(new CircleEvent("refreshWebCircleTasks"));
        }
        SysTrace.endSection();
    }

    private void saveImpInBg(final Collection<ActionCalculator> calculators){
        if (mIsInObtainImpressing){
            LogUtil.d(TAG, "saveImpInBg, but mIsInObtainImpression is true, just return");
            return;
        }
        mBgExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mIsInObtainImpressing)
                    return;
                mIsInObtainImpressing = true;
                LogUtil.d(TAG, "start saveImpInBg....");
                try{
                    for (ActionCalculator calculator: calculators){
                        saveImpressInBgMyThrowException(calculator);
                    }
                }catch (Exception e){
                    if (e.getMessage() != null && e.getMessage().contains("WebView getUrl")){
                        LogUtil.d(TAG, "saveImpInBg failed, should be webView question, save imp delay");
                    }else{
                        LogUtil.d(TAG, "saveImpInBg failed, may ConcurrentModificationException or IndexOutOfException", e);
                    }
                    saveAllWindowImpressionDelayedForViewTreeChange();
                }finally {
                    mIsInObtainImpressing = false;
                }
            }
        });
    }


    private ActionCalculator findCalculatorByWindow(View root) {
        for (WeakReference viewReference : mActionCalculatorMap.keySet()) {
            if (viewReference.get() == root) {
                return mActionCalculatorMap.get(viewReference);
            }
        }
        return null;
    }

    private void saveImpressInBgMyThrowException(ActionCalculator calculator) {
        if (calculator != null) {
            List<ActionEvent> events = calculator.obtainImpress();
            if (events == null) return;
            for (ActionEvent event : events) {
                mCoreMessageProcessor.persistEvent(event);
            }
        }
    }

    private void updateSettingsIfNeeded() {
        if (shouldCancelUpdateSettings()) return;
        if (mConfig.isEnabled() && mCheckSettingsTask == null) {
            mCheckSettingsTask = new FetchTagListTask() {
                @Override
                public void afterRequest(Integer responseCode, byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                    super.afterRequest(responseCode, data, mLastModified, mResponseHeaders);
                    if (responseCode != HttpURLConnection.HTTP_OK
                            && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED
                            && sSettingsRetryCount < MAX_RETRY_CHECK_SETTINGS_COUNT) {
                        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
                            @Override
                            public void run() {
                                sSettingsRetryCount++;
                                updateSettingsIfNeeded();
                            }
                        }, 5000);
                    } else {
                        mLastSettingsUpdateTime = System.currentTimeMillis();
                    }
                    mCheckSettingsTask = null;
                }
            };
            mCheckSettingsTask.run();
        }
    }

    private boolean shouldCancelUpdateSettings() {
        return GConfig.ISOP()
                || (mLastSettingsUpdateTime != -1 && System.currentTimeMillis() - mLastSettingsUpdateTime < 1000 * 24 * 60 * 60);
    }


    @SuppressLint("HandlerLeak")
    private class ProcessorHandler extends Handler{
        private static final int MSG_SEND_ACTIVITY_PAGE = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_SEND_ACTIVITY_PAGE:
                    flushPendingActivityPageEvent();
                    break;
            }
        }
    }

}
