package com.growingio.android.sdk.autoburry.page;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

import com.growingio.android.sdk.autoburry.AutoBuryAppState;
import com.growingio.android.sdk.autoburry.AutoBuryMessageProcessor;
import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.autoburry.SuperFragment;
import com.growingio.android.sdk.autoburry.SuperViewPager;
import com.growingio.android.sdk.autoburry.page.visitor.ListenerInfoVisitor;
import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.collection.AbstractConfiguration;
import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.ImpressionMark;
import com.growingio.android.sdk.collection.MessageProcessor;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.cp_annotation.Subscribe;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by denghuaxin on 2018/1/16.
 */

public class PageObserver {
    private final static String TAG = "GIO.PageObserver";
    private static final Handler mHander = new Handler(Looper.getMainLooper());
    private static Callback callback;

    private CoreAppState coreAppState;

    public PageObserver(CoreAppState coreAppState, AutoBuryAppState autoBuryAppState){
        this.coreAppState = coreAppState;
        callback = new Callback(autoBuryAppState);
    }

    @Subscribe
    public void onViewTreeChanged(ViewTreeStatusChangeEvent event){
        Activity activity = coreAppState.getResumedActivity();
        if (activity != null){
            post(activity);
        }
    }

    @Subscribe
    public void onActivityLifecycle(ActivityLifecycleEvent event){
        Activity activity = event.getActivity();
        if (activity == null) return;
        if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_RESUMED){
            callback.nextForceRefreshTimeMill = System.currentTimeMillis() + 500;
            post(activity);
        }else if (event.event_type == ActivityLifecycleEvent.EVENT_TYPE.ON_PAUSED){
            mHander.removeCallbacks(callback);
        }
    }

    public static void scheduleViewPageDetectByFragmentChange(Activity activity){
        // 部分客户不在Application onCreate中初始化SDK， 而是在某个界面开始才启用SDK(防止下直接Crash，我们不支持非Application onCreate中初始化SDK)
        if (callback == null)
            return;
        callback.nextForceRefreshTimeMill = System.currentTimeMillis() + 500;
        if (activity != null)
            post(activity);
    }

    private static void post(Activity activity) {
        mHander.removeCallbacks(callback);
        callback.setActivity(activity);
        if (System.currentTimeMillis() >= callback.nextForceRefreshTimeMill){
            LogUtil.d(TAG, "last callback time over three seconds, and force refresh page");
            callback.run();
        }else{
            mHander.postDelayed(callback, 500);
        }
    }

    static class Callback implements Runnable {
        private WeakReference<Activity> mActivity;
        private ListenerInfoVisitor onlickListenerVisitor;
        private AutoBuryAppState autoBuryAppState;
        private int currentWidth;
        private int currentHeight;

        private Object currentPageObj;
        private Rect mRectBuff;
        private int[] mPointBuff;

        private long nextForceRefreshTimeMill = -1;

        public Callback(AutoBuryAppState appState) {
            autoBuryAppState = appState;
            this.onlickListenerVisitor = new ListenerInfoVisitor();
            mRectBuff = new Rect();
            mPointBuff = new int[2];
        }

        public void setActivity(Activity activity) {
            this.mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        public void run() {
            try {
                SysTrace.beginSection("gio.PageRun");
                Activity activity = mActivity.get();
                if (activity == null) {
                    LogUtil.e(TAG, "mActivity == null");
                    return;
                }
                if (!autoBuryAppState.isPageManualModel(activity)){
                    currentPageObj = null;
                    resolveLargerChildPage(activity, activity.getWindow().getDecorView());
                    checkAndSendPage(activity);
                    currentPageObj = null;
                }
                /**
                 * 考虑到部分用户在性能的取舍下，增加配置项，关闭视图树的遍历，仅在准确度优先的场景下遍历视图树
                 * 遍历视图树的操作，有以下两个作用：
                 * 1. 在manifest中配置tag标识view需要采集imp（gio-tag-）
                 * 2. 更准确的采集change事件，比如未在代码中设置setOnFocusChangeListener（字节码方式失效）
                 */
                if (CoreInitialize.config().getRunMode() == AbstractConfiguration.AccuracyPriorityMode) {
                    focusListenerAndImp(activity.getWindow().getDecorView());
                }
            } catch (Throwable e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }finally {
                SysTrace.endSection();
                nextForceRefreshTimeMill = System.currentTimeMillis() + 3000;
            }
        }

        private void focusListenerAndImp(View rootView){
            if (rootView instanceof ViewGroup){
                LinkedList<ViewGroup> stack = new LinkedList<>();
                stack.add((ViewGroup) rootView);
                while (!stack.isEmpty()){
                    ViewGroup viewGroup = stack.pop();
                    int childSize = viewGroup.getChildCount();
                    for (int i = 0; i < childSize; i++){
                        View child = viewGroup.getChildAt(i);
                        Object tag = child.getTag();
                        // 检测并触发imp
                        if (tag instanceof String
                                && ((String) tag).startsWith("gio-tag-")
                                && child.getTag(AbstractGrowingIO.GROWING_IMP_TAG_MARKED) == null){
                            String eventId = ((String) tag).substring("gio-tag-".length());
                            if (!TextUtils.isEmpty(eventId)){
                                LogUtil.d(TAG, "found impView with eventId: ", eventId);
                                AutoBuryObservableInitialize.impObserver().markViewImpression(new ImpressionMark(child, eventId));
                            }
                        }
                        if (child instanceof  ViewGroup){
                            stack.add((ViewGroup) child);
                        }else if (child instanceof EditText){
                            onlickListenerVisitor.handle(child);
                        }
                    }
                }
            }
        }

        private void checkAndSendPage(Activity activity){
            MessageProcessor messageProcessor = CoreInitialize.messageProcessor();
            AutoBuryMessageProcessor autoBuryMessageProcessor = AutoBuryObservableInitialize.autoBuryMessageProcessor();
            Object lastFragmentObj = autoBuryAppState.getForegroundFragment();
            if (currentPageObj != null){
                if (lastFragmentObj != currentPageObj){
                    LogUtil.d(TAG, "found different special page, and send page: ", currentPageObj);
                    if (lastFragmentObj != null){
                        SuperFragment lastSuperFragment = SuperFragment.createSuperFragment(lastFragmentObj);
                        autoBuryAppState.onPageFragmentInvisible(lastSuperFragment);
                    }
                    SuperFragment currentSuperFragment = SuperFragment.createSuperFragment(currentPageObj);
                    autoBuryAppState.onPageFragmentVisible(currentSuperFragment);
                }
            }else{
                boolean isLastPageEvent = messageProcessor.isLastEventPage(activity) || messageProcessor.isPendingPage(activity);
                autoBuryAppState.clearForegroundFragment(activity);
                if (!isLastPageEvent){
                    LogUtil.d(TAG, "checkAndSendPage, no special page, and send activity page");
                    autoBuryMessageProcessor.savePageForPureActivity(activity);
                }
            }
        }

        private void resolveLargerChildPage(Activity activity, View rootView){
            Pair<View, Object> pageView = findPageView(activity, rootView, null);
            currentPageObj = pageView == null ? null : pageView.second;
        }

        private Pair<View, Object> findPageView(Activity activity, View view, ViewParent parentView){
            Pair<View, Object> result = null;
            if (view instanceof ViewGroup){
                int childSize = ((ViewGroup) view).getChildCount();
                for (int i = 0; i < childSize; i++){
                    View child = ((ViewGroup) view).getChildAt(i);
                    if (!ViewHelper.isViewSelfVisible(child))
                        continue;
                    Pair<View, Object> newResult = findPageView(activity, child, (ViewGroup) view);
                    result = findLargerVisiblePage(result, newResult);
                }
            }
            if (result == null && parentView != null){
                Object fragment = isPageView(activity, view, parentView);
                if (fragment != null){
                    result = Pair.create(view, fragment);
                }
            }
            return result;
        }

        private Pair<View, Object> findLargerVisiblePage(Pair<View, Object> oldResult, Pair<View, Object> newResult){
            if (newResult == null)
                return oldResult;
            if (oldResult == null){
                return newResult;
            }
            View oldView = oldResult.first;
            View newView = newResult.first;
            oldView.getGlobalVisibleRect(mRectBuff);
            int oldArea = mRectBuff.width() * mRectBuff.height();
            newView.getGlobalVisibleRect(mRectBuff);
            int newArea = mRectBuff.width() * mRectBuff.height();
            if (oldArea >= newArea){
                return oldResult;
            }else{
                return newResult;
            }
        }


        private void resolveCenterPageObj(Activity activity, View rootView){
            currentWidth = rootView.getWidth();
            currentHeight = rootView.getHeight();
            int pointerX = currentWidth / 2;
            int pointerY = currentHeight * 3 / 5;

            LinkedList<View> leafs = new LinkedList<>();
            LinkedList<ViewGroup> currentLevelViews = new LinkedList<>();
            currentLevelViews.add((ViewGroup) rootView);

            while (!currentLevelViews.isEmpty()){
                // 广度遍历ViewTree
                ViewGroup current = currentLevelViews.pop();
                if (!ViewHelper.isViewSelfVisible(current))
                    continue;
                int currentSize = current.getChildCount();
                boolean hasChildIsLeaf = false;
                for (int i = 0; i < currentSize; i++){
                    View child = current.getChildAt(i);
                    Util.getVisibleRectOnScreen(child, mRectBuff, false, mPointBuff);
                    boolean isChildContainPoint = mRectBuff.contains(pointerX, pointerY);
                    if (isChildContainPoint){
                        hasChildIsLeaf = true;
                        if (child instanceof ViewGroup){
                            currentLevelViews.add((ViewGroup) child);
                        }else{
                            leafs.add(child);
                        }
                    }
                }
                if (!hasChildIsLeaf){
                    // 没有子View包含当前点， 此ViewGroup应被当做叶节点
                    leafs.add(current);
                }
            }
            Set<View> resolvedViews = new HashSet<>();
            LinkedList<Pair<View, Object>> pageViews = new LinkedList<>();
            for (View currentLeaf : leafs){
                View current = currentLeaf;
                while (current != null && current != rootView){
                    ViewParent parent = current.getParent();
                    Object pageObject = isPageView(activity, current, parent);
                    if (pageObject != null){
                        LogUtil.d(TAG, "found page view: ", current, ", and pageObj: ", pageObject);
                        pageViews.add(Pair.create(current, pageObject));
                        break;
                    }
                    if (parent == rootView)
                        break;
                    View parentView = (View) parent;
                    if (resolvedViews.contains(parentView))
                        break;
                    resolvedViews.add(parentView);
                    current = parentView;
                }
            }
            choiceBestPage(pageViews);
        }

        private void choiceBestPage(LinkedList<Pair<View, Object>> pageViews){
            if (pageViews.size() == 0)
                return;
            Object bestPageObj = null;
            if (pageViews.size() == 1){
                Pair<View, Object> pageView = pageViews.getFirst();
                bestPageObj = pageView.second;
            }else{
                for (Pair<View, Object> pageView: pageViews){
                    View currentView = pageView.first;
                    if (currentView.getWidth() * 3 < currentWidth
                            || currentView.getHeight() * 3 < currentHeight){
                        LogUtil.d(TAG, "choiceBestPage, found pageView's height or width not valid: ", pageView.second);
                    }else{
                        bestPageObj = pageView.second;
                        break;
                    }
                }
                if (bestPageObj == null){
                    bestPageObj = pageViews.getFirst().second;
                }
            }

            LogUtil.d(TAG, "choiceBestPage, and special page size: ", pageViews.size(), ", and choice best page: ", bestPageObj);
            currentPageObj = bestPageObj;
        }

        private Object isPageView(Activity activity, View current, ViewParent parentView){
            SuperViewPager superViewPager = null;
            if (ClassExistHelper.instanceOfAndroidXViewPager(parentView)){
                superViewPager = new SuperViewPager.AndroidXViewPager((androidx.viewpager.widget.ViewPager) parentView);
            }else if (ClassExistHelper.instanceOfSupportViewPager(parentView)){
                superViewPager = new SuperViewPager.V4ViewPager((ViewPager) parentView);
            }
            if (superViewPager != null){
                if (current != superViewPager.getCurrentView())
                    return null;
                if (superViewPager.isFragmentViewPager()){
                    Object fragment = superViewPager.getCurrentItemObj();
                    SuperFragment superFragment = SuperFragment.createSuperFragment(fragment);
                    if (autoBuryAppState.shouldTrackFragment(superFragment)){
                        LogUtil.d(TAG, "isPageView, found fragment needed track: ", fragment);
                        return fragment;
                    }else if (autoBuryAppState.isTrackCustomFragment(activity, (ViewGroup) parentView)){
                        LogUtil.d(TAG, "isPageView, found custom fragment needed track: ", parentView);
                        return current;
                    }
                }else{
                    if (autoBuryAppState.isTrackCustomFragment(activity, (ViewGroup) parentView)){
                        LogUtil.d(TAG, "isPageView, found custom fragment(not fragment PageAdapter)", parentView);
                        return current;
                    }
                }
            }

            return autoBuryAppState.getFragmentByView(activity, current);
        }
    }
}
