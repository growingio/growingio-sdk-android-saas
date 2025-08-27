package com.growingio.android.sdk.utils;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.SessionManager;

import java.lang.reflect.Field;

public class GIOMockUtil {

    public static void mockSessionManager(SessionManager sessionManager){
        mock("sessionManager", sessionManager);
    }

    private static void mock(String fieldName, Object value){
        try {
            Field field = CoreInitialize.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
