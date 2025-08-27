package com.growingio.android.sdk.utils.rom;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:37
 * email  gaoguanling@growingio.com
 */

public class MeizuChecker extends RomPermissionChecker {

    public MeizuChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean check() {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return checkOp(24); //OP_SYSTEM_ALERT_WINDOW = 24;
        }
        return false;
    }

    @Override
    public Intent getApplyPermissionIntent() {
        Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
        // intent.setClassName("com.meizu.safe", "com.meizu.safe.security.AppSecActivity");
        intent.putExtra("packageName", mContext.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }


}
