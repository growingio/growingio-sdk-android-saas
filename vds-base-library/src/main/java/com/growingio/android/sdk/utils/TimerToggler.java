package com.growingio.android.sdk.utils;

import android.os.Handler;
import android.os.SystemClock;

/**
 * 时间过滤触发器
 * 外部触发条件过于频繁, 这里予以过滤
 */
public class TimerToggler implements Runnable {

    private final Handler mHandler;
    private final Runnable mAction;

    // other - 未触发的事件首次时间, -1 代表首次, 0 代表至少触发过一次,
    private long mFirstToggleTime = -1;

    private long mDelayTime;
    private long mMaxDelayTime;
    private boolean mFirstTimeDelay;

    private TimerToggler(Runnable action) {
        mHandler = new Handler();
        mAction = action;
    }

    public void toggle() {
        long currentTime = SystemClock.uptimeMillis();
        if (mDelayTime == 0) {
            // 没有设置延时时间
            takeAction();
        } else if (mFirstToggleTime == -1 && !mFirstTimeDelay) {
            // 首次， 不延时
            takeAction();
        } else if (mFirstToggleTime > 0 && currentTime - mFirstToggleTime >= mMaxDelayTime) {
            // 超过最大延时时间
            takeAction();
        } else {
            if (mFirstToggleTime <= 0) {
                mFirstToggleTime = currentTime;
            }
            mHandler.removeCallbacks(this);
            long targetTime = Math.min(mFirstToggleTime + mMaxDelayTime, currentTime + mDelayTime);
            mHandler.postAtTime(this, targetTime);
        }
    }

    void takeAction() {
        mHandler.removeCallbacks(this);
        mFirstToggleTime = 0;
        mAction.run();
    }

    public void reset() {
        mFirstToggleTime = -1;
        mHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        takeAction();
    }

    public static class Builder {
        private long delayTime = 50;
        private long maxDelayTime = 600;
        private Runnable action;
        private boolean firstTimeDelay = true;

        public Builder(Runnable action) {
            this.action = action;
        }

        /**
         * 触发添加的延时时间
         */
        public Builder delayTime(long delayTime) {
            if (delayTime >= 50L) this.delayTime = delayTime;
            else this.delayTime = 50L;
            return this;
        }

        /**
         * 延时到触发的最大延时时间
         */
        public Builder maxDelayTime(long maxDelayTime) {
            this.maxDelayTime = maxDelayTime;
            return this;
        }

        /**
         * @param firstTimeDelay true 首次触发有延时处理
         */
        public Builder firstTimeDelay(boolean firstTimeDelay) {
            this.firstTimeDelay = firstTimeDelay;
            return this;
        }

        public TimerToggler build() {
            TimerToggler toggler = new TimerToggler(this.action);
            toggler.mMaxDelayTime = maxDelayTime;
            toggler.mDelayTime = delayTime;
            toggler.mFirstTimeDelay = firstTimeDelay;
            return toggler;
        }
    }
}
