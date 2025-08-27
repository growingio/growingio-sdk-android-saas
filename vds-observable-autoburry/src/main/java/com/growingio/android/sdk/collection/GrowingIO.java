package com.growingio.android.sdk.collection;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.EditText;

import androidx.annotation.Nullable;

import com.growingio.android.sdk.autoburry.AutoBuryAppState;
import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.autoburry.ImpObserver;
import com.growingio.android.sdk.deeplink.DeeplinkCallback;
import com.growingio.android.sdk.utils.ClassExistHelper;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class GrowingIO extends AbstractGrowingIO<GrowingIO>{

    private AutoBuryAppState autoBuryAppState;
    private ImpObserver impObserver;

    GrowingIO(Configuration configuration) {
        super(configuration);
        autoBuryAppState = AutoBuryObservableInitialize.autoBuryAppState();
        impObserver = AutoBuryObservableInitialize.impObserver();
        GInternal.getInstance().addFeaturesVersion("au", GConfig.GROWING_VERSION);
    }

    GrowingIO(){
        super();
    }

    public static GrowingIO startWithConfiguration(Application application, Configuration configuration){
        // just for kotlin
        return AbstractGrowingIO.startWithConfiguration(application, configuration);
    }

    static GrowingIO customStart(Configuration configuration){
        setHybridJSSDKUrlPrefix(configuration.hybridJSSDKUrlPrefix);
        setJavaCirclePluginHost(configuration.javaCirclePluginHost);
        return AbstractGrowingIO.customStart(configuration);
    }

    public static void trackBanner(final View banner, final List<String> bannerContents) {
        if (!(banner instanceof AdapterView)
                && !ClassExistHelper.instanceOfAndroidXRecyclerView(banner)
                && !ClassExistHelper.instanceOfAndroidXViewPager(banner)
                && !ClassExistHelper.instanceOfSupportViewPager(banner)
                && !ClassExistHelper.instanceOfSupportRecyclerView(banner)) {
            new IllegalArgumentException("当前只支持AdapterView, ViewPager 和 RecyclerView 实现的Banner").printStackTrace();
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                banner.setTag(GROWING_BANNER_KEY, bannerContents);
            }
        });
    }

    public static void ignoredView(final View view) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (view != null)
                    view.setTag(GROWING_IGNORE_VIEW_KEY, true);
            }
        });
    }

    public void ignoreViewImp(final View view){
        if (view != null){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    view.setTag(GROWING_IGNORE_VIEW_IMP_KEY, true);
                }
            });
        }
    }

    /**
     * 手动模式， 将接管此activity界面的page识别
     * - 建议在onCreate中调用, 以及其他客户认为是Page改变时(例如FragmentTransaction.add时, ViewPager切换时)
     * - 不需要在onResume中再次设置, 我们会根据策略自动在resume中重发
     * - 如果无法再onCreate中获知当前的pageName, 建议设置一个默认值(比如DEFAULT), 否则自动识别的Page可能被发出
     * - 手动模式与默认的自动识别page事件(trackAllFragment, trackFragment, ignoreFragment)是互斥关系, 如果切换到手动模式, 则用户必须负责此界面所有的page识别
     * - 调用此方法自动切换进入手动模式， activity范围内无法切换回自动模式
     * - GrowingIO所有方法强制为主线程调用
     * @param activity 对应的activity
     * @param pageName 用户认为的pageName
     * @return this
     */
    public GrowingIO manualPageShow(final Activity activity, @NonNull final String pageName){
        if (activity == null){
            Log.e(TAG, ErrorLog.VALUE_BE_EMPTY);
            return this;
        }
        if (!mArgumentChecker.isIllegalEventName(pageName)){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    autoBuryAppState.manualPage(activity, pageName);
                }
            });
        }
        return this;
    }

    public GrowingIO trackWebView(final View webView) {
        webView.setTag(GROWING_TRACK_WEB_VIEW, Boolean.TRUE);
        return this;
    }

    public GrowingIO markViewImpression(final ImpressionMark mark){
        if (mArgumentChecker.isIllegalEventName(mark.getEventId())){
            return this;
        }
        if (mark.getVariable() != null){
            mark.setVariable(mArgumentChecker.validJSONObject(mark.getVariable()));
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                impObserver.markViewImpression(mark);
            }
        });
        return this;
    }

    public GrowingIO stopMarkViewImpression(final View markedView){
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                impObserver.stopStampViewImp(markedView);
            }
        });
        return this;
    }


    public GrowingIO setTrackAllFragment(final Activity activity, final boolean trackAllFragment){
        if (activity == null)
            return this;
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                autoBuryAppState.setTrackAllFragment(activity, trackAllFragment);
            }
        });
        return this;
    }

    // ================== Params is Object
    private GrowingIO ignoreFragmentInternal(final Activity activity, final Object fragment){
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                autoBuryAppState.ignoreFragment(activity, fragment);
            }
        });
        return this;
    }

    private GrowingIO setPageNameInternal(final Object obj, final String pageName){
        if (!mArgumentChecker.isIllegalEventName(pageName)){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    autoBuryAppState.setPageAlias(obj, pageName);
                }
            });
        }
        return this;
    }

    private GrowingIO trackFragmentInternal(final Activity activity, final Object fragment){
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                autoBuryAppState.trackFragment(activity, fragment);
            }
        });
        return this;
    }

    private GrowingIO trackFragmentViewPagerInternal(final Activity activity, final Object viewPager, final View view, final String pagename){
        if (!mArgumentChecker.isIllegalEventName(pagename)){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    autoBuryAppState.trackCustomFragment(activity, (View) viewPager, view, pagename);
                }
            });
        }
        return this;
    }

    private GrowingIO setPageVariableInternal(final Object pageObj, String pageDesc, JSONObject variable){
        if (pageObj == null){
            Log.e(TAG, ErrorLog.argumentBeNull(pageDesc));
            return this;
        }
        if ((variable = mArgumentChecker.validJSONObject(variable, true)) != null){
            final JSONObject finalVariable = variable;
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setPageVariable(pageObj, finalVariable);
                }
            });
        }
        return this;
    }

    private GrowingIO setPageVariableInternal(final Object pageObj, String pageDesc, final String key, final String value){
        if (pageObj == null){
            Log.e(TAG, ErrorLog.argumentBeNull(pageDesc));
            return this;
        }
        if (mArgumentChecker.isIllegalEventName(key) || (value != null && mArgumentChecker.isIllegalValue(value))){
            return this;
        }
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                getAPPState().setPageVariable(pageObj, key, value);
            }
        });
        return this;
    }

    private GrowingIO setPageVariableInternal(final Object pageObj, String pageDesc, final String key, final boolean value){
        if (pageObj == null){
            Log.e(TAG, ErrorLog.argumentBeNull(pageDesc));
            return this;
        }
        if (!mArgumentChecker.isIllegalEventName(key)){
            runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    getAPPState().setPageVariable(pageObj, key, value);
                }
            });
        }
        return this;
    }

    private GrowingIO setPageVariableInternal(final Object pageObj, String pageDesc, final String key, final Number value){
        if (pageObj == null){
            Log.e(TAG, ErrorLog.argumentBeNull(pageDesc));
        }else{
            if (!mArgumentChecker.isIllegalEventName(key) && !mArgumentChecker.isIllegalValue(value)){
                runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        getAPPState().setPageVariable(pageObj, key, value);
                    }
                });
            }
        }
        return this;
    }
    // ================== Params is Object END

    // ================== Params is System Fragment
    public GrowingIO ignoreFragment(Activity activity, android.app.Fragment fragment) {
        return ignoreFragmentInternal(activity, fragment);
    }

    public GrowingIO setPageName(Activity activity, String name) {
        return setPageNameInternal(activity, name);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public GrowingIO setPageName(android.app.Fragment fragment, String name) {
        return setPageNameInternal(fragment, name);
    }

    public GrowingIO trackFragment(Activity activity, android.app.Fragment fragment) {
        return trackFragmentInternal(activity, fragment);
    }

    public GrowingIO setPageVariable(Activity activity, JSONObject variable) {
        return setPageVariableInternal(activity, "activity", variable);
    }

    public GrowingIO setPageVariable(Activity activity, String key, String value) {
        return setPageVariableInternal(activity, "activity", key, value);
    }

    public GrowingIO setPageVariable(Activity activity, String key, boolean value) {
        return setPageVariableInternal(activity, "activity", key, value);
    }

    public GrowingIO setPageVariable(Activity activity, String key, Number value) {
        return setPageVariableInternal(activity, "activity", key, value);
    }

    public GrowingIO setPageVariable(android.app.Fragment fragment, JSONObject variable) {
        return setPageVariableInternal(fragment, "fragment", variable);
    }

    public GrowingIO setPageVariable(android.app.Fragment fragment, String key, String value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariable(android.app.Fragment fragment, String key, boolean value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariable(android.app.Fragment fragment, String key, Number value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }
    // ================== Params is System Fragment END

    // ================== AndroidX Fragment
    /*
     * 由于Google在处理AndroidX时会注释掉第二个相同的方法, 为了保证逻辑不出现串跳, 约定所有AndroidX的方法， 必须出现在Support包前
     * 包括InstanceOf判断
     */
    public GrowingIO ignoreFragmentX(Activity activity, androidx.fragment.app.Fragment fragment) {
        return ignoreFragmentInternal(activity, fragment);
    }

    public GrowingIO setPageNameX(androidx.fragment.app.Fragment fragment, String name) {
        return setPageNameInternal(fragment, name);
    }

    public GrowingIO trackFragmentX(Activity activity, androidx.fragment.app.Fragment fragment) {
        return trackFragmentInternal(activity, fragment);
    }

    public GrowingIO trackFragmentX(Activity activity, androidx.viewpager.widget.ViewPager viewPager, View view, String pagename) {
        return trackFragmentViewPagerInternal(activity, viewPager, view, pagename);
    }
    public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, JSONObject variable) {
        return setPageVariableInternal(fragment, "fragment", variable);
    }

    public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, String value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, boolean value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, Number value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }
    // ================== AndroidX Fragment END

    // ================== Support Fragment
    public GrowingIO ignoreFragment(Activity activity, Fragment fragment) {
        return ignoreFragmentInternal(activity, fragment);
    }

    public GrowingIO setPageName(Fragment fragment, String name) {
        return setPageNameInternal(fragment, name);
    }

    public GrowingIO trackFragment(Activity activity, Fragment fragment) {
        return trackFragmentInternal(activity, fragment);
    }

    public GrowingIO trackFragment(Activity activity, ViewPager viewPager, View view, String pagename) {
        return trackFragmentViewPagerInternal(activity, viewPager, view, pagename);
    }
    public GrowingIO setPageVariable(Fragment fragment, JSONObject variable) {
        return setPageVariableInternal(fragment, "fragment", variable);
    }

    public GrowingIO setPageVariable(Fragment fragment, String key, String value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariable(Fragment fragment, String key, boolean value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }

    public GrowingIO setPageVariable(Fragment fragment, String key, Number value) {
        return setPageVariableInternal(fragment, "fragment", key, value);
    }
    // ================== Support Fragment END

    public static void setViewInfo(View view, String info) {
        view.setTag(GROWING_INHERIT_INFO_KEY, info);
    }

    public static void setViewContent(View view, String content) {
        view.setTag(GROWING_CONTENT_KEY, content);
    }

    @Deprecated
    public static void setTabName(View tab, String name) {
        tab.setTag(GROWING_VIEW_NAME_KEY, name);
    }

    @Deprecated
    public static void setPressed(final View view) {
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                view.setPressed(true);
                view.setClickable(true);
                view.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        view.setPressed(false);
                    }
                }, ViewConfiguration.getPressedStateDuration());
            }
        });
    }

    public GrowingIO trackEditText(final EditText editText){
        runOnUIThread(new Runnable() {
            @Override
            public void run() {
                editText.setTag(GROWING_TRACK_TEXT, true);
            }
        });
        return (GrowingIO) this;
    }

    static class EmptyGrowingIO extends GrowingIO{
        // 此类禁止手动添加改写， 确认有更改时， 在这里利用Android Studio的generate override功能自动生成
        // 然后利用正则自动匹配返回响应值

        @Override
        public GrowingIO ignoreFragment(Activity activity, android.app.Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO ignoreFragment(Activity activity, Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO setPageName(Activity activity, String name) {
            return this;
        }

        @Override
        public GrowingIO setPageName(android.app.Fragment fragment, String name) {
            return this;
        }

        @Override
        public GrowingIO setPageName(Fragment fragment, String name) {
            return this;
        }

        @Override
        public GrowingIO trackFragment(Activity activity, Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO trackEditText(EditText editText) {
            return this;
        }

        @Override
        public GrowingIO trackFragment(Activity activity, android.app.Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO trackFragment(Activity activity, ViewPager viewPager, View view, String pagename) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Activity activity, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Activity activity, String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Activity activity, String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Activity activity, String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(android.app.Fragment fragment, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(android.app.Fragment fragment, String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(android.app.Fragment fragment, String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(android.app.Fragment fragment, String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Fragment fragment, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Fragment fragment, String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Fragment fragment, String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(Fragment fragment, String key, Number value) {
            return this;
        }

        @Override
        public void disableDataCollect() {
            // ignore
        }

        @Override
        public void enableDataCollect() {
            // ignore
        }

        @Override
        public GrowingIO setThrottle(boolean throttle) {
            return this;
        }

        @Override
        public GrowingIO disable() {
            return this;
        }

        @Override
        public GrowingIO resume() {
            return this;
        }

        @Override
        public GrowingIO stop() {
            return this;
        }

        @Override
        public String getDeviceId() {
            return null;
        }

        @Override
        public String getVisitUserId() {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public GrowingIO setTestHandler(Handler handler) {
            return this;
        }

        @Override
        public GrowingIO setGeoLocation(double latitude, double longitude) {
            return this;
        }

        @Override
        public GrowingIO clearGeoLocation() {
            return this;
        }

        @Override
        public GrowingIO setUserId(String userId) {
            return this;
        }

        @Override
        public GrowingIO clearUserId() {
            return this;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public GrowingIO setPeopleVariable(JSONObject variables) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPeopleVariable(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setEvar(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setVisitor(JSONObject visitorVariable) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setAppVariable(String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setChannel(String channel) {
            return this;
        }

        @Override
        public GrowingIO disableImpression() {
            return this;
        }

        @Override
        public GrowingIO setImp(boolean enable) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, Number number) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO track(String eventName, Number number, JSONObject variable) {
            return this;
        }

        @Override
        public void trackPage(String pagename, String lastpage, long ptm) {
            // empty
        }

        @Override
        public void saveVisit(String pagename) {
            // empty
        }

        @Override
        public void trackPage(String pageName) {
            // empty
        }

        @Override
        public GrowingIO markViewImpression(ImpressionMark mark) {
            return this;
        }

        @Override
        public GrowingIO stopMarkViewImpression(View markedView) {
            return this;
        }

        @Override
        public GrowingIO setPageVariable(String pageName, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO onNewIntent(Activity activity, Intent intent) {
            return this;
        }

        @Override
        public GrowingIO manualPageShow(@NonNull Activity activity, @NonNull String pageName) {
            return this;
        }

        @Override
        public GrowingIO setTrackAllFragment(Activity activity, boolean trackAllFragment) {
            return this;
        }

        @Override
        public GrowingIO ignoreFragmentX(Activity activity, androidx.fragment.app.Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO setPageNameX(androidx.fragment.app.Fragment fragment, String name) {
            return this;
        }

        @Override
        public GrowingIO trackFragmentX(Activity activity, androidx.fragment.app.Fragment fragment) {
            return this;
        }

        @Override
        public GrowingIO trackFragmentX(Activity activity, androidx.viewpager.widget.ViewPager viewPager, View view, String pagename) {
            return this;
        }

        @Override
        public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, JSONObject variable) {
            return this;
        }

        @Override
        public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, String value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, boolean value) {
            return this;
        }

        @Override
        public GrowingIO setPageVariableX(androidx.fragment.app.Fragment fragment, String key, Number value) {
            return this;
        }

        @Override
        public GrowingIO setImeiEnable(boolean imeiEnable) {
            return this;
        }

        @Override
        public GrowingIO setGoogleAdIdEnable(boolean googleAdIdEnable) {
            return this;
        }

        @Override
        public GrowingIO setAndroidIdEnable(boolean androidIdEnable) {
            return this;
        }

        @Override
        public GrowingIO setOAIDEnable(boolean oaidEnable) {
            return this;
        }

        @Override
        public boolean doDeeplinkByUrl(@Nullable String url, @Nullable DeeplinkCallback callback) {
            return false;
        }
    }
}
