package com.growingio.android.sdk.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RadioGroup;

import com.growingio.android.sdk.base.event.DiagnoseEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.collection.MessageProcessor;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.models.ViewNode;
import com.growingio.android.sdk.models.ViewTraveler;
import com.growingio.android.sdk.view.FloatViewContainer;
import com.growingio.eventcenter.bus.EventBus;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xyz on 15/11/3.
 */
public class ViewHelper {
    private final static String TAG = "GIO.ViewHelper";


    public static boolean isWindowNeedTraverse(View root, String prefix, boolean skipOtherActivity) {
        if (root.hashCode() == CoreInitialize.coreAppState().getCurrentRootWindowsHashCode())
            return true;
        return !(root instanceof FloatViewContainer
                || !(root instanceof ViewGroup)
                || (skipOtherActivity
                && (root.getWindowVisibility() == View.GONE
                || root.getVisibility() != View.VISIBLE
                || TextUtils.equals(prefix, WindowHelper.getMainWindowPrefix())
                || root.getWidth() == 0
                || root.getHeight() == 0))
        );
    }

    public static int getMainWindowCount(View[] windowRootViews) {
        int mainWindowCount = 0;
        WindowHelper.init();
        for (View windowRootView : windowRootViews) {
            if (windowRootView == null) continue;
            String prefix = WindowHelper.getWindowPrefix(windowRootView);
            mainWindowCount += prefix.equals(WindowHelper.getMainWindowPrefix()) ? 1 : 0;
        }
        return mainWindowCount;
    }

    public static void traverseWindows(View[] windowRootViews, ViewTraveler traverseHover) {
        boolean skipOtherActivity = getMainWindowCount(windowRootViews) > 1;
        WindowHelper.init();
        try {
            for (View windowRootView : windowRootViews) {
                View root = windowRootView;
                String prefix = WindowHelper.getWindowPrefix(root);
                if (isWindowNeedTraverse(root, prefix, skipOtherActivity)) {
                    traverseWindow(root, prefix, traverseHover);
                }
            }
        } catch (OutOfMemoryError ignored) {
            EventBus.getDefault().post(new DiagnoseEvent("oomt"));
        }
    }

    public static void traverseWindow(View rootView, String windowPrefix, ViewTraveler callBack) {
        if (rootView == null) {
            return;
        }
        int[] offset = new int[2];
        rootView.getLocationOnScreen(offset);
        boolean fullscreen = (offset[0] == 0 && offset[1] == 0);
        ViewNode rootNode = new ViewNode(rootView, 0, -1, Util.isListView(rootView), fullscreen, false, false,
                LinkedString.fromString(windowPrefix),
                LinkedString.fromString(windowPrefix), windowPrefix, callBack);
        Object inheritableObject = rootView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
        if (inheritableObject instanceof String) {
            rootNode.mInheritableGrowingInfo = (String) inheritableObject;
        }
        if (rootNode.isNeedTrack()) {
            // TODO: 2018/12/17 没太明白这里是为了防止什么， 不敢改
            if (!WindowHelper.isDecorView(rootView)) {
                rootNode.traverseViewsRecur();
            } else {
                rootNode.traverseChildren();
            }
        }
    }

    public static ViewNode getClickViewNode(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        WindowHelper.init();
        View[] windows = WindowHelper.getWindowViews();
        try {
            for (View window : windows) {
                if (window.getClass() == WindowHelper.sPopupWindowClazz) {
                    View menuView = findMenuItemView(window, menuItem);
                    if (menuView != null) {
                        return getClickViewNode(menuView);
                    }
                }
            }
            for (View window : windows) {
                if (window.getClass() != WindowHelper.sPopupWindowClazz) {
                    View menuView = findMenuItemView(window, menuItem);
                    if (menuView != null) {
                        return getClickViewNode(menuView);
                    }
                }
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ViewNode getClickViewNode(View view) {
        if (!CoreInitialize.config().isEnabled() || view == null) {
            return null;
        }
        Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
        if (activity == null || Util.isIgnoredView(view)) {
            return null;
        }

        ViewNode viewNode = getViewNode(view, sClickTraveler);

        if (viewNode == null) {
            return null;
        }

        sClickTraveler.resetActionStructList();
        sClickTraveler.traverseCallBack(viewNode);
        viewNode.traverseChildren();

        return viewNode;
    }
    
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static boolean isViewSelfVisible(View mView) {
        if (mView == null || mView.getWindowVisibility() == View.GONE) {
            return false;
        }

        // home键back后, DecorView的visibility是 INVISIBLE, 即onResume时Window并不可见, 对GIO而言此时是可见的
        if (WindowHelper.isDecorView(mView))
            return true;

        if (!(mView.getWidth() > 0
                && mView.getHeight() > 0
                && mView.getAlpha() > 0
                && mView.getLocalVisibleRect(new Rect()))) {
            return false;
        }
    
        //动画导致用户可见但是仍然 invisible,
        if (mView.getVisibility() != View.VISIBLE
                && mView.getAnimation() != null
                && mView.getAnimation().getFillAfter()) {
                return true;
        } else return mView.getVisibility() == View.VISIBLE;
    
    }
    
    
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static boolean viewVisibilityInParents(View view) {
        if (view == null)
            return false;
        
        if (!isViewSelfVisible(view))
            return false;
        
        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            if (isViewSelfVisible((View) viewParent)) {
                viewParent = viewParent.getParent();
                if (viewParent == null) {
                    // LogUtil.d(TAG, "Hit detached view: ", viewParent);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
    

    public static ActionEvent getClickActionEvent(ViewNode viewNode) {
        if (viewNode == null || viewNode.mView == null) {
            return null;
        }

        if (!CoreInitialize.config().isEnabled()) {
            return null;
        }
        Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
        if (activity == null || Util.isIgnoredView(viewNode.mView)) {
            return null;
        }

        ActionEvent click = ActionEvent.makeClickEvent();
        MessageProcessor messageProcessor = CoreInitialize.messageProcessor();
        click.mPageName = messageProcessor.getPageNameWithPending();
        click.elems = sClickTraveler.actionStructList;
        click.setPageTime(messageProcessor.getPTMWithPending());

        return click;
    }

    public static void persistClickEvent(ActionEvent click, ViewNode viewNode) {
        EventBus.getDefault().post(click);
    }

    private static boolean shouldChangeOn(View view, ViewNode viewNode) {
        if (view instanceof EditText) {
            Object tag = view.getTag(GrowingIO.GROWING_MONITORING_FOCUS_KEY);
            String lastText = tag == null ? "" : tag.toString();
            String nowText = ((EditText) view).getText().toString();
            if ((TextUtils.isEmpty(nowText) && TextUtils.isEmpty(lastText)) || lastText.equals(nowText)) {
                return false;
            }
            view.setTag(GrowingIO.GROWING_MONITORING_FOCUS_KEY, nowText);
            return true;
        }
        return false;
    }

    public static void changeOn(View view) {
        if (!GConfig.sCanHook || !CoreInitialize.config().isEnabled()) {
            return;
        }
        Activity activity = ActivityUtil.findActivity(view.getContext());
        if (activity == null || Util.isIgnoredView(view)) {
            return;
        }

        ViewNode viewNode = getViewNode(view, changeTraveler);

        if (viewNode == null) {
            return;
        }

        if (!shouldChangeOn(view, viewNode)) {
            return;
        }

        changeTraveler.resetActionStructList();
        changeTraveler.traverseCallBack(viewNode);

        ActionEvent change = ActionEvent.makeChangeEvent();
        change.mPageName = CoreInitialize.messageProcessor().getPageNameWithPending();
        change.elems = changeTraveler.actionStructList;
        change.setPageTime(CoreInitialize.messageProcessor().getPTMWithPending());
        EventBus.getDefault().post(change);
    }

    public static ViewNode getViewNode(View view, @Nullable ViewTraveler viewTraveler) {
        ArrayList<View> viewTreeList = new ArrayList<View>(8);
        ViewParent parent = view.getParent();
        viewTreeList.add(view);
        /*
         * view:
         * "AppCompactButton[2]#login_btn
         * parents:
         * ["LinearLayout[3]#login_container" ,"RelativeLayout[1]", ,"FrameLayout[0]#content", "PhoneWindow$DecorView"]
         */
        while (parent instanceof ViewGroup) {
            if (Util.isIgnoredView((View) parent)) {
                return null;
            }
            viewTreeList.add((ViewGroup) parent);
            parent = parent.getParent();
        }

        int endIndex = viewTreeList.size() - 1;
        View rootView = viewTreeList.get(endIndex);
        WindowHelper.init();

        String bannerText = null;
        String inheritableObjInfo = null;

        int viewPosition = 0;
        int listPos = -1;
        boolean mHasListParent = false;
        boolean mParentIdSettled = false;
        String prefix = WindowHelper.getWindowPrefix(rootView);
        String opx = prefix;
        String px = prefix;

        if (!WindowHelper.isDecorView(rootView) && !(rootView.getParent() instanceof View)) {
            opx += "/" + Util.getSimpleClassName(rootView.getClass());
            px = opx;
            if (GConfig.USE_ID) {
                String id = Util.getIdName(rootView, mParentIdSettled);
                if (id != null) {
                    if (rootView.getTag(GrowingIO.GROWING_VIEW_ID_KEY) != null) {
                        mParentIdSettled = true;
                    }
                    opx += "#" + id;
                    px += "#" + id;
                }
            }
        }
        Object inheritableObject = rootView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
        if (inheritableObject != null && inheritableObject instanceof String) {
            inheritableObjInfo = (String) inheritableObject;
        }
        if (rootView instanceof ViewGroup) {
            ViewGroup parentView = (ViewGroup) rootView;
            for (int i = endIndex - 1; i >= 0; i--) {
                viewPosition = 0;
                View childView = viewTreeList.get(i);
                Object viewName = childView.getTag(GrowingIO.GROWING_VIEW_NAME_KEY);
                if (viewName != null) {
                    opx = "/" + viewName;
                    px += "/" + viewName;
                } else {
                    viewName = Util.getSimpleClassName(childView.getClass());
                    viewPosition = parentView.indexOfChild(childView);
                    if (ClassExistHelper.instanceOfAndroidXViewPager(parentView)){
                        viewPosition = ((androidx.viewpager.widget.ViewPager)parentView).getCurrentItem();
                        mHasListParent = true;
                    }else if (ClassExistHelper.instanceOfSupportViewPager(parentView)) {
                        viewPosition = ((ViewPager) parentView).getCurrentItem();
                        mHasListParent = true;
                    } else if (parentView instanceof AdapterView) {
                        AdapterView listView = (AdapterView) parentView;
                        viewPosition = listView.getFirstVisiblePosition() + viewPosition;
                        mHasListParent = true;
                    } else if (ClassExistHelper.instanceOfRecyclerView(parentView)) {
                        int adapterPosition = getChildAdapterPositionInRecyclerView(childView, parentView);
                        if (adapterPosition >= 0) {
                            mHasListParent = true;
                            viewPosition = adapterPosition;
                        }
                    }
                    if (parentView instanceof ExpandableListView) {
                        ExpandableListView listParent = (ExpandableListView) parentView;
                        long elp = listParent.getExpandableListPosition(viewPosition);
                        if (ExpandableListView.getPackedPositionType(elp) == ExpandableListView.PACKED_POSITION_TYPE_NULL) {
                            if (viewPosition < listParent.getHeaderViewsCount()) {
                                opx = opx + "/ELH[" + viewPosition + "]/" + viewName + "[0]";
                                px = px + "/ELH[" + viewPosition + "]/" + viewName + "[0]";
                            } else {
                                int footerIndex = viewPosition - (listParent.getCount() - listParent.getFooterViewsCount());
                                opx = opx + "/ELF[" + footerIndex + "]/" + viewName + "[0]";
                                px = px + "/ELF[" + footerIndex + "]/" + viewName + "[0]";
                            }
                        } else {
                            int groupIdx = ExpandableListView.getPackedPositionGroup(elp);
                            int childIdx = ExpandableListView.getPackedPositionChild(elp);
                            if (childIdx != -1) {
                                listPos = childIdx;
                                px = opx + "/ELVG[" + groupIdx + "]/ELVC[-]/" + viewName + "[0]";
                                opx = opx + "/ELVG[" + groupIdx + "]/ELVC[" + childIdx + "]/" + viewName + "[0]";
                            } else {
                                listPos = groupIdx;
                                px = opx + "/ELVG[-]/" + viewName + "[0]";
                                opx = opx + "/ELVG[" + groupIdx + "]/" + viewName + "[0]";
                            }
                        }
                    } else if (Util.isListView(parentView)) {
                        // 处理有特殊的position的元素
                        Object bannerTag = parentView.getTag(GrowingIO.GROWING_BANNER_KEY);
                        if (bannerTag != null && bannerTag instanceof List && ((List) bannerTag).size() > 0) {
                            viewPosition = Util.calcBannerItemPosition((List) bannerTag, viewPosition);
                            bannerText = Util.truncateViewContent(String.valueOf(((List) bannerTag).get(viewPosition)));
                        }
                        listPos = viewPosition;
                        px = opx + "/" + viewName + "[-]";
                        opx = opx + "/" + viewName + "[" + listPos + "]";
                    } else if (ClassExistHelper.instanceofAndroidXSwipeRefreshLayout(parentView)
                            || ClassExistHelper.instanceOfSupportSwipeRefreshLayout(parentView)) {
                        opx = opx + "/" + viewName + "[0]";
                        px = px + "/" + viewName + "[0]";
                    } else {
                        opx = opx + "/" + viewName + "[" + viewPosition + "]";
                        px = px + "/" + viewName + "[" + viewPosition + "]";
                    }
                    if (GConfig.USE_ID) {
                        String id = Util.getIdName(childView, mParentIdSettled);
                        if (id != null) {
                            if (childView.getTag(GrowingIO.GROWING_VIEW_ID_KEY) != null) {
                                mParentIdSettled = true;
                            }
                            opx += "#" + id;
                            px += "#" + id;
                        }
                    }
                }

                inheritableObject = childView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
                if (childView instanceof RadioGroup) {
                    RadioGroup radioGroup = (RadioGroup) childView;
                    View theView = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                    if (theView != null) {
                        String childInheritableGrowingInfo = (String) theView.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
                        if (!TextUtils.isEmpty(childInheritableGrowingInfo))
                            inheritableObject = childInheritableGrowingInfo;
                    }
                }
                if (inheritableObject instanceof String) {
                    inheritableObjInfo = (String) inheritableObject;
                }
                if (childView instanceof ViewGroup) {
                    parentView = (ViewGroup) childView;
                } else {
                    break;
                }
            }
        }

        inheritableObject = view.getTag(GrowingIO.GROWING_INHERIT_INFO_KEY);
        if (inheritableObject instanceof String) {
            inheritableObjInfo = (String) inheritableObject;
        }

        ViewNode viewNode = new ViewNode(view, viewPosition, listPos, mHasListParent, prefix.equals(WindowHelper.getMainWindowPrefix()), true, mParentIdSettled,
                LinkedString.fromString(opx),
                LinkedString.fromString(px), prefix, viewTraveler);
        viewNode.mViewContent = Util.getViewContent(view, bannerText);
        viewNode.mInheritableGrowingInfo = inheritableObjInfo;
        viewNode.mClickableParentXPath = LinkedString.fromString(px);
        viewNode.mBannerText = bannerText;

        return viewNode;
    }

    public static int getChildAdapterPositionInRecyclerView(View childView, ViewGroup parentView) {
        if (ClassExistHelper.instanceOfAndroidXRecyclerView(parentView)){
            return ((androidx.recyclerview.widget.RecyclerView)parentView).getChildAdapterPosition(childView);
        }else if (ClassExistHelper.instanceOfSupportRecyclerView(parentView)){
            // For low version RecyclerView
            try{
                return ((RecyclerView) parentView).getChildAdapterPosition(childView);
            }catch (Throwable e){
                return ((RecyclerView) parentView).getChildPosition(childView);
            }

        } else if (ClassExistHelper.sHasCustomRecyclerView) {
            return ClassExistHelper.invokeCRVGetChildAdapterPositionMethod(parentView, childView);
        }
        return -1;
    }

    private static class ViewNodeTraveler extends ViewTraveler {
        private long currentTime;
        private ArrayList<ActionStruct> actionStructList = new ArrayList<ActionStruct>();

        public void resetActionStructList() {
            currentTime = System.currentTimeMillis();
            actionStructList.clear();
        }

        @Override
        public void traverseCallBack(ViewNode viewNode) {
            if (actionStructList != null) {
                ActionStruct struct = new ActionStruct();
                struct.xpath = viewNode.mParentXPath;
                struct.content = viewNode.mViewContent;
                struct.index = viewNode.mLastListPos;
                struct.time = currentTime;
                struct.obj = viewNode.mInheritableGrowingInfo;
                actionStructList.add(struct);
            }
        }
    }

    private static ViewNodeTraveler changeTraveler = new ViewNodeTraveler();
    private static ViewNodeTraveler sClickTraveler = new ViewNodeTraveler() {
        @Override
        public boolean needTraverse(ViewNode viewNode) {
            return super.needTraverse(viewNode) && !Util.isViewClickable(viewNode.mView);
        }
    };

    private static View findMenuItemView(View view, MenuItem item) throws InvocationTargetException, IllegalAccessException {
        if (WindowHelper.getMenuItemData(view) == item) {
            return view;
        } else if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View menuView = findMenuItemView(((ViewGroup) view).getChildAt(i), item);
                if (menuView != null) {
                    return menuView;
                }
            }
        }
        return null;
    }

    public static boolean isContentView(Activity activity, View view) {
        if (activity == null || view == null || view.getContext() == null)
            return false;
        return ActivityUtil.findActivity(view.getContext()) == activity;
    }

}
