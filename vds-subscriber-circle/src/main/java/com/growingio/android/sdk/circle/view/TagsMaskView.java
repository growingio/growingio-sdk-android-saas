package com.growingio.android.sdk.circle.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;

import com.growingio.android.sdk.autoburry.AutoBuryObservableInitialize;
import com.growingio.android.sdk.collection.Constants;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.models.Tag;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.FloatWindowManager;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.utils.ViewHelper;
import com.growingio.android.sdk.utils.WindowHelper;
import com.growingio.android.sdk.view.FloatViewContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lishaojie on 16/3/15.
 */
public class TagsMaskView extends FloatViewContainer {
    private int mFloatType;
    private List<ViewNode> mTagNodes;
    private ViewNode mSameListItemNode;
    private int mAnimationOffset = 0;

    public TagsMaskView(Context context) {
        super(context);
        mTagNodes = new ArrayList<ViewNode>();
    }

    @SuppressLint("RtlHardcoded")
    public void show() {
        if (getParent() != null) {
            setVisibility(VISIBLE);
        } else {
            int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            WindowManager.LayoutParams tagsParams = new WindowManager.LayoutParams(
                    ScreenshotHelper.getScreenShort(), ScreenshotHelper.getScreenLong(),
                    mFloatType, flags, PixelFormat.TRANSLUCENT);
            tagsParams.gravity = Gravity.TOP | Gravity.LEFT;
            tagsParams.setTitle("TagsWindow:" + getContext().getPackageName());
            FloatWindowManager.getInstance().addView(this,tagsParams);
//            ((WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).addView(this, tagsParams);
        }
    }

    private ViewTraveler mFindViewTraveler = new ViewTraveler() {
        float radius = Util.dp2Px(getContext(), 3);
        ShapeDrawable redBackground = new ShapeDrawable(new RoundRectShape(
                new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null
        ));

        ShapeDrawable yellowBackground = new ShapeDrawable(new RoundRectShape(
                new float[]{radius, radius, radius, radius, radius, radius, radius, radius}, null, null
        ));

        {
            redBackground.getPaint().setColor(Constants.GROWINGIO_COLOR_LIGHT_RED);
            redBackground.getPaint().setStrokeWidth(Util.dp2Px(getContext(), 1));
            redBackground.getPaint().setAntiAlias(true);
            yellowBackground.getPaint().setColor(Constants.GROWINGIO_COLOR_LIGHT_YELLOW);
            yellowBackground.getPaint().setStrokeWidth(Util.dp2Px(getContext(), 1));
            yellowBackground.getPaint().setAntiAlias(true);
        }

        @Override
        public void traverseCallBack(ViewNode viewNode) {
            if (mSameListItemNode != null) {
                if (mSameListItemNode.mParentXPath != null && mSameListItemNode.mParentXPath.equals(viewNode.mParentXPath)) {
                    addMask(viewNode);
                }
            } else {
                for (ViewNode tagNode : mTagNodes) {
                    if (match(tagNode, viewNode)) {
                        addMask(viewNode);
                    }
                }
            }
        }

        void addMask(ViewNode node) {
            TagMask mask = new TagMask(getContext());
            mask.setBackgroundDrawable(mSameListItemNode != null ? redBackground : yellowBackground);
            addView(mask);
            Rect rect = new Rect();
            Util.getVisibleRectOnScreen(node.mView, rect, node.mFullScreen);
            mask.updatePosition(rect);
            if (mSameListItemNode != null) {
                AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                alphaAnimation.setDuration(150);
                alphaAnimation.setStartOffset(mAnimationOffset++ * 30);
                mask.startAnimation(alphaAnimation);
            }
        }

        boolean match(ViewNode pattern, ViewNode node) {
            // 作为热图这里存在性能问题是可以接受的
            String patternXpath = pattern.mParentXPath == null ? "" : pattern.mParentXPath.toStringValue();
            String nodeXPath = node.mParentXPath == null ? "" : node.mParentXPath.toStringValue();
            return Util.isIdentifyXPath(patternXpath, nodeXPath)
                    && (pattern.mViewContent == null || pattern.mViewContent.equals(node.mViewContent))
                    && (pattern.mLastListPos == -2 || pattern.mLastListPos == node.mLastListPos);
        }
    };

    public void showSameListItemNode(ViewNode node) {
        mSameListItemNode = node;
        removeAllViews();
        setVisibility(VISIBLE);
        mAnimationOffset = 0;
        ViewHelper.traverseWindows(WindowHelper.getWindowViews(), mFindViewTraveler);
    }

    public void hideSameListItemNode() {
        setVisibility(GONE);
        mSameListItemNode = null;
        removeAllViews();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }

    public void setTags(List<Tag> tags) {
        clearTags();
        if (tags == null || tags.size() == 0 || !PendingStatus.mCanShowCircleTag) return;
        String spn = CoreInitialize.coreAppState().getSPN();
        for (Tag tag : tags) {
            if (tag.eventType.equals("elem") && spn.equals(tag.filter.domain))
                addTag(tag);
        }
        if (mTagNodes.size() > 0) {
            ViewHelper.traverseWindows(WindowHelper.getWindowViews(), mFindViewTraveler);
        }
    }

    public void addTag(Tag tag) {
        Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
        if (activity != null) {
            String currentPage = AutoBuryObservableInitialize.autoBuryAppState().getPageName(activity);
            if (tag.filter.path == null || tag.filter.path.length() == 0 || tag.filter.path.equals(currentPage)) {
                ViewNode node = new ViewNode();
                if (!TextUtils.isEmpty(tag.filter.content)) {
                    node.mViewContent = tag.filter.content;
                }
                node.mHasListParent = !TextUtils.isEmpty(tag.filter.index);
                if (node.mHasListParent) {
                    try {
                        node.mLastListPos = Integer.valueOf(tag.filter.index);
                    } catch (NumberFormatException ignored) {
                        node.mLastListPos = -2;
                    }
                } else {
                    node.mLastListPos = -2;
                }
                node.mParentXPath = LinkedString.fromString(tag.filter.xpath);
                mTagNodes.add(node);
            }
        }
    }

    public void clearTags() {
        mTagNodes.clear();
        removeAllViews();
    }

    public void setFloatType(int mFloatType) {
        this.mFloatType = mFloatType;
    }

}
