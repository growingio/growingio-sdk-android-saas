package com.growingio.android.sdk.view;

import android.content.Context;
import android.widget.FrameLayout;

import com.growingio.android.sdk.utils.NonUiContextUtil;

/**
 * Created by creative lishaojie on 2015/12/23 0023.
 */
public class FloatViewContainer extends FrameLayout {
    public FloatViewContainer(Context context) {
        super(NonUiContextUtil.getWindowContext(context));
    }
}
