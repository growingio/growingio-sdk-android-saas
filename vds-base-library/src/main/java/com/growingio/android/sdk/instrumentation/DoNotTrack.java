package com.growingio.android.sdk.instrumentation;

/**
 * Created by lishaojie on 16/6/20.
 */

public @interface DoNotTrack {

    boolean isStatic() default false;

    String scope() default "";
}

