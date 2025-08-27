package com.growingio.android.sdk.utils.rom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:38
 * email  gaoguanling@growingio.com
 */

public class QikuChecker extends RomPermissionChecker {

    public QikuChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean check() {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 19) {
            return checkOp(24);
        }
        return true;
    }

    @Override
    public Intent getApplyPermissionIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$OverlaySettingsActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!isIntentAvailable(intent)) {
            intent.setClassName("com.qihoo360.mobilesafe", "com.qihoo360.mobilesafe.ui.index.AppEnterActivity");
            if (!isIntentAvailable(intent)) {
                intent = null;
            }
        }
        return intent;
    }

    private boolean isIntentAvailable(Intent intent) {
        if (intent == null) {
            return false;
        }
        return mContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
}
