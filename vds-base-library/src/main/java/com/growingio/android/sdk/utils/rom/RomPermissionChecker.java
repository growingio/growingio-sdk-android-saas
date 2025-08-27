package com.growingio.android.sdk.utils.rom;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.util.Log;

import com.growingio.android.sdk.utils.LogUtil;

import java.lang.reflect.Method;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:32
 * email  gaoguanling@growingio.com
 */

public abstract class RomPermissionChecker {

    public static String TAG;
    protected Activity mContext;

    public RomPermissionChecker(Activity activity) {
        TAG = this.getClass().getSimpleName();
        mContext = activity;
    }

    public abstract boolean check();

    public abstract Intent getApplyPermissionIntent();

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected boolean checkOp(int op) {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            AppOpsManager manager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
            try {
                Class clazz = AppOpsManager.class;
                Method method = clazz.getDeclaredMethod("checkOp", int.class, int.class, String.class);
                return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op, Binder.getCallingUid(), mContext.getPackageName());
            } catch (Exception ignore) {
                LogUtil.i(TAG, Log.getStackTraceString(ignore));
            }
        } else {
            LogUtil.i(TAG, "Below API 19 cannot invoke!");
        }
        return false;
    }

    protected Intent getValidIntent() {
        Intent intent = getApplyPermissionIntent();
        return intent.resolveActivityInfo(mContext.getPackageManager(), PackageManager.MATCH_DEFAULT_ONLY) != null ? intent : null;
    }
}
