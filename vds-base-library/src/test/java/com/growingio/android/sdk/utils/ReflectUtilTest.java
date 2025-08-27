package com.growingio.android.sdk.utils;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Created by liangdengke on 2018/4/28.
 */
public class ReflectUtilTest {
    @Test
    public void getMethod() throws Exception {
        Method method = ReflectUtil.getMethod("java.lang.String", "substring", "(I)Ljava/lang/String;");
        assertEquals(String.class, method.getDeclaringClass());

        method = ReflectUtil.getMethod("java.lang.String", "toString", "()Ljava/lang/String;");
        assertEquals(String.class, method.getDeclaringClass());
    }

}