package com.growingio.android.sdk.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射相关的工具类
 * <strong>改类使用Task initSrc映射到了plugin中有使用， 做出更改时需要手动调用initSrc的Task</strong>
 * Created by liangdengke on 2018/4/28.
 */
public abstract class ReflectUtil {

    private static final String TAG = "GIO.ReflectUtil";

    @SuppressWarnings("unchecked")
    public static <T> T getFiledValue(Field field, Object instance){
        try {
            return field == null ? null : (T) field.get(instance);
        } catch (IllegalAccessException e) {
            LogUtil.e(TAG, e.getMessage(), e);
            return null;
        }
    }
    
    public static Object callMethod(Object instance, String methodName, Object... args) {
        Class[] argsClass = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null) {
                argsClass[i] = args[i].getClass();
            }
        }
        Method method = getMethod(instance.getClass(), methodName, argsClass);
        if (method != null){
            try {
                return method.invoke(instance, args);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage(), e);
            }
        }
        return null;
    }

    public static Class<?> getMethodFromSignature(String signature){
        throw new RuntimeException("NOT implementation");
    }

    public static Method getMethod(String className, String methodName, String desc){
        return getMethod(null, className, methodName, desc);
    }

    /**
     * 从方法签名中获取Method
     */
    public static Method getMethod(ClassLoader classLoader, String className,
                                   String methodName, String desc){
        try {
            if (className.contains("/")){
                className = className.replace('/', '.');
            }
            Class<?> searchClass;
            if (classLoader != null){
                 searchClass = classLoader.loadClass(className);
            }else{
                searchClass = Class.forName(className);
            }
            return searchClass.getMethod(methodName, parseArgumentTypeByDesc(desc, classLoader));
        } catch (Exception e) {
            LogUtil.d(TAG, e);
        }
        return null;
    }

    public static Class<?>[] parseArgumentTypeByDesc(String desc, ClassLoader classLoader) throws ClassNotFoundException {
        List<Class<?>> params = new ArrayList<>();
        SourceString sourceString = new SourceString(desc);
        sourceString.classLoader = classLoader;
        while (sourceString.hasNextParam()){
            params.add(sourceString.nextParamType());
        }
        Class<?>[] paramArray = new Class[params.size()];
        params.toArray(paramArray);
        return paramArray;
    }

    public static Method getDeclaredRecur(Class<?> clazz, String methodName, String desc){
        try {
            return getDeclaredRecur(clazz, methodName, parseArgumentTypeByDesc(desc, clazz.getClassLoader()));
        }catch (Exception e){
            LogUtil.d(TAG, e);
        }
        return null;
    }

    public static Method getDeclaredRecur(Class<?> clazz, String methodName, Class<?>... params){
        while (clazz != Object.class){
            try {
                Method method = clazz.getDeclaredMethod(methodName, params);
                if (method != null){
                    return method;
                }
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params){
        try {
            return clazz.getMethod(methodName, params);
        } catch (NoSuchMethodException e) {
            LogUtil.d(TAG, e);
            return null;
        }
    }

    public static Field findFieldObj(Class<?> clazz, String fieldName){
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            LogUtil.d(TAG, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T findField(Class<?> clazz, Object instance, String fieldName){
        Field field = findFieldObj(clazz, fieldName);
        if (field == null)
            return null;
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            LogUtil.d(TAG, e);
            return null;
        }
    }


    public static Field findFieldObjRecur(Class<?> current, String fieldName){
        while (current != Object.class){
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T findFieldRecur(Object instance, String fieldName){
        Field field = findFieldObjRecur(instance.getClass(), fieldName);
        if (field != null){
            try {
                return (T) field.get(instance);
            } catch (IllegalAccessException e) {
                LogUtil.d(TAG, e);
            }
        }
        return null;
    }

    /**
     * originalString: (Ljava/lang/String;I)D
     */
    static class SourceString{
        private final String originalString;
        private int currentIndex = 1;
        private ClassLoader classLoader;

        public SourceString(String str){
            if (str.contains("/")){
                originalString = str.replace('/', '.');
            }else{
                originalString = str;
            }
        }

        public boolean hasNextParam(){
            return originalString.charAt(currentIndex) != ')';
        }

        public Class<?> nextParamType() throws ClassNotFoundException {
            char chatAtCurrent = originalString.charAt(currentIndex);
            currentIndex += 1;
            switch (chatAtCurrent){
                case 'Z':
                    return boolean.class;
                case 'B':
                    return byte.class;
                case 'C':
                    return char.class;
                case 'S':
                    return short.class;
                case 'I':
                    return int.class;
                case 'J':
                    return long.class;
                case 'F':
                    return float.class;
                case 'D':
                    return double.class;
                case 'L':
                    return objParamType(false);
                case '[':
                    char nextChar = originalString.charAt(currentIndex);
                    currentIndex += 1;
                    if ('L' == nextChar){
                        return objParamType(true);
                    }
                    switch (nextChar){
                        case 'Z':
                            return boolean[].class;
                        case 'B':
                            return byte[].class;
                        case 'C':
                            return char[].class;
                        case 'S':
                            return short[].class;
                        case 'I':
                            return int[].class;
                        case 'J':
                            return long[].class;
                        case 'F':
                            return float[].class;
                        case 'D':
                            return double[].class;
                    }
                    break;
                default:
                    throw new RuntimeException("not support this signature: " + originalString);
            }

            throw new RuntimeException("ignore: " + originalString);
        }

        Class<?> objParamType(boolean isArray) throws ClassNotFoundException {
            int splitIndex = originalString.indexOf(';', currentIndex);
            String paramTypeSubStr = originalString.substring(currentIndex, splitIndex);
            if (isArray){
                paramTypeSubStr = "[L" + paramTypeSubStr + ';';
            }
            currentIndex = splitIndex + 1;
            if (classLoader != null){
                return classLoader.loadClass(paramTypeSubStr);
            }
            return Class.forName(paramTypeSubStr);
        }
    }

    /* ========> HOLDER FOR PLUGIN  <========== */
}
