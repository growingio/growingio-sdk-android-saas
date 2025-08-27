package com.growingio.android.sdk.circle.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by lishaojie on 16/3/15.
 */
public class TagMask extends View {
    public TagMask(Context context) {
        super(context);
    }

    public void updatePosition(Rect rect) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.leftMargin = rect.left;
        params.topMargin = rect.top;
        params.width = rect.width();
        params.height = rect.height();
        setLayoutParams(params);
    }

}
