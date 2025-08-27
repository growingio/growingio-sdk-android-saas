package com.growingio.android.sdk.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * classDesc:
 * 1. 8.0 以上机型在 Bitmap.Config == HARDWARE 时截图失败，重写替换对应配置
 * 2. 处理 paint 的 Shader 中如果是 BitmapShader ，并且 bitmap.getConfig() == Config.HARDWARE
 */
public class SoftWareCanvas extends Canvas {
    
    private static final String TAG = "GIO.SoftWareCanvas";
    
    private WeakSet<Bitmap> bitmapWeakSet = new WeakSet<>();
    private Bitmap mBitmap;
    
    public SoftWareCanvas(@NonNull Bitmap bitmap) {
        super(bitmap);
        mBitmap = bitmap;
    }
    
    private Bitmap drawOnSFCanvas(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Config.HARDWARE) {
            Bitmap sfBitmap = bitmap.copy(Config.ARGB_8888, false);
            bitmapWeakSet.add(sfBitmap);
            return sfBitmap;
        }
        return bitmap;
    }
    
    /**
     * 将画笔中如果含有 BitmapShader 的 bitmap.getConfig() == Config.HARDWARE 替换
     *
     * @param paint 画笔呀
     * @return
     */
    private Paint replaceBitmapShader(Paint paint) {
        if (paint == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && paint.getShader() instanceof BitmapShader) {
            Paint gioPaint = new Paint(paint);
            BitmapShader userBitmapShader = (BitmapShader) gioPaint.getShader();
            try {
                Field mBitmap = BitmapShader.class.getField("mBitmap");
                mBitmap.setAccessible(true);
                if (((Bitmap) mBitmap.get(userBitmapShader)).getConfig() == Config.HARDWARE) {
                    Field mTileX = BitmapShader.class.getDeclaredField("mTileX");
                    Field mTileY = BitmapShader.class.getDeclaredField("mTileY");
                    mTileX.setAccessible(true);
                    mTileY.setAccessible(true);
                    Bitmap sfBitmap = ((Bitmap) mBitmap.get(userBitmapShader)).copy(Config.ARGB_8888, false);
                    bitmapWeakSet.add(sfBitmap);
                    // 获取用户 TileMode
                    Class<BitmapShader> mClass = BitmapShader.class;
                    Constructor<BitmapShader> constructor = mClass.getDeclaredConstructor(Bitmap.class, int.class, int.class);
                    constructor.setAccessible(true);
                    BitmapShader gioBitmapShader = constructor.newInstance(sfBitmap, mTileX.get(userBitmapShader), mTileY.get(userBitmapShader));
                    // 获取用户 BitmapShader 的 Matrix
                    Matrix matrix = new Matrix();
                    paint.getShader().getLocalMatrix(matrix);
                    gioBitmapShader.setLocalMatrix(matrix);
                    gioPaint.setShader(gioBitmapShader);
                    return gioPaint;
                }
            } catch (Exception e) {
                LogUtil.e(TAG, e.toString());
            }
        }
        return paint;
    }
    
    public void destroy() {
        for (Bitmap bitmap : bitmapWeakSet) {
            bitmap.recycle();
        }
        bitmapWeakSet.clear();
    }
    
    @Override
    public void drawLines(@NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
        super.drawLines(pts, offset, count, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmap(@NonNull Bitmap bitmap, float left, float top, @Nullable Paint paint) {
        Bitmap drawBitmap = drawOnSFCanvas(bitmap);
        if (drawBitmap.getDensity() != mBitmap.getDensity()){
            int leftInt = (int) left;
            int topInt = (int) top;
            Rect rect = new Rect(leftInt, topInt, leftInt + drawBitmap.getWidth(), topInt + drawBitmap.getHeight());
            super.drawBitmap(drawBitmap, rect, rect,replaceBitmapShader(paint));
        }else{
            super.drawBitmap(drawBitmap, left, top, replaceBitmapShader(paint));
        }
    }
    
    @Override
    public void drawBitmap(@NonNull Bitmap bitmap, @Nullable Rect src, @NonNull RectF dst, @Nullable Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), src, dst, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmap(@NonNull Bitmap bitmap, @Nullable Rect src, @NonNull Rect dst, @Nullable Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), src, dst, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmap(@NonNull int[] colors, int offset, int stride, float x, float y, int width, int height, boolean hasAlpha, @Nullable Paint paint) {
        super.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmap(@NonNull int[] colors, int offset, int stride, int x, int y, int width, int height, boolean hasAlpha, @Nullable Paint paint) {
        super.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmap(@NonNull Bitmap bitmap, @NonNull Matrix matrix, @Nullable Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), matrix, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawBitmapMesh(@NonNull Bitmap bitmap, int meshWidth, int meshHeight, @NonNull float[] verts, int vertOffset, @Nullable int[] colors, int colorOffset, @Nullable Paint paint) {
        super.drawBitmapMesh(drawOnSFCanvas(bitmap), meshWidth, meshHeight, verts, vertOffset, colors, colorOffset, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawRoundRect(@NonNull RectF rect, float rx, float ry, @NonNull Paint paint) {
        super.drawRoundRect(rect, rx, ry, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, @NonNull Paint paint) {
        super.drawRoundRect(left, top, right, bottom, rx, ry, replaceBitmapShader(paint));
    }
    
    @Override
    public void setBitmap(@Nullable Bitmap bitmap) {
        super.setBitmap(drawOnSFCanvas(bitmap));
    }

    @Override
    public int saveLayer(@Nullable RectF bounds, @Nullable Paint paint, int saveFlags) {
        return super.saveLayer(bounds, replaceBitmapShader(paint), saveFlags);
    }

    @Override
    public int saveLayer(@Nullable RectF bounds, @Nullable Paint paint) {
        return super.saveLayer(bounds, replaceBitmapShader(paint));
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, @Nullable Paint paint, int saveFlags) {
        return super.saveLayer(left, top, right, bottom, replaceBitmapShader(paint), saveFlags);
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, @Nullable Paint paint) {
        return super.saveLayer(left, top, right, bottom, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawArc(@NonNull RectF oval, float startAngle, float sweepAngle, boolean useCenter, @NonNull Paint paint) {
        super.drawArc(oval, startAngle, sweepAngle, useCenter, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawCircle(float cx, float cy, float radius, @NonNull Paint paint) {
        super.drawCircle(cx, cy, radius, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, @NonNull Paint paint) {
        super.drawLine(startX, startY, stopX, stopY, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawLines(@NonNull float[] pts, @NonNull Paint paint) {
        super.drawLines(pts, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawOval(@NonNull RectF oval, @NonNull Paint paint) {
        super.drawOval(oval, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawOval(float left, float top, float right, float bottom, @NonNull Paint paint) {
        super.drawOval(left, top, right, bottom, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPaint(@NonNull Paint paint) {
        super.drawPaint(replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPath(@NonNull Path path, @NonNull Paint paint) {
        super.drawPath(path, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPoint(float x, float y, @NonNull Paint paint) {
        super.drawPoint(x, y, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPoints(float[] pts, int offset, int count, @NonNull Paint paint) {
        super.drawPoints(pts, offset, count, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPoints(@NonNull float[] pts, @NonNull Paint paint) {
        super.drawPoints(pts, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPosText(@NonNull char[] text, int index, int count, @NonNull float[] pos, @NonNull Paint paint) {
        super.drawPosText(text, index, count, pos, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawPosText(@NonNull String text, @NonNull float[] pos, @NonNull Paint paint) {
        super.drawPosText(text, pos, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawRect(@NonNull RectF rect, @NonNull Paint paint) {
        super.drawRect(rect, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawRect(@NonNull Rect r, @NonNull Paint paint) {
        super.drawRect(r, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint) {
        super.drawRect(left, top, right, bottom, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawText(@NonNull char[] text, int index, int count, float x, float y, @NonNull Paint paint) {
        super.drawText(text, index, count, x, y, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawText(@NonNull String text, float x, float y, @NonNull Paint paint) {
        super.drawText(text, x, y, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawText(@NonNull String text, int start, int end, float x, float y, @NonNull Paint paint) {
        super.drawText(text, start, end, x, y, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawText(@NonNull CharSequence text, int start, int end, float x, float y, @NonNull Paint paint) {
        super.drawText(text, start, end, x, y, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawTextOnPath(@NonNull char[] text, int index, int count, @NonNull Path path, float hOffset, float vOffset, @NonNull Paint paint) {
        super.drawTextOnPath(text, index, count, path, hOffset, vOffset, replaceBitmapShader(paint));
    }
    
    @Override
    public void drawTextOnPath(@NonNull String text, @NonNull Path path, float hOffset, float vOffset, @NonNull Paint paint) {
        super.drawTextOnPath(text, path, hOffset, vOffset, replaceBitmapShader(paint));
    }
}
