package com.growingio.android.sdk;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;

/**
 * Created by liangdengke on 2018/11/13.
 */
public class FakeIntent extends Intent {

    HashMap<String, Object> map = new HashMap<>();

    @NonNull
    @Override
    public Intent putExtra(String name, int value) {
        map.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public Intent putExtra(String name, String value) {
        map.put(name, value);
        return this;
    }

    @Override
    public int getIntExtra(String name, int defaultValue) {
        Integer integer = (Integer) map.get(name);
        if (integer == null){
            return defaultValue;
        }
        return integer;
    }

    @Nullable
    @Override
    public String getAction() {
        return null;
    }

    @Override
    public String getStringExtra(String name) {
        return (String) map.get(name);
    }

    @Override
    public boolean hasExtra(String name) {
        return map.containsKey(name);
    }

    @Override
    public void removeExtra(String name) {
        map.remove(name);
    }
}
