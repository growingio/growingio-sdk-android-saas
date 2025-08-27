package com.growingio.android.sdk.autoburry;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.growingio.android.sdk.autoburry.util.FragmentUtil;
import com.growingio.android.sdk.utils.ActivityUtil;
import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.ObjectUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Super Fragment which represent Fragment, V4Fragment, View
 * - can resume
 * - check visible
 *
 * 可以预见的将来， 可以将Activity也包含在SuperFragment内
 * Created by liangdengke on 2018/7/13.
 */
public abstract class SuperFragment<T> {

    /* 代表Fragment包含的原始对象: Fragment, V4Fragment, View等 */
    private final WeakReference<T> fragmentRef;

    protected SuperFragment(T fragment) {
        this.fragmentRef = new WeakReference<>(fragment);
    }

    public T getFragment(){
        return fragmentRef.get();
    }

    @Nullable
    public View getView(){
        return null;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hashCode(fragmentRef.get());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || ((obj instanceof SuperFragment) && ObjectUtils.equals(fragmentRef, ((SuperFragment) obj).fragmentRef));
    }

    @Override
    public String toString() {
        return ObjectUtils.toString(getFragment());
    }

    /**
     * check 此SuperFragment是否隶属于Activity
     * @return true -- if SuperFragment属于Activity
     */
    public boolean isBelongActivity(@Nullable Activity activity){
        return false;
    }

    @Nullable
    public Activity getActivity(){
        return null;
    }

    @Nullable
    public static Activity getActivityFromFragment(Object fragment){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (fragment instanceof Fragment){
                return ((Fragment) fragment).getActivity();
            }else if (ClassExistHelper.instanceOfAndroidXFragment(fragment)){
                return ((androidx.fragment.app.Fragment)fragment).getActivity();
            }else if (ClassExistHelper.instanceOfSupportFragment(fragment)){
                return ((android.support.v4.app.Fragment)fragment).getActivity();
            }
        }
        return null;
    }

    /**
     * 判断界面是否可见
     */
    public abstract boolean isVisible();

    public List<SuperFragment> getChildren(){
        return Collections.emptyList();
    }

    public static SuperFragment createSuperFragment(Object fragment){
        if (fragment instanceof Fragment){
            return new SystemFragment((Fragment) fragment);
        }else if (ClassExistHelper.instanceOfAndroidXFragment(fragment)){
            return new AndroidXFragment((androidx.fragment.app.Fragment) fragment);
        }else if (ClassExistHelper.instanceOfSupportFragment(fragment)){
            return new V4Fragment((android.support.v4.app.Fragment) fragment);
        }else if (fragment instanceof View){
            return new ViewFragment((View) fragment);
        }
        throw new IllegalArgumentException("fragment only support Fragment, V4Fragment and View");
    }

    public static class SystemFragment extends SuperFragment<Fragment>{

        private SystemFragment(Fragment fragment) {
            super(fragment);
        }

        @Override
        public boolean isVisible() {
            Fragment fragment = getFragment();
            return fragment != null && FragmentUtil.isVisible(fragment);
        }

        @Override
        public List<SuperFragment> getChildren() {
            Fragment fragment = getFragment();
            if (fragment == null)
                return super.getChildren();
            List<Fragment> fragmentList = getChildFragments(fragment);
            List<SuperFragment> result = new ArrayList<>();
            for (Fragment child: fragmentList){
                result.add(SuperFragment.createSuperFragment(child));
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        private static List<Fragment> getChildFragments(Fragment fragment) {
            FragmentManager manager = fragment.getChildFragmentManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                return manager.getFragments();
            } else if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    Field activeField = manager.getClass().getDeclaredField("mActive");
                    activeField.setAccessible(true);
                    return (List<Fragment>) activeField.get(manager);
                } catch (Exception e) {
                    Log.w("SuperFragment", "getChildFragments failed. " + e.getMessage());
                }
            }
            return Collections.emptyList();
        }

        @Override
        public boolean isBelongActivity(Activity activity) {
            Fragment fragment = getFragment();
            return fragment != null && fragment.getActivity() == activity;
        }

        @Nullable
        @Override
        public Activity getActivity() {
            Fragment fragment = getFragment();
            return fragment == null ? null : fragment.getActivity();
        }

        @Nullable
        @Override
        public View getView() {
            Fragment fragment = getFragment();
            return fragment == null ? null : fragment.getView();
        }
    }

    public static class V4Fragment extends SuperFragment<android.support.v4.app.Fragment>{

        private V4Fragment(android.support.v4.app.Fragment fragment) {
            super(fragment);
        }

        @Override
        public boolean isVisible() {
            android.support.v4.app.Fragment fragment = getFragment();
            return fragment != null && FragmentUtil.isVisible(fragment);
        }

        @Override
        public List<SuperFragment> getChildren() {
            android.support.v4.app.Fragment fragment = getFragment();
            if (fragment == null){
                return super.getChildren();
            }
            List<android.support.v4.app.Fragment> fragmentList = fragment.getChildFragmentManager().getFragments();
            List<SuperFragment> result = new ArrayList<>();
            for (android.support.v4.app.Fragment child : fragmentList){
                result.add(SuperFragment.createSuperFragment(child));
            }
            return result;
        }

        @Override
        public boolean isBelongActivity(Activity activity) {
            android.support.v4.app.Fragment fragment = getFragment();
            return fragment != null && fragment.getActivity() == activity;
        }

        @Nullable
        @Override
        public View getView() {
            android.support.v4.app.Fragment fragment = getFragment();
            return fragment == null ? null :  fragment.getView();
        }

        @Nullable
        @Override
        public Activity getActivity() {
            android.support.v4.app.Fragment fragment = getFragment();
            return fragment == null ? null : fragment.getActivity();
        }
    }

    public static class AndroidXFragment extends SuperFragment<androidx.fragment.app.Fragment>{

        protected AndroidXFragment(androidx.fragment.app.Fragment fragment) {
            super(fragment);
        }

        @Override
        public boolean isVisible() {
            androidx.fragment.app.Fragment fragment = getFragment();
            return fragment != null && FragmentUtil.isAndroidXVisible(fragment);
        }

        @Override
        public boolean isBelongActivity(@Nullable Activity activity) {
            return getActivity() == activity;
        }

        @Nullable
        @Override
        public Activity getActivity() {
            androidx.fragment.app.Fragment fragment = getFragment();
            return fragment == null ? null: fragment.getActivity();
        }
    }

    public static class ViewFragment extends SuperFragment<View>{

        private ViewFragment(View fragment) {
            super(fragment);
        }

        @Override
        public boolean isVisible() {
            // TODO: 2018/7/13 这里的View可见性判断并不完全， 存在ViewPager的漏洞
            View view = getFragment();
            return view != null && view.isShown();
        }

        @Override
        public boolean isBelongActivity(Activity activity) {
            View view = getFragment();
            return view != null && ActivityUtil.findActivity(view.getContext()) == activity;
        }

        @Nullable
        @Override
        public View getView() {
            return getFragment();
        }

        @Nullable
        @Override
        public Activity getActivity() {
            View view = getView();
            return  view == null ? null : ActivityUtil.findActivity(view.getContext());
        }
    }
}
