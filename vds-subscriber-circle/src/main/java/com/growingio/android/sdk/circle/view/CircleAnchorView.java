package com.growingio.android.sdk.circle.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;

import com.growingio.android.sdk.api.TagStore;
import com.growingio.android.sdk.autoburry.AutoBuryAppState;
import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.circle.CircleManager;
import com.growingio.android.sdk.circle.HybridEventEditDialog;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreAppState;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.models.Screenshot;
import com.growingio.android.sdk.models.Tag;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.FloatWindowManager;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.android.sdk.view.FloatViewContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@TargetApi(15)
public class CircleAnchorView extends FloatViewContainer {

    final static String TAG = "GrowingIO.FloatView";
    private final int MAX_OVERLAP_EDGE_DISTANCE = 10;
    private final int UPDATE_CIRCLED_RECORD_DELAYED_TIME = 1000 * 20;
    static int ANCHOR_VIEW_SIZE;
    // calc the point
    private float xInScreen;
    private float yInScreen;
    private float xDownInScreen;
    private float yDownInScreen;
    private float xInView;
    private float yInView;
    private boolean mIsInTouch = false;
    private View mShowingMaskInWebView;

    private Point mLastMovePoint = null;
    private CircleMagnifierView mMagnifierView;
    private FloatViewContainer mMaskView;
    private TagsMaskView mTagsView;
    private CircleTipMask mCircleTipMaskView;
    private Rect mHitRect;
    private ViewNode mTopsideHitView;
    private Rect mVisibleRectBuffer = new Rect();
    private List<ViewNode> mHitViewNodes = new ArrayList<ViewNode>();
    private View[] mWindowRootViews;
    private int mMinMoveDistance;

    private GConfig mConfig;
    private CoreAppState mCoreAppState;
    private AutoBuryAppState autoBuryAppState;

    CircleManager getCircleManager() {
        return CircleManager.getInstance();
    }

    public CircleAnchorView(Context context) {
        super(context);
        init();
    }

    public void init() {
        ANCHOR_VIEW_SIZE = Util.dp2Px(getContext(), 48);
        ShapeDrawable background = new ShapeDrawable();
        background.setShape(new RoundRectShape(
                new float[]{ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f,
                        ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f, ANCHOR_VIEW_SIZE / 2f}, null, null
        ));
        mConfig = CoreInitialize.config();
        mCoreAppState = CoreInitialize.coreAppState();
        autoBuryAppState = AutoBuryObservableInitialize.autoBuryAppState();
        background.getPaint().setStyle(Paint.Style.FILL);
        background.getPaint().setColor(Constants.GROWINGIO_COLOR_RED);
        background.getPaint().setAntiAlias(true);
        setBackgroundDrawable(background);
        mMinMoveDistance = Util.dp2Px(getContext(), 4);
        initMaskView();
        mMagnifierView = new CircleMagnifierView(getContext());
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getCircleManager().showCircleModeDialog();
                setVisibility(GONE);
                mMaskView.setVisibility(GONE);
                mTagsView.setVisibility(GONE);
                hideCircleTipMask();
            }
        });
    }

    @SuppressLint("RtlHardcoded")
    private void initMaskView() {
        mMaskView = new FloatViewContainer(getContext());
        float radius = Util.dp2Px(getContext(), 3);
        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(
                new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null
        ));
        background.getPaint().setColor(Constants.GROWINGIO_COLOR_LIGHT_RED);
        background.getPaint().setStrokeWidth(Util.dp2Px(getContext(), 1));
        background.getPaint().setAntiAlias(true);
        mMaskView.setBackgroundDrawable(background);
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams maskParams = new WindowManager.LayoutParams(
                0, 0,
                PendingStatus.FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
        maskParams.gravity = Gravity.TOP | Gravity.LEFT;
        maskParams.setTitle("MaskWindow:" + getContext().getPackageName());

        FloatWindowManager.getInstance().addView(mMaskView, maskParams);
        mTagsView = new TagsMaskView(getContext());
        mTagsView.setFloatType(PendingStatus.FLOAT_VIEW_TYPE);
        mTagsView.show();
        if (mConfig.shouldShowTags()) {
            mTagsView.setTags(TagStore.getInstance().getTags());
        }
    }

    private void updateLatestCircledRecord() {
        ThreadUtils.cancelTaskOnUiThread(updateLatestCircledRecordThread);
        ThreadUtils.postOnUiThreadDelayed(updateLatestCircledRecordThread, UPDATE_CIRCLED_RECORD_DELAYED_TIME);
    }

    private Runnable updateLatestCircledRecordThread = new Runnable() {
        @Override
        public void run() {
            TagStore.getInstance().initial();
        }
    };

    @SuppressLint("RtlHardcoded")
    public void show() {
        if (mConfig.shouldShowTags())
            updateLatestCircledRecord();

        showCircleTipIfNeeded();
        if (getParent() == null) {
            Point point = mConfig.getFloatPosition();
            boolean portrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            if (mConfig.shouldShowCircleTip()) {
                point.x = (ScreenshotHelper.getScreenShort() - ANCHOR_VIEW_SIZE) / 2;
                point.y = (ScreenshotHelper.getScreenLong() - ANCHOR_VIEW_SIZE) / 2;
            }
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    ANCHOR_VIEW_SIZE, ANCHOR_VIEW_SIZE,
                    PendingStatus.FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            layoutParams.x = portrait ? point.x : point.y;
            layoutParams.y = portrait ? point.y : point.x;
            layoutParams.setTitle("AnchorWindow:" + getContext().getPackageName());
            FloatWindowManager.getInstance().addView(this, layoutParams);
            mMagnifierView.attachToWindow();
        } else {
            setVisibility(VISIBLE);
            mTagsView.setVisibility(VISIBLE);
            bringToFront();
        }
    }

    @SuppressLint("RtlHardcoded")
    private void showCircleTipIfNeeded() {
        if (mConfig.shouldShowCircleTip()) {
            if (mCircleTipMaskView == null) {
                mCircleTipMaskView = new CircleTipMask(getContext());
                int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

                WindowManager.LayoutParams maskParams = new WindowManager.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                        PendingStatus.FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
                maskParams.gravity = Gravity.TOP | Gravity.LEFT;
                maskParams.setTitle("CircleTipWindow:" + getContext().getPackageName());
                mCircleTipMaskView.setLayoutParams(maskParams);
                mCircleTipMaskView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideCircleTipMask();
                    }
                });
            }
            if (mCircleTipMaskView.getParent() == null) {
                FloatWindowManager.getInstance().addView(mCircleTipMaskView, (WindowManager.LayoutParams) mCircleTipMaskView.getLayoutParams());
                bringToFront();
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        boolean handled = false;
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                xInView = event.getX();
                yInView = event.getY();
                xDownInScreen = event.getRawX();
                yDownInScreen = event.getRawY();
                xInScreen = event.getRawX();
                yInScreen = event.getRawY();

                mIsInTouch = true;
                mWindowRootViews = WindowHelper.getSortedWindowViews();
                mTagsView.clearTags();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsInTouch) {
                    xInScreen = event.getRawX();
                    yInScreen = event.getRawY();
                    if (Math.abs(xInScreen - xDownInScreen) < mMinMoveDistance
                            && Math.abs(yInScreen - yDownInScreen) < mMinMoveDistance) {
                        break;
                    }
                    if (mCircleTipMaskView != null) {
                        hideCircleTipMask();
                    }
                    updateViewPosition();
                    Rect rect = new Rect();
                    getGlobalVisibleRect(rect);
                    if (!rect.contains((int) xDownInScreen, (int) yDownInScreen)) {
                        mLastMovePoint = new Point((int) xInScreen, (int) yInScreen);
                        findTopsideHitView();
                    } else {
                        mMagnifierView.setVisibility(GONE);
                    }
                    handled = true;
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                mIsInTouch = false;
                mMaskView.setVisibility(GONE);
                mMagnifierView.setVisibility(GONE);
                mMaskView.getLayoutParams().width = 0;
                hideMaskInWebView();

                if (mHitRect != null) {
                    mHitViewNodes.clear();
                    reverseArray(mWindowRootViews);
                    ViewHelper.traverseWindows(mWindowRootViews, mTraverseMask);
                    if (mHitViewNodes.size() > 0) {
                        View first = mHitViewNodes.get(0).mView;
                        if (first instanceof WebView || ClassExistHelper.instanceOfX5WebView(first)) {
                            findElementAt(first);
                        } else {
                            Collections.sort(mHitViewNodes, mViewNodeComparator);
                            showEventDetailDialog("elem", mHitViewNodes);
                        }
                    } else {
                        CircleManager.getInstance().updateTagsIfNeeded();
                    }
                    mHitRect = null;
                    handled = true;
                } else {
                    if (Math.abs(xInScreen - xDownInScreen) < mMinMoveDistance
                            && Math.abs(yInScreen - yDownInScreen) < mMinMoveDistance) {
                        performClick();
                    } else {
                        mTagsView.setVisibility(mConfig.shouldShowTags() ? VISIBLE : GONE);
                        CircleManager.getInstance().updateTagsIfNeeded();
                    }
                }
                mWindowRootViews = null;

                break;
            default:
                break;
        }
        return handled;
    }

    private void reverseArray(Object[] array) {
        if (array == null || array.length <= 1) {
            return;
        }
        for (int i = 0; i < array.length / 2; i++) {
            Object temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }

    private void showEventDetailDialog(String eventType, List<ViewNode> hitViews) {
        setVisibility(GONE);
        final HybridEventEditDialog window = new HybridEventEditDialog();
        String path = autoBuryAppState.getPageName();
        window.setContent(mCoreAppState.getForegroundActivity(), hitViews, path, mCoreAppState.getSPN(), new Runnable() {
            @Override
            public void run() {
                showDialog(window, HybridEventEditDialog.class.getName());
            }
        });
    }

    // update this view position
    private void updateViewPosition() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) this.getLayoutParams();
        int x = (int) (xInScreen - xInView);
        int y = (int) (yInScreen - yInView);

        // check the point is out edge
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        boolean portrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        int maxWidth = (portrait ? ScreenshotHelper.getScreenShort(): ScreenshotHelper.getScreenLong()) - getWidth();
        int maxHeight = (portrait ? ScreenshotHelper.getScreenLong() : ScreenshotHelper.getScreenShort()) - getHeight();
        if (x > maxWidth) x = maxWidth;
        if (y > maxHeight) y = maxHeight;
        if (y < 0) y = 0;

        params.x = x;
        params.y = y;
        FloatWindowManager.getInstance().updateViewLayout(this, params);
    }

    private Comparator<ViewNode> mViewNodeComparator = new Comparator<ViewNode>() {
        @Override
        public int compare(ViewNode lhs, ViewNode rhs) {
            int l = lhs.mView instanceof AdapterView ? -1 : 1;
            int r = rhs.mView instanceof AdapterView ? -1 : 1;
            return r - l;
        }
    };

    // the first traverse, used to get the topside view
    private ViewTraveler mTraverseHover = new ViewTraveler() {
        @Override
        public boolean needTraverse(ViewNode viewNode) {
            if (ViewHelper.isViewSelfVisible(viewNode.mView)) {
                Util.getVisibleRectOnScreen(viewNode.mView, mVisibleRectBuffer, viewNode.mFullScreen);
                return mVisibleRectBuffer.contains(mLastMovePoint.x, mLastMovePoint.y);
            }
            return false;
        }

        @Override
        public void traverseCallBack(ViewNode viewNode) {
            boolean isClickable = Util.isViewClickable(viewNode.mView);
            if (!isClickable && (viewNode.mInClickableGroup || TextUtils.isEmpty(viewNode.mViewContent))) {
                return;
            }
            mHitViewNodes.add(0, viewNode);
        }
    };

    // the second traverse, used to get all views under the topside
    private ViewTraveler mTraverseMask = new ViewTraveler() {

        @Override
        public void traverseCallBack(ViewNode viewNode) {
            boolean isClickable = Util.isViewClickable(viewNode.mView);
            if (!isClickable && (viewNode.mInClickableGroup || TextUtils.isEmpty(viewNode.mViewContent))) {
                return;
            }
            Util.getVisibleRectOnScreen(viewNode.mView, mVisibleRectBuffer, viewNode.mFullScreen);
            if (isFuzzyContainRect(mHitRect, mVisibleRectBuffer)) {
                final double scaledFactor = ScreenshotHelper.getScaledFactor();
                Screenshot screenshot = new Screenshot();
                screenshot.x = String.valueOf((int) (mVisibleRectBuffer.left * scaledFactor));
                screenshot.y = String.valueOf((int) (mVisibleRectBuffer.top * scaledFactor));
                screenshot.w = String.valueOf((int) (mVisibleRectBuffer.width() * scaledFactor));
                screenshot.h = String.valueOf((int) (mVisibleRectBuffer.height() * scaledFactor));
                viewNode.mScreenshot = screenshot;
                mHitViewNodes.add(0, viewNode);
            }
        }
    };

    private boolean isFuzzyContainRect(Rect parent, Rect child) {
        return parent.contains(child) && parent.width() - child.width() < MAX_OVERLAP_EDGE_DISTANCE
                && parent.height() - child.height() < MAX_OVERLAP_EDGE_DISTANCE;
    }

    private void findTopsideHitView() {
        mTopsideHitView = null;
        mHitRect = null;
        mHitViewNodes.clear();
        ViewHelper.traverseWindows(mWindowRootViews, mTraverseHover);
        updateMaskViewPosition();
    }

    private void updateMaskViewPosition() {
        if (mHitViewNodes.size() > 0) {
            mTopsideHitView = mHitViewNodes.get(0);
            mHitRect = new Rect();
            Util.getVisibleRectOnScreen(mTopsideHitView.mView, mHitRect, mTopsideHitView.mFullScreen);
            if (mTopsideHitView.mView instanceof WebView || ClassExistHelper.instanceOfX5WebView(mTopsideHitView.mView)) {
                mMaskView.setVisibility(GONE);
                mMagnifierView.setVisibility(GONE);
                int[] loc = new int[2];
                mTopsideHitView.mView.getLocationOnScreen(loc);
                hoverOn(mTopsideHitView.mView, xInScreen - loc[0], yInScreen - loc[1]);
                mShowingMaskInWebView = mTopsideHitView.mView;
            } else {
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) mMaskView.getLayoutParams();
                mMaskView.setVisibility(VISIBLE);
                if (mHitRect.left != params.x
                        || mHitRect.top != params.y
                        || mHitRect.width() != params.width
                        || mHitRect.height() != params.height) {
                    params.width = mHitRect.width();
                    params.height = mHitRect.height();
                    params.x = mHitRect.left;
                    params.y = mHitRect.top;

                    FloatWindowManager.getInstance().removeView(mMaskView);
                    FloatWindowManager.getInstance().addView(mMaskView, params);
                }
                mMagnifierView.showIfNeed(mHitRect, (int) (xInScreen - xInView + ANCHOR_VIEW_SIZE / 2), (int) (yInScreen - yInView + ANCHOR_VIEW_SIZE / 2));
            }
        } else {
            mMaskView.setVisibility(GONE);
            mMaskView.getLayoutParams().width = 0;
            mMagnifierView.setVisibility(GONE);
            hideMaskInWebView();
        }
    }

    public void hoverOn(View webView, float x, float y) {
        Util.callJavaScript(webView, "_vds_hybrid.hoverOn", x, y);
    }

    public void findElementAt(View webView) {
        Util.callJavaScript(webView, "_vds_hybrid.findElementAtPoint");
        mShowingMaskInWebView = webView;
    }

    public void hideMaskInWebView() {
        if (mShowingMaskInWebView != null) {
            Util.callJavaScript(mShowingMaskInWebView, "_vds_hybrid.cancelHover");
            mShowingMaskInWebView = null;
        }
    }

    private void showDialog(DialogFragment dialog, String tag) {
        getCircleManager().showDialog(dialog, tag);
    }

    private void hideCircleTipMask() {
        FloatWindowManager.getInstance().removeView(mCircleTipMaskView);
        mCircleTipMaskView = null;
        mConfig.setShowCircleTip(false);
    }

    public void remove() {
        FloatWindowManager.getInstance().removeView(this);
        FloatWindowManager.getInstance().removeView(mMaskView);
        if (mMagnifierView != null) {
            mMagnifierView.remove();
        }
        FloatWindowManager.getInstance().removeView(mCircleTipMaskView);
        if (mTagsView != null) {
            mTagsView.clearTags();
            FloatWindowManager.getInstance().removeView(mTagsView);
        }
    }

    public void setTags(List<Tag> tags) {
        mTagsView.setTags(tags);
    }

    public boolean isMoving() {
        return mIsInTouch;
    }
}
