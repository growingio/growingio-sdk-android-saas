package com.growingio.android.sdk.autoburry;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

import com.growingio.android.sdk.autoburry.page.GrowingIOPageIgnore;
import com.growingio.android.sdk.autoburry.page.PageObserver;
import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.ErrorLog;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.EncryptionUtil;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WeakSet;
import com.growingio.cp_annotation.Subscribe;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 涉及无埋点专属的状态中心
 * 主要设计与Page相关的trackFragment， trackEditText之类的
 *
 * Created by liangdengke on 2018/7/4.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AutoBuryAppState {
    private static final String TAG = "GIO.AutoAppState";

    private SuperFragment mForegroundFragment;

    /*
    * TODO: 对于Fragment的自动重建， 这里缺少对应的策略
    * - mActivitiesWithFragments: 保存需要标记为页面的Fragment(包含用户手动track的和自动track到的)
    * - mActivitiesWithIgnoredFragments: 标记用户手动忽略的所有Fragment
    * - mActivitiesWithCustomViewPager: 需要标记为页面的ViewPager的hash值
    *
    * 两种情况:
    * - trackAllFragment true: 1. 检测到fragment的任何生命周期与transaction事件时自动添加(去重忽略), 2. 设置ignoreFragment时需要将withFragments剔除
    * - trackAllFragment false: 1. 用户手动调用trackFragment
    * trackFragment与ignoreFragment以用户调用顺序为准， 后者优先. 均为主线程调用
    * */
    WeakHashMap<Activity, WeakSet<Object>> mActivitiesWithFragments = new WeakHashMap<>();
    WeakHashMap<Activity, List<Integer>> mActivitiesWithIgnoredFragments = new WeakHashMap<>(2);
    WeakHashMap<Activity, List<Integer>> mActivitiesWithCustomViewPager = new WeakHashMap<>(2);

    /**
     * activity对应的手动模式PageName名称
     */
    WeakHashMap<Activity, String> mActivitiesManualPageNames = new WeakHashMap<>(0);
    // 与trackAllFragment策略相冲的activity
    WeakSet<Activity> mTrackAllFragmentSpecialActivities = new WeakSet<>();

    private WeakHashMap<Object, String> mPageAlias = new WeakHashMap<>(0);
    private List<WeakReference<EditText>> trackingEditTexts = new LinkedList<>();

    // @Inject
    private CoreAppState mCoreAppState;
    private GConfig mConfig;
    AutoBuryMessageProcessor autoBuryMessageProcessor;

    AutoBuryAppState(CoreAppState coreAppState, GConfig config){
        this.mCoreAppState = coreAppState;
        this.mConfig = config;
    }

    // used for preload class
    public AutoBuryAppState(){}

    // called from PageObserver
    public void silentTrackEditText(EditText editText) {
        Iterator<WeakReference<EditText>> iterator = trackingEditTexts.iterator();
        while (iterator.hasNext()) {
            WeakReference<EditText> reference = iterator.next();
            EditText tmpEdit = reference.get();
            if (tmpEdit == null) {
                iterator.remove();
            }
            if (tmpEdit == editText) {
                return;
            }
        }
        trackingEditTexts.add(new WeakReference<EditText>(editText));
    }

    public Object getForegroundFragment() {
        return mForegroundFragment == null ? null : mForegroundFragment.getFragment();
    }

    public Object getForegroundFragment(Activity activity) {
        if (mForegroundFragment != null && mForegroundFragment.isBelongActivity(activity)) {
            return mForegroundFragment.getFragment();
        }
        return null;
    }

    @Subscribe
    public void onActivityLifeCycleChange(ActivityLifecycleEvent event) {
        Activity activity = event.getActivity();
        switch (event.event_type) {
            case ON_CREATED:
                if (activity == null){
                    LogUtil.d(TAG, "onActivityCreated, but activity not found, return");
                    return;
                }
                LogUtil.d(TAG, "onActivityCreated ", activity);
                break;
            case ON_NEW_INTENT:
                break;
            case ON_RESUMED:
                if (activity == null){
                    LogUtil.d(TAG, "onActivityResumed, but activity not found, return");
                    return;
                }
                LogUtil.d(TAG, "onActivityResumed ", activity);
                mForegroundFragment = null;
                break;
            case ON_PAUSED:
                if (activity == null){
                    LogUtil.d(TAG, "onActivityPaused, but activity not found, return");
                    return;
                }
                LogUtil.d(TAG, "onActivityPaused ", activity);
                View focusView = activity.getWindow().getDecorView().findFocus();
                if (focusView instanceof EditText) {
                    LogUtil.d(TAG, "onActivityPaused, and focus view is EditText");
                    ViewHelper.changeOn(focusView);
                }
                break;
            case ON_STOPPED:
                if (activity == null){
                    LogUtil.d(TAG, "onActivityStopped, but activity not found, return");
                    return;
                }
                LogUtil.d(TAG, "onActivityStopped ", activity);
                break;
            case ON_DESTROYED:
                if (activity == null){
                    LogUtil.d(TAG, "onActivityDestroyed, but activity not found, return");
                    return;
                }
                LogUtil.d(TAG, "onActivityDestroyed ", activity);
                mActivitiesWithFragments.remove(activity);
                mActivitiesWithIgnoredFragments.remove(activity);
                break;
        }
    }

    public void onPageFragmentVisible(SuperFragment fragment) {
        if (shouldTrackFragment(fragment)) {
            mForegroundFragment = fragment;
            autoBuryMessageProcessor.onFragmentPage(fragment);
        }
    }

    public void onPageFragmentInvisible(SuperFragment fragment) {
        if (fragment.equals(mForegroundFragment)) {
            mForegroundFragment = null;
        }
    }

    /**
     * 将activity设置为手动发送page模式, 禁止一切自动的page识别
     * 并立即发送一个page事件
     */
    public void manualPage(Activity activity, String pageName){
        String oldPageName = mActivitiesManualPageNames.get(activity);
        if (oldPageName != null){
            if (oldPageName.equals(pageName)){
                Log.e(TAG, "manualPage, but oldPageName.equal(pageName) return");
                return;
            }
        }
        mActivitiesWithFragments.remove(activity);
        mActivitiesWithIgnoredFragments.remove(activity);
        mActivitiesWithCustomViewPager.remove(activity);
        mActivitiesManualPageNames.put(activity, pageName);
        if (mCoreAppState.getResumedActivity() != null){
            autoBuryMessageProcessor.savePageForManualModel(activity);
        }
    }

    public void setTrackAllFragment(Activity activity, boolean trackAllFragment){
        if (mConfig.shouldTrackAllFragment() == trackAllFragment){
            Log.e(TAG, ErrorLog.TRACK_FRAGMENT_ERROR);
            return;
        }
        mTrackAllFragmentSpecialActivities.add(activity);
    }

    // 此Activity的Fragment是否应该被track，activity为null时， 全局形式
    public boolean shouldTrackAllFragment(@Nullable Activity activity){
        boolean totalTrackAllFragment = mConfig.shouldTrackAllFragment();
        if (activity == null)
            return totalTrackAllFragment;
        if (totalTrackAllFragment){
            return !mTrackAllFragmentSpecialActivities.contains(activity);
        }else{
            return mTrackAllFragmentSpecialActivities.contains(activity);
        }
    }

    public boolean shouldTrackFragment(@NonNull SuperFragment fragment) {
        Activity foregroundActivity = mCoreAppState.getResumedActivity();
        return foregroundActivity != null && shouldTrackFragment(foregroundActivity, fragment);
    }

    @VisibleForTesting boolean shouldTrackFragment(Activity activity, SuperFragment superFragment) {

        if (!superFragment.isBelongActivity(activity)) {
            return false;
        }

        Object fragment = superFragment.getFragment();
        if (fragment == null)
            return false;
;
        if (isIgnoredFragment(activity, fragment))
            return false;

        if (isBannerView(superFragment.getView()))
            return false;

        if (shouldTrackAllFragment(activity)
                && !(superFragment instanceof SuperFragment.ViewFragment)) {
            return true;
        }

        Set<Object> fragments = mActivitiesWithFragments.get(activity);
        if (fragments != null && fragments.contains(fragment))
            return true;
        return mPageAlias.get(fragment) != null;
    }

    protected boolean isIgnoredActivity(Activity activity){
        if(activity.getClass().getAnnotation(GrowingIOPageIgnore.class) != null){
            Log.d(TAG, activity.getClass().getName() + " is ignore ...");
            return true;
        }
        return false;
    }

    private boolean isIgnoredFragment(Activity activity, Object fragment){
        if(fragment.getClass().getAnnotation(GrowingIOPageIgnore.class) != null){
            Log.d(TAG, fragment.getClass().getName() + " is ignore ...");
            return true;
        }
        List<Integer> ignores = mActivitiesWithIgnoredFragments.get(activity);
        return ignores != null && ignores.contains(fragment.hashCode());
    }

    private boolean isBannerView(View view){
        if (view == null) return false;
        while (view != null){
            if (view.getTag(GrowingIO.GROWING_BANNER_KEY) != null)
                return true;
            ViewParent parent = view.getParent();
            if (parent instanceof View){
                view = (View) parent;
            }else{
                view = null;
            }
        }
        return false;
    }

    public void setPageAlias(Object pageObject, String alias) {
        mPageAlias.put(pageObject, alias);
    }

    /**
     * just called for generating PageEvents,
     */
    public String getPageName(){
        Activity activity = mCoreAppState.getResumedActivity();
        return  activity == null ? null : getPageName(activity);
    }

    /**
     * just called for generating PageEvents
     */
    public Object getCurrentPage(){
        Activity activity = mCoreAppState.getResumedActivity();
        if (activity != null && mForegroundFragment != null){
            return mForegroundFragment.getFragment();
        }
        return activity;
    }

    public void clearForegroundFragment(Activity activity){
        if (mForegroundFragment != null && mForegroundFragment.isBelongActivity(activity)){
            mForegroundFragment = null;
        }
    }

    public String getFragmentPageName(Object fragment) {
        String className;
        if (fragment != null) {
            String alias = mPageAlias.get(fragment);
            if (alias != null) {
                className = alias;
                LogUtil.d(TAG, "GET className from userSet :" + className);
            } else {
                className = getEndcodedName(fragment.getClass());
            }
            return className;
        }
        return null;
    }

    public String getPageName(Activity activity) {
        if (activity == null){
            return "UNKNOWN";
        }
        String manualPageName = mActivitiesManualPageNames.get(activity);
        if (manualPageName != null) {
            return manualPageName;
        }
        String className;
        if (mForegroundFragment != null) {
            Object fragment = getForegroundFragment(activity);
            className = getFragmentPageName(fragment);
            if (className != null) {
                return className;
            }
        }

            String alias = mPageAlias.get(activity);
            if (alias != null) {
                className = alias;
            } else {
                className = getEndcodedName(activity.getClass());
            }

        return className;
    }

    private static String getEndcodedName(Class<?> componentClass) {
        String className;
        try {
            Field viewsField = componentClass.getDeclaredField(Constants.SIGN_FIELD_NAME);
            viewsField.setAccessible(true);
            String giName = EncryptionUtil.AESDecode((String) viewsField.get(null));
            if (!TextUtils.isEmpty(giName)) {
                className = Constants.SIGN_FLAG + giName;
            } else {
                className = Util.getSimpleClassName(componentClass);
            }
        } catch (Exception ignore) {
            className = Util.getSimpleClassName(componentClass);
        }
        return className;
    }

    public void trackFragment(Activity activity, Object fragment) {
        trackFragmentWithRef(activity, fragment);
    }

    public void trackFragmentWithFilter(Object fragment){
        Activity activity = SuperFragment.getActivityFromFragment(fragment);
        if (activity == null){
            activity = mCoreAppState.getForegroundActivity();
        }
        if (!shouldTrackAllFragment(activity)){
            LogUtil.d(TAG, "trackFragmentWithFilter, and not trackAllFragment. return ...");
            return;
        }
        if (activity == null || fragment == null){
            LogUtil.d(TAG, "trackFragmentWithFilter, and activity is null. return ...");
            return;
        }

        if (isPageManualModel(activity)){
            LogUtil.d(TAG, "trackFragmentWithFilter, current Activity is manual page model, return...");
            return;
        }

        if (isIgnoredFragment(activity, fragment)){
            LogUtil.d(TAG, "trackFragmentWithFilter, and ignored fragment. return");
            return;
        }

        // Fragment Action改变时一般需要PageObserver重新扫描
//        PageObserver.post(activity);

        WeakSet<Object> fragments = mActivitiesWithFragments.get(activity);
        if (fragments != null && fragments.contains(fragment)){
            LogUtil.d(TAG, "trackFragmentWithFilter, and contained, return");
            return;
        }
        if (fragment instanceof Fragment){
            trackFragment(activity, fragment);
        }else if (ClassExistHelper.instanceOfAndroidXFragment(fragment)){
            trackFragment(activity, fragment);
        }else if (ClassExistHelper.instanceOfSupportFragment(fragment)){
            trackFragment(activity, fragment);
        }else{
            LogUtil.d(TAG, "trackFragmentWithFilter, and unknown type");
        }
    }

    private void trackFragmentWithRef(Activity activity, Object fragment) {
        WeakSet<Object> fragments = mActivitiesWithFragments.get(activity);
        if (fragments == null) {
            fragments = new WeakSet<>();
            mActivitiesWithFragments.put(activity, fragments);
        }
        fragments.add(fragment);

        List<Integer> ignoreFragmentHashCodes = mActivitiesWithIgnoredFragments.get(activity);
        if (ignoreFragmentHashCodes != null){
            int hashCode = fragment.hashCode();
            if (ignoreFragmentHashCodes.contains(hashCode)){
                ignoreFragmentHashCodes.remove((Integer) hashCode);
            }
        }
        PageObserver.scheduleViewPageDetectByFragmentChange(activity);
    }


    public void ignoreFragment(Activity activity, Object fragment) {
        ignoreFragmentWithRef(activity, fragment);
    }

    private void ignoreFragmentWithRef(Activity activity, Object fragment) {
        List<Integer> fragments = mActivitiesWithIgnoredFragments.get(activity);
        if (fragments == null) {
            fragments = new ArrayList<Integer>(1);
            mActivitiesWithIgnoredFragments.put(activity, fragments);
        }
        fragments.add(fragment.hashCode());
        WeakSet<Object> trackFragments = mActivitiesWithFragments.get(activity);
        if (trackFragments != null && trackFragments.contains(fragment)){
            trackFragments.remove(fragment);
        }
        if (mForegroundFragment != null && mForegroundFragment.getFragment() == fragment){
            mForegroundFragment = null;
        }
        PageObserver.scheduleViewPageDetectByFragmentChange(activity);
        // 是否需要将ViewPager中的也给删除， 感觉不用啦， 一般不会冲突
    }

    /**
     * @return 返回 activity的page识别是否是手动模式
     */
    public boolean isPageManualModel(Activity activity){
        return mActivitiesManualPageNames.containsKey(activity);
    }

    public void trackCustomFragment(Activity activity, View viewPager, View page, String pagename) {
        List<Integer> fragments = mActivitiesWithCustomViewPager.get(activity);
        if (fragments == null) {
            fragments = new ArrayList<Integer>(1);
            mActivitiesWithCustomViewPager.put(activity, fragments);
        }
        fragments.add(viewPager.hashCode());
        setPageAlias(page, pagename);
    }

    public boolean isFragmentView(Activity activity, View view) {
        WeakSet<Object> fragments = mActivitiesWithFragments.get(activity);
        if (fragments == null || fragments.isEmpty())
            return false;
        for (Object fragment : fragments) {
            if (fragment == null)
                continue;
            if (fragment instanceof Fragment) {
                if (((Fragment)fragment).getView() == view) {
                    return true;
                }
            }else if (ClassExistHelper.instanceOfAndroidXFragment(fragment)){
                if (((androidx.fragment.app.Fragment)fragment).getView() == view){
                    return true;
                }
            } else if (ClassExistHelper.instanceOfSupportFragment(fragment)) {
                if (((android.support.v4.app.Fragment) fragment).getView() == view) {
                    return true;
                }
            }
        }
        return false;
    }

    public Object getFragmentByView(Activity activity, View view) {
        WeakSet<Object> fragments = mActivitiesWithFragments.get(activity);
        if (fragments == null)
            return null;
        for (Object fragment : fragments) {
            if (fragment == null)
                continue;
            if (fragment instanceof Fragment) {
                if (((Fragment) fragment).getView() == view) {
                    return fragment;
                }
            }else if (ClassExistHelper.instanceOfAndroidXFragment(fragment)){
                if (((androidx.fragment.app.Fragment)fragment).getView() == view){
                    return fragment;
                }
            }else if (ClassExistHelper.instanceOfSupportFragment(fragment)) {
                if (((android.support.v4.app.Fragment) fragment).getView() == view) {
                    return fragment;
                }
            }
        }
        return null;
    }

    public boolean isTrackCustomFragment(Activity activity, ViewGroup view) {
        List<Integer> fragments = mActivitiesWithCustomViewPager.get(activity);
        return fragments != null && fragments.contains(view.hashCode());
    }
}
