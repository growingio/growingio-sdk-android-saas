package com.growingio.android.sdk.collection

import android.app.Activity
import android.text.TextUtils
import com.growingio.android.sdk.utils.JsonUtil
import com.growingio.android.sdk.utils.PowerMockUtils
import com.growingio.android.sdk.utils.ThreadUtils
import com.growingio.eventcenter.EventCenter
import org.json.JSONObject
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/9/20.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([TextUtils.class, ThreadUtils.class, EventCenter.class])
class CoreAppStateTest extends Specification {

    GConfig config
    CoreAppState appState
    MessageProcessor msgProcessor
    SessionManager sessionManager

    def setup(){
        PowerMockUtils.mockTextUtil()
        PowerMockUtils.mockEventCenter()
        GConfig.isReplace = true
        config = Mock(GConfig){
            isEnabled() >> true
        }
        appState = Spy(CoreAppState) {
            getForegroundActivity() >> Mock(Activity)
        }
        msgProcessor = Mock(MessageProcessor)
        sessionManager = Mock(SessionManager)
        appState.setMsgProcessor(msgProcessor)
        appState.setSessionManager(sessionManager)
        appState.mConfig = config
        appState.lastUserId = null
    }

    def "test static method"(){
        when:
        println "OK"

        then:
        TextUtils.isEmpty(null)
        TextUtils.isEmpty("")
        !TextUtils.isEmpty("non empty")
    }

    def "test setUserId(null)"(){
        when:'from null to null'
        appState.setUserId(null)
        then:
        0 * sessionManager.updateSessionByUserIdChanged()
        config.getAppUserId() >> null


        when: 'from non-null to null'
        CoreAppState.lastUserId = null
        appState.setUserId(null)
        then:
        config.getAppUserId() >> "old id"
        0 * sessionManager.updateSessionByUserIdChanged()
    }

    def "test setUserId(non-null)"(){
        when: 'from null to non-null'
        appState.setUserId("non-null")
        then:
        config.getAppUserId() >> null
        0 * sessionManager.updateSessionByUserIdChanged()

        when: 'from non-null to non-null, but same'
        appState.setUserId("non-null")
        then:
        0 * sessionManager.updateSessionByUserIdChanged()
        config.getAppUserId() >> "non-null"

        when: 'from non-null to non-null, not same'
        appState.setUserId("non-null")
        then:
        1 * sessionManager.updateSessionByUserIdChanged()
        config.getAppUserId() >> "non-null 2"
    }

    def "test setUserId non-null to null, to non-null"(){
        when: 'same'
        CoreAppState.lastUserId = "non-null"
        appState.setUserId(null)
        then:
        config.getAppUserId() >> "non-null"
        0 * sessionManager.updateSessionByUserIdChanged()

        when: 'same'
        appState.setUserId("non-null")
        then:
        config.getAppUserId() >> null
        0 * sessionManager.updateSessionByUserIdChanged()

        when: 'not same'
        CoreAppState.lastUserId = "non-null"
        appState.setUserId(null)
        then:
        config.getAppUserId() >> "non-null"
        0 * sessionManager.updateSessionByUserIdChanged()

        when: 'not same'
        appState.setUserId("non-null 2")
        then:
        config.getAppUserId() >> null
        sessionManager.updateSessionByUserIdChanged()
    }

    def "test setPageVariable"(){
        when: "set page1 JsonObject"
        JSONObject firstTimeJsonObject = new JSONObject();
        firstTimeJsonObject.put("key1", "value1")
        firstTimeJsonObject.put("key2", "value2")
        appState.setPageVariable("page1", firstTimeJsonObject)
        def jsonHelper = appState.getPageVariableHelper("page1")

        ThreadUtils.run()
        def json = jsonHelper.getVariable()

        then:
        2 == json.length()
        1 * msgProcessor.onPageVariableUpdated(_)

        when: "set page1 key and value"
        appState.setPageVariable("page1", "key1", "value1")
        ThreadUtils.run()
        json = jsonHelper.getVariable()

        then:
        2 == json.length()
        0 * msgProcessor.onPageVariableUpdated(_)

        when: "set page1 key and value diff"
        appState.setPageVariable("page1", "key1", "value1-diff")
        ThreadUtils.run()
        json = jsonHelper.getVariable()

        then:
        2 == json.length()
        "value1-diff" == json.getString("key1")
        1 * msgProcessor.onPageVariableUpdated(_)

        when: "set page1 key and value another key"
        appState.setPageVariable("page1", "key3", "value3")
        ThreadUtils.run()
        json = jsonHelper.getVariable()
        then:
        3 == json.length()
        1 * msgProcessor.onPageVariableUpdated(_)

        when: "set JsonObject null"
        appState.setPageVariable("page1", (JSONObject)null)
        ThreadUtils.run()
        json = jsonHelper.getVariable()
        then:
        0 == json.length()
        0 * msgProcessor.onPageVariableUpdated(_)

        when: "set page2"
        appState.setPageVariable("page2", JsonUtil.copyJson(firstTimeJsonObject, false))
        ThreadUtils.run()
        jsonHelper = appState.getPageVariableHelper("page2")
        json = jsonHelper.getVariable()
        then:
        2 == json.length()
        1 * msgProcessor.onPageVariableUpdated(_)

        when: "set JsonObject for merge"
        JSONObject anotherJSON = new JSONObject()
        anotherJSON.put("page2-key", "value1")
        anotherJSON.put("page2-key2", "value2")
        anotherJSON.put("key1", "page2-key-diff")

        appState.setPageVariable("page2", anotherJSON)
        ThreadUtils.run()
        json = jsonHelper.getVariable()
        then:
        4 == json.length()
        1 * msgProcessor.onPageVariableUpdated(_)
        "page2-key-diff" == json.getString("key1")

        when: 'setJsonObject empty'
        appState.setPageVariable("page2", new JSONObject())
        ThreadUtils.run()
        json = jsonHelper.getVariable()

        then:
        0 == json.length()
        0 * msgProcessor.onPageVariableUpdated(_)
    }


    def "test setLocation normal"(){
        when: 'normal, without resume activity'
        appState.clearLocation()
        appState.setResumedActivity(null)
        appState.setLocation(20, 30)
        then:
        0 * msgProcessor.saveVisit(_)

        when: 'normal, with resume Activity'
        appState.clearLocation()
        appState.setResumedActivity(Mock(Activity))
        appState.setLocation(20, 30)
        then:
        1 * msgProcessor.saveVisit(false)
    }

    def "test setLocation zero"(){
        when:
        appState.clearLocation()
        appState.setResumedActivity(Mock(Activity))
        appState.setLocation(0, 0)
        then:
        0 * msgProcessor.saveVisit(_)
    }

    def "test setLocation valid"(){
        appState.clearLocation()
        appState.setResumedActivity(Mock(Activity))
        appState.setLocation(20, 30.1)

        when: 'test five minutes'
        appState.mLastSetLocationTime = System.currentTimeMillis() - 1000 * 60 * 10
        appState.setLocation(20, 30.1000001)

        then:
        1 * msgProcessor.saveVisit(false)

        when: 'test delta'
        appState.setLocation(20.3, 30.100001)
        then:
        1 * msgProcessor.saveVisit(false)
    }
}
