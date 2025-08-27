package com.growingio.android.sdk.deeplink;

/**
 * <p>
 *
 * @author cpacm 2021/1/4
 */
public class DeepLinkEvent {

    private int type = 0;

    public DeepLinkEvent(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static final int DEEPLINK_ACTIVATE = 1;
}
