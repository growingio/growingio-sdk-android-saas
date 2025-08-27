package com.growingio.android.sdk.deeplink;

import android.app.Application;
import android.os.Build;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class DeeplinkManagerTest2 {

    @Test
    public void testIsDeeplinkUrl(){
        DeeplinkManager manager = new DeeplinkManager();
        Assert.assertFalse(manager.isDeepLinkUrl(null, null));
        Assert.assertFalse(manager.isDeepLinkUrl("", null));
        Assert.assertTrue(manager.isDeepLinkUrl("http://gio.ren", null));
        Assert.assertTrue(manager.isDeepLinkUrl("https://test.datayi.cn",null));
        Assert.assertTrue(manager.isDeepLinkUrl("https://datayi.cn", null));
        Assert.assertFalse(manager.isDeepLinkUrl("test://datayi.cn", null));
    }


    @Test
    public void parseTrackId(){
        DeeplinkManager manager = new DeeplinkManager();
        Assert.assertEquals("test", manager.parseTrackerId("http://gio.ren/test"));
        Assert.assertEquals("test", manager.parseTrackerId("https://gio.ren/test"));
        Assert.assertEquals("test/test", manager.parseTrackerId("https://datayi.cn/test/test"));
    }
}
