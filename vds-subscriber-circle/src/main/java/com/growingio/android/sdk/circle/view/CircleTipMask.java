package com.growingio.android.sdk.circle.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.Gravity;
import android.widget.ImageView;

import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.view.FloatViewContainer;

/**
 * Created by lishaojie on 16/2/17.
 */
public class CircleTipMask extends FloatViewContainer {

    private int arrowWidth;
    private int arrowHeight;

    private ImageView arrowImageView;
    private int[] location = new int[2];

    public CircleTipMask(Context context) {
        super(context);
        init();
    }

    private void init() {
        setBackgroundColor(0xbf000000);
        arrowImageView = new android.support.v7.widget.AppCompatImageView(getContext()) {
            Paint bgPaint = new Paint();
            {
                bgPaint.setColor(Color.WHITE);
                bgPaint.setStyle(Paint.Style.FILL);
            }
            @SuppressLint("MissingSuperCall")
            @Override
            public void draw(Canvas canvas) {
                Path arrow = new Path();
                arrow.moveTo(getWidth() * 0.4f, 0);
                arrow.lineTo(getWidth() * 0.6f, 0);
                arrow.lineTo(getWidth() * 0.6f, getHeight() * 0.7f);
                arrow.lineTo(getWidth(), getHeight() * 0.7f);
                arrow.lineTo(getWidth() * 0.5f, getHeight());
                arrow.lineTo(0, getHeight() * 0.7f);
                arrow.lineTo(getWidth() * 0.4f, getHeight() * 0.7f);
                arrow.close();
                canvas.drawPath(arrow, bgPaint);
            }
        };

        arrowWidth = Util.dp2Px(getContext(), 42);
        arrowHeight = Util.dp2Px(getContext(), 80);
        LayoutParams arrowParam = new LayoutParams(arrowWidth, arrowHeight);
        arrowParam.gravity = Gravity.LEFT | Gravity.TOP;
        addView(arrowImageView, arrowParam);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        getLocationOnScreen(location);
        boolean portrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        int arrowLeft = ((portrait ? ScreenshotHelper.getScreenShort() : ScreenshotHelper.getScreenLong()) - arrowWidth) / 2 - location[0];
        int arrowTop = ((portrait ? ScreenshotHelper.getScreenLong() : ScreenshotHelper.getScreenShort()) - CircleAnchorView.ANCHOR_VIEW_SIZE)/2 - arrowHeight - location[1];
        arrowImageView.layout(arrowLeft, arrowTop, arrowLeft + arrowWidth, arrowTop + arrowHeight);
    }
}
