package com.growingio.android.sdk.collection;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.ipc.GrowingIOIPC;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.cp_annotation.Subscribe;
import com.growingio.eventcenter.bus.ThreadMode;

import java.util.UUID;

/**
 * Created by zyl on 15/4/16.
 */
public class SessionManager {
    private static final String TAG = "GIO.SessionManager";
    private static final int SAND_APP_CLOSE_DELAY = 10 * 1000;

    private Runnable mSendAppCloseTask;
    private boolean mNextPassMustSendVisit =false;       // 下一个Resume或者Pause生命周期中必须发送当前Visit
    private boolean mNextResumeSendVisit = false;     // 下一个Resume, 可能需要重发visit(不切换session的情况下)

    private final MessageProcessor messageProcessor;
    private final GrowingIOIPC growingIOIPC;
    private final GConfig config;

    public SessionManager(MessageProcessor processor, GrowingIOIPC ipc, GConfig config){
        messageProcessor = processor;
        growingIOIPC = ipc;
        this.config = config;
    }

    // only for preloadClass
    SessionManager(){
        messageProcessor = null;
        growingIOIPC = null;
        config = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActivityLifecycle(ActivityLifecycleEvent event){
        switch (event.event_type){
            case ON_RESUMED:
                onActivityResume();
                break;
            case ON_PAUSED:
                onActivityPause();
                break;
        }
    }

    /**
     * Get current session id
     * @return session id
     */
    public String getSessionIdInner() {
        String sessionId = growingIOIPC.getSessionId();
        if (TextUtils.isEmpty(sessionId)){
            sessionId = UUID.randomUUID().toString();
            growingIOIPC.setSessionId(sessionId);
            LogUtil.d(TAG, "found sessionId is null or empty, generate one sessionId: ", sessionId);
            mNextPassMustSendVisit = true;
        }
        return sessionId;
    }

    // 后台地理位置改变时, 标记下次Activity resume时, 若不改变session, 重发一个visit
    public void nextResumeResendVisit(){
        mNextResumeSendVisit = true;
    }

    // only for touch
    public static String getSessionId(){
        SessionManager manager = CoreInitialize.sessionManager();
        if (manager != null){
            return manager.getSessionIdInner();
        }
        return null;
    }

    @VisibleForTesting
    void onActivityResume() {
        long currentTime = System.currentTimeMillis();
        growingIOIPC.setLastResumeTime(currentTime);
        ThreadUtils.cancelTaskOnUiThread(mSendAppCloseTask);
        if (mNextPassMustSendVisit){
            messageProcessor.saveVisit(true);
            mNextPassMustSendVisit = false;
        }else{
            boolean lastPauseOverTime = (currentTime - getLastPauseTime()) > config.getSessionInterval();
            if (lastPauseOverTime) {
                growingIOIPC.setSessionId(UUID.randomUUID().toString());
                messageProcessor.saveVisit(true);
            }else if (mNextResumeSendVisit){
                messageProcessor.saveVisit(true);
                mNextResumeSendVisit = false;
            }
        }
    }

    private void onActivityPause(){
        if (mNextPassMustSendVisit){
            messageProcessor.saveVisit(true);
            mNextPassMustSendVisit = false;
        }
        updateLastPauseTime(System.currentTimeMillis());
        ThreadUtils.cancelTaskOnUiThread(mSendAppCloseTask);
        mSendAppCloseTask = new Runnable() {
            @Override
            public void run() {
                long lastPauseTime = getLastPauseTime();
                if (growingIOIPC.getLastResumeTime() < lastPauseTime) {
                    messageProcessor.setAppClose(lastPauseTime);
                }
            }
        };
        ThreadUtils.postOnUiThreadDelayed(mSendAppCloseTask, SAND_APP_CLOSE_DELAY);
    }

    public void updateSessionByUserIdChanged(){
        growingIOIPC.setSessionId(UUID.randomUUID().toString());
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageProcessor.saveVisit(true);
            }
        });
    }

    private void updateLastPauseTime(long pauseTime){
        growingIOIPC.setLastPauseTime(pauseTime);
    }

    private long getLastPauseTime(){
        return growingIOIPC.getLastPauseTime();
    }
}
