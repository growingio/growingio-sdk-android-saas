package com.growingio.android.sdk.utils;

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.PixelCopy;
import android.view.PixelCopy.OnPixelCopyFinishedListener;
import android.view.Window;

import androidx.annotation.RequiresApi;

/**
 * Android T之后直接反射修改Paint中BitmapShader会直接抛出异常
 * 并且因为直接调用反射，会被veridex检测到
 * 因为T版本之后BitmapShader相关属性加入了黑名单，目前状态为blocked
 * 可以继续尝试去获取window，mViews、mParams、mRoots在Android T之后状态为unsupported
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class SynchronousPixelCopy implements OnPixelCopyFinishedListener {
    private static String TAG = "GIO.SynchronousPixelCopy";

    private static final long TIMEOUT_MILLIS = 1000;
    private static Handler sHandler;
    static {
        HandlerThread thread = new HandlerThread("PixelCopyHelper");
        thread.start();
        sHandler = new Handler(thread.getLooper());
    }

    private int mStatus = -1;

    public int request(Window source, Bitmap dest) {
        synchronized (this) {
            mStatus = -1;
            PixelCopy.request(source, dest, this, sHandler);
            return getResultLocked();
        }
    }

    private int getResultLocked() {
        long now = SystemClock.uptimeMillis();
        final long end = now + TIMEOUT_MILLIS;
        while (mStatus == -1 && now <= end) {
            try {
                this.wait(end - now);
            } catch (InterruptedException e) { }
            now = SystemClock.uptimeMillis();
        }
        if (mStatus == -1) {
            LogUtil.d(TAG, "PixelCopy request didn't complete within " + TIMEOUT_MILLIS + "ms");
        }
        return mStatus;
    }

    @Override
    public void onPixelCopyFinished(int copyResult) {
        synchronized (this) {
            mStatus = copyResult;
            this.notify();
        }
    }
}