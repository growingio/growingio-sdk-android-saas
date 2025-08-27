package com.growingio.android.sdk.utils;

import android.widget.ImageView;

import com.growingio.android.sdk.collection.ActionCalculator;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.eventcenter.bus.EventBus;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.growingio.android.sdk.utils.DHashcode.cacheHash;

public class ImplEventAsyncExecutor {

    public final static String TAG = "GIO.ImplEventAsyncExecutor";
    private static ImplEventAsyncExecutor implEventAsyncExecutor;
    private final Executor threadPool;

    public static ImplEventAsyncExecutor getInstance() {
        if (implEventAsyncExecutor == null) {
            implEventAsyncExecutor = new ImplEventAsyncExecutor();
        }
        return implEventAsyncExecutor;
    }

    private ImplEventAsyncExecutor() {
        threadPool = Executors.newFixedThreadPool(8);
    }

    public void execute(final ActionEvent mEvent, final List<ViewNode> mList) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                postSingleEvent(mEvent, mList);
            }
        });
    }

    private void postSingleEvent(ActionEvent mEvent, List<ViewNode> mList) {
        try {
            List<ActionStruct> mNewImpressViews = new LinkedList<ActionStruct>();
            for (ViewNode viewNode : mList) {
                ImageView imageView = null;
                if (viewNode.mView instanceof ImageView){
                    imageView = (ImageView) viewNode.mView;
                }
                if (imageView == null){
                    continue;
                }
                if (cacheHash.containsKey(imageView.hashCode())) {
                    viewNode.mImageViewDHashCode = cacheHash.get(imageView.hashCode());
                } else {
                    String hashcode = DHashcode.getDHash(imageView);
                    viewNode.mImageViewDHashCode = hashcode;
                    cacheHash.put(imageView.hashCode(), hashcode);
                }
                LogUtil.i(TAG, "Dhashcode: " + viewNode.mImageViewDHashCode);
                ActionStruct actionStruct = ActionCalculator.genActionStruct(viewNode);
                mNewImpressViews.add(actionStruct);
            }
            if (mNewImpressViews.size() > 0) {
                mEvent.elems.addAll(mNewImpressViews);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            EventBus.getDefault().post(mEvent);
//            MessageProcessor.getInstance().persistEvent(mEvent);
            if (cacheHash.size() > 100)
                cacheHash.clear();
        }
    }

}
