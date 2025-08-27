package com.growingio.android.sdk.heatmap;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.growingio.android.sdk.models.HeatMapData;
import com.growingio.android.sdk.models.ViewNode;

/**
 * Created by 郑童宇 on 2016/11/29.
 * HeatMapNode代表绘制热力图时的热力图元素节点
 */

public class HeatMapNode {
    public int idx;
    public int clickCount;
    public double clickPercent;
    public View targetView;
    public ImageView heatMapNodeView;

    public HeatMapNode(ViewNode viewNode, HeatMapData.ItemBean item) {
        idx = item.getIdx();
        clickCount = item.getCnt();
        clickPercent = item.getPercent();
        targetView = viewNode.mView;
    }

    public void reset() {
    }

    public void initHeatMapNodeView(Context context, Bitmap bitmap) {
        heatMapNodeView = new ImageView(context);
        heatMapNodeView.setImageBitmap(bitmap);
        heatMapNodeView.setLayoutParams(new FrameLayout.LayoutParams(targetView.getWidth(), targetView.getHeight()));
        heatMapNodeView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public void updatePosition(int[] heatMapViewScreenLocation) {
        if (heatMapNodeView == null) {
            return;
        }

        int targetViewWidth = targetView.getWidth();
        int targetViewHeight = targetView.getHeight();
        int[] screenLocation = new int[2];
        targetView.getLocationOnScreen(screenLocation);

        FrameLayout.LayoutParams sourceLayoutParams = (FrameLayout.LayoutParams) heatMapNodeView.getLayoutParams();

        if (sourceLayoutParams != null && sourceLayoutParams.width == targetViewWidth && sourceLayoutParams.height == targetViewHeight &&
                sourceLayoutParams.leftMargin == screenLocation[0] && sourceLayoutParams.topMargin == screenLocation[1]) {
            return;
        }

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(targetViewWidth, targetViewHeight);
        layoutParams.leftMargin = screenLocation[0] - heatMapViewScreenLocation[0];
        layoutParams.topMargin = screenLocation[1] - heatMapViewScreenLocation[1];
        heatMapNodeView.setLayoutParams(layoutParams);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean canDraw() {
        if (targetView.getVisibility() != View.VISIBLE || targetView.getAlpha() == 0) {
            return false;
        }

        View viewParent = targetView.getRootView();
        return viewParent != null && viewParent.getParent() != null;
    }
}