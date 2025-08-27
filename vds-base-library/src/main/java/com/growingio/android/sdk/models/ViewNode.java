package com.growingio.android.sdk.models;

import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.growingio.android.sdk.collection.AbstractGrowingIO;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;

import java.util.List;

import static com.growingio.android.sdk.collection.GrowingIO.GROWING_RN_PAGE_KEY;

/**
 * Created by xyz on 16/1/14.
 */
public class ViewNode {
    private static final String TAG = "GIO.ViewNode";


    public static final String ANONYMOUS_CLASS_NAME = "Anonymous";

    public View mView;
    public int mLastListPos = -1;
    public boolean mFullScreen;
    private int mViewIndex;
    public boolean mHasListParent;
    public boolean mInClickableGroup;
    public int mViewPosition;
    public LinkedString mParentXPath;
    public LinkedString mOriginalParentXpath;
    public String mWindowPrefix;
    ViewTraveler mViewTraveler;


    public String mViewName;
    public Screenshot mScreenshot;
    public String mBannerText;
    public String mViewContent;
    public String mInheritableGrowingInfo;
    public boolean mParentIdSettled = false;
    public Rect mClipRect;
    public WebElementInfo mWebElementInfo;
    public LinkedString mClickableParentXPath;
    public String mImageViewDHashCode;
    public String mPatternXPath;

    // h5 传递过来的elem中包含了isTrackingEditText属性, 必须有个地方进行保存, 作为临时变量
    // 约定hybrid 开头的变量， native元素不能使用
    public boolean hybridIsTrackingEditText = false;


    private int mHashCode = -1;
    public void setViewTraveler(ViewTraveler viewTraveler) {
        this.mViewTraveler = viewTraveler;
    }
    public ViewNode() {
    }

    /**
     * 在基本信息已经计算出来的基础上计算XPath,用于ViewHelper构建Xpath,和基于parentView构建Xpath
     */
    public ViewNode(View view, int viewIndex, int lastListPos, boolean hasListParent, boolean fullScreen,
                    boolean inClickableGroup, boolean parentIdSettled, LinkedString originalParentXPath, LinkedString parentXPath, String windowPrefix, ViewTraveler viewTraveler) {

        mView = view;
        mLastListPos = lastListPos;
        mFullScreen = fullScreen;
        mViewIndex = viewIndex;
        mHasListParent = hasListParent;
        mInClickableGroup = inClickableGroup;
        mParentIdSettled = parentIdSettled;
        mParentXPath = parentXPath;
        mOriginalParentXpath = originalParentXPath;
        mWindowPrefix = windowPrefix;

        mViewTraveler = viewTraveler;

        if (GConfig.ISRN() && GConfig.isUsingRNOptimizedPath() && mView != null ) {
            identifyRNChangeablePath();
        }
    }

    public void traverseViewsRecur() {
        if (mViewTraveler != null && mViewTraveler.needTraverse(this)) {
            mViewName = Util.getSimpleClassName(mView.getClass());
            viewPosition();
            calcXPath();
            viewContent();

            if (needTrack())
                mViewTraveler.traverseCallBack(this);
            if (ClassExistHelper.instanceOfX5WebView(mView))
                return;
            traverseChildren();
        }
    }

    public void traverseChildren() {
        if (mView instanceof ViewGroup && !(mView instanceof Spinner)) {
            ViewGroup viewGroup = (ViewGroup) mView;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View childView = viewGroup.getChildAt(i);
                ViewNode childViewNode = new ViewNode(childView, i, mLastListPos,
                        mHasListParent || Util.isListView(mView), mFullScreen,
                        mInClickableGroup || Util.isViewClickable(mView), mParentIdSettled,
                        LinkedString.copy(mOriginalParentXpath),
                        LinkedString.copy(mParentXPath), mWindowPrefix, mViewTraveler);
                if (Util.isViewClickable(mView)) {
                    childViewNode.mClickableParentXPath = mParentXPath;
                } else {
                    childViewNode.mClickableParentXPath = mClickableParentXPath;
                }
                childViewNode.mBannerText = mBannerText;
                childViewNode.mInheritableGrowingInfo = mInheritableGrowingInfo;
                /**
                 * 在traverseViewsRecur之前childViewNode的XPath等信息还是parentView的，而不是childViewNode的；
                 */
                childViewNode.traverseViewsRecur();
            }
        }
    }

    @Override
    public int hashCode() {
        if (mHashCode == -1) {
            int result = 17;
            result = result * 31 + (mViewContent != null ? mViewContent.hashCode() : 0);
            result = result * 31 + (mParentXPath != null ? mParentXPath.hashCode() : 0);
            result = result * 31 + (mInheritableGrowingInfo != null ? mInheritableGrowingInfo.hashCode() : 0);
            result = result * 31 + mLastListPos;
            mHashCode = result;
        }
        return mHashCode;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ViewNode && object.hashCode() == this.hashCode();
    }

    public ViewNode copyWithoutView() {
        return new ViewNode(null, mViewIndex, mLastListPos, mHasListParent,
                mFullScreen, mInClickableGroup, mParentIdSettled,
                LinkedString.fromString(mOriginalParentXpath.toStringValue()),
                LinkedString.fromString(mParentXPath.toStringValue()), mWindowPrefix, null);
    }


    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public boolean isNeedTrack() {
        return ViewHelper.isViewSelfVisible(mView) && !Util.isIgnoredView(mView);
    }

    public boolean isIgnoreImp(){
        return mView.getTag(AbstractGrowingIO.GROWING_IGNORE_VIEW_IMP_KEY) != null;
    }

    private void viewContent() {
        if (mView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY) != null)
            mInheritableGrowingInfo = (String) mView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
        mViewContent = Util.getViewContent(mView, mBannerText);
    }

    private void viewPosition() {
        int idx = mViewIndex;
        if (mView.getParent() != null && (mView.getParent() instanceof ViewGroup)) {
            ViewGroup parent = (ViewGroup) mView.getParent();
            if (ClassExistHelper.instanceOfAndroidXViewPager(parent)){
                idx = ((androidx.viewpager.widget.ViewPager)parent).getCurrentItem();
            }else if (ClassExistHelper.instanceOfSupportViewPager(parent)) {
                idx = ((ViewPager) parent).getCurrentItem();
            } else if (parent instanceof AdapterView) {
                AdapterView listView = (AdapterView) parent;
                idx = listView.getFirstVisiblePosition() + mViewIndex;
            } else if (ClassExistHelper.instanceOfRecyclerView(parent)) {
                int adapterPosition = ViewHelper.getChildAdapterPositionInRecyclerView(mView, parent);
                if (adapterPosition >= 0) {
                    idx = adapterPosition;
                }
            }
        }
        mViewPosition = idx;
    }

    private void calcXPath() {
        Object parentObject = mView.getParent();
        if (parentObject == null || (WindowHelper.isDecorView(mView) && !(parentObject instanceof View))) {
            return;
        }
        Object viewName = mView.getTag(GrowingIO.GROWING_VIEW_NAME_KEY);
        if (viewName != null) {
            mOriginalParentXpath = LinkedString.fromString("/").append(viewName);
            mParentXPath.append("/").append(viewName);
            return;
        }

        if (parentObject instanceof View) {
            View parent = (View) parentObject;

            if (parent instanceof ExpandableListView) {
                // 处理ExpandableListView
                ExpandableListView listParent = (ExpandableListView) parent;
                long elp = ((ExpandableListView) mView.getParent()).getExpandableListPosition(mViewPosition);
                if (ExpandableListView.getPackedPositionType(elp) == ExpandableListView.PACKED_POSITION_TYPE_NULL) {
                    mHasListParent = false;
                    if (mViewPosition < listParent.getHeaderViewsCount()) {
                        mOriginalParentXpath.append("/ELH[").append(mViewPosition).append("]/").append(mViewName).append("[0]");
                        mParentXPath.append("/ELH[").append(mViewPosition).append("]/").append(mViewName).append("[0]");
                    } else {
                        int footerIndex = mViewPosition - (listParent.getCount() - listParent.getFooterViewsCount());
                        mOriginalParentXpath.append("/ELF[").append(footerIndex).append("]/").append(mViewName).append("[0]");
                        mParentXPath.append("/ELF[").append(footerIndex).append("]/").append(mViewName).append("[0]");
                    }
                } else {
                    int groupIdx = ExpandableListView.getPackedPositionGroup(elp);
                    int childIdx = ExpandableListView.getPackedPositionChild(elp);
                    if (childIdx != -1) {
                        mLastListPos = childIdx;
                        mParentXPath = LinkedString.fromString(mOriginalParentXpath.toStringValue()).append("/ELVG[").append(groupIdx).append("]/ELVC[-]/").append(mViewName).append("[0]");
                        mOriginalParentXpath.append("/ELVG[").append(groupIdx).append("]/ELVC[").append(childIdx).append("]/").append(mViewName).append("[0]");
                    } else {
                        mLastListPos = groupIdx;
                        mParentXPath = LinkedString.fromString(mOriginalParentXpath.toStringValue()).append("/ELVG[-]/").append(mViewName).append("[0]");
                        mOriginalParentXpath.append("/ELVG[").append(groupIdx).append("]/").append(mViewName).append("[0]");
                    }
                }
            } else if (Util.isListView(parent)) {
                // 处理有特殊的position的元素

                Object bannerTag = parent.getTag(GrowingIO.GROWING_BANNER_KEY);
                if (bannerTag instanceof List && ((List) bannerTag).size() > 0) {
                    mViewPosition = Util.calcBannerItemPosition((List) bannerTag, mViewPosition);
                    mBannerText = Util.truncateViewContent(String.valueOf(((List) bannerTag).get(mViewPosition)));
                }
                mLastListPos = mViewPosition;
                mParentXPath = LinkedString.fromString(mOriginalParentXpath.toStringValue()).append("/").append(mViewName).append("[-]");
                mOriginalParentXpath.append("/").append(mViewName).append("[").append(mLastListPos).append("]");
            } else if (ClassExistHelper.instanceofAndroidXSwipeRefreshLayout(parentObject)
                    || ClassExistHelper.instanceOfSupportSwipeRefreshLayout(parentObject)) {
                mOriginalParentXpath.append("/").append(mViewName).append("[0]");
                mParentXPath.append("/").append(mViewName).append("[0]");
            } else {
                mOriginalParentXpath.append("/").append(mViewName).append("[").append(mViewPosition).append("]");
                mParentXPath.append("/").append(mViewName).append("[").append(mViewPosition).append("]");
            }
        }else {
            mOriginalParentXpath.append("/").append(mViewName).append("[").append(mViewPosition).append("]");
            mParentXPath.append("/").append(mViewName).append("[").append(mViewPosition).append("]");
        }

        if (GConfig.USE_ID) {
            String id = Util.getIdName(mView, mParentIdSettled);
            if (id != null) {
                if (mView.getTag(GrowingIO.GROWING_VIEW_ID_KEY) != null)
                    mParentIdSettled = true;
                mOriginalParentXpath.append("#").append(id);
                mParentXPath.append("#").append(id);
            }
        }
    }

    private boolean needTrack() {
        ViewParent parent = mView.getParent();
        if (parent != null) {
            if (mView.isClickable()
                    || mView instanceof TextView
                    || mView instanceof ImageView
                    || mView instanceof WebView
                    || parent instanceof AdapterView
                    || parent instanceof RadioGroup
                    || mView instanceof Spinner
                    || mView instanceof RatingBar
                    || mView instanceof SeekBar
                    || ClassExistHelper.instanceOfX5WebView(mView)) {
                return true;
            }
        }
        return false;
    }


    public void getVisibleRect(View view, Rect rect, boolean fullscreen) {
        if (fullscreen) {
            view.getGlobalVisibleRect(rect);
        } else {
            int[] offset = new int[2];
            view.getLocationOnScreen(offset);
            view.getLocalVisibleRect(rect);
            rect.offset(offset[0], offset[1]);
        }
    }

    /**
     * 判断当前View是否有必要进行removeRNChangeablePath操作
     */
    private void identifyRNChangeablePath() {
        ViewParent parent = mView.getParent();
        if (!(parent instanceof View)) {
            return;
        }

        boolean shouldRemoveChangeablePath = false;

        View viewParent = (View) parent;
        Object viewRNPage = mView.getTag(GROWING_RN_PAGE_KEY);
        Object viewParentRNPage = viewParent.getTag(GROWING_RN_PAGE_KEY);
        LogUtil.d("GIO.HandleRNView", "IdentifyRNChangeablePath: ", mView.getClass().getName());
        LogUtil.d("GIO.HandleRNView", "mParentXPath: ", mParentXPath);
        LogUtil.d("GIO.HandleRNView", "viewRNPage: ", viewRNPage);

        if (viewRNPage != null) {
            if (!viewRNPage.equals(viewParentRNPage)) {
                shouldRemoveChangeablePath = true;
            }
        } else {
            if (viewParentRNPage != null) {
                shouldRemoveChangeablePath = true;
            }
        }

        if (shouldRemoveChangeablePath) {
            LogUtil.d("GIO.HandleRNView", "viewParentRNPage: ", viewParentRNPage);
            removeRNChangeablePath();
        }
    }

    /**
     * 此方法用于移除与父元素有不同GROWING_RN_VIEW_PAGE_KEY属性的元素的mParentXPath,并将对应的mViewIndex置为0
     * 因为React Native中Navigator中每个子项是由ViewGroup包含的,而这个ViewGroup是被隐藏同时不可点击的,
     * 所以对于React Native而言子元素和父元素如果GROWING_RN_VIEW_PAGE_KEY不同必然属于Imp元素而非Click元素
     */
    private void removeRNChangeablePath() {
        mParentXPath = new LinkedString();
        mOriginalParentXpath = new LinkedString();
        mViewIndex = 0;
    }


    public static class WebElementInfo {
        public String mHost;
        public String mPath;
        public String mQuery;
        public String mHref;
        public String mNodeType;
    }
}
