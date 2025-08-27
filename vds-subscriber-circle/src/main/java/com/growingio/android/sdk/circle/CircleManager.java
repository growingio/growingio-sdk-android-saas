package com.growingio.android.sdk.circle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.graphics.Point;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.growingio.android.sdk.api.TagStore;
import com.growingio.android.sdk.autoburry.AutoBuryAppState;
import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.autoburry.VdsJsBridgeManager;
import com.growingio.android.sdk.base.event.HeatMapEvent;
import com.growingio.android.sdk.base.event.SocketEvent;
import com.growingio.android.sdk.circle.view.CircleAnchorView;
import com.growingio.android.sdk.circle.view.CircleModeChooserDialog;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.debugger.DebuggerInitialize;
import com.growingio.android.sdk.debugger.DebuggerManager;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.Tag;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.models.WebEvent;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.GJSONStringer;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.eventcenter.EventCenter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

import static com.growingio.android.sdk.pending.PendingStatus.isAppCircleEnabled;
import static com.growingio.android.sdk.pending.PendingStatus.isDebuggerEnabled;
import static com.growingio.android.sdk.pending.PendingStatus.isEnable;
import static com.growingio.android.sdk.pending.PendingStatus.isProjection;
import static com.growingio.android.sdk.pending.PendingStatus.isWebCircleEnabled;

/**
 * Created by xyz on 15/9/9.
 */
@TargetApi(15)
public class CircleManager {

    private static final long DELAY_FOR_REQUEST_HEAT_MAP = 100;
    private static final String TAG = "GIO.CircleManager";
    private static final int PAGE_SNAPSHOT_DELAY = 200;

    private String mMaxSizeText;
    private CircleAnchorView mCircleAnchorView;
    private int mWaitingWebImpressionCount;
    private Runnable mWebViewSnapshotTimeout;
    private List<ViewNode> mPendingWebNodes;
    private SnapshotMessageListener mMessageListener;
    private SnapshotMessageListener circleMessageListener = new SnapshotMessageListener() {
        @Override
        public void onMessage(String message) {
            EventCenter.getInstance().post(new SocketEvent(SocketEvent.EVENT_TYPE.SEND, message));
        }
    };

    private AutoBuryAppState autoBuryAppState;
    private CoreAppState coreAppState;
    private DebuggerManager debuggerManager;
    private int snapshotKey;
    private String currentSnapShotKey;

    // 上一次由于滚动引起的截屏时间
    private long lastLayoutSnapShotTime;

    private final static Object sInstanceLocker = new Object();
    private static CircleManager sInstance;

    public static CircleManager getInstance() {
        synchronized (sInstanceLocker) {
            if (sInstance == null) {
                sInstance = new CircleManager();
            }
        }
        return sInstance;
    }

    private CircleManager(){
        autoBuryAppState = AutoBuryObservableInitialize.autoBuryAppState();
        coreAppState = CoreInitialize.coreAppState();
        debuggerManager = DebuggerInitialize.debuggerManager();
    }


    public void onResumed(Activity activity) {
        LogUtil.d(TAG, "onResumed, should show circleView and check heatMap");
        showCircleView(activity);
        updateHeatMap();
    }

    public void defaultListener(){
        setSnapshotMessageListener(circleMessageListener);
    }

    public void setSnapshotMessageListener(SnapshotMessageListener listener) {
        mMessageListener = listener;
    }


    public void refreshWebCircleTasks() {
        if (isProjection()) {
            // 如果距离上次Layout事件没有超过2s， 则延时2s
            long deltaTime = System.currentTimeMillis() - lastLayoutSnapShotTime < 2000 ? 2000 : PAGE_SNAPSHOT_DELAY;
            ThreadUtils.cancelTaskOnUiThread(mRefreshSnapshotTask);
            ThreadUtils.postOnUiThreadDelayed(mRefreshSnapshotTask, deltaTime);
        }
    }

    public String getCurrentSnapShotKey() {
        return currentSnapShotKey;
    }

    private GConfig getConfig() {
        return CoreInitialize.config();
    }

    public void launchAppCircle() {
        LogUtil.d(TAG, "launchAppCircle()");
        if (getCurrentActivity() == null) {
            LogUtil.d(TAG, "launchAppCircle() getCurrentActivity() == null return");
            return;
        }
        // get history events
        if (!getTagStore().isTagsReady()) {
            if (getTagStore().isLoading()) {
                LogUtil.d(TAG, "launchAppCircle() getTagStore().isLoading() return");
                return;
            }
            Activity activity = getCurrentActivity();
            final ProgressDialog progressDialog = new ProgressDialog(activity);
            getTagStore().setInitSuccess(new TagStore.InitSuccess() {
                @Override
                public void initSuccess() {
                    try {
                        LogUtil.d(TAG, "launchAppCircle()->initSuccess()");
                        launchAppCircle();
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    } catch (final IllegalArgumentException ignored) {
                    }

                }
            });
            try {

                progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                progressDialog.setMessage("正在加载历史标签");
                progressDialog.show();
                coreAppState.onGIODialogShow(activity, progressDialog);
            } catch (WindowManager.BadTokenException exception) {
                // Can't show dialog because Activity isn't running, show toast instead
                Toast.makeText(getCurrentActivity(), "正在加载历史标签", Toast.LENGTH_LONG).show();
            }
            getTagStore().initial();
            return;
        }
        LogUtil.d(TAG, "launchAppCircle() -> addCircleView()");
        if (addCircleView()) {
            HybridEventEditDialog.prepareWebView(getCurrentActivity());
        }
    }


    public void showDialog(DialogFragment dialog, String tag) {
        // 在嵌套两层的ActivityGroup的子Activity上无法调用DialogFragment.show，这里需要使用最顶层的ActivityGroup
        Activity current = getCurrentActivity();
        if (current == null) {
            return;
        }

        // 查找最上层的StateSaved的第一个Activity
        Field field = null;
        while (true){
            Activity parent = current.getParent();
            if (parent == null){
                break;
            }
            FragmentManager manager = parent.getFragmentManager();
            if (field == null){
                try {
                    field = manager.getClass().getDeclaredField("mStateSaved");
                    field.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
            if (field != null){
                try {
                    boolean isStateSaved = (boolean) field.get(manager);
                    if (isStateSaved){
                        break;
                    }
                } catch (IllegalAccessException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
            current = parent;
        }

        try {
            if (dialog.isAdded() || current.getFragmentManager().findFragmentByTag(tag) != null) {
                return;
            }
            dialog.show(current.getFragmentManager(), tag);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void gotWebSnapshotNodes(List<ViewNode> nodes, String host, String path) {
        if (isProjection()) {
            VdsJsBridgeManager.getInstance().onSnapshotFinished(null, nodes);
        } else if (isAppCircleEnabled()) {
            showWebEventEditDialog(nodes, host, path);
        }
    }

    private void showWebEventEditDialog(List<ViewNode> nodes, String host, String path) {
        if (mCircleAnchorView != null) {
            mCircleAnchorView.setVisibility(View.GONE);
        }
        Activity activity = getCurrentActivity();
        if (activity != null) {
            String page = autoBuryAppState.getPageName(activity);
            final HybridEventEditDialog dialog = new HybridEventEditDialog();
            dialog.setContent(activity, nodes, page, getAppState().getSPN(), new Runnable() {
                @Override
                public void run() {
                    showDialog(dialog, HybridEventEditDialog.class.getName());
                }
            });
        }
    }

    @SuppressLint("RtlHardcoded")
    public boolean addCircleView() {
        LogUtil.d(TAG, "addCircleView()");
        Activity activity = getCurrentActivity();
        if (activity == null || (isProjection())) {
            LogUtil.d(TAG, "addCircleView() 半途 return");
            return false;
        }
        if (debuggerManager.checkWindowPermission(activity)) {
            if (mCircleAnchorView == null) {
                EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.INIT));
                mCircleAnchorView = new CircleAnchorView(activity.getApplicationContext());
            }
            mCircleAnchorView.show();
            return true;
        } else {
            return false;
        }
    }


    public void removeFloatViews() {
        if (mCircleAnchorView != null) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) mCircleAnchorView.getLayoutParams();
            Point point = new Point();
            point.x = params.x;
            point.y = params.y;

            getConfig().saveFloatPosition(point.x, point.y);
            mCircleAnchorView.remove();
            mCircleAnchorView = null;
        }
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.HIDE));
    }

    void showCircleView(Activity activity) {
        LogUtil.d(TAG, "showCircleView() -> isAppCircleEnabled():true");
        if (activity == null){
            activity = coreAppState.getForegroundActivity();
        }
        addCircleView();
        FragmentManager fragmentManager = activity.getFragmentManager();
        if ((fragmentManager.findFragmentByTag(HybridEventEditDialog.class.getName()) != null
                && !fragmentManager.findFragmentByTag(HybridEventEditDialog.class.getName()).isRemoving())
                || (fragmentManager.findFragmentByTag(CircleModeChooserDialog.class.getName()) != null
                && !fragmentManager.findFragmentByTag(CircleModeChooserDialog.class.getName()).isRemoving())) {
            mCircleAnchorView.setVisibility(View.GONE);
            LogUtil.d(TAG, "showCircleView() -> addCircleView()");
        }
    }

    void updateHeatMap() {
        ThreadUtils.cancelTaskOnUiThread(delayForRequestHeatMapRunnable);
        if (!HybridEventEditDialog.hasEditDialog())
            ThreadUtils.postOnUiThreadDelayed(delayForRequestHeatMapRunnable, DELAY_FOR_REQUEST_HEAT_MAP);
    }

    public void refreshSnapshotWithType(final String eventType, final ViewNode target,
                                        final VPAEvent event) {
        if (getCurrentActivity() == null) {
            return;
        }
        mPendingWebNodes = null;
        mWaitingWebImpressionCount = 0;
        VdsJsBridgeManager.getInstance().registerSnapshotCallback(mSnapshotCallback);
        ViewHelper.traverseWindow(getCurrentActivity().getWindow().getDecorView(), "", mWebViewChecker);
        mWebViewSnapshotTimeout = new Runnable() {
            @Override
            public void run() {
                ThreadUtils.cancelTaskOnUiThread(mRefreshSnapshotTask);
                sendUserActionMessage(eventType, target, event);
                mPendingWebNodes = null;
            }
        };
        if (mWaitingWebImpressionCount > 0) {
            ThreadUtils.postOnUiThreadDelayed(mWebViewSnapshotTimeout, PAGE_SNAPSHOT_DELAY);
        } else {
            ThreadUtils.postOnUiThread(mWebViewSnapshotTimeout);
        }
    }

    private Runnable mRefreshSnapshotTask = new Runnable() {
        @Override
        public void run() {
            lastLayoutSnapShotTime = System.currentTimeMillis();
            refreshSnapshotWithType("touch", null, null);
        }
    };

    private VdsJsBridgeManager.SnapshotCallback mSnapshotCallback = new VdsJsBridgeManager.SnapshotCallback() {
        @Override
        public void onSnapshotFinished(List<ViewNode> nodes) {
            refreshSnapshotWithType("page", null, null);
        }
    };

    private ViewTraveler mWebViewChecker = new ViewTraveler() {
        @Override
        public void traverseCallBack(ViewNode viewNode) {
            if (viewNode.mView instanceof WebView) {
                WebView webView = (WebView) viewNode.mView;
                if (VdsJsBridgeManager.isWebViewHooked(webView)) {
                    mWaitingWebImpressionCount++;
                    Util.callJavaScript(webView, "_vds_hybrid.snapshotAllElements");
                }
            }
        }
    };

    public void showCircleModeDialog() {
        CircleModeChooserDialog dialog = new CircleModeChooserDialog();
        showDialog(dialog, CircleModeChooserDialog.class.getName());
    }


    private CoreAppState getAppState() {
        return coreAppState;
    }

    private TagStore getTagStore() {
        return TagStore.getInstance();
    }

    public void setShowTags(boolean show) {
        if (mCircleAnchorView != null && isEnable() && isAppCircleEnabled()) {
            if (show) {
                mCircleAnchorView.setTags(getTagStore().getTags());
            } else {
                mCircleAnchorView.setTags(null);
            }
            getConfig().setShowTags(show);
            if (getCurrentActivity() != null) {
                ViewHelper.traverseWindow(getCurrentActivity().getWindow().getDecorView(), "", mWebTagsTraveler);
            }
        }
    }

    private ViewTraveler mWebTagsTraveler = new ViewTraveler() {
        @Override
        public void traverseCallBack(ViewNode viewNode) {
            if (viewNode.mView instanceof WebView || ClassExistHelper.instanceOfX5WebView(viewNode.mView)) {
                View webView = viewNode.mView;
                JSONArray tags = new JSONArray();
                for (Tag tag : getTagStore().getWebTags()) {
                    tags.put(tag.toJson());
                }
                boolean show = getConfig().shouldShowTags();
                if (VdsJsBridgeManager.isWebViewHooked(webView)) {
                    if (show) {
                        Util.callJavaScript(webView, "_vds_hybrid.setTags", TagStore.getInstance().getWebTags());
                        Util.callJavaScript(webView, "_vds_hybrid.setShowCircledTags", true);
                    } else {
                        Util.callJavaScript(webView, "_vds_hybrid.setTags");
                        Util.callJavaScript(webView, "_vds_hybrid.setShowCircledTags", false);
                    }
                }
            }
        }
    };

    private Runnable mDelayedSetWebViewTags = new Runnable() {
        @Override
        public void run() {
            if (isAppCircleEnabled() && getConfig().shouldShowTags()) {
                if (mCircleAnchorView != null && mCircleAnchorView.getVisibility() == View.VISIBLE && !mCircleAnchorView.isMoving()) {
                    mCircleAnchorView.setTags(getTagStore().getTags());
                    if (getCurrentActivity() != null) {
                        ViewHelper.traverseWindow(getCurrentActivity().getWindow().getDecorView(), "", mWebTagsTraveler);
                    }
                }
                ThreadUtils.postOnUiThreadDelayed(this, PAGE_SNAPSHOT_DELAY);
            }
        }
    };

    public void updateTagsIfNeeded() {
        if (mCircleAnchorView != null && isEnable() && isAppCircleEnabled() && getConfig().shouldShowTags()) {
            mCircleAnchorView.setTags(getTagStore().getTags());
            ThreadUtils.cancelTaskOnUiThread(mDelayedSetWebViewTags);
            ThreadUtils.postOnUiThreadDelayed(mDelayedSetWebViewTags, PAGE_SNAPSHOT_DELAY);
        }
    }


    private void sendUserActionMessage(String userAction, ViewNode target, VPAEvent event) {
        ThreadUtils.cancelTaskOnUiThread(mWebViewSnapshotTimeout);
        String actionDesc = "";
        if ("click".equals(userAction)) {
            actionDesc = "点击了" + getMaxSizeText(target);
        } else if ("touch".equals(userAction)) {
            actionDesc = "更新截图";
        } else if ("page".equals(userAction)) {
            actionDesc = "进入了";
        }
        if (mMessageListener != null && isWebCircleEnabled()) {
            mMessageListener.onMessage(mergeNodes(userAction, actionDesc, target, mPendingWebNodes, event));
        } else if (mMessageListener != null && isDebuggerEnabled()) {
            EventCenter.getInstance().post(new SocketEvent(SocketEvent.EVENT_TYPE.SCREEN_UPDATE));
        }
    }

    private String mergeNodes(String action, String desc, ViewNode
            target, List<ViewNode> nodes, VPAEvent event) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            return action;
        }
        if (action.equals("page")) {
            String title = "";
            if (event instanceof PageEvent) {
                title = ((PageEvent) event).getTitle();
                if (TextUtils.isEmpty(title)) {
                    title = event.mPageName;
                }
            } else if (event instanceof WebEvent) {
                try {
                    title = event.toJson().getString("tl");
                    if (TextUtils.isEmpty(title)) {
                        title = event.toJson().getString("p");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            desc = "进入了" + title;
        }
        ScreenshotInfo screenshotInfo = new ScreenshotInfo(activity, nodes, target);
        JSONObject jsonObject = screenshotInfo.getScreenShotInfo();

        try {
            jsonObject.put("msgId", "user_action");
            currentSnapShotKey = Process.myPid() + "-" + snapshotKey++;
            jsonObject.put("sk", currentSnapShotKey);
            jsonObject.put("userAction", action);
            jsonObject.put("actionDesc", desc);
            jsonObject.put("sdkVersion", GConfig.GROWING_VERSION);
            jsonObject.put("appVersion", getConfig().getAppVersion());
            jsonObject.put("sdkConfig", getSDKConfig());
            jsonObject.put("domain", getAppState().getSPN());
            jsonObject.put("page", autoBuryAppState.getPageName(activity));
            return new GJSONStringer().convertToString(jsonObject);
        } catch (Exception e) {
            Log.e("WebSocketProxy", "send screenshot info message error", e);
        }
        return "";
    }

    private JSONObject getSDKConfig() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sdkVersion", GConfig.GROWING_VERSION);
            jsonObject.put("appVersion", getConfig().getAppVersion());
            jsonObject.put("isUseId", GConfig.USE_ID);
            jsonObject.put("isTrackingAllFragments", getConfig().shouldTrackAllFragment());
            jsonObject.put("isTrackWebView", getConfig().shouldTrackWebView());
            jsonObject.put("schema", GConfig.sGrowingScheme);
            jsonObject.put("channel", getConfig().getChannel());
        } catch (JSONException e) {
            LogUtil.d("GIO", e.getMessage());
        }
        return jsonObject;
    }

    private String getMaxSizeText(final ViewNode node) {
        if (node == null) {
            return "按钮";
        }
        if (!TextUtils.isEmpty(node.mViewContent)) {
            return node.mViewContent;
        }
        mMaxSizeText = null;
        if (node.mView instanceof ViewGroup && !(node.mView instanceof WebView) && !ClassExistHelper.instanceOfX5WebView(node.mView)) {
            node.setViewTraveler(new ViewTraveler() {
                float mMaxTextSize = 0;

                @Override
                public boolean needTraverse(ViewNode viewNode) {
                    return viewNode == node || super.needTraverse(viewNode) && !Util.isViewClickable(viewNode.mView);
                }

                @Override
                public void traverseCallBack(ViewNode viewNode) {
                    if (!TextUtils.isEmpty(viewNode.mViewContent)
                            && TextUtils.isGraphic(viewNode.mViewContent)) {
                        float textSize = viewNode.mView instanceof TextView ? ((TextView) viewNode.mView).getTextSize() : 0;
                        if (textSize > mMaxTextSize) {
                            mMaxTextSize = textSize;
                            mMaxSizeText = viewNode.mViewContent;
                        }
                    }
                }
            });
            node.traverseChildren();
        } else {
            mMaxSizeText = node.mViewContent;
        }
        return TextUtils.isEmpty(mMaxSizeText) ? "按钮" : mMaxSizeText;
    }

    public interface SnapshotMessageListener {
        void onMessage(String message);
    }

    private Runnable delayForRequestHeatMapRunnable = new Runnable() {
        @Override
        public void run() {
            EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.UPDATE));
        }
    };

    private Activity getCurrentActivity() {
        return getAppState().getForegroundActivity();
    }
}