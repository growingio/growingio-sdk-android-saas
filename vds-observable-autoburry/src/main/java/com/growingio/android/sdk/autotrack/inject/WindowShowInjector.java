package com.growingio.android.sdk.autotrack.inject;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.growingio.android.sdk.autoburry.VdsAgent;

/**
 * <p>
 *
 * @author cpacm 2023/8/21
 */
public class WindowShowInjector {
    public static void toastShow(Toast toast) {
        VdsAgent.showToast(toast);
    }

    public static void dialogShow(Dialog dialog) {
        VdsAgent.showDialog(dialog);
    }

    public static void timePickerDialogShow(TimePickerDialog dialog) {
        VdsAgent.showDialog(dialog);
    }

    public static void dialogFragmentShow(DialogFragment fragment, FragmentManager fm, String tag) {
        VdsAgent.showDialogFragment(fragment, fm, tag);
    }

    public static void dialogFragmentShowFt(DialogFragment fragment, FragmentTransaction ft, String tag) {
        VdsAgent.showDialogFragment(fragment, ft, tag);
    }

    public static void dialogFragmentSystemShow(android.app.DialogFragment fragment, android.app.FragmentManager fm, String tag) {
        VdsAgent.showDialogFragment(fragment, fm, tag);
    }

    public static void dialogFragmentSystemShowFt(android.app.DialogFragment fragment, android.app.FragmentTransaction ft, String tag) {
        VdsAgent.showDialogFragment(fragment, ft, tag);
    }

    public static void dialogFragmentV4Show(android.support.v4.app.DialogFragment fragment, android.support.v4.app.FragmentManager fm, String tag) {
        VdsAgent.showDialogFragment(fragment, fm, tag);
    }

    public static void dialogFragmentV4ShowFt(android.support.v4.app.DialogFragment fragment, android.support.v4.app.FragmentTransaction ft, String tag) {
        VdsAgent.showDialogFragment(fragment, ft, tag);
    }

    public static void popupMenuShow(PopupMenu menu) {
        VdsAgent.showPopupMenuX(menu);
    }

    public static void popupWindowShowAsDropDown(PopupWindow window, View view, int x, int y, int g) {
        VdsAgent.showAsDropDown(window, view, x, y, g);
    }

    public static void popupWindowShowAtLocation(PopupWindow window, View view, int g, int x, int y) {
        VdsAgent.showAtLocation(window, view, g, x, y);
    }

}
