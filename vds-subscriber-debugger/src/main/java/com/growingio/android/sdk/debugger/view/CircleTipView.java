package com.growingio.android.sdk.debugger.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.growingio.android.sdk.base.event.message.MessageEvent;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;
import com.growingio.android.sdk.debugger.event.ExitAndKillAppEvent;
import com.growingio.android.sdk.models.ActionEvent;
import com.growingio.android.sdk.models.ActionStruct;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.FloatWindowManager;
import com.growingio.android.sdk.utils.LinkedString;
import com.growingio.android.sdk.utils.ScreenshotHelper;
import com.growingio.android.sdk.utils.ThreadUtils;
import com.growingio.android.sdk.utils.Util;
import com.growingio.android.sdk.view.FloatViewContainer;
import com.growingio.eventcenter.EventCenter;

import java.util.Collections;

/**
 * author CliffLeopard
 * time   2017/9/6:下午4:22
 * email  gaoguanling@growingio.com
 */

public abstract class CircleTipView extends FloatViewContainer {

    private float yInView;
    private float yInScreen;
    private float yDownInScreen;
    private int mMinMoveDistance;
    private TextView mContent;
    private TextView mDragTip;
    private static int sYOffset = 0;
    private boolean isError = false;

    public abstract String getStrDialogTittle();

    public abstract String getStrDialogCancel();

    public abstract String getStrDialogOk();

    public abstract void doing();

    private String getStrDialogContent(){
        return "APP版本:   " + CoreInitialize.config().getAppVersion() + "\n" +
                "SDK版本:   " + GConfig.AGENT_VERSION + "\n";
    }

    protected CircleTipView(Context context) {
        super(context);
        init();
    }

    public void setError(boolean error) {
        isError = error;
        if (error){
            mDragTip.clearAnimation();
            mContent.clearAnimation();
            mDragTip.setVisibility(View.GONE);
            mContent.setGravity(Gravity.CENTER);
            setContent("设备已断开连接, 请重新连接或" + getStrDialogOk());
            setBackgroundColor(Color.parseColor("#fa6244"));
        }else{
            mDragTip.clearAnimation();
            mContent.clearAnimation();
            mDragTip.setVisibility(View.VISIBLE);
            mContent.setGravity(Gravity.LEFT);
            setBackgroundColor(0xFF0090FF);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                yDownInScreen = event.getRawY();
                yInScreen = yDownInScreen;
                yInView = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                yInScreen = event.getRawY();
                if (Math.abs(yInScreen - yDownInScreen) < mMinMoveDistance) {
                    break;
                }
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) this.getLayoutParams();
                params.y = (int) (yInScreen - yInView);
                FloatWindowManager.getInstance().updateViewLayout(this, params);
                handled = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                sYOffset = (int) yInScreen;
                if (Math.abs(yInScreen - yDownInScreen) < mMinMoveDistance) {
                    performClick();
                }
                break;
            default:
                break;
        }
        return handled;
    }

    @SuppressLint("SetTextI18n")
    protected void init() {

        mContent = new TextView(getContext());
        mDragTip = new TextView(getContext());
        mDragTip.setGravity(Gravity.RIGHT);
        mContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mDragTip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        int padding = Util.dp2Px(getContext(), 4);
        int paddingVertical = Util.dp2Px(getContext(), 6);
        int paddingHorizontal = Util.dp2Px(getContext(), 8);
        mContent.setPadding(paddingHorizontal, padding, paddingHorizontal, padding);
        mDragTip.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        mContent.setTextColor(0xffffffff);
        mDragTip.setTextColor(0xffffffff);
        mDragTip.setText("如有遮挡请拖动此条");
        addView(mContent, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(mDragTip, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setBackgroundColor(0xFF0090FF);
        mMinMoveDistance = Util.dp2Px(getContext(), 10);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = CoreInitialize.coreAppState().getForegroundActivity();
                if (activity == null)
                    return;
                AlertDialog dialog = onCreateDialog(activity);
                dialog.show();
                CoreInitialize.coreAppState().onGIODialogShow(activity, dialog);

                ThreadUtils.postOnUiThreadDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hardCodeSaveClick();
                        EventCenter.getInstance().post(new MessageEvent(MessageEvent.MessageType.IMP));
                    }
                }, 300);
            }
        });
    }


    protected AlertDialog onCreateDialog(Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if (isError){
            builder.setTitle(Html.fromHtml("<font color='#f35657'>设备已断开连接</font>"));
            builder.setMessage("当前设备已于Web端断开连接， 如需" + getStrDialogCancel() + "请扫码重新连接\n");
        }else{
            builder.setTitle(Html.fromHtml("<font color='#212121'>" + getStrDialogTittle() + "</font>"));
            builder.setMessage(getStrDialogContent());
            builder.setNegativeButton(Html.fromHtml("<font color='#7c7c7c'>" + getStrDialogCancel() + "</font>"), null);
        }

        builder.setPositiveButton(Html.fromHtml("<font color='#f35657'>" + getStrDialogOk() + "</font>"),
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    EventCenter.getInstance().post(new ExitAndKillAppEvent());
                }
        });
        return builder.create();
    }

    @SuppressLint("RtlHardcoded")
    public void show() {
        if (getParent() != null) {
            setVisibility(VISIBLE);
        } else {
            int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    PendingStatus.FLOAT_VIEW_TYPE, flags, PixelFormat.TRANSLUCENT);
            layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
            if (sYOffset == 0) {
                layoutParams.y = getStatusBarHeight();
            } else {
                layoutParams.y = sYOffset;
            }
            FloatWindowManager.getInstance().addView(this, layoutParams);
        }
        setKeepScreenOn(true);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void remove() {
        FloatWindowManager.getInstance().removeView(this);
        setKeepScreenOn(false);
    }

    public void setContent(String content) {
        mContent.setText(content);
    }

    private void hardCodeSaveClick() {
        ActionStruct actionStruct = new ActionStruct();
        actionStruct.xpath = LinkedString.fromString("GioWindow/FloatViewContainer[0]/TextView[0]");
        actionStruct.time = System.currentTimeMillis();
        actionStruct.content = mContent.getText().toString();

        ActionEvent actionEvent = ActionEvent.makeClickEvent();
        actionEvent.mPageName = "GIOActivity";
        actionEvent.setPageTime(actionStruct.time);
        actionEvent.elems = Collections.singletonList(actionStruct);
        CoreInitialize.messageProcessor().persistEvent(actionEvent);
    }
}
