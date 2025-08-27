package com.growingio.android.sdk.utils.rom;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.growingio.android.sdk.utils.LogUtil;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:37
 * email  gaoguanling@growingio.com
 */

public class HuaweiChecker extends RomPermissionChecker {

    public HuaweiChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean check() {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return checkOp(24); //OP_SYSTEM_ALERT_WINDOW = 24;
        }
        return true;
    }

    @Override
    public Intent getApplyPermissionIntent() {

        Intent intent = new Intent();
        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.addviewmonitor.AddViewMonitorActivity");//悬浮窗管理页面
            intent.setComponent(comp);
            if (RomChecker.getEmuiVersion() != 3.1) {
                comp = new ComponentName("com.huawei.systemmanager", "com.huawei.notificationmanager.ui.NotificationManagmentActivity");//悬浮窗管理页面
                intent.setComponent(comp);
            }
        } catch (SecurityException e) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.huawei.systemmanager",
                    "com.huawei.permissionmanager.ui.MainActivity");
            intent.setComponent(comp);
        } catch (ActivityNotFoundException e) {
            /**
             * 手机管家版本较低 HUAWEI SC-UL10
             */
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.Android.settings", "com.android.settings.permission.TabItem");//权限管理页面 android4.4
            intent.setComponent(comp);
        } catch (Exception e) {
            intent = null;
            LogUtil.i(TAG, Log.getStackTraceString(e));
        }
        return intent;
    }
}
