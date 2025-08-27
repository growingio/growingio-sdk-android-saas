package com.growingio.android.sdk.base.event;

/**
 * author CliffLeopard
 * time   2018/7/4:下午8:48
 * email  gaoguanling@growingio.com
 */
public class ScreenStatusEvent {
    public ScreenStatusType type;
    public ScreenStatusEvent(ScreenStatusType type){
        this.type = type;
    }
    public enum ScreenStatusType{
        SCREEN_ON,
        SCREEN_OFF,
        SCREEN_PRESENT,
    }
}
