package com.growingio.android.sdk.utils

import android.text.TextUtils
import com.growingio.eventcenter.EventCenter
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito

class PowerMockUtils{
    static def mockTextUtil(){
        PowerMockito.mockStatic(TextUtils.class, new Answer() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                String arg = invocation.getArgument(0)
                return arg == null || arg.length() == 0
            }
        })
    }

    static def mockEventCenter() {
        EventCenter eventCenter = PowerMockito.mock(EventCenter.class)
        PowerMockito.mockStatic(EventCenter.class, {
            return eventCenter
        })
        PowerMockito.doNothing().when(eventCenter, "post", Object)
    }
}