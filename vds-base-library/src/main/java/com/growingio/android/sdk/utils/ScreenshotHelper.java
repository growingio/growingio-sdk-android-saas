package com.growingio.android.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.view.FloatViewContainer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyz on 15/9/11.
 */
public class ScreenshotHelper {

    private static String TAG = "GIO.ScreenshotHelper";
    private static boolean hasInitial = false;

    public static void initial() {
        if (hasInitial) return;
        CoreAppState state = CoreInitialize.coreAppState();
        if (state == null) return;
        Context context = state.getGlobalContext();
        if (context == null) return;
        DisplayMetrics displayMetrics = NonUiContextUtil.getDisplayMetrics(context);

        if (displayMetrics.widthPixels < displayMetrics.heightPixels){
            sScreenShort = displayMetrics.widthPixels;
            sScreenLong = displayMetrics.heightPixels;
        }else{
            sScreenLong = displayMetrics.widthPixels;
            sScreenShort = displayMetrics.heightPixels;
        }

        sScaledFactor = 720d / sScreenShort;
        sScaledShort = 720;
        sScaledLong = (int) (sScaledFactor * (sScreenLong));
        hasInitial = true;

        TextPaint paint = new TextPaint();
        int padding = (int) (displayMetrics.density * 2);
        Rect bounds = new Rect();
        String str = "截图失败";
        paint.setAntiAlias(true);
        paint.setColor(0xff333333);
        paint.setTextSize(displayMetrics.density * 14);
        paint.getTextBounds(str, 0, str.length(), bounds);
        sErrorBitmap = Bitmap.createBitmap(bounds.width() + padding * 2, bounds.height() + padding * 2, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(sErrorBitmap);
        canvas.drawText(str, padding, bounds.height(), paint);
    }

    private static int sScreenShort = -1;
    private static int sScaledShort = -1;
    private static int sScreenLong = -1;
    private static int sScaledLong = -1;
    private static double sScaledFactor = -1;

    private static Bitmap sErrorBitmap;

    private static byte[] compressViewsCapture(View[] views, RectF elemRect) {
        byte[] result = null;
        try {
            Bitmap fullScreenBitmap = mergeViewLayers(views);
            if (elemRect != null) {
                Canvas canvas = new Canvas(fullScreenBitmap);
                Context app = CoreInitialize.coreAppState().getGlobalContext();
                Paint elemPaint = new Paint();
                float radius = Util.dp2Px(app, 3);
                elemPaint.setColor(Constants.GROWINGIO_COLOR_LIGHT_RED);
                canvas.drawRoundRect(elemRect, radius, radius, elemPaint);
                elemPaint.setStyle(Paint.Style.STROKE);
                elemPaint.setStrokeWidth(Util.dp2Px(app, 1));
                elemPaint.setColor(Constants.GROWINGIO_COLOR_RED);
                canvas.drawRoundRect(elemRect, radius, radius, elemPaint);
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            fullScreenBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            fullScreenBitmap.recycle();
            result = stream.toByteArray();
            stream.close();
        } catch (Exception ignored) {
            if (sErrorBitmap != null) {
                ByteArrayOutputStream fallbackStream = new ByteArrayOutputStream();
                sErrorBitmap.compress(Bitmap.CompressFormat.JPEG, 50, fallbackStream);
                return fallbackStream.toByteArray();
            }
        }
        return result;
    }

    static Bitmap mergeViewLayers(View[] views) {
        CoreAppState coreAppState = CoreInitialize.coreAppState();
        Activity activity = coreAppState.getForegroundActivity();
        boolean isPortrait = activity == null || activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;

        int widthPixels = isPortrait ? getScaledShort() : getScaledLong();
        int heightPixels = isPortrait ? getScaledLong() : getScreenShort();
        WindowHelper.init();
        invalidateLayerTypeView(views);

        List<View> viewList = filterInvalidViewInternal(views);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            try {
                Window window = activity.getWindow();
                int[] location = new int[2];
                LogUtil.d(TAG, "getScreenshotBitmap: " + getScreenShort() + " " + getScreenLong());
                window.getDecorView().getLocationOnScreen(location);
                Bitmap bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
                SynchronousPixelCopy synchronousPixelCopy = new SynchronousPixelCopy();
                int copyResult = synchronousPixelCopy.request(window, bitmap);
                if (copyResult == PixelCopy.SUCCESS) {
                    LogUtil.d(TAG, "getScreenshotBitmap by PixelCopy");
                    Bitmap screenshot = tryRenderDialog(viewList, bitmap);
                    return screenshot;
                }
            } catch (Exception ignored) {
                LogUtil.d(TAG, "getScreenshotBitmap by PixelCopy failed");
            }
        }

        Bitmap fullScreenBitmap = Bitmap.createBitmap(
                widthPixels,
                heightPixels, Bitmap.Config.RGB_565);

        int[] windowOffset = new int[2];
        SoftWareCanvas canvas = new SoftWareCanvas(fullScreenBitmap);
        canvas.scale((float) getScaledFactor(), (float) getScaledFactor());
        for (View view : viewList) {
            view.getLocationOnScreen(windowOffset);
            canvas.save();
            canvas.translate(windowOffset[0], windowOffset[1]);
            view.draw(canvas);
            canvas.restore();
            canvas.destroy();
        }
        return fullScreenBitmap;
    }

    private static Bitmap tryRenderDialog(List<View> windowViews, Bitmap original) {
        List<DecorView> views = filterBaseApplicationWindowInternal(windowViews);
        if (views.isEmpty()) return original;
        try {
            Canvas canvas = new Canvas(original);
            canvas.scale((float) getScaledFactor(), (float) getScaledFactor());
            for (DecorView view : views) {
                if (!view.isDialog()) {
                    drawPanel(canvas, view);
                }
            }

            for (DecorView dialog : views) {
                if (dialog.isDialog()) {
                    int dimColorAlpha = (int) (dialog.getLayoutParams().dimAmount * 255);
                    canvas.drawColor(Color.argb(dimColorAlpha, 0, 0, 0));
                    drawPanel(canvas, dialog);
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "tryRenderDialog failed", e);
        }
        return original;
    }

    private static List<View> filterInvalidViewInternal(View[] views) {
        List<View> viewList = new ArrayList<>();
        boolean skipOther = ViewHelper.getMainWindowCount(views) > 1;

        for (View view: views) {
            if (view instanceof FloatViewContainer
                    || view.getVisibility() != View.VISIBLE
                    || view.getWidth() == 0
                    || view.getHeight() == 0
                    || !ViewHelper.isWindowNeedTraverse(view, WindowHelper.getWindowPrefix(view), skipOther)
                    || "DO_NOT_DRAW".equals(view.getTag())) {
                continue;
            }
            viewList.add(view);
        }

        return viewList;
    }

    private static List<DecorView> filterBaseApplicationWindowInternal(List<View> views) {
        List<DecorView> viewList = new ArrayList<>();
        for (View view : views) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params instanceof WindowManager.LayoutParams) {
                WindowManager.LayoutParams windowParams = (WindowManager.LayoutParams) params;
                if (windowParams.type != WindowManager.LayoutParams.TYPE_BASE_APPLICATION) {
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    int x = location[0];
                    int y = location[1];
                    Rect area = new Rect(x, y, x + view.getWidth(), y + view.getHeight());

                    viewList.add(new DecorView(view, area, windowParams));
                }
            }
        }
        return viewList;
    }

    private static void drawPanel(Canvas canvas, DecorView info) {
        View panelView = info.getView();
        if (panelView.getWidth() == 0 || panelView.getHeight() == 0) {
            return;
        }
        canvas.save();
        canvas.translate(info.getRect().left * 1.0f, info.getRect().top * 1.0f);
        panelView.draw(canvas);
        canvas.restore();
    }

    /**
     * 硬件加速开启的情况下, Android使用不同的绘制模型
     * maybe: 通用情况下应该记录View， 而不是遍历View
     */
    private static void invalidateLayerTypeView(View[] views){
        for (View view : views){
            if (ViewHelper.viewVisibilityInParents(view) && view.isHardwareAccelerated()){
                checkAndInvalidate(view);
                if (view instanceof ViewGroup){
                    invalidateViewGroup((ViewGroup) view);
                }
            }
        }
    }

    private static void checkAndInvalidate(View view){
        if (view.getLayerType() != View.LAYER_TYPE_NONE){
            view.invalidate();
        }
    }

    private static void invalidateViewGroup(ViewGroup viewGroup){
        for (int index = 0; index < viewGroup.getChildCount(); index++){
            View child = viewGroup.getChildAt(index);
            if (ViewHelper.isViewSelfVisible(child)){
                checkAndInvalidate(child);
                if (child instanceof ViewGroup){
                    invalidateViewGroup((ViewGroup) child);
                }
            }
        }
    }

    public static byte[] captureAllWindows(View[] rootViews, RectF elemRect) {
        byte[] result = compressViewsCapture(rootViews, elemRect);
        if (result == null) {
            result = new byte[0];
        }
        return result;
    }

    public static int getScreenShort() {
        return sScreenShort;
    }

    public static int getScaledShort() {
        return sScaledShort;
    }

    public static int getScreenLong() {
        return sScreenLong;
    }

    public  static int getScaledLong() {
        return sScaledLong;
    }

    public static double getScaledFactor() {
        return sScaledFactor;
    }

    private static final class DecorView {
        private final View mView;
        private final Rect mRect;
        private final WindowManager.LayoutParams mLayoutParams;

        public DecorView(View view, Rect rect, WindowManager.LayoutParams layoutParams) {
            mView = view;
            mRect = rect;
            mLayoutParams = layoutParams;
        }

        public View getView() {
            return mView;
        }

        public Rect getRect() {
            return mRect;
        }

        public WindowManager.LayoutParams getLayoutParams() {
            return mLayoutParams;
        }

        public boolean isDialog() {
            return mLayoutParams.type == WindowManager.LayoutParams.TYPE_APPLICATION;
        }
    }
}
