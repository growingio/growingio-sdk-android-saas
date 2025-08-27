package com.growingio.android.sdk.heatmap;

import com.growingio.android.sdk.base.event.ActivityLifecycleEvent;
import com.growingio.android.sdk.base.event.HeatMapEvent;
import com.growingio.android.sdk.base.event.ViewTreeStatusChangeEvent;
import com.growingio.cp_annotation.Subscribe;

/**
 * author CliffLeopard
 * time   2018/7/5:下午3:18
 * email  gaoguanling@growingio.com
 */
public class HeatMapSubscriber {
    @Subscribe
    public void onViewTreeChange(ViewTreeStatusChangeEvent viewTreeStatusChangeEvent) {
        switch (viewTreeStatusChangeEvent.getStatusType()) {
            case FocusChanged:
                break;
            case LayoutChanged:
                HeatMapManager.getInstance().traverseNodeImmediately();
                break;
            case ScrollChanged:
                HeatMapManager.getInstance().traverseNodeImmediately();
                break;
        }
    }

    @Subscribe
    public void onActivityLifeCycleChange(ActivityLifecycleEvent event) {

        switch (event.event_type) {
            case ON_CREATED:

                break;
            case ON_NEW_INTENT:

            case ON_RESUMED:

            case ON_PAUSED:

            case ON_DESTROYED:

                break;
        }
    }

    @Subscribe
    public void onHeatMapEvent(HeatMapEvent event) {
        switch (event.type) {
            case INIT:
                HeatMapManager.getInstance().initHeatMapView();
                break;
            case STATE_ON:
                HeatMapManager.getInstance().setHeatMapState(true);
                break;
            case STATE_OFF:
                HeatMapManager.getInstance().setHeatMapState(false);
                break;
            case HIDE:
                if (HeatMapManager.getInstance().isHeatMapOn())
                    HeatMapManager.getInstance().hideHeatMapView();
                break;
            case SHOW:
                if (HeatMapManager.getInstance().isHeatMapOn()){
                    HeatMapManager.getInstance().showHeatMapView();
                }
                break;
            case UPDATE:
                HeatMapManager heatMapManager = HeatMapManager.getInstance();
                if (heatMapManager != null && heatMapManager.isHeatMapOn()) {
                    heatMapManager.getHeatMapData();
                    heatMapManager.showHeatMapView();
                }
        }
    }
}
