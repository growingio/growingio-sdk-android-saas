package com.growingio.android.sdk.debugger;

import android.net.Uri;

/**
 * Mobile Debugger, Web圈App, App圈选一些通用的事件回调
 * 通用流程包括:
 * - DebuggerManager扫码， 获取url, 去除loginToken, 判断扫码事件类型
 * - 调用onFirstLaunch， 立即进入相应的功能状态
 *
 * Created by liangdengke on 2018/9/13.
 */
public interface DebuggerEventListener {
    void onFirstLaunch(Uri validData);
    void onLoginSuccess();
    void onPageResume();
    void onPagePause();
    void onExit();
}
