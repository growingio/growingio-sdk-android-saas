package com.growingio.android.sdk.message;

/**
 * author CliffLeopard
 * time   2018/4/20:上午11:41
 * email  gaoguanling@growingio.com
 */
public class HandleType {
    public static final int SAVE_EVENT = 0x100000;
    public static final int DB_MSG_FLAG = 0x200000;
    public static final int DB_CREATE_DB = DB_MSG_FLAG + 1;
    public static final int DB_UPGRADE_DB = DB_CREATE_DB + 1;
    public static final int DB_SAVE_EVENT = DB_UPGRADE_DB + 1;
    public static final int DB_CLEAN_EVENT = DB_SAVE_EVENT + 1;
    public static final int DB_READ_DB = DB_CLEAN_EVENT + 1;
    public static final int MU_NEW_EVENT_SAVED = 0x400000;
    public static final int MU_UPLOAD_EVENT = MU_NEW_EVENT_SAVED + 1;
    public static final int MU_UPLOAD_EVENT_SUCCESS = MU_UPLOAD_EVENT + 1;
    public static final int CONFIG_SAVE_SERVER_SETTINGS = 0x80000;
    public static final int CONFIG_DEVICE_ACTIVATED = CONFIG_SAVE_SERVER_SETTINGS + 1;

}
