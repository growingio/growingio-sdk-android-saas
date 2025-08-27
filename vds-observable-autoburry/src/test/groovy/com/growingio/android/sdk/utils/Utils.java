package com.growingio.android.sdk.utils;

import java.lang.reflect.Method;

/**
 * Created by liangdengke on 2018/11/13.
 */
public class Utils {

    public static String constructorMethodStr(Method method){
        StringBuilder builder = new StringBuilder();
        String className = method.getDeclaringClass().getName().replace(".", "/");
        builder.append(className);
        builder.append(".");
        builder.append(method.getName());
        builder.append("(");
        for (Class<?> aClass : method.getParameterTypes()) {
            builder.append(signatureFromClass(aClass));
        }
        builder.append(")");
        if (method.getReturnType() == void.class){
            builder.append("V");
        }else{
            builder.append(signatureFromClass(method.getReturnType()));
        }
        return builder.toString();
    }

    private static String signatureFromClass(Class aClass){
        if (aClass == boolean.class){
            return "Z";
        }else if (aClass == byte.class){
            return "B";
        }else if (aClass == char.class){
            return "C";
        }else if (aClass == short.class){
            return "S";
        }else if (aClass == int.class){
            return "I";
        }else if (aClass == long.class){
            return "J";
        }else if (aClass == float.class){
            return "F";
        }else if (aClass == double.class){
            return "D";
        }else if (aClass.isArray()){
            throw new UnsupportedOperationException("暂时没有需求， 请自行适配");
        }else{
            return "L" + aClass.getName().replace(".", "/") + ";";
        }
    }
}
