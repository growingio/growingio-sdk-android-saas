package com.growingio.android.sdk.autoburry;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ActionMenuView;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.NewIntentEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.base.event.message.MessageEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.ClickEventAsyncExecutor;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ReflectUtil;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;

import static com.growingio.android.sdk.utils.ViewHelper.getClickViewNode;

/**
 * Created by lishaojie on 16/6/24.
 */

public class VdsAgent {
    final static String TAG = "GIO.VdsAgent";
    private static ThreadLocal<Boolean> sWebViewProcessChanging = new ThreadLocal<>();
    private static ThreadLocal<Boolean> sNotHandleClickResult = new ThreadLocal<>();

    public static void clickOn(View view) {

        if (GConfig.sCanHook)
            try {
                SysTrace.beginSection("gio.Click");
                if (persistClickEventRunnable.havePendingEvent()) {
                    return;
                }
                ViewNode viewNode = getClickViewNode(view);
                if (viewNode == null) {
                    return;
                }
                /**
                 * 对于无BannerText的ImageView组件,计算图片DHashcode作为v字段的值
                 */
                if (CoreInitialize.config().isImageViewCollectionEnable() && TextUtils.isEmpty(viewNode.mViewContent) && view instanceof ImageView) {
                    ActionEvent actionEvent = ViewHelper.getClickActionEvent(viewNode);
                    ClickEventAsyncExecutor.getInstance().execute(new WeakReference<View>(view), viewNode, actionEvent.clone());
                } else {
                    persistClickEventRunnable.resetData(viewNode);
                    handleClickResult(true);
                }
            } catch (Throwable e) {
                LogUtil.d(e);
            }finally {
                SysTrace.endSection();
            }
    }

    public static void clickOn(DialogInterface dialogInterface, int which) {
        try {
            if (dialogInterface instanceof AlertDialog) {
                clickOn(((AlertDialog) dialogInterface).getButton(which));
            }
        } catch (Exception e) {
            LogUtil.d(e);
        }
    }

    public static void clickOn(AdapterView adapterView, View view, int position, long rowId) {
        if (adapterView instanceof Spinner) {
            clickOn(adapterView);
        } else {
            clickOn(view);
        }
    }

    public static void onClick(Object object, View view) {
        try {
            if (object instanceof View.OnClickListener)
                clickOn(view);
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    /**
     * 防止focus事件丢失
     *
     * @see com.growingio.android.sdk.autoburry.page.visitor.ListenerInfoVisitor
     */
    public static void onFocusChange(Object object, View view, boolean hasFocus) {
        if (object instanceof View.OnFocusChangeListener && view instanceof TextView) {
            LogUtil.d(TAG, "onFocusChanged");
            ViewHelper.changeOn(view);
        }
    }

    public static void onProgressChangedStart(View webView, int progress) {
        if (GConfig.sCanHook
                && (sWebViewProcessChanging.get() == null || !sWebViewProcessChanging.get())) {
            sWebViewProcessChanging.set(true);
            VdsJsHelper jsHelper = (VdsJsHelper) webView.getTag(GrowingIO.GROWING_WEB_BRIDGE_KEY);
            if (jsHelper == null)
                return;
            SysTrace.beginSection("gio.onProgressChanged");
            jsHelper.onVdsAgentProgressChanged(webView, progress);
            SysTrace.endSection();
        }
    }

    public static void onProgressChangedEnd(View webView, int process) {
        sWebViewProcessChanging.set(false);
    }

    public static void onClick(Object object, DialogInterface dialogInterface, int which) {
        try {
            if (object instanceof DialogInterface.OnClickListener) {
                clickOn(dialogInterface, which);
            }
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void lambdaOnDialogClick(DialogInterface dialogInterface, int which){
        clickOn(dialogInterface, which);
    }

    public static void lambdaOnClick(View view){
        clickOn(view);
    }

    public static void lambdaOnItemClick(AdapterView parent, View view, int position, long id){
        clickOn(parent, view, position, id);
    }

    public static void lambdaOnCheckedChangeRadioGroup(RadioGroup radioGroup, int i){
        View childView = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
        clickOn(childView);
    }

    public static void lambdaOnCheckedChangedCompoundButton(CompoundButton button, boolean checked){
        clickOn(button);
    }

    public static void lambdaOnRatingChanged(RatingBar ratingBar, float rating, boolean fromUser){
        if (fromUser){
            clickOn(ratingBar);
        }
    }

    public static void lambdaOnGroupClick(ExpandableListView parent, View v, int groupPosition, long id){
        try{
            if (!GConfig.sCanHook || persistClickEventRunnable.havePendingEvent()) {
                return;
            }

            ViewNode viewNode = getClickViewNode(v);
            persistClickEventRunnable.resetData(viewNode);
            if (!threadLocalResult(sNotHandleClickResult)){
                handleClickResult(true);
            }
        }catch (Throwable e){
            LogUtil.e(TAG, e.getMessage(), e);
        }
    }

    private static boolean threadLocalResult(ThreadLocal<Boolean> threadLocal){
        return threadLocal.get() != null && threadLocal.get();
    }

    public static void lambdaOnChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id){
        try {
            if (!GConfig.sCanHook || persistClickEventRunnable.havePendingEvent()) {
                return;
            }
            ViewNode viewNode = getClickViewNode(v);
            persistClickEventRunnable.resetData(viewNode);
            if (!threadLocalResult(sNotHandleClickResult)){
                handleClickResult(true);
            }
        }catch (Throwable e){
            LogUtil.e(TAG, e.getMessage(), e);
        }
    }

    public static void lambdaOnMenuItemClick(MenuItem menuItem){
        try{
            if (!GConfig.sCanHook || persistClickEventRunnable.havePendingEvent()) {
                return;
            }
            ViewNode viewNode = getClickViewNode(menuItem);
            persistClickEventRunnable.resetData(viewNode);
            if (!threadLocalResult(sNotHandleClickResult)){
                handleClickResult(true);
            }
        }catch (Throwable e){
            LogUtil.e(TAG, e.getMessage(), e);
        }
    }


    public static void onItemClick(Object object, AdapterView parent, View view, int position, long id) {
        try {
            if (object instanceof AdapterView.OnItemClickListener || object instanceof AdapterView.OnItemSelectedListener) {
                clickOn(parent, view, position, id);
            }
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void onItemSelected(Object object, AdapterView parent, View view, int position, long id) {
        if (parent != null && parent instanceof Spinner) {
            // 目前只需要将Spinner的onItemSelected回调触发点击事件,因为Spinner的元素点击只会触发onItemSelected回调
            onItemClick(object, parent, view, position, id);
        }
    }

    public static void onStopTrackingTouch(Object thisObj, SeekBar seekBar) {
        try {
            clickOn(seekBar);
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void onRatingChanged(Object thisObj, RatingBar ratingBar, float rating, boolean fromUser) {
        try {
            if (fromUser) {
                clickOn(ratingBar);
            }
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void onCheckedChanged(Object object, RadioGroup radioGroup, int i) {
        try {
            if (object instanceof RadioGroup.OnCheckedChangeListener) {
                View childView = (View) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                clickOn(childView);
            }
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void onCheckedChanged(Object object, CompoundButton button, boolean checked) {
        try {
            if (object instanceof CompoundButton.OnCheckedChangeListener)
                clickOn(button);
        } catch (Throwable e) {
            LogUtil.d(e);
        }
    }

    public static void onFragmentTransactionAdd(Object fragmentTransaction, int containerId, Object fragment, Object action){
        onFragmentTransactionAdd(fragmentTransaction, containerId, fragment, null, action);
    }

    public static void onFragmentTransactionAdd(Object fragmentTransaction, int containerId, Object fragment, String tag, Object action){
        if (!GConfig.sCanHook) return;
        AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
    }

    public static void onFragmentTransactionAdd(Object fragmentTransaction, Object fragment, String tag, Object action){
        onFragmentTransactionAdd(fragmentTransaction, 0, fragment, tag, action);
    }

    public static void onFragmentTransactionReplace(Object fragmentTransaction, int containerId, Object fragment, String tag, Object action){
        if (!GConfig.sCanHook) return;
        AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
    }

    public static void onFragmentShow(Object fragmentTransaction, Object fragment, Object transaction){
        if (!GConfig.sCanHook) return;
        AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
    }

    public static void onFragmentAttach(Object fragmentTransaction, Object fragment, Object transaction){
        if (!GConfig.sCanHook) return;
        AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
    }

    public static void onFragmentTransactionReplace(Object fragmentTransaction, int containerId, Object fragment, Object action){
        onFragmentTransactionReplace(fragmentTransaction, containerId, fragment, null, action);
    }

    public static void onFragmentResume(final Object fragment) {
        if (!GConfig.sCanHook) return;
        AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
    }

    public static void onFragmentPause(final Object fragment) {
        // ignore
    }

    public static void setFragmentUserVisibleHint(final Object fragment, final boolean visibleToUser) {
        if (!GConfig.sCanHook) return;
        if (visibleToUser){
            AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
        }
        ViewTreeStatusChangeEvent viewTreeScrollEvent = new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.ScrollChanged);
        EventCenter.getInstance().post(viewTreeScrollEvent);
    }

    public static void onFragmentHiddenChanged(Object fragment, boolean hidden) {
        if (!GConfig.sCanHook) return;
        if (!hidden){
            AutoBuryObservableInitialize.autoBuryAppState().trackFragmentWithFilter(fragment);
        }
        ViewTreeStatusChangeEvent viewTreeStatusChangeEvent = new ViewTreeStatusChangeEvent((ViewTreeStatusChangeEvent.StatusType.ScrollChanged));
        EventCenter.getInstance().post(viewTreeStatusChangeEvent);
    }

    public static void showDialogFragment(Object dialogFragment, Object manager, String tag){
        trySaveNewWindow();
    }

    public static void showDialogFragment(Object dialogFragment, Object transaction, String tag, int result){
        trySaveNewWindow();
    }

    public static void showAlertDialogBuilder(AlertDialog.Builder builder, AlertDialog dialog) {
        trySaveNewWindow();
    }

    public static void showDialog(Dialog dialog) {
        trySaveNewWindow();
    }

    public static void showDialog(TimePickerDialog dialog) {
        showDialog((Dialog) dialog);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void showPopupMenu(PopupMenu menu) {
        trySaveNewWindow();
    }

    public static void showPopupMenuX(androidx.appcompat.widget.PopupMenu menu) {
        trySaveNewWindow();
    }

    public static void showToast(Toast toast) {
        WindowHelper.onToastShow(toast);
        trySaveNewWindow();
    }

    public static void showAsDropDown(PopupWindow window, View view) {
        trySaveNewWindow();
    }

    public static void showAsDropDown(PopupWindow window, View view, int xoff, int yoff) {
        trySaveNewWindow();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void showAsDropDown(PopupWindow window, View view, int xoff, int yoff, int gravity) {
        trySaveNewWindow();
    }

    public static void showAtLocation(PopupWindow window, View parent, int gravity, int x, int y) {
        trySaveNewWindow();
    }

    public static void onNewIntent(Object activity, Intent intent) {
        if (GConfig.sCanHook && activity instanceof Activity) {
            EventCenter.getInstance().post(ActivityLifecycleEvent.createOnNewIntentEvent((Activity) activity, intent));
        }
    }

    public static void onBroadcastReceiver(BroadcastReceiver receiver, Context context, Intent intent){
        onBroadcastServiceIntent(intent);
    }

    private static void onBroadcastServiceIntent(Intent intent){
        if (GConfig.sCanHook){
            LogUtil.d(TAG, "onBroadcastServiceIntent");
            EventCenter.getInstance().post(new NewIntentEvent(intent));
        }
    }

    public static void onServiceStart(Service service, Intent intent, int startId){
        onBroadcastServiceIntent(intent);
    }

    public static void onServiceStartCommand(Service service, Intent intent, int flags, int startId){
        onBroadcastServiceIntent(intent);
    }


    /**
     * @param webView 确保是WebView或者是X5的WebView
     */
    public static void loadUrl(View webView, String url) {
        loadUrl(webView, url, null);
    }

    public static void loadDataWithBaseURL(View webView, String baseUrl, String data,
                                           String mimeType, String encoding, String historyUrl) {
        LogUtil.d(TAG, String.format(Locale.CHINA, "loadDataWithBaseURL: baseURL=%s, data=%s", baseUrl, data));
        hookWebViewLoad(webView);
    }

    public static void loadData(View webView, String data, String mimeType, String encoding) {
        LogUtil.d(TAG, String.format("loadData: data=%s", data));
        hookWebViewLoad(webView);
    }

    /**
     * @param webView 确保WebView是系统的WebView或者X5的WebView
     */
    public static void loadUrl(View webView, String url, Map<String, String> headers) {
        LogUtil.i(TAG, "loadUrl: " + url);
        hookWebViewLoad(webView);
    }

    private static void hookWebViewLoad(View webView) {
        Class<?> webViewClass = webView.getClass();
        if (!GConfig.sCanHook
                || isTaoBao(webViewClass)) {
            return;
        }
        VdsJsBridgeManager.hookWebViewIfNeeded(webView);
        LogUtil.d(TAG, "trackWebView: ", webView, " with client ", null);
    }

    public static URLConnection openConnection(URLConnection con) throws IOException {
        LogUtil.d(TAG, "openConnection: ", con);
        return con;
    }


    public static void setWebChromeClient(WebView webView, final WebChromeClient client) {
        if (client == null || client.getClass() == WebChromeClient.class)
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, null);
        else
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, true);
    }

    public static void setWebChromeClient(com.tencent.smtt.sdk.WebView webView, com.tencent.smtt.sdk.WebChromeClient client) {
        if (client == null || client.getClass() == com.tencent.smtt.sdk.WebChromeClient.class) {
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, null);
        } else {
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, true);
        }
    }

    public static void setWebChromeClient(com.uc.webview.export.WebView webView, com.uc.webview.export.WebChromeClient client) {
        if (client == null || client.getClass() == com.uc.webview.export.WebChromeClient.class) {
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, null);
        } else {
            webView.setTag(GrowingIO.GROWING_WEB_CLIENT_KEY, true);
        }
    }


    private static boolean isTaoBao(Class cls) {
        if (GConfig.mSupportTaobaoWebView) {
            return false;
        }
        while (true) {
            if (cls.getName().equals("android.webkit.WebView") || cls.getName().equals("java.lang.Object"))
                return false;
            if (cls.getName().equals("android.taobao.windvane.webview.WVWebView"))
                return true;

            cls = cls.getSuperclass();
        }
    }

    private static boolean isX5WebView(Object obj) {
        Class cls = obj.getClass();
        while (true) {
            if (cls.getName().equals("java.lang.Object"))
                return false;
            if (cls.getName().equals("com.tencent.smtt.sdk.WebView"))
                return true;
            cls = cls.getSuperclass();
        }
    }



    private static void trySaveNewWindow() {
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()) {
            EventBus.getDefault().post(new MessageEvent(MessageEvent.MessageType.IMP));
        }
    }

    private static boolean handleBooleanResult(Object returnValueObject) {
        boolean result = false;

        if (returnValueObject instanceof Boolean) {
            result = (Boolean) returnValueObject;
        }

        return result;
    }

    public static void handleClickResult(Object returnValueObject) {
        boolean result = handleBooleanResult(returnValueObject);

        if (result && persistClickEventRunnable.havePendingEvent()) {
            ThreadUtils.cancelTaskOnUiThread(persistClickEventRunnable);
            ThreadUtils.postOnUiThread(persistClickEventRunnable);
        } else {
            persistClickEventRunnable.resetData(null);
        }
    }
    public static boolean onOptionsItemSelected(Object object, MenuItem item) {
        if (!GConfig.sCanHook || persistClickEventRunnable.havePendingEvent()) {
            return false;
        }

        ViewNode viewNode = null;

        if (object instanceof Activity
                && !ClassExistHelper.instanceOfAndroidXFragmentActivity(object)
                && !ClassExistHelper.instanceOfSupportFragmentActivity(object)) {
            viewNode = getClickViewNode(item);
        }

        persistClickEventRunnable.resetData(viewNode);

        return false;
    }

    public static boolean onMenuItemClick(Object object, MenuItem item) {
        boolean isMenuItemClickListener = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            isMenuItemClickListener = object instanceof Toolbar.OnMenuItemClickListener
                    || object instanceof ActionMenuView.OnMenuItemClickListener;
        }
        if (isMenuItemClickListener
                || object instanceof MenuItem.OnMenuItemClickListener
                || object instanceof PopupMenu.OnMenuItemClickListener){
            sNotHandleClickResult.set(true);
            lambdaOnMenuItemClick(item);
            sNotHandleClickResult.set(false);
        }
        return false;
    }

    public static boolean onGroupClick(Object thisObject, ExpandableListView parent, View v, int groupPosition, long id) {
        if (thisObject instanceof ExpandableListView.OnGroupClickListener){
            sNotHandleClickResult.set(true);
            lambdaOnGroupClick(parent, v, groupPosition, id);
            sNotHandleClickResult.set(false);
        }
        return false;
    }

    public static boolean onChildClick(Object thisObject, ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        if (thisObject instanceof ExpandableListView.OnChildClickListener){
            sNotHandleClickResult.set(true);
            lambdaOnChildClick(parent, v, groupPosition, childPosition, id);
            sNotHandleClickResult.set(false);
        }
        return false;
    }

    /**
     * 运行期判断是否是正确的Hook实现
     */
    public static boolean isRightClass(String className, String methodName, String methodDesc, String desClass) {
        Method method = ReflectUtil.getMethod(className, methodName, methodDesc);
        if (method == null) {
            // 对于混淆的类， 肯定不是需要hook的实现
            return false;
        }
        desClass = desClass.replace('/', '.');
        return method.getDeclaringClass().getName().equals(desClass);
    }

    // 移除gps信息采集，保留方法，避免低版本插件注入导致崩溃
    public static void onLocationChanged(Object thisObject, Location location) {
//        try {
//            if (GConfig.sCanHook && thisObject instanceof LocationListener) {
//                GrowingIO.getInstance().setGeoLocation(location.getLatitude(), location.getLongitude());
//            }
//        } catch (Throwable e) {
//            LogUtil.d(e);
//        }
    }

    public static void onReceiveLocation(Object bdLocationListener, BDLocation location) {
//        try {
//            if (GConfig.sCanHook && bdLocationListener instanceof BDLocationListener) {
//                int locType = location.getLocType();
//                if (locType == BDLocation.TypeGpsLocation
//                        || locType == BDLocation.TypeNetWorkLocation
//                        || locType == BDLocation.TypeOffLineLocation) {
//                    GrowingIO.getInstance().setGeoLocation(location.getLatitude(), location.getLongitude());
//                }
//            }
//        } catch (Throwable e) {
//            LogUtil.d(e);
//        }
    }

    public static void onLocationChanged(Object thisObject, AMapLocation location) {
//        try {
//            if (location.getErrorCode() == 0 && GConfig.sCanHook && thisObject instanceof AMapLocationListener) {
//                GrowingIO.getInstance().setGeoLocation(location.getLatitude(), location.getLongitude());
//            }
//        } catch (Throwable e) {
//            LogUtil.d(e);
//        }
    }

    public static void onLocationChanged(Object thisObject, TencentLocation location, int error, String reason) {
//        try {
//            if (error == TencentLocation.ERROR_OK && GConfig.sCanHook && thisObject instanceof TencentLocationListener) {
//                GrowingIO.getInstance().setGeoLocation(location.getLatitude(), location.getLongitude());
//            }
//        } catch (Throwable e) {
//            LogUtil.d(e);
//        }
    }

    // Notification 有关的
    public static void onPendingIntentGetActivityBefore(Context context, int requestCode, Intent intent,
                                                        int flags, Bundle bundle){
        onPendingIntentCreateBefore(intent);
    }

    private static void onPendingIntentCreateBefore(Intent intent){
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()){
            AutoBuryObservableInitialize.notificationProcessor().hookPendingIntentCreateBefore(intent);
        }
    }

    private static void onPendingIntentCreateAfter(Intent intent, PendingIntent pendingIntent){
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()){
            AutoBuryObservableInitialize.notificationProcessor().hookPendingIntentCreateAfter(intent, pendingIntent);
        }
    }

    public static void onPendingIntentGetBroadcastBefore(Context context, int requestCode,
                                                         Intent intent,  int flags){
        onPendingIntentCreateBefore(intent);
    }

    public static void onPendingIntentGetBroadcastAfter(Context context, int requestCode,
                                                        Intent intent,  int flags, PendingIntent pendingIntent){
        onPendingIntentCreateAfter(intent, pendingIntent);
    }

    public static void onPendingIntentGetServiceBefore(Context context, int requestCode,
                                                       Intent intent, int flags){
        onPendingIntentCreateBefore(intent);
    }

    public static void onPendingIntentGetServiceAfter(Context context, int requestCode,
                                                      Intent intent, int flags, PendingIntent pendingIntent){
        onPendingIntentCreateAfter(intent, pendingIntent);
    }

    public static void onPendingIntentGetActivityAfter(Context context, int requestCode, Intent intent,
                                                       int flags, Bundle bundle, PendingIntent pendingIntent){
        onPendingIntentCreateAfter(intent, pendingIntent);
    }

    public static void onPendingIntentGetActivityShortBefore(Context context, int requestCode, Intent intent, int flags){
        onPendingIntentCreateBefore(intent);
    }

    public static void onPendingIntentGetForegroundServiceBefore(Context context, int requestCode, Intent intent, int flags){
        onPendingIntentCreateBefore(intent);
    }

    public static void onPendingIntentGetForegroundServiceAfter(Context context, int requestCode, Intent intent, int flags, PendingIntent pendingIntent){
        onPendingIntentCreateAfter(intent, pendingIntent);
    }

    public static void onPendingIntentGetActivityShortAfter(Context context, int requestCode, Intent intent, int flags, PendingIntent pendingIntent){
        onPendingIntentCreateAfter(intent, pendingIntent);
    }

    public static void onNotify(NotificationManager manager, String tag, int id, Notification notification){
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()){
            AutoBuryObservableInitialize.notificationProcessor().onNotify(tag, id, notification);
        }
    }

    public static void onNotify(NotificationManager manager, int id, Notification notification){
        onNotify(manager, null, id, notification);
    }

    public static void onXiaoMiMessageArrived(PushMessageReceiver pushMessageReceiver, Context context, MiPushMessage miPushMessage){
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()){
            AutoBuryObservableInitialize.notificationProcessor().onXiaoMiMessageArrived(miPushMessage);
        }
    }

    public static void onXiaoMiMessageClick(PushMessageReceiver pushMessageReceiver, Context context, MiPushMessage miPushMessage){
        if (GConfig.sCanHook && CoreInitialize.config().isEnabled()){
            AutoBuryObservableInitialize.notificationProcessor().onXiaoMiMessageClicked(miPushMessage);
        }
    }

    /**
     * <strong>需要注意这个setVisibility并不十分准确， 目前仅能监听实现为View setViewVisibility, 需按需拓展</strong>
     */
    public static void onSetViewVisibility(View view, int visible){
        if (visible != View.GONE && GConfig.sCanHook){
            EventCenter.getInstance().post(new ViewTreeStatusChangeEvent(ViewTreeStatusChangeEvent.StatusType.LayoutChanged));
        }
    }

    private static class PersistClickEventRunnable implements Runnable {
        private ViewNode viewNode;
        private ActionEvent actionEvent;

        public void resetData(ViewNode viewNode) {
            this.viewNode = viewNode;

            if (viewNode != null) {
                actionEvent = ViewHelper.getClickActionEvent(viewNode);
            }
        }

        public boolean havePendingEvent() {
            return viewNode != null;
        }

        @Override
        public void run() {
            try {
                ViewHelper.persistClickEvent(actionEvent, viewNode);
            } catch (Throwable e) {
                LogUtil.d(e);
            }

            viewNode = null;
        }
    }

    private static PersistClickEventRunnable persistClickEventRunnable = new PersistClickEventRunnable();
}
