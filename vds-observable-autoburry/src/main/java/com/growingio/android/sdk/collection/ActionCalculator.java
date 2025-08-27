package com.growingio.android.sdk.collection;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import com.growingio.android.sdk.autoburry.VdsJsBridgeManager;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.ImplEventAsyncExecutor;
import com.growingio.android.sdk.utils.SysTrace;
import com.growingio.android.sdk.utils.ViewHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyz on 15/12/26.
 */
public class ActionCalculator {
    static final String TAG = "GIO.ActionCalculator";
    private final String mWindowPrefix;
    private SparseBooleanArray mImpressedViews = new SparseBooleanArray();
    List<ActionStruct> mNewImpressViews;
    private WeakReference<View> mRootView;
    private List<WeakReference<View>> mImpressedWebView = new ArrayList<WeakReference<View>>();
    private List<ViewNode> mTodoViewNode = new ArrayList<ViewNode>();
    private long mPtm;
    private String mPage;
    private GConfig mConfig;

    public ActionCalculator(String pageName, long ptm, View root, String windowPrefix) {
        this.mPtm = ptm;
        this.mRootView = new WeakReference<View>(root);
        this.mPage = pageName;
        this.mWindowPrefix = windowPrefix;
        mConfig = CoreInitialize.config();
    }

    @Nullable
    public List<ActionEvent> obtainImpress() {
        SysTrace.beginSection("gio.obtainImpress");
        List<ActionEvent> events = null;
        if (mConfig != null && mConfig.shouldSendImp()) {
            mNewImpressViews = new ArrayList<ActionStruct>();
            if (mRootView != null && mRootView.get() != null
                    && mRootView.get().getTag(AbstractGrowingIO.GROWING_IGNORE_VIEW_IMP_KEY) == null) {
                // FIXME: 理论上在ViewHelper中更改比较好， 没明白那句是做什么的， 在这里改了。
                ViewHelper.traverseWindow(mRootView.get(), mWindowPrefix, mViewTraveler);
            }

            events = new ArrayList<ActionEvent>(2);
            ActionEvent impEvents = makeActionEvent(events);
            mNewImpressViews = null;

            if (mTodoViewNode.size() > 0){
                if (impEvents == null){
                    impEvents = ActionEvent.makeImpEvent();
                    impEvents.setPageTime(mPtm);
                    impEvents.mPageName = mPage;
                }
                ImplEventAsyncExecutor.getInstance().execute(impEvents, mTodoViewNode);
                mTodoViewNode = new ArrayList<ViewNode>();
                SysTrace.endSection();
                return null;
            }
        }
        SysTrace.endSection();
        return events;
    }

    ActionEvent makeActionEvent(List<ActionEvent> events){
        int newImpSize = mNewImpressViews.size();
        if (newImpSize <= 0){
            return null;
        }
        int end = 0;
        ActionEvent actionEvent = null;
        for (int i = 0; i < newImpSize;){
            end = i + 100;
            end = Math.min(end, newImpSize);
            List<ActionStruct> actions = mNewImpressViews.subList(i, end);
            actionEvent = ActionEvent.makeImpEvent();
            actionEvent.elems = actions;
            actionEvent.setPageTime(mPtm);
            actionEvent.mPageName = mPage;
            events.add(actionEvent);
            i = end;
        }
        return actionEvent;
    }

    public long getPtm() {
        return mPtm;
    }

    public String getPage() {
        return mPage;
    }


    @SuppressWarnings("NewApi")
    private ViewTraveler mViewTraveler = new ViewTraveler() {

        public void traverseCallBack(ViewNode viewNode) {
            boolean isNew = false;
            if (mConfig.isImageViewCollectionEnable() && viewNode.mView instanceof ImageView && TextUtils.isEmpty(viewNode.mViewContent)) {
                mTodoViewNode.add(viewNode);
                return ;
            }
            if (!mImpressedViews.get(viewNode.hashCode())) {
                ActionStruct actionStruct = genActionStruct(viewNode);
                mImpressedViews.put(viewNode.hashCode(), true);
                mNewImpressViews.add(actionStruct);
                isNew = true;
            }
            if (viewNode.mView instanceof WebView || ClassExistHelper.instanceOfX5WebView(viewNode.mView)) {
                for (WeakReference<View> view : mImpressedWebView) {
                    if (view.get() == viewNode.mView) {
                        isNew = false;
                        break;
                    }
                }
                if (isNew) {
                    mImpressedWebView.add(new WeakReference<View>(viewNode.mView));
                    VdsJsBridgeManager.refreshImpressionForce(viewNode.mView);
                }
            }
        }

        @Override
        public boolean needTraverse(ViewNode viewNode) {
            return super.needTraverse(viewNode) && !viewNode.isIgnoreImp();
        }
    };

    public static ActionStruct genActionStruct(ViewNode viewNode) {
        ActionStruct actionStruct = new ActionStruct();
        actionStruct.xpath = viewNode.mParentXPath;
        actionStruct.time = System.currentTimeMillis();
        actionStruct.index = viewNode.mLastListPos;
        actionStruct.content = viewNode.mViewContent;
        actionStruct.obj = viewNode.mInheritableGrowingInfo;
        actionStruct.imgHashcode = viewNode.mImageViewDHashCode;
        return actionStruct;
    }

}
