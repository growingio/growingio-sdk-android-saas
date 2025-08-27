package com.growingio.android.sdk.autoburry;

import android.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import com.growingio.android.sdk.utils.ClassExistHelper;
import com.growingio.android.sdk.utils.CollectionsUtil;
import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.ReflectUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一AndroidX与V4 ViewPager的对外接口
 * Created by liangdengke on 2018/12/3.
 */
public abstract class SuperViewPager<T extends ViewGroup> {

    private static final String TAG = "GIO.ViewPager";
    protected T viewPager;

    protected SuperViewPager(T instance){
        viewPager = instance;
    }

    public Object getCurrentItemObj(){
        if (!isProguardFine())
            return null;
        List mItems = CollectionsUtil.nonEmptyList(getViewPagerMItem());
        int currentItem = getCurrentItem();
        try{
            for (Object item: mItems){
                int position = getItemPosition(item);
                if (position == currentItem){
                    return getItemObject(item);
                }
            }
        }catch (Throwable throwable){
            LogUtil.d(TAG, throwable);
        }
        return null;
    }

    public boolean isFragmentViewPager(){
        if (!isProguardFine()){
            return false;
        }
        List mItems = getViewPagerMItem();
        if (CollectionsUtil.isEmpty(mItems)){
            return false;
        }
        Object item = mItems.get(0);
        return item instanceof Fragment
                || ClassExistHelper.instanceOfAndroidXFragment(item)
                || (ClassExistHelper.instanceOfSupportFragment(item));
    }

    /**
     * @return 当前ViewPager的可见View
     */
    public View getCurrentView(){
        if (!isProguardFine())
            return null;
        Object currentItemObj = getCurrentItemObj();
        int childSize = viewPager.getChildCount();
        Object pageAdapter = getPageAdapter();
        if (pageAdapter == null || currentItemObj == null)
            return null;
        for (int i = 0; i < childSize; i++){
            View current = viewPager.getChildAt(i);
            if (isViewFromObject(pageAdapter, current, currentItemObj)){
                return current;
            }
        }
        return null;
    }

    public abstract boolean isViewFromObject(Object pageAdapter, View child, Object object);

    public T getViewPager(){
        return viewPager;
    }

    public abstract Object getPageAdapter();


    public abstract int getCurrentItem();
    protected abstract boolean isProguardFine();
    protected abstract ArrayList getViewPagerMItem();
    protected abstract int getItemPosition(Object itemObj) throws Exception;
    protected abstract Object getItemObject(Object itemObj) throws Exception;

    private static Field initItemsField(Class viewPagerClazz, String type){
        try {
            Field mItems = viewPagerClazz.getDeclaredField("mItems");
            mItems.setAccessible(true);
            return mItems;
        }catch (Throwable throwable){
            LogUtil.e(TAG, "Not found " + type + " ViewPager's mItem Field, 请参考GrowingIO官网的混淆文件进行配置");
            return null;
        }
    }

    private static Class initItemInfoClass(Class viewPagerClazz, String type){
        try {
            return Class.forName(viewPagerClazz.getName() + "$ItemInfo");
        } catch (ClassNotFoundException e) {
            LogUtil.e(TAG, "Not found " + type + " ViewPager$ItemInfo, 请参考GrowingIO官网的混淆文件进行配置");
            return null;
        }
    }

    public static class V4ViewPager extends SuperViewPager<ViewPager>{
        private static Field s_mItems;
        private static Field s_itemPosition;
        private static Field s_itemObj;
        private static boolean s_proguardFine;
        static {
            if (ClassExistHelper.sHasSupportViewPager){
                String type = "V4";
                s_mItems = SuperViewPager.initItemsField(ViewPager.class, type);
                Class itemClass = SuperViewPager.initItemInfoClass(ViewPager.class, type);
                if (itemClass != null){
                    s_itemPosition = ReflectUtil.findFieldObj(itemClass, "position");
                    s_itemObj = ReflectUtil.findFieldObj(itemClass, "object");

                    if (s_itemPosition == null || s_itemObj == null){
                        LogUtil.e(TAG, "请参考GrowingIO官网混淆文件进行配置");
                    }else{
                        s_proguardFine = true;
                    }
                }
            }
        }

        public V4ViewPager(ViewPager instance) {
            super(instance);
        }

        @Override
        public boolean isViewFromObject(Object pageAdapter, View child, Object object) {
            if (pageAdapter != null){
                return ((PagerAdapter)pageAdapter).isViewFromObject(child, object);
            }
            return false;
        }

        @Override
        public Object getPageAdapter() {
            return viewPager.getAdapter();
        }

        @Override
        public int getCurrentItem() {
            return viewPager.getCurrentItem();
        }

        @Override
        protected boolean isProguardFine() {
            return s_proguardFine;
        }

        @Override
        protected ArrayList getViewPagerMItem() {
            return ReflectUtil.getFiledValue(s_mItems, viewPager);
        }

        @Override
        protected int getItemPosition(Object itemObj) throws Exception{
            return (int) s_itemPosition.get(itemObj);
        }

        @Override
        protected Object getItemObject(Object itemObj) throws Exception{
            return s_itemObj.get(itemObj);
        }
    }

    public static class AndroidXViewPager extends SuperViewPager<androidx.viewpager.widget.ViewPager> {
        private static Field s_mItems;
        private static Field s_itemPosition, s_itemObj;
        private static boolean s_proguardFine;
        static {
            if (ClassExistHelper.sHasAndroidXViewPager){
                String type = "AndroidX";
                s_mItems = SuperViewPager.initItemsField(androidx.viewpager.widget.ViewPager.class, type);
                Class itemClass = SuperViewPager.initItemInfoClass(androidx.viewpager.widget.ViewPager.class, type);
                if (itemClass != null){
                    s_itemPosition = ReflectUtil.findFieldObj(itemClass, "position");
                    s_itemObj = ReflectUtil.findFieldObj(itemClass, "object");
                    if (s_itemPosition == null || s_itemObj == null){
                        LogUtil.e(TAG, "请参考GrowingIO官网混淆文件进行配置");
                    }else{
                        s_proguardFine = true;
                    }
                }
            }
        }

        public AndroidXViewPager(androidx.viewpager.widget.ViewPager instance) {
            super(instance);
        }

        @Override
        public boolean isViewFromObject(Object pageAdapter, View child, Object object) {
            if (pageAdapter != null){
                return ((androidx.viewpager.widget.PagerAdapter)pageAdapter).isViewFromObject(child, object);
            }
            return false;
        }

        @Override
        public Object getPageAdapter() {
            return viewPager.getAdapter();
        }

        @Override
        public int getCurrentItem() {
            return viewPager.getCurrentItem();
        }

        @Override
        protected boolean isProguardFine() {
            return s_proguardFine;
        }

        @Override
        protected ArrayList getViewPagerMItem() {
            return ReflectUtil.getFiledValue(s_mItems, viewPager);
        }

        @Override
        protected int getItemPosition(Object itemObj) throws Exception{
            return (int) s_itemPosition.get(itemObj);
        }

        @Override
        protected Object getItemObject(Object itemObj) throws Exception{
            return s_itemObj.get(itemObj);
        }
    }
}
