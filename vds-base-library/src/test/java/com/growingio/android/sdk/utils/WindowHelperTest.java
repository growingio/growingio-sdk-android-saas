package com.growingio.android.sdk.utils;

import android.view.View;

import com.growingio.android.sdk.utils.WindowHelper;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by lishaojie on 16/7/31.
 */

public class WindowHelperTest {
    @Test
    public void stripNullValueTest() throws Exception {
        View[] result = new View[3];
        result[0] = new View(null);
        result[1] = null;
        result[2] = new View(null);
        result = WindowHelper.filterNullAndDismissToastView(result);
        assertEquals(result.length, 2);
        for (View view : result) {
            assertNotNull(view);
        }
        result = WindowHelper.filterNullAndDismissToastView(result);
        assertEquals(result.length, 2);
    }

    @Test
    public void onToastShow(){
        View[] result = new View[3];
        result[0] = new View(null);
        result[1] = new View(null);
        result[2] = new View(null);
        WindowHelper.showingToast.put(result[0], System.currentTimeMillis() - 2000);
        WindowHelper.showingToast.put(result[1], System.currentTimeMillis() + 200000);
        View[] returnViews = WindowHelper.filterNullAndDismissToastView(result);
        assertEquals(2, returnViews.length);
    }
}
