package com.growingio.android.sdk.base.event;

import com.growingio.android.sdk.models.ViewNode;

import java.util.List;

/**
 * Created by liangdengke on 2018/7/14.
 */
public class CircleGotWebSnapshotNodeEvent {
    private final List<ViewNode> nodes;
    private final String host;
    private final String path;

    public CircleGotWebSnapshotNodeEvent(List<ViewNode> nodes, String host, String path) {
        this.nodes = nodes;
        this.host = host;
        this.path = path;
    }

    public List<ViewNode> getNodes() {
        return nodes;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }
}
