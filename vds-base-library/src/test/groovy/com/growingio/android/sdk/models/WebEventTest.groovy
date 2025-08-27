package com.growingio.android.sdk.models

import android.text.TextUtils
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.GConfig
import com.growingio.android.sdk.collection.SessionManager
import com.growingio.android.sdk.utils.GIOMockUtil
import com.growingio.android.sdk.utils.LinkedString
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([TextUtils.class])
class WebEventTest extends Specification {

    def coreAppState = Mock(CoreAppState)
    GConfig mConfig = Mock(GConfig)
    def viewNode = Mock(ViewNode)
    def sessionManager = Mock(SessionManager)

    def setup(){
        GIOMockUtil.mockSessionManager(sessionManager)
        PowerMockito.mockStatic(TextUtils)
        Mockito.when(TextUtils.isEmpty(Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                String arg = invocation.getArgument(0)
                return arg == null || arg.length() == 0
            }
        })
    }

    private WebEvent generateWebEvent(String eventStr, String pageName){
        return new WebEvent(eventStr, viewNode, pageName){
            @Override
            protected CoreAppState getAPPState() {
                return coreAppState
            }

            @Override
            protected GConfig getConfig() {
                return WebEventTest.this.mConfig
            }
        }
    }



    def "test web page events"(){
        setup:
        def pageEvent = "{\"u\":\"c7d6bc69-aa6c-3fac-8c66-e740003ba1bb\",\"s\":\"f1b3c38e-5980-4432-856e-1423c688117e\",\"tl\":\"Hybrid 打点事件测试 Demo\",\"t\":\"page\",\"tm\":1571284057066,\"pt\":\"file\",\"d\":\"\",\"p\":\"/android_asset/gio_hybrid.html\",\"rp\":\"\",\"v\":\"Hybrid 打点事件测试 Demo\"}"
        def webEvent = generateWebEvent(pageEvent, "fakePage")

        when:
        def result = webEvent.toJson()

        then:
        coreAppState.getSPN() >> "com.test.ldk"
        coreAppState.getAppVariable() >> null
        mConfig.getAppUserId() >> "ldk"
        sessionManager.getSessionIdInner() >> "mock---s"

        result.getString("p").startsWith("fakePage::")
        result.getString("cs1") == "ldk"
        result.getString("s") == "mock---s"

    }

    def "test click events"(){
        setup:
        def clickEvent = "{\"u\":\"c7d6bc69-aa6c-3fac-8c66-e740003ba1bb\",\"s\":\"fc48524c-2d50-4387-adf7-de65c627461c\",\"t\":\"clck\",\"tm\":1571290283802,\"ptm\":1571290280615,\"d\":\"\",\"p\":\"/android_asset/gio_hybrid.html\",\"e\":[{\"v\":\"gio('track','eventid111');\",\"x\":\"/label/button.button\"}]}"
        def event = generateWebEvent(clickEvent, "fakePage")
        viewNode.mOriginalParentXpath = LinkedString.fromString("originalParentXpath")
        viewNode.mParentXPath = LinkedString.fromString("parentXpath")

        when:
        def result = event.toJson()

        then:
        coreAppState.getSPN() >> "com.test.ldk"
        coreAppState.getAppVariable() >> null
        sessionManager.getSessionIdInner() >> "mock---s"

        result.getString("d").startsWith("com.test.ldk::")
        result.getJSONArray("e").getJSONObject(0).getString("x").startsWith("parentXpath::")
        result.getString("p").startsWith("fakePage::")
    }

    def "test chng events"(){
        setup:
        def chngMsg = "{\"u\":\"c7d6bc69-aa6c-3fac-8c66-e740003ba1bb\",\"s\":\"a7612838-e9b1-4e33-909e-958ed37b24a1\",\"t\":\"chng\",\"tm\":1571290906926,\"ptm\":1571290897541,\"d\":\"\",\"p\":\"/android_asset/gio_hybrid.html\",\"e\":[{\"x\":\"/form/label/input#name.name\"}]}"
        def event = generateWebEvent(chngMsg, "fakePage")
        viewNode.mOriginalParentXpath = LinkedString.fromString("originParentXPath")
        viewNode.mParentXPath = LinkedString.fromString("parentXPath")

        when:
        def result = event.toJson()

        then:
        coreAppState.getSPN() >> "com.test.ldk"
        coreAppState.getAppVariable() >> null
        sessionManager.getSessionIdInner() >> "mock---s"

        result.getString("d").startsWith("com.test.ldk::")
        result.getJSONArray("e").getJSONObject(0).getString("x").startsWith("parentXPath::")
        result.getString("p").startsWith("fakePage::")
    }
}
