package com.growingio.android.sdk.circle.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.growingio.android.sdk.debugger.event.ExitAndKillAppEvent;
import com.growingio.android.sdk.api.TagStore;
import com.growingio.android.sdk.base.event.HeatMapEvent;
import com.growingio.android.sdk.circle.CircleManager;
import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GrowingIO;
import com.growingio.android.sdk.pending.PendingStatus;
import com.growingio.android.sdk.utils.Util;
import com.growingio.eventcenter.EventCenter;

import static android.view.View.VISIBLE;

/**
 * Created by xyz on 15/11/2.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CircleModeChooserDialog extends DialogFragment {

    private boolean mHideCircleView = false;


    CircleManager getCircleManager() {
        return CircleManager.getInstance();
    }

    @SuppressLint("SetTextI18n")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.HIDE));
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context context = inflater.getContext();
        ViewGroup view = new ScrollView(context);
        view.setBackgroundColor(0xffefefef);
        LinearLayout outerLayout = new LinearLayout(context);
        view.addView(outerLayout);
        outerLayout.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(context);
        title.setText("圈选");
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setBackgroundColor(0xff34aadd);
        outerLayout.addView(title, ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 56));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        Switch showHeatMapSwitch = new Switch(context);
        showHeatMapSwitch.setText("开启热图");
        showHeatMapSwitch.setTextColor(0xff333333);
        showHeatMapSwitch.setTextSize(16);
        showHeatMapSwitch.setVisibility(VISIBLE);
        // TODO: 2018/7/14 这里应该是是否热图显示了
        showHeatMapSwitch.setChecked(PendingStatus.mIsHeatMapOn);
        showHeatMapSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                HeatMapEvent event;
                if (isChecked){
                    event = new HeatMapEvent(HeatMapEvent.EVENT_TYPE.STATE_ON);
                }else{
                    event = new HeatMapEvent(HeatMapEvent.EVENT_TYPE.STATE_OFF);
                }
                EventCenter.getInstance().post(event);
            }
        });

        Switch showCricledViewSwitch = new Switch(context);
        showCricledViewSwitch.setText("显示已圈选");
        showCricledViewSwitch.setTextColor(0xff333333);
        showCricledViewSwitch.setTextSize(16);
        showCricledViewSwitch.setVisibility(VISIBLE);
        showCricledViewSwitch.setChecked(CoreInitialize.config().shouldShowTags());
        showCricledViewSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                CircleManager.getInstance().setShowTags(isChecked);
                if(isChecked)
                    updateLastestCircledRecord();
            }
        });
        linearLayout.addView(showHeatMapSwitch, ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 60));
        linearLayout.addView(showCricledViewSwitch, ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 60));
        View divider = new View(context);
        divider.setBackgroundColor(0xffb9b9b9);
        RelativeLayout relativeLayout = new RelativeLayout(context);
        TextView circleTip = new TextView(context);
        circleTip.setText("提示: 拖动小红点进行圈选");
        circleTip.setTextColor(0xffa5a5a5);
        circleTip.setId(View.generateViewId());
        circleTip.setTextSize(12);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        relativeLayout.addView(circleTip, params);
        TextView version = new TextView(context);
        version.setTextSize(12);
        version.setTextColor(0xffa5a5a5);
        version.setText("版本: " + GrowingIO.getVersion());
        params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Util.dp2Px(context, 5);
        params.addRule(RelativeLayout.ALIGN_LEFT, circleTip.getId());
        params.addRule(RelativeLayout.BELOW, circleTip.getId());
        relativeLayout.addView(version, params);
        LinearLayout.LayoutParams relativeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        relativeParams.topMargin = Util.dp2Px(context, 10);
        relativeParams.bottomMargin = relativeParams.topMargin;
        linearLayout.addView(relativeLayout, relativeParams);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.leftMargin = Util.dp2Px(context, 15);
        contentParams.rightMargin = contentParams.leftMargin;
        outerLayout.addView(linearLayout, contentParams);

        divider = new View(context);
        divider.setBackgroundColor(0xffb9b9b9);
        outerLayout.addView(divider, ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 1));
        LinearLayout bottomLayout = new LinearLayout(context);
        bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
        outerLayout.addView(bottomLayout);
        TextView resume = new TextView(context);
        resume.setText("返回圈选");
        resume.setTextColor(0xff333333);
        resume.setGravity(Gravity.CENTER);
        TextView exit = new TextView(context);
        exit.setText("退出圈选");
        exit.setTextColor(0xff333333);
        exit.setGravity(Gravity.CENTER);
        divider = new View(context);
        divider.setBackgroundColor(0xffb9b9b9);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 48));
        bottomParams.weight = 1;
        bottomLayout.addView(resume, bottomParams);
        bottomLayout.addView(divider, new LinearLayout.LayoutParams(Util.dp2Px(context, 1), ViewGroup.LayoutParams.MATCH_PARENT));
        bottomParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.dp2Px(context, 48));
        bottomParams.weight = 1;
        bottomLayout.addView(exit, bottomParams);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CircleModeChooserDialog.this.dismiss();
                EventCenter.getInstance().post(new ExitAndKillAppEvent());

            }
        });
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CircleModeChooserDialog.this.dismiss();
                getCircleManager().addCircleView();
            }
        });
        return view;
    }

    private void updateLastestCircledRecord() {
        TagStore.getInstance().initial();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        getDialog().getWindow().setLayout((int) (dm.widthPixels - dm.density * 40), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (!mHideCircleView) {
            CircleManager.getInstance().addCircleView();
        }
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.SHOW));
    }

    @Override
    public void dismiss() {
        super.dismiss();
        EventCenter.getInstance().post(new HeatMapEvent(HeatMapEvent.EVENT_TYPE.SHOW));
    }
}
