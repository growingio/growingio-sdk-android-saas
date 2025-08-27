package com.growingio.android.sdk.utils;

import android.view.View;
import android.widget.ImageView;

import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewNode;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClickEventAsyncExecutor{

    public final static String TAG = "GIO.ClickEventAsyncExecutor";
    private static ClickEventAsyncExecutor clickEventAsyncExecutor;
    private final Executor threadPool;

    public static ClickEventAsyncExecutor getInstance(){
        if (clickEventAsyncExecutor == null) {
            clickEventAsyncExecutor = new ClickEventAsyncExecutor();
        }
        return clickEventAsyncExecutor;
    }

    private ClickEventAsyncExecutor() {
        threadPool = Executors.newSingleThreadExecutor();
    }

    public void execute(final WeakReference<View> mView, final ViewNode mViewNode, final ActionEvent mActionEvent) {
        if (mView.get() == null)
            return ;
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                postSingleEvent(mView, mViewNode, mActionEvent);
            }
        });
    }

    private void postSingleEvent(WeakReference<View> mView, ViewNode mViewNode, ActionEvent mActionEvent) {
        try {
            ImageView imageView = null;
            if (mView.get() instanceof ImageView){
                imageView = (ImageView) mView.get();
            }
            if (imageView == null) {
                return;
            }
            if (DHashcode.cacheHash.containsKey(imageView.hashCode())) {
                mViewNode.mImageViewDHashCode = DHashcode.cacheHash.get(imageView.hashCode());
            } else {
                String hashcode = DHashcode.getDHash(imageView);
                mViewNode.mImageViewDHashCode = hashcode;
                DHashcode.cacheHash.put(imageView.hashCode(), hashcode);
            }
            LogUtil.i(TAG, "Dhashcode: "+mViewNode.mImageViewDHashCode);
            //add image'v back
            if (mActionEvent == null)
                return ;
            List<ActionStruct> elems = mActionEvent.elems;
            for(ActionStruct actionStruct: elems){
                if (isEqual(actionStruct, mViewNode)) {
                    actionStruct.imgHashcode = mViewNode.mImageViewDHashCode;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            ViewHelper.persistClickEvent(mActionEvent, mViewNode);
            if (DHashcode.cacheHash.size() > 100)
                DHashcode.cacheHash.clear();
        }
    }

    private boolean isEqual(ActionStruct actionStruct, ViewNode viewNode){
        if (actionStruct == null || actionStruct.xpath == null)
            return false;
        return actionStruct.xpath.equals(viewNode.mParentXPath) && ObjectUtils.equals(actionStruct.obj, viewNode.mInheritableGrowingInfo);
    }

}
