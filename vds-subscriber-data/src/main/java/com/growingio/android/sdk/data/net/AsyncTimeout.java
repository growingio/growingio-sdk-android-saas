package com.growingio.android.sdk.data.net;

import com.growingio.android.sdk.utils.LogUtil;

import java.net.HttpURLConnection;

/**
 * 由于Java层的Socket没有暴露SO_SNDTIMEO属性接口, 且HTTPURLConnection类并没有writeTimeOut属性
 * 提供writeTimeOut接口, 仅调用enter与exit函数即可
 */
public class AsyncTimeout {
    private static final String TAG = "GIO.Timeout";

    public static class Timeout{
        private HttpURLConnection connection;
        private long timeoutMills;
        private Timeout next;
        private long deadlineMills;
        private volatile boolean exit = false;

        public Timeout(HttpURLConnection connection, long timeoutMills){
            this.connection = connection;
            this.timeoutMills = timeoutMills;
        }
    }

    private Timeout head;
    private Timeout fakeHead;
    private WatchDog watchDog;

    /**
     * 对应的connection将进入write模式, 开启一个timeout计时
     */
    public void enter(Timeout timeout){
        timeout.deadlineMills = System.currentTimeMillis() + timeout.timeoutMills;
        synchronized (this){
            if (head == null){
                head = timeout;
            }else if (head.deadlineMills > timeout.deadlineMills){
                // 需要插在头部
                Timeout oldHead = head;
                head = timeout;
                head.next = oldHead;
                notifyAll();
            } else {
                insertTimeoutLocked(timeout);
            }
            checkChildThreadStartLocked();
        }
    }

    /**
     * 通知 AsyncTimeout, write过程结束
     */
    public void exit(Timeout timeout){
        timeout.exit = true;
        timeout.connection = null;
        synchronized (this){
            notifyAll();
        }
    }

    private void insertTimeoutLocked(Timeout timeout){
        Timeout current = head;
        Timeout next = head.next;
        while (next != null && next.deadlineMills < timeout.deadlineMills){
            current = next;
            next = current.next;
        }
        current.next = timeout;
        timeout.next = next;
    }

    private void checkChildThreadStartLocked(){
        if (watchDog == null){
            watchDog = new WatchDog();
            watchDog.setDaemon(true);
            watchDog.start();
        }
    }

    private void wakeTimeoutConnectionLocked(long currentTime){
        if (head == null)
            return;

        if (fakeHead == null){
            fakeHead = new Timeout(null, 0);
        }
        fakeHead.next = head;

        Timeout lastTimeOut = fakeHead;
        Timeout current = head;
        while (current != null){
            if (checkAndDisconnectLocked(current, currentTime)){
                // 从列表中删除
                lastTimeOut.next = current.next;
            }else{
                lastTimeOut = current;
            }
            current = current.next;
        }
        head = fakeHead.next;
        fakeHead.next = null;
    }

    private boolean checkAndDisconnectLocked(Timeout timeout, long currentTime){
        if (timeout.exit)
            return true;
        if (timeout.deadlineMills <= currentTime){
            HttpURLConnection connection = timeout.connection;
            if (connection != null){
                LogUtil.d(TAG, "writeTimeout: ", connection);
                connection.disconnect();
                timeout.connection = null;
            }
            return true;
        }
        return false;
    }

    private class WatchDog extends Thread{
        @Override
        public void run() {
            synchronized (AsyncTimeout.this){
                while (head != null){
                    long currentTime = System.currentTimeMillis();
                    wakeTimeoutConnectionLocked(currentTime);
                    if (head == null){
                        try {
                            // 没有任务需要等待了, 等待50秒, 停止该watchdog
                            AsyncTimeout.this.wait(50_000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                        if (head == null){
                            watchDog = null;
                            LogUtil.d(TAG, "watchdog: 30(maybe) seconds pass, rm watchdog");
                            return;
                        }else{
                            continue;
                        }
                    }

                    Timeout waiting = head;
                    try {
                        AsyncTimeout.this.wait(waiting.deadlineMills - currentTime);
                    } catch (InterruptedException e) {
                        LogUtil.d(TAG, e.getMessage(), e);
                    } catch (Throwable e){
                        LogUtil.e(TAG, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
