package com.growingio.android.sdk.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by liangdengke on 2018/4/2.
 */
public final class CollectionsUtil {

    private CollectionsUtil(){}

    public static boolean isEmpty(Collection<?> collection){
        return collection == null || collection.size() == 0;
    }

    public static <T> List<T> nonEmptyList(List<T> list){
        if (list == null)
            return Collections.emptyList();
        return list;
    }
}
