package com.growingio.android.sdk.debugger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.view.WindowManager;

import com.growingio.android.sdk.api.LoginAPI;
import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.HttpCallBack;
import com.growingio.android.sdk.base.event.SocketEvent;
import com.growingio.android.sdk.base.event.ValidUrlEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.debugger.event.ExitAndKillAppEvent;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.models.PageEvent;
import com.growingio.android.sdk.models.VPAEvent;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.rom.FloatPermissionUtil;
import com.growingio.android.sdk.utils.rom.RomChecker;
import com.growingio.android.sdk.utils.rom.FloatPermissionChecker;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.ThreadMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.growingio.android.sdk.pending.PendingStatus.isProjection;
import static com.growingio.android.sdk.pending.PendingStatus.mLoginToken;

/**
 * 用户分发与悬浮窗有关的事件
 * - onCreate， onNewIntent时进行判断是否有圈选， mobile debugger， web圈app的存在
 * - 在生命周期改变时， 隐藏， 显示悬浮窗
 * - 在socket生命周期改变时， 隐藏， 显示， 更改悬浮窗提示
 * Created by liangdengke on 2018/8/8.
 */
public class DebuggerManager {
    private static final String TAG = "GIO.Debugger";

    // @Inject
    private CoreAppState coreAppState;

    // inner properties
    private LoginAPI loginAPI;
    private AlertDialog needSystemAlertPermissionDialog;
    private boolean isLoginDone = false;
    private boolean hasCheckPermissionAgain = false;

    private Map<String, DebuggerEventListener> type2EventListener = new HashMap<>();
    private DebuggerEventListener currentEventListener = null;

    public DebuggerManager(CoreAppState coreAppState) {
        this.coreAppState = coreAppState;
    }

    public DebuggerEventListener getDebuggerEventListenerByType(String type) {
        return type2EventListener.get(type);
    }

    public void registerDebuggerEventListener(String type, DebuggerEventListener listener) {
        type2EventListener.put(type, listener);
    }

    @Subscribe
    public void onActivityLifecycle(ActivityLifecycleEvent event) {
        switch (event.event_type) {
            case ON_CREATED:
            case ON_NEW_INTENT:
                checkMultipleProcessState(event.getActivity());
                break;
            case ON_RESUMED:
                onResumeActivity(event.getActivity());
                break;
            case ON_PAUSED:
            case ON_DESTROYED:
                // 判断隐藏所有的floatView, 之所以在Destory中也进行处理是防止在onCreate中finish
                onPauseActivity(event.getActivity());
                break;
        }
    }

    @Subscribe
    public void onViewTreeChange(ViewTreeStatusChangeEvent viewTreeStatusChangeEvent) {
        EventCenter.getInstance().post(new SocketEvent(SocketEvent.EVENT_TYPE.SCREEN_UPDATE));
    }

    @Subscribe
    public void onValidUrlSchema(ValidUrlEvent event) {
        if (event.activity != null && event.type == ValidUrlEvent.DEEPLINK) {
            ScreenshotHelper.initial();
            launchFloatViewIfNeed(event.data, event.activity);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPageEvent(PageEvent pageEvent) {
        if (currentEventListener != null && coreAppState.getResumedActivity() != null) {
            currentEventListener.onPageResume();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onExitAndKillApp(ExitAndKillAppEvent event) {
        LogUtil.d(TAG, "onExitAndKillApp", new Exception("just for log"));
        exit();
        killApp();
    }

    private void checkMultipleProcessState(Activity activity) {
        if (PendingStatus.isValidMultiProcessState()) {
            LogUtil.d(TAG, "found multi process state, and launch float view");
            ScreenshotHelper.initial();
            launchFloatViewIfNeed(null, activity);
        }
    }


    /**
     * Activity resume:
     * - 判断圈选是否显示
     * - 判断mobile debugger 是否显示
     * - 判断web圈选是否显示
     */
    public void onResumeActivity(Activity activity) {
        if (currentEventListener != null) {
            PendingStatus.syncModelOnResume();
            if (PendingStatus.isEnable()
                    && checkWindowPermission(activity)) {
                currentEventListener.onPageResume();
                ThreadUtils.postOnUiThreadDelayed(mCheckCanDrawOverlayPermissionDelay, 1000);
            }
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private Runnable mCheckCanDrawOverlayPermissionDelay = new Runnable() {
        @Override
        public void run() {
            Activity activity = coreAppState.getResumedActivity();
            if (activity == null)
                return;
            boolean hasPermission = FloatPermissionUtil.getPermissionChecker(activity).check();
            LogUtil.d(TAG, "onResume, delayed times, to check has canDrawOverlay permission: ", hasPermission);
            if (!hasPermission && currentEventListener != null) {
                LogUtil.d(TAG, "don't has canDrawOverlay permission, check permission again");
                hasCheckPermissionAgain = true;
                checkWindowPermission(activity);
            }
        }
    };

    /**
     * Activity pause
     * - 判断是否需要隐藏各种悬浮窗
     */
    public void onPauseActivity(Activity activity) {
        Activity current = coreAppState.getForegroundActivity();
        if (current == activity && currentEventListener != null) {
            currentEventListener.onPagePause();
            ThreadUtils.cancelTaskOnUiThread(mCheckCanDrawOverlayPermissionDelay);
        }
    }

    private DebuggerEventListener getEventListenerFromUriType(boolean mainProcess) {
        String uriType = null;
        if (PendingStatus.isAppCircleEnabled()) {
            uriType = "app-circle";
        } else if (PendingStatus.isWebCircleEnabled()) {
            uriType = "web-circle";
        } else if (PendingStatus.isDebuggerEnabled()) {
            uriType = "mobile-debugger";
        } else {
            return null;
        }
        if (mainProcess) {
            uriType += "-main";
        } else {
            uriType += "-non-main";
        }
        return getDebuggerEventListenerByType(uriType);
    }

    /**
     * 在生命周期中， 判断是否应该显示MobileDebugger， 圈选， Web圈App
     * 并由Debugger进行分发启动事件
     *
     * @param data 非null时表示从浏览器跳转得到, 为null时表示多进程OK
     */
    void launchFloatViewIfNeed(Uri data, final Activity activity) {
        LogUtil.d(TAG, "launchFloatViewIfNeed()");
        if (coreAppState.getForegroundActivity() == null) {
            coreAppState.setForegroundActivity(activity);
        }
        Boolean shouldFindEventMainProcessListener = null;
        if (data != null) {
            LogUtil.d(TAG, "isValidData:true");
            String circleType = data.getQueryParameter("circleType");
            if (circleType == null && data.getQueryParameter("dataCheckRoomNumber") != null) {
                // only for data-check
                LogUtil.d(TAG, "found data-check url, and set circleType to debugger");
                circleType = PendingStatus.DATA_CHECK;
            }
            mLoginToken = data.getQueryParameter("loginToken");
            PendingStatus.setSpecialModelFromType(circleType);
            shouldFindEventMainProcessListener = true;
        } else {
            shouldFindEventMainProcessListener = false;
        }
        if (shouldFindEventMainProcessListener != null) {
            if (currentEventListener != null) {
                LogUtil.d(TAG, "currentEventListener is not null, may be re-create Activity or multiple special model");
                isLoginDone = false;
                currentEventListener.onExit();
                currentEventListener = null;
            }
            currentEventListener = getEventListenerFromUriType(shouldFindEventMainProcessListener);
            LogUtil.d(TAG, "currentEventListener=", currentEventListener, ", and shouldFindEventMainProcessListener: ", shouldFindEventMainProcessListener);
            if (currentEventListener == null) {
                LogUtil.e(TAG, "not found valid event listener");
                PendingStatus.disable();
                return;
            }
            currentEventListener.onFirstLaunch(data);
        }
    }

    public boolean checkWindowPermission(Activity activity) {
        FloatPermissionChecker checker = new FloatPermissionChecker.Builder(activity).build();
        final Intent intent = checker.getIntentOrNull();
        if (checker.checkOp()) {
            return true;
        } else {
            try {
                showGuideDialog(activity, intent);
            } catch (Exception ignore) {
                needSystemAlertPermissionDialog = null;
            }
            return false;
        }
    }

    private void showGuideDialog(Activity activity, final Intent intent) {
        if (needSystemAlertPermissionDialog != null
                && needSystemAlertPermissionDialog.getOwnerActivity() == activity
                && needSystemAlertPermissionDialog.isShowing())
            return;
        if (needSystemAlertPermissionDialog != null) {
            needSystemAlertPermissionDialog.dismiss();
            needSystemAlertPermissionDialog = null;
        }
        AlertDialog.Builder builder =
                new AlertDialog
                        .Builder(activity)
                        .setTitle("GrowingIO SDK提示")
                        .setMessage("使用圈选功能,需要您开启当前应用的悬浮窗权限")
                        .setPositiveButton(intent == null ? "自行设置" : "去设置", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    if (needSystemAlertPermissionDialog != null) {
                                        needSystemAlertPermissionDialog.dismiss();
                                        needSystemAlertPermissionDialog = null;
                                    }
                                } catch (Exception ignore) {
                                }
                                if (intent != null) {
                                    CoreInitialize.coreAppState().getGlobalContext().startActivity(intent);
                                }
                            }
                        })
                        .setCancelable(false);

        if (hasCheckPermissionAgain
                || (RomChecker.isHuaweiRom() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            builder.setNegativeButton("已设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        LogUtil.d(TAG, "权限已设置， 启动之");
                        if (needSystemAlertPermissionDialog != null) {
                            needSystemAlertPermissionDialog.dismiss();
                            needSystemAlertPermissionDialog = null;
                        }
                    } catch (Exception ignore) {
                    }
                }
            });
        }
        needSystemAlertPermissionDialog = builder.create();
        needSystemAlertPermissionDialog.show();
        coreAppState.onGIODialogShow(activity, needSystemAlertPermissionDialog);
    }

    public void exit() {
        LogUtil.d(TAG, "exit");
        getLoginAPI().logout();
        PendingStatus.disable();
        isLoginDone = false;
        if (currentEventListener != null) {
            currentEventListener.onExit();
        }
    }

    public void killApp() {
        final Activity activity = coreAppState.getForegroundActivity();
        if (activity != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            activity.startActivity(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // never be able to deliver the result
                activity.setResult(Activity.RESULT_CANCELED);
                Activity nonEmbeddedActivity = activity;
                while(nonEmbeddedActivity.getParent() != null) {
                    nonEmbeddedActivity = nonEmbeddedActivity.getParent();
                }
                nonEmbeddedActivity.finishAffinity();
            } else {
                activity.finish();
            }
        }
        ThreadUtils.postOnUiThreadDelayed(new Runnable() {
            @Override
            public void run() {
                GrowingIOIPC growingIOIPC = CoreInitialize.growingIOIPC();
                int myPid = Process.myPid();
                if (growingIOIPC != null) {
                    for (Integer pid : growingIOIPC.getAlivePid()) {
                        if (myPid == pid)
                            continue;
                        LogUtil.d(TAG, "kill process: ", pid);
                        Process.killProcess(pid);
                    }
                }
                Process.killProcess(myPid);
            }
        }, 1000);
    }

    private LoginAPI getLoginAPI() {
        if (loginAPI == null) {
            synchronized (this) {
                loginAPI = new LoginAPI();
            }
        }
        return loginAPI;
    }

    public void login() {
        String loginToken = PendingStatus.mLoginToken;
        getLoginAPI().setHttpCallBack(new HttpCallBack() {
            @Override
            public void afterRequest(final Integer responseCode, final byte[] data, long mLastModified, Map<String, List<String>> mResponseHeaders) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (coreAppState.getForegroundActivity() != null) {
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                loginSuccess();
                            } else {
                                loginFailed(responseCode, data);
                            }
                        }
                    }
                });
            }
        });
        getLoginAPI().login(loginToken);
    }

    public boolean isLoginDone() {
        return isLoginDone;
    }

    public void loginSuccess() {
        LogUtil.d(TAG, "loginSuccess");
        isLoginDone = true;
        if (currentEventListener != null) {
            currentEventListener.onLoginSuccess();
        }
    }

    public void loginFailed(int responseCode, byte[] data) {
        LogUtil.d(TAG, "loginFailed");
        String errorMsg = "发生未知错误";
        String title = isProjection() ? "请重新扫描" : "请重新唤醒App";
        if (responseCode == 422) {
            try {
                JSONObject jsonObject = new JSONObject(new String(data));
                errorMsg = jsonObject.getString("error");
            } catch (JSONException ignored) {
            }
        } else if (responseCode >= 500) {
            errorMsg = "服务器错误，请稍后重新扫描二维码";
        } else if (responseCode == 0) {
            errorMsg = "检测不到网络连接，请确保已接入互联网";
            title = "请连接网络";
        }
        Activity current = coreAppState.getResumedActivity();
        exit();
        if (current != null && !current.isFinishing()) {
            try {
                Dialog dialog =
                        new AlertDialog.Builder(current).setTitle(title).setMessage(errorMsg)
                                .setPositiveButton("知道了", null).create();
                dialog.show();
                coreAppState.onGIODialogShow(current, dialog);
            } catch (WindowManager.BadTokenException ignore) {
            }
        }
    }
}
