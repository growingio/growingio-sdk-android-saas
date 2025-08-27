package com.growingio.android.sdk.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by liangdengke on 2018/4/28.
 */
public class SourceStringTest {

    @Test
    public void objParamType() throws Exception {
        ReflectUtil.SourceString sourceString = new ReflectUtil.SourceString("Ljava/lang/String;I)D");
        Class<?> obj = sourceString.objParamType(false);
        assertTrue(String.class == obj);

        sourceString = new ReflectUtil.SourceString("Ljava/lang/String;I)D");
        assertTrue(String[].class == sourceString.objParamType(true));
    }

    @Test
    public void nextParamType() throws ClassNotFoundException {
        ReflectUtil.SourceString sourceString = new ReflectUtil.SourceString("()V");
        List<Class<?>> paramTypes = new ArrayList<>();

        while (sourceString.hasNextParam()){
            paramTypes.add(sourceString.nextParamType());
        }
        assertTrue(paramTypes.isEmpty());

        sourceString = new ReflectUtil.SourceString("(IJ)I");
        paramTypes.clear();
        while (sourceString.hasNextParam()){
            paramTypes.add(sourceString.nextParamType());
        }
        assertEquals(2, paramTypes.size());
        assertTrue(int.class == paramTypes.get(0));
        assertTrue(long.class == paramTypes.get(1));

        sourceString = new ReflectUtil.SourceString("(ILjava/lang/String;[I)J");
        paramTypes.clear();
        while (sourceString.hasNextParam()){
            paramTypes.add(sourceString.nextParamType());
        }
        assertEquals(3, paramTypes.size());
        assertTrue(int.class == paramTypes.get(0));
        assertTrue(String.class == paramTypes.get(1));
        assertTrue(int[].class == paramTypes.get(2));
    }

}