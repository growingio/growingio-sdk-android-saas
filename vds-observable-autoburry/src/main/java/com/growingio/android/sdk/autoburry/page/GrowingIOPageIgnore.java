package com.growingio.android.sdk.autoburry.page;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *   通过注解对无埋点的 Activity进行忽略，不发送事件
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface GrowingIOPageIgnore {
}
