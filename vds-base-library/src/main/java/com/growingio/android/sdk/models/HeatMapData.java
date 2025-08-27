package com.growingio.android.sdk.models;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 郑童宇 on 2016/11/24.
 */

public class HeatMapData {
    private static String TAG = "GIO.HeatMapData";

    /**
     * x : /div/div/aside/div/a
     * h : /portal
     * v :
     * items : [{"idx":0,"cnt":3,"percent":0.007614213197969543}]
     */

    private String x;
    private String h;
    private String v;
    private ItemBean[] items;

    public HeatMapData(JSONObject jsonObject) {
        try {
            x = jsonObject.getString("x");
            h = jsonObject.getString("h");
            v = jsonObject.getString("v");
            items = ItemBean.parseArray(jsonObject.getJSONArray("items"));
        } catch (JSONException e) {
            LogUtil.e(TAG, "HeatMapData DataBean解析异常" + e);
        }
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getH() {
        return h;
    }

    public void setH(String h) {
        this.h = h;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public ItemBean[] getItems() {
        return items;
    }

    public void setItems(ItemBean[] items) {
        this.items = items;
    }

    public static HeatMapData[] parseArray(JSONArray jsonArray) {
        int jsonArrayLength = jsonArray.length();

        HeatMapData[] dataBeanArray = new HeatMapData[jsonArrayLength];

        for (int i = 0; i < jsonArrayLength; i++) {
            try {
                dataBeanArray[i] = new HeatMapData(jsonArray.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return dataBeanArray;
    }

    public static class ItemBean {
        /**
         * idx : 0
         * cnt : 3
         * percent : 0.007614213197969543
         */

        private int idx;
        private int cnt;
        private double percent;

        public ItemBean(JSONObject jsonObject) {
            try {
                idx = jsonObject.getInt("idx");
                cnt = jsonObject.getInt("cnt");
                percent = jsonObject.getDouble("percent");
            } catch (JSONException e) {
                LogUtil.e(TAG, "HeatMapData ItemsBean" + e);
            }
        }

        public int getIdx() {
            return idx;
        }

        public void setIdx(int idx) {
            this.idx = idx;
        }

        public int getCnt() {
            return cnt;
        }

        public void setCnt(int cnt) {
            this.cnt = cnt;
        }

        public double getPercent() {
            return percent;
        }

        public void setPercent(double percent) {
            this.percent = percent;
        }

        public static ItemBean[] parseArray(JSONArray jsonArray) {
            int jsonArrayLength = jsonArray.length();

            ItemBean[] itemBeanArray = new ItemBean[jsonArrayLength];

            for (int i = 0; i < jsonArrayLength; i++) {
                try {
                    itemBeanArray[i] = new ItemBean(jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return itemBeanArray;
        }
    }
}