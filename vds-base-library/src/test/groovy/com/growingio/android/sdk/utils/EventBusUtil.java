package com.growingio.android.sdk.utils;

import com.growingio.eventcenter.EventCenter;
import com.growingio.eventcenter.bus.EventBus;

import java.lang.reflect.Field;

public class EventBusUtil {

    public static void mockEventBus(EventBus eventBus) throws IllegalAccessException, NoSuchFieldException {
        Field defaultInstance = EventBus.class.getDeclaredField("defaultInstance");
        defaultInstance.setAccessible(true);
        defaultInstance.set(null, eventBus);

        Field initStart = EventCenter.class.getDeclaredField("initStart");
        initStart.setAccessible(true);
        initStart.set(EventCenter.getInstance(), true);
    }

}
