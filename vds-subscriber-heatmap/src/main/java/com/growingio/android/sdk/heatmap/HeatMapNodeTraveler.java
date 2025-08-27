package com.growingio.android.sdk.heatmap;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.models.HeatMapData;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Creator: tongyuzheng
 * Create Time: 2016/11/30
 * Description: 在此类中遍历页面元素与检索是否存在与热图数据匹配的元素并缓存，目前匹配规则是对于普通元素使用Xpath，index进行匹配，
 * 对于特殊元素（Spinner和RadioGroup）的子元素使用Xpath，index，Value进行匹配
 */

public class HeatMapNodeTraveler extends ViewTraveler {
    private final String TAG = "GIO.HeatMapNodeTraveler";

    private boolean isImmediateTraverse = false;

    private final int AUTO_UPDATE_DURATION = 1000;
    private final int IMMEDIATE_UPDATE_DURATION = 50;

    private int heatMapDataArrayLength;
    private HeatMapView heatMapView;
    private HeatMapData[] heatMapDataArray;
    private ArrayList<HeatMapNode> heatMapNodeList;
    private HashMap<View, HeatMapNode> cacheHeatNodeMap;

    public HeatMapNodeTraveler(HeatMapView heatMapView) {
        this.heatMapView = heatMapView;
        heatMapNodeList = new ArrayList<HeatMapNode>();
        cacheHeatNodeMap = new HashMap<View, HeatMapNode>();
    }

    public void updateHeatMapDataArray(HeatMapData[] heatMapDataArray) {
        if (heatMapDataArray == null) {
            return;
        }

        this.heatMapDataArray = heatMapDataArray;
        heatMapDataArrayLength = heatMapDataArray.length;
        beginTraverseImmediately();
    }

    public void clear() {
        cacheHeatNodeMap.clear();
        heatMapDataArray = new HeatMapData[0];
        heatMapDataArrayLength = 0;
        stopTraverse();
    }

    public void stopTraverse() {
        isImmediateTraverse = false;
        ThreadUtils.cancelTaskOnUiThread(traverseRunnable);
    }

    public void beginTraverseImmediately() {
        if (isImmediateTraverse) {
            return;
        }

        isImmediateTraverse = true;
        ThreadUtils.cancelTaskOnUiThread(traverseRunnable);
        ThreadUtils.postOnUiThreadDelayed(traverseRunnable, IMMEDIATE_UPDATE_DURATION);
    }

    private void traverse() {
        heatMapNodeList.clear();
        ViewHelper.traverseWindows(WindowHelper.getWindowViews(), this);
        heatMapView.updateHeatMapNode(heatMapNodeList);
        isImmediateTraverse = false;
        ThreadUtils.postOnUiThreadDelayed(traverseRunnable, AUTO_UPDATE_DURATION);
    }

    @Override
    public void traverseCallBack(ViewNode viewNode) {
        matchNode(viewNode);
    }

    private void matchNode(ViewNode viewNode) {

        boolean isViewClickable = Util.isViewClickable(viewNode.mView);
        if (!isViewClickable) {
            return;
        }

        HeatMapNode cacheHeatMapNode = getCacheHeatMapNode(viewNode, null);

        if (cacheHeatMapNode != null) {
            addHeatMapNodeFromCache(cacheHeatMapNode);
            return;
        }

        boolean isSpecialView = isSpecialView(viewNode.mView);
        HeatMapData heatMapData = findBestMatch(viewNode);
        if (heatMapData != null){
            if (isSpecialView) {
                ViewNode specialChildViewNode = specialViewChildMatchNode(viewNode, heatMapData);
                if (specialChildViewNode != null) {
                    generateHeatMapNode(specialChildViewNode, heatMapData, true);
                }
            } else {
                generateHeatMapNode(viewNode, heatMapData, false);

            }
        }
    }

    @Nullable
    private HeatMapData findBestMatch(ViewNode viewNode){
        HeatMapData result = null;
        Boolean isValueMatch = null;
        for (int i = 0; i < heatMapDataArrayLength; i++){
            HeatMapData heatMapData = heatMapDataArray[i];
            if (matchXpath(viewNode, heatMapData)){
                if (result != null){
                    if (isValueMatch == null) {
                        isValueMatch = isValueMatch(viewNode, result);
                        if (isValueMatch)
                            // 目前只匹配到第一个value match的情况即可
                            break;
                    }
                    if (isValueMatch = isValueMatch(viewNode, heatMapData)){
                        result = heatMapData;
                        break;
                    }
                }else{
                    result = heatMapData;
                }
            }
        }
        return result;
    }

    private boolean generateHeatMapNode(ViewNode viewNode, HeatMapData heatMapData, boolean isSpecialView) {
        HeatMapData.ItemBean[] items = heatMapData.getItems();

        //        if (isInListView(viewNode)) {
        //            items = matchValue(viewNode, heatMapData);
        //        } else {
        //            items = heatMapData.getItems();
        //        }

        HeatMapData.ItemBean item = matchIndex(viewNode, items);

        if (item == null) {
            return false;
        }

        HeatMapNode cacheHeatMapNode = getCacheHeatMapNode(viewNode, item);

        if (cacheHeatMapNode == null) {
            addHeatMapNode(viewNode, item);
        } else {
            addHeatMapNodeFromCache(cacheHeatMapNode);
        }

        if (isSpecialView) {
            return false;
        }

        return true;
    }

    private HeatMapNode getCacheHeatMapNode(ViewNode viewNode, HeatMapData.ItemBean item) {
        boolean isInListView = isInListView(viewNode);

        HeatMapNode cacheHeatMapNode = null;

        if (item == null) {
            if (!isInListView) {
                cacheHeatMapNode = cacheHeatNodeMap.get(viewNode.mView);
            }
        } else {
            if (isInListView) {
                cacheHeatMapNode = cacheHeatNodeMap.get(viewNode.mView);

                if (cacheHeatMapNode != null && cacheHeatMapNode.idx != item.getIdx()) {
                    cacheHeatMapNode = null;
                }
            }
        }

        return cacheHeatMapNode;
    }

    private void addHeatMapNodeFromCache(HeatMapNode heatMapNode) {
        heatMapNode.reset();
        heatMapNodeList.add(heatMapNode);
    }

    private void addHeatMapNode(ViewNode viewNode, HeatMapData.ItemBean item) {
        HeatMapNode heatMapNode = new HeatMapNode(viewNode, item);
        heatMapNodeList.add(heatMapNode);
        cacheHeatNodeMap.put(viewNode.mView, heatMapNode);
    }

    private boolean matchXpath(ViewNode viewNode, HeatMapData heatMapData) {
        String x = heatMapData.getX();

        if (x.startsWith("#")) {
            if (!GConfig.USE_ID) {
                return false;
            }

            if (viewNode.mParentXPath.endsWith(x)) {
                return true;
            }
        } else if (LinkedString.fromString(x).equals(viewNode.mParentXPath)) {
            return true;
        }

        return false;
    }

    private boolean isSpecialView(View view) {
        return view instanceof Spinner || view instanceof RadioGroup;
    }

    /**
     * 判断当前特殊View的子元素是否和热图数据匹配，因为Spinner和RadioGroup的特殊性，我们只使用Xpath+index是肯定无法正确进行匹配的，所以对于这两类元素的子元素增加value进行精准匹配
     * @param viewNode
     * @param heatMapData
     * @return
     */
    private ViewNode specialViewChildMatchNode(ViewNode viewNode, HeatMapData heatMapData) {
        int childCount;
        String viewContent;
        ViewNode childViewNode;

        ViewGroup viewGroup = (ViewGroup) viewNode.mView;

        childCount = viewGroup.getChildCount();

        for (int i = 0; i < childCount; i++) {
            childViewNode = ViewHelper.getViewNode(viewGroup.getChildAt(i), null);
            viewContent = childViewNode.mViewContent;

            if (viewContent.equals(heatMapData.getV())) {
                return childViewNode;
            }
        }

        return null;
    }

    private boolean isValueMatch(ViewNode viewNode, HeatMapData heatMapData){
        String v = heatMapData.getV();
        String viewContent = viewNode.mViewContent;
        return v == viewContent || ((v != null) && v.equals(viewContent));
    }

    /**
     * 以前计划的用value来匹配元素所调用的方法，目前热图匹配暂时放弃使用value来进行元素匹配
     * @param viewNode
     * @param heatMapData
     * @return
     */
    private HeatMapData.ItemBean[] matchValue(ViewNode viewNode, HeatMapData heatMapData) {
        String v = heatMapData.getV();
        String viewContent = viewNode.mViewContent;

        if (v.equals(viewContent)) {
            return heatMapData.getItems();
        }

        return null;
    }

    private HeatMapData.ItemBean matchIndex(ViewNode viewNode, HeatMapData.ItemBean[] items) {
        HeatMapData.ItemBean item;

        int itemsLength = items.length;

        if (itemsLength == 1 && !isInListView(viewNode)) {
            return items[0];
        }

        for (int i = 0; i < itemsLength; i++) {
            item = items[i];

            if (item.getIdx() == viewNode.mLastListPos) {
                return item;
            }
        }

        return null;
    }

    private boolean isInListView(ViewNode viewNode) {
        return viewNode.mLastListPos != -1;
    }

    private Runnable traverseRunnable = new Runnable() {
        @Override
        public void run() {
            traverse();
        }
    };
}
