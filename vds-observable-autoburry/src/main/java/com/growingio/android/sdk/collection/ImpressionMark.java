package com.growingio.android.sdk.collection;

import android.view.View;

import androidx.annotation.FloatRange;

import com.growingio.android.sdk.utils.LogUtil;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class ImpressionMark {

    private final WeakReference<View> view;
    private final String eventId;

    private Number num;
    private JSONObject variable;
    private long delayTime = 500L;
    private String globalId;
    private boolean collectV = true;          // 默认采集元素内容
    private float visibleScale = 0;           // 默认: 任何像素可见就算可见

    public ImpressionMark(View view, String eventId){
        this.view = new WeakReference<>(view);
        this.eventId = eventId;
    }

    public View getView() {
        return view.get();
    }

    public ImpressionMark setGlobalId(String globalId) {
        this.globalId = globalId;
        return this;
    }

    public String getGlobalId() {
        return globalId;
    }

    public String getEventId() {
        return eventId;
    }

    public Number getNum() {
        return num;
    }

    /**
     * @deprecated 官网没有设置Num之处， 下个API变更变更删除此API
     */
    @Deprecated
    public ImpressionMark setNum(Number num) {
        this.num = num;
        return this;
    }

    public JSONObject getVariable() {
        return variable;
    }

    public ImpressionMark setVariable(JSONObject variable) {
        this.variable = variable;
        return this;
    }

    public long getDelayTimeMills() {
        return delayTime;
    }

    public ImpressionMark setDelayTimeMills(long delayTime) {
        this.delayTime = delayTime;
        return this;
    }

    public ImpressionMark setCollectContent(boolean collectV) {
        this.collectV = collectV;
        return this;
    }

    public boolean isCollectContent(){
        return this.collectV;
    }

    /**
     * 设置有效曝光比例, 当曝光比例大于等于visibleScale时算View可见
     * 当可见像素值 / 总像素值 >= visibleScale 时认为是有效曝光
     * @param visibleScale 有效曝光比例, 0 -- 任意像素可见为有效曝光, 1 -- 全部像素可见时为有效曝光
     */
    public ImpressionMark setVisibleScale(@FloatRange(from = 0.0f, to = 1.0f) float visibleScale) {
        if (visibleScale < 0 || visibleScale > 1){
            String errorMsg = "visibleScale 区间为[0, 1], current visibleScale is " + visibleScale;
            if (GConfig.DEBUG){
                throw new IllegalArgumentException(errorMsg);
            }else{
                LogUtil.e("GIO.ImpressionMark", errorMsg);
            }
            return this;
        }
        this.visibleScale = visibleScale;
        return this;
    }

    public float getVisibleScale() {
        return visibleScale;
    }
}
