package com.growingio.android.sdk.autoburry.page.visitor;

import android.view.View;

import com.growingio.android.sdk.autoburry.page.proxy.OnFocusChangeListenerProxy;
import com.growingio.android.sdk.collection.GrowingIO;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Created by denghuaxin on 2018/1/30.
 */
public class ListenerInfoVisitor {
    private static final String TAG = "GIO.ListenerInfo";
    private Field mListenerInfoField;
    private Field mOnFocusChangeListenerField;
    private Field mOnClickListenerField;
    private Class<?> ListenerInfoClass;

    public boolean handle(View current) {
        //只支持EditText change事件
        if (!current.isClickable() || !checkEnv(current) || (current.getTag(GrowingIO.GROWING_HOOK_LISTENTER) != null))
             return false;
        Object listenerInfo = getListenerInfo(current);
        if (listenerInfo == null) {
            listenerInfo = getNewListenerInfo(current);
            setListenerInfo(current, listenerInfo);
        }
        if (listenerInfo == null) {
            return false;
        }
        setOnFocusChangeListener(listenerInfo, new OnFocusChangeListenerProxy(getOnFocusChangeListener(listenerInfo)));
        current.setTag(GrowingIO.GROWING_HOOK_LISTENTER, true);
        return false;
    }

    protected boolean checkEnv(View view) {
        try {
            this.mListenerInfoField = View.class.getDeclaredField("mListenerInfo");
            this.mListenerInfoField.setAccessible(true);
            this.ListenerInfoClass = Class.forName(String.format("%s$ListenerInfo", View.class.getName()));
            this.mOnFocusChangeListenerField = ListenerInfoClass.getDeclaredField("mOnFocusChangeListener");
            this.mOnFocusChangeListenerField.setAccessible(true);
            this.mOnClickListenerField = ListenerInfoClass.getDeclaredField("mOnClickListener");
            this.mOnClickListenerField.setAccessible(true);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    protected Object getListenerInfo(View view) {
        try {
            return mListenerInfoField.get(view);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setListenerInfo(View view, Object listenerInfo) {
        try {
            mListenerInfoField.set(view, listenerInfo);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected Object getNewListenerInfo(View view) {
        try {
            Class<?>[] args = null;
            Constructor<?> constructor = ListenerInfoClass.getDeclaredConstructor(args);
            constructor.setAccessible(true);
            if (constructor != null){
                Object[] argsObj = null;
                return constructor.newInstance(argsObj);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    protected View.OnClickListener getOnClickListener(Object listenerInfo) {
        try {
            return (View.OnClickListener) mOnClickListenerField.get(listenerInfo);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setOnClickListener(Object listenerInfo, View.OnClickListener onClickListener) {
        try {
            mOnClickListenerField.set(listenerInfo, onClickListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected View.OnFocusChangeListener getOnFocusChangeListener(Object listenerInfo) {
        try {
            return (View.OnFocusChangeListener) mOnFocusChangeListenerField.get(listenerInfo);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void setOnFocusChangeListener(Object listenerInfo, View.OnFocusChangeListener onFocusChangeListener) {
        try {
            mOnFocusChangeListenerField.set(listenerInfo, onFocusChangeListener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
