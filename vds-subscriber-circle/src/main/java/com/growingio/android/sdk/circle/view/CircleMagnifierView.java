package com.growingio.android.sdk.circle.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import com.growingio.android.sdk.circle.HybridEventEditDialog;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.FloatWindowManager;
import com.growingio.android.sdk.utils.SoftWareCanvas;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.android.sdk.view.FloatViewContainer;

public class CircleMagnifierView extends View {

    private Rect mMaskRect;
    private Bitmap mBitmap;
    private int mRectWidth;
    private int mRectHeight;
    private int mArrowWidthMax;
    private int mArrowWidthMin;
    private float mCurrentX, mCurrentY;
    private int mMaxHitWidth;
    private final float SCALE_FACTOR = 1.17f;
    private RelativeLocation mRelativeLocation = RelativeLocation.TOP;

    enum RelativeLocation {
        TOP, LEFT, RIGHT
    }

    public CircleMagnifierView(Context context) {
        super(context);
        init();
    }

    public void init() {
        mMaxHitWidth = Util.dp2Px(getContext(), 80);
        mRectWidth = Util.dp2Px(getContext(), 120);
        mRectHeight = Util.dp2Px(getContext(), 80);
        mArrowWidthMin = Util.dp2Px(getContext(), 16);
        mArrowWidthMax = Util.dp2Px(getContext(), 18);
    }

    @SuppressLint("RtlHardcoded")
    public void attachToWindow() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                0, 0,
                PendingStatus.FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.setTitle("CircleMagnifierWindow:" + getContext().getPackageName());
        setVisibility(GONE);
        FloatWindowManager.getInstance().addView(this, params);
    }

    public void showIfNeed(Rect rect, int x, int y) {
        if (rect == null || rect.width() >= mMaxHitWidth || rect.height() >= mMaxHitWidth) {
            setVisibility(GONE);
            return;
        }

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        float delta = (mRectWidth / 2 - Util.dp2Px(getContext(), 2)) / SCALE_FACTOR;

        if (x > screenWidth - delta) {
            mCurrentX = screenWidth - delta;
        } else if (x < delta) {
            mCurrentX = delta;
        } else {
            mCurrentX = x;
        }
        mCurrentY = y < mRectHeight / 2 ? mRectHeight / 2 : y;
        mMaskRect = rect;
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) this.getLayoutParams();

        int extra = Util.dp2Px(getContext(), 1);
        // 计算显示的位置 优先级为:上>左>右
        if (rect.top - mRectHeight >= mArrowWidthMin + extra) {
            // 显示在上方
            params.width = mRectWidth;
            params.height = mRectHeight + mArrowWidthMin;
            params.x = rect.left + rect.width() / 2 - params.width / 2;
            params.y = rect.top - params.height - extra;
            mRelativeLocation = RelativeLocation.TOP;

        } else if (rect.left - getWidth() >= mArrowWidthMin + extra) {
            // 显示在左边
            params.width = mRectWidth + mArrowWidthMin;
            params.height = mRectHeight;
            params.x = rect.left - params.width - extra;
            params.y = rect.top + rect.height() / 2 - params.height / 2;
            mRelativeLocation = RelativeLocation.LEFT;

        } else {
            // 显示在右边
            params.width = mRectWidth + mArrowWidthMin;
            params.height = mRectHeight;
            params.x = rect.right + extra;
            params.y = rect.top + rect.height() / 2 - params.height / 2;
            mRelativeLocation = RelativeLocation.RIGHT;
        }

        FloatWindowManager.getInstance().updateViewLayout(this, params);
        setVisibility(VISIBLE);
        bringToFront();
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        View[] views = WindowHelper.getSortedWindowViews();
        if (views.length <= 0) return;

        float strokeWidth = getResources().getDisplayMetrics().density * 2;
        float roundRadius = getResources().getDisplayMetrics().density * 5;
        int halfRectWidth = mRectWidth / 2;
        int halfRectHeight = mRectHeight / 2;
        int leftOffset = 0;

        Path arrowPath = new Path();
        if (mRelativeLocation == RelativeLocation.TOP) {
            arrowPath.moveTo(halfRectWidth - mArrowWidthMax / 2, mRectHeight - strokeWidth);
            arrowPath.lineTo(halfRectWidth, mRectHeight + mArrowWidthMin - strokeWidth);
            arrowPath.lineTo(halfRectWidth + mArrowWidthMax / 2, mRectHeight - strokeWidth);
            arrowPath.close();
        } else if (mRelativeLocation == RelativeLocation.LEFT) {
            arrowPath.moveTo(mRectWidth - strokeWidth, halfRectHeight - mArrowWidthMax / 2);
            arrowPath.lineTo(mRectWidth + mArrowWidthMin - strokeWidth, halfRectHeight);
            arrowPath.lineTo(mRectWidth - strokeWidth, halfRectHeight + mArrowWidthMax / 2);
            arrowPath.close();
        } else if (mRelativeLocation == RelativeLocation.RIGHT) {
            leftOffset = mArrowWidthMin;
            arrowPath.moveTo(leftOffset + strokeWidth, halfRectHeight - mArrowWidthMax / 2);
            arrowPath.lineTo(strokeWidth, halfRectHeight);
            arrowPath.lineTo(leftOffset + strokeWidth, halfRectHeight + mArrowWidthMax / 2);
            arrowPath.close();
        }

        Path clipPath = new Path();
        clipPath.addRoundRect(new RectF(strokeWidth + leftOffset, strokeWidth, mRectWidth + leftOffset - strokeWidth, mRectHeight - strokeWidth),
                roundRadius, roundRadius, Path.Direction.CCW);

        Paint clipPaint = new Paint();
        clipPaint.setColor(0xFFF0F0F0);
        clipPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        clipPaint.setFilterBitmap(true);
        canvas.drawPath(clipPath, clipPaint);
        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        // 合并图层
        mergeViews(views, roundRadius);

        canvas.save();
        canvas.translate(leftOffset, 0);
        canvas.drawBitmap(mBitmap, 0, 0, clipPaint);
        canvas.restore();

        // 灰色边框
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(0xFFF0F0F0);
        canvas.drawPath(clipPath, paint);

        // 画箭头
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(0xFFF0F0F0);
        canvas.drawPath(arrowPath, paint);
    }

    private void mergeViews(View[] views, float roundRadius) {
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(mRectWidth, mRectHeight, Bitmap.Config.RGB_565);
        }
        SoftWareCanvas originalCanvas = new SoftWareCanvas(mBitmap);
        originalCanvas.scale(SCALE_FACTOR, SCALE_FACTOR);
        originalCanvas.translate(-mCurrentX + mRectWidth / (2 * SCALE_FACTOR), -mCurrentY + mRectHeight / (2 * SCALE_FACTOR));
        int[] windowOffset = new int[2];
        boolean skipOther = ViewHelper.getMainWindowCount(views) > 1;
        WindowHelper.init();
        for (View view : views) {
            if (view instanceof FloatViewContainer
                    || view.getVisibility() != View.VISIBLE
                    || view.getWidth() == 0
                    || view.getHeight() == 0
                    || !ViewHelper.isWindowNeedTraverse(view, WindowHelper.getWindowPrefix(view), skipOther)
                    || HybridEventEditDialog.DO_NOT_DRAW.equals(view.getTag())) {
                continue;
            }
            view.getLocationOnScreen(windowOffset);
            originalCanvas.save();
            originalCanvas.translate(windowOffset[0], windowOffset[1]);
            view.draw(originalCanvas);
            originalCanvas.restore();
        }

        // 画蒙层
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(Util.dp2Px(getContext(), 1));
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Constants.GROWINGIO_COLOR_RED);
        originalCanvas.drawRoundRect(new RectF(mMaskRect), roundRadius, roundRadius, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Constants.GROWINGIO_COLOR_LIGHT_RED);
        originalCanvas.drawRoundRect(new RectF(mMaskRect), roundRadius, roundRadius, paint);
        originalCanvas.destroy();
    }

    public void remove() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (getParent() != null) {
            FloatWindowManager.getInstance().removeView(this);
        }
    }

}
