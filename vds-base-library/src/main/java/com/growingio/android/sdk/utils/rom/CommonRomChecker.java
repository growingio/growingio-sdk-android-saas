package com.growingio.android.sdk.utils.rom;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.growingio.android.sdk.utils.LogUtil;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * author CliffLeopard
 * time   2017/10/14:下午5:09
 * email  gaoguanling@growingio.com
 */

public class CommonRomChecker extends RomPermissionChecker {

    public CommonRomChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean check() {
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                return Settings.canDrawOverlays(mContext);
            } catch (Exception e) {
                LogUtil.i(TAG, Log.getStackTraceString(e));
            }
        }
        return true;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public Intent getApplyPermissionIntent() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + mContext.getPackageName()));
        return intent;
    }
}
