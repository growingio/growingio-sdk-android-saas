package com.growingio.android.sdk.autoburry.util;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewParent;

import com.growingio.android.sdk.autoburry.SuperViewPager;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.ViewHelper;

/**
 * 关于Fragment的工具类， 包括系统自带与v4包中的
 * Created by liangdengke on 2018/4/8.
 */
public final class FragmentUtil {

    private FragmentUtil(){}

    // 判断Fragment是否可见， 递归其父Fragment是否可见
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static boolean isVisible(@NonNull Fragment fragment){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return fragment.isVisible();
        }

        Fragment current = fragment;
        while (current != null){
            View view = current.getView();
            if (!current.isVisible() || !ViewHelper.viewVisibilityInParents(view))
                return false;
            current = current.getParentFragment();
        }
        return true;
    }

    public static boolean isAndroidXVisible(androidx.fragment.app.Fragment fragment){
        androidx.fragment.app.Fragment current = fragment;
        while (current != null){
            if (!current.isVisible() || !ViewHelper.viewVisibilityInParents(current.getView()))
                return false;
            View view = current.getView();
            ViewParent parent = view.getParent();
            if (ClassExistHelper.instanceOfAndroidXViewPager(parent)){
                androidx.viewpager.widget.ViewPager viewPager = (androidx.viewpager.widget.ViewPager) parent;
                Object target = new SuperViewPager.AndroidXViewPager(viewPager).getCurrentItemObj();
                if (target != current){
                    return false;
                }
            }
            current = current.getParentFragment();
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static boolean isVisible(@NonNull android.support.v4.app.Fragment fragment){
        android.support.v4.app.Fragment current = fragment;
        while (current != null){
            if (!current.isVisible() || !ViewHelper.viewVisibilityInParents(current.getView())) return false;
            View view = current.getView();
            ViewParent parent = view.getParent();
            if (ClassExistHelper.instanceOfSupportViewPager(parent)){
                ViewPager viewPager = (ViewPager) parent;
                Object target = new SuperViewPager.V4ViewPager(viewPager).getCurrentItemObj();
                if (target != current) return false;
            }
            current = current.getParentFragment();
        }
        return true;
    }

    public static boolean isParentX(@NonNull androidx.fragment.app.Fragment parentFragment,
                                    @NonNull androidx.fragment.app.Fragment childFragment){
        androidx.fragment.app.Fragment current = childFragment.getParentFragment();
        while (current != null){
            if (current == parentFragment)
                return true;
            current = current.getParentFragment();
        }
        return false;
    }

    /**
     * @return true if parentFragment is an ancestor of childFragment
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static boolean isParent(@NonNull Fragment parentFragment, @NonNull Fragment childFragment){
        Fragment current = childFragment.getParentFragment();
        while (current != null){
            if (current == parentFragment) return true;
            current = current.getParentFragment();
        }
        return false;
    }

    public static boolean isParent(@NonNull android.support.v4.app.Fragment parentFragment,
                                   @NonNull android.support.v4.app.Fragment childFragment){
        android.support.v4.app.Fragment current = childFragment.getParentFragment();
        while (current != null){
            if (current == parentFragment) return true;
            current = current.getParentFragment();
        }
        return false;
    }

}
