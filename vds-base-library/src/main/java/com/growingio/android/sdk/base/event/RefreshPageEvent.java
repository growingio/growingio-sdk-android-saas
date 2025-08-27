package com.growingio.android.sdk.base.event;

/**
 * 需要刷新Page事件的事件:
 * 目前由AppVar, setUserId进行更改
 * Created by liangdengke on 2018/7/14.
 */
public class RefreshPageEvent {

    private final boolean withImpression;
    private final boolean newPTM;

    /**
     * @param withImpression 是否刷新IMP事件
     * @param newPTM         是否重新刷新PTM事件
     */
    public RefreshPageEvent(boolean withImpression, boolean newPTM) {
        this.withImpression = withImpression;
        this.newPTM = newPTM;
    }

    public boolean isWithImpression() {
        return withImpression;
    }

    public boolean isNewPTM() {
        return newPTM;
    }
}
