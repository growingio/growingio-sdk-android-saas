package com.growingio.android.sdk.heatmap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.models.HeatMapData;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.view.FloatViewContainer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by 郑童宇 on 2016/11/19.
 * 此版本统一以Index优先,不考虑v的情况
 */

public class HeatMapView extends FloatViewContainer {
    private int maxClickCount;

    private int[] paletteIntegerArray;

    private final int CLICK_OFFSET_AREA = 100;
    private final int GRADIENT_BITMAP_SIZE = 150;
    private final int GRADIENT_BITMAP_CENTER = GRADIENT_BITMAP_SIZE / 2;
    private final int GRADIENT_BITMAP_RADIUS = GRADIENT_BITMAP_SIZE / 2;
    private final int PALETTE_PIXEL_LENGTH = 256;
    private final int HEAT_MAP_NODE_IMAGE_MAX_ALPHA = 200;
    private final int DRAW_DURATION = 15;

    private Bitmap gradientBitmap;

    private HeatMapData[] heatMapDataArray;

    private ArrayList<HeatMapNode> heatMapNodeList = new ArrayList<HeatMapNode>();
    private ArrayList<HeatMapNode> drawHeatMapNodeList = new ArrayList<HeatMapNode>();
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Bitmap> heatMapNodeImageMap = new HashMap<Integer, Bitmap>();
    private ArrayList<View> drawHeatMapNodeViewList = new ArrayList<View>();

    private Context context;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public HeatMapView(Context context) {
        super(context);

        init(context);
    }

    private void init(Context context) {
        this.context = context;

        setBackgroundColor(0x33000000);

        new Thread(createBitmapRunnable).start();

        ThreadUtils.postOnUiThreadDelayed(refreshHeatMapNodeRunnable, DRAW_DURATION);
    }

    private void refreshHeatMap() {
        drawHeatMapNodeList.clear();
        drawHeatMapNodeList.addAll(heatMapNodeList);

        int drawHeatMapNodeListLength = drawHeatMapNodeList.size();
        HeatMapNode heatMapNode;
        Bitmap bitmap;

        Object object = new Object();
        int[] heatMapViewScreenLocation = new int[2];
        getLocationOnScreen(heatMapViewScreenLocation);

        for (int i = 0; i < drawHeatMapNodeListLength; i++) {
            heatMapNode = drawHeatMapNodeList.get(i);

            if (heatMapNode.heatMapNodeView == null) {
                bitmap = getHeatNodeBitmap(heatMapNode);

                if (bitmap != null) {
                    heatMapNode.initHeatMapNodeView(context, bitmap);
                    addHeatMapNodeView(heatMapNode.heatMapNodeView);
                }
            }

            if (heatMapNode.heatMapNodeView.getParent() == null) {
                addHeatMapNodeView(heatMapNode.heatMapNodeView);
            }

            if (heatMapNode.heatMapNodeView != null && heatMapNode.canDraw()) {
                heatMapNode.updatePosition(heatMapViewScreenLocation);
                heatMapNode.heatMapNodeView.setTag(GrowingIO.GROWING_HEAT_MAP_KEY, object);
            }
        }

        int drawHeatMapNodeViewListLength = drawHeatMapNodeViewList.size();

        View view;

        for (int i = 0; i < drawHeatMapNodeViewListLength; i++) {
            view = drawHeatMapNodeViewList.get(i);

            if (view.getTag(GrowingIO.GROWING_HEAT_MAP_KEY) != object) {
                removeHeatMapNodeView(view);
                i--;
                drawHeatMapNodeViewListLength--;
            }
        }
    }

    private void addHeatMapNodeView(View view) {
        drawHeatMapNodeViewList.add(view);
        addView(view);
    }

    private void removeHeatMapNodeView(View view) {
        drawHeatMapNodeViewList.remove(view);
        removeView(view);
    }

    public void updateHeatMapNode(ArrayList<HeatMapNode> heatMapNodeList) {
        this.heatMapNodeList.clear();
        this.heatMapNodeList.addAll(heatMapNodeList);
    }

    public void show() {
        setVisibility(VISIBLE);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void clearData() {
        heatMapDataArray = new HeatMapData[0];
        heatMapNodeList.clear();
    }

    public void updateData(HeatMapData[] heatMapDataArray) {
        this.heatMapDataArray = heatMapDataArray;
        updateClickData();

        if (heatMapDataArray.length == 0) {
            Toast.makeText(CoreInitialize.coreAppState().getForegroundActivity(), "当前页面尚无热图数据", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateClickData() {
        maxClickCount = 0;

        int heatMapDataArrayLength = heatMapDataArray.length;

        for (int i = 0; i < heatMapDataArrayLength; i++) {
            HeatMapData.ItemBean[] items = heatMapDataArray[i].getItems();
            HeatMapData.ItemBean item;

            int itemsLength = items.length;

            for (int j = 0; j < itemsLength; j++) {
                item = items[j];
                int itemCount = item.getCnt();

                if (itemCount > maxClickCount) {
                    maxClickCount = itemCount;
                }
            }
        }
    }

    private int getClickOffset(int clickCount) {
        if (maxClickCount == 0) {
            return CLICK_OFFSET_AREA / 2;
        }

        int clickOffset = clickCount * CLICK_OFFSET_AREA / maxClickCount;
        return clickOffset;
    }

    private Bitmap getHeatNodeBitmap(HeatMapNode heatMapNode) {
        return heatMapNodeImageMap.get(getClickOffset(heatMapNode.clickCount));
    }

    private void createPalette() {
        Bitmap bitmap = Bitmap.createBitmap(PALETTE_PIXEL_LENGTH, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        LinearGradient radialGradient =
                new LinearGradient(0, 0, PALETTE_PIXEL_LENGTH, 1, new int[]{Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED}, new float[]{0.25f, 0.55f, 0.85f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(radialGradient);
        canvas.drawLine(0, 0, PALETTE_PIXEL_LENGTH, 1, paint);

        paletteIntegerArray = new int[PALETTE_PIXEL_LENGTH * 1];

        bitmap.getPixels(paletteIntegerArray, 0, PALETTE_PIXEL_LENGTH, 0, 0, PALETTE_PIXEL_LENGTH, 1);
    }

    private void createGradientBitmap() {
        RadialGradient radialGradient =
                new RadialGradient(GRADIENT_BITMAP_CENTER, GRADIENT_BITMAP_CENTER, GRADIENT_BITMAP_RADIUS, new int[]{0xFF000000, 0xFF000000, 0x00000000}, new float[]{0f, 0.15f, 1f},
                        Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(radialGradient);
        RectF heatNodeRectF = new RectF(0, 0, GRADIENT_BITMAP_SIZE, GRADIENT_BITMAP_SIZE);
        gradientBitmap = Bitmap.createBitmap(GRADIENT_BITMAP_SIZE, GRADIENT_BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(gradientBitmap);
        canvas.drawOval(heatNodeRectF, paint);
    }

    private void generateHeatMapNodeImage(int clickOffset) {
        float templateAlpha = (float) clickOffset / CLICK_OFFSET_AREA;
        templateAlpha = 0.4f + (templateAlpha * 0.6f);

        if (!heatMapNodeImageMap.containsKey(clickOffset)) {
            heatMapNodeImageMap.put(clickOffset, palette(Bitmap.createBitmap(gradientBitmap), templateAlpha));
        }
    }

    private Bitmap palette(Bitmap bitmap, float templateAlpha) {
        int pixel;
        int alpha;
        int resultAlpha;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bitmapPixelTotalLength = width * height;
        int[] pixels = new int[bitmapPixelTotalLength];

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < bitmapPixelTotalLength; i++) {
            pixel = pixels[i];
            alpha = (int) ((pixel >> 24 & 0x000000FF) * templateAlpha);

            if (alpha > HEAT_MAP_NODE_IMAGE_MAX_ALPHA) {
                resultAlpha = HEAT_MAP_NODE_IMAGE_MAX_ALPHA;
            } else {
                resultAlpha = alpha;
            }

            pixels[i] = (paletteIntegerArray[alpha] & 0x00FFFFFF) | (resultAlpha << 24);
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private Runnable createBitmapRunnable = new Runnable() {
        @Override
        public void run() {
            createPalette();
            createGradientBitmap();

            for (int i = 0; i <= CLICK_OFFSET_AREA; i++) {
                generateHeatMapNodeImage(i);
            }
        }
    };

    private Runnable refreshHeatMapNodeRunnable = new Runnable() {
        @Override
        public void run() {
            refreshHeatMap();
            ThreadUtils.postOnUiThreadDelayed(refreshHeatMapNodeRunnable, DRAW_DURATION);
        }
    };
}
