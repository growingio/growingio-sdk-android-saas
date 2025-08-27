package com.growingio.android.sdk.utils.rom;

import android.app.Activity;
import android.os.Build;

/**
 * author CliffLeopard
 * time   2017/10/14:下午4:50
 * email  gaoguanling@growingio.com
 */

public class FloatPermissionUtil {

    public static RomPermissionChecker getPermissionChecker(Activity activity) {
        if (Build.VERSION.SDK_INT < 23) {
            if (RomChecker.isMiuiRom()) {
                return new MiUiChecker(activity);
            } else if (RomChecker.isMeizuRom()) {
                return new MeizuChecker(activity);
            } else if (RomChecker.isHuaweiRom()) {
                return new HuaweiChecker(activity);
            } else if (RomChecker.is360Rom()) {
                return new QikuChecker(activity);
            }
        } else if (RomChecker.isMeizuRom()) {
            return new MeizuChecker(activity);
        }
        return new CommonRomChecker(activity);
    }
}
