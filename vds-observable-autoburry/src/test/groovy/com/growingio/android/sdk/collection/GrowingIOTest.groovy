package com.growingio.android.sdk.collection

import android.util.Log
import android.view.View
import com.growingio.android.sdk.utils.ArgumentChecker
import com.growingio.android.sdk.utils.ThreadUtils
import org.json.JSONObject
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

@RunWith(PowerMockRunner)
@PowerMockRunnerDelegate(Sputnik)
@PrepareForTest([ThreadUtils, CoreInitialize, Log])
class GrowingIOTest extends Specification {

    Thread mainThread, bgThread
    CoreAppState coreAppState
    GrowingIO gio
    GConfig config
    MessageProcessor messageProcessor
    DeviceUUIDFactory deviceUUIDFactory
    boolean  logged = false
    Runnable mainRunnable;

    def setup(){
        PowerMockito.mockStatic(ThreadUtils)
        PowerMockito.when(ThreadUtils.runningOnUiThread()).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                return Thread.currentThread() == mainThread
            }
        })
        PowerMockito.when(ThreadUtils.postOnUiThread(Mockito.anyObject())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                mainRunnable = invocation.arguments[0]
                return null
            }
        })
        PowerMockito.mockStatic(CoreInitialize)
        coreAppState = Mock(CoreAppState)
        messageProcessor = Mock(MessageProcessor)
        deviceUUIDFactory = Mock(DeviceUUIDFactory)
        PowerMockito.when(CoreInitialize.coreAppState()).thenReturn(coreAppState)
        PowerMockito.when(CoreInitialize.messageProcessor()).thenReturn(messageProcessor)
        PowerMockito.when(CoreInitialize.deviceUUIDFactory()).thenReturn(deviceUUIDFactory)
        gio = new GrowingIO()
        config = Mock(GConfig)
        gio.mGConfig = config
        mainThread = Thread.currentThread()
        PowerMockito.mockStatic(Log)
        PowerMockito.when(Log.e(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                logged = true
                return null
            }
        })
        gio.mArgumentChecker = new ArgumentChecker(config)
    }

    def checkLog(){
        if (logged){
            logged = false
            return true
        }
        return false
    }

    def 'generate'(){
        setup:
        //gio.disableDataCollect()
        //config.setGDPREnabled(true)
        printTestFunc("disableDataCollect", "", "gio.disableDataCollect()", "1 * config.setGDPREnabled(false)")

        //gio.enableDataCollect()
        //config.setGDPREnabled(false)
        printTestFunc("enableDataCollect", "", "gio.enableDataCollect()", "1 * config.setGDPREnabled(true)")

        //def view = Mock(View)
        //gio.setViewID(view, "test")
        //1 * view.setTag(GrowingIO.GROWING_VIEW_ID_KEY, "test")
        printTestFunc("setViewId", "def view = Mock(View)", "gio.setViewID(view, \"test\")", "1 * view.setTag(GrowingIO.GROWING_VIEW_ID_KEY, \"test\")")

        //gio.setThrottle(true)
        //1 * config.setThrottle(true)
        printTestFunc("setThrottle", "",
                "gio.setThrottle(true)", "1 * config.setThrottle(true)")
        printTestFunc("disable", "",
                "gio.disable()", "1 * config.disableAll()")
        printTestFunc("setGeoLocation", "",
                "gio.setGeoLocation(100, 200)", "1 * coreAppState.setLocation(100, 200)")
        printTestFunc("clearGeoLocation", "",
                "gio.clearGeoLocation()", "1 * coreAppState.clearLocation()")
        printTestFunc("setUserId", "",
                "gio.setUserId(\"good\")", "1 * coreAppState.setUserId(\"good\")")
        printTestFunc("clearUserId", "",
                "gio.clearUserId()", "1 * coreAppState.clearUserId()")


        def setup = """JSONObject peopleVariable = new JSONObject()
        peopleVariable.put("myName", "ldk")"""
        //gio.setPeopleVariable(peopleVariable)
        printTestFunc("setPeopleVariable", setup,
                "gio.setPeopleVariable(peopleVariable)",
                "1 * coreAppState.setPeopleVariable(_)")
        printTestFunc("setPeopleVariableKeyV", "",
                "gio.setPeopleVariable(\"myName\", \"ldk\")",
                "1 * coreAppState.setPeopleVariable(\"myName\", \"ldk\")")

        printTestFunc("setEvar", setup,
                "gio.setEvar(peopleVariable)",
                "1 * coreAppState.setConversionVariable(_)")
        printTestFunc("setEvarKeyV", "",
                "gio.setEvar(\"myKey\", true)",
                "1 * coreAppState.setConversionVariable(\"myKey\", true)")

        printTestFunc("setVisitorNull", "",
                "gio.setVisitor(null)", "1 * coreAppState.setVisitorVariable(null)")
        printTestFunc("setVisitorNonNull", setup,
                "gio.setVisitor(peopleVariable)", "1 * coreAppState.setVisitorVariable(_)")

        def thenC = """
        1 * deviceUUIDFactory.setImeiEnable(true)
        1 * messageProcessor.saveVisit(false)"""
        printTestFunc("setImeiEnableFalse", "", "gio.setImeiEnable(true)", thenC)

        printTestFunc("track", setup,
                "gio.track(\"test\")",
                "1 * messageProcessor.saveCustomEvent(_)")
    }


    def printTestFunc(String functionName, String setup, String whenC, String thenC){
        def result = """
    def "${functionName}"(){
        $setup
        when:
        $whenC
        then:
        $thenC

        when:
        bgThread = Thread.start {
            $whenC
        }
        waitComplete()
        then:
        $thenC
        checkLog()
    }
"""
        println(result)
    }


    def waitComplete(){
        if (bgThread != null){
            bgThread.join()
            bgThread = null
        }
        if (mainRunnable != null){
            mainRunnable.run()
            mainRunnable = null
        }
    }


    // 以下代码有单元测试自动生成粘贴， 请勿手动改动
    def "disableDataCollect"(){

        when:
        gio.disableDataCollect()
        then:
        1 * config.setGDPREnabled(false)

        when:
        bgThread = Thread.start {
            gio.disableDataCollect()
        }
        waitComplete()
        then:
        1 * config.setGDPREnabled(false)
        checkLog()
    }


    def "enableDataCollect"(){

        when:
        gio.enableDataCollect()
        then:
        1 * config.setGDPREnabled(true)

        when:
        bgThread = Thread.start {
            gio.enableDataCollect()
        }
        waitComplete()
        then:
        1 * config.setGDPREnabled(true)
        checkLog()
    }


    def "setViewId"(){
        def view = Mock(View)
        when:
        gio.setViewID(view, "test")
        then:
        1 * view.setTag(GrowingIO.GROWING_VIEW_ID_KEY, "test")

        when:
        bgThread = Thread.start {
            gio.setViewID(view, "test")
        }
        waitComplete()
        then:
        1 * view.setTag(GrowingIO.GROWING_VIEW_ID_KEY, "test")
        checkLog()
    }


    def "setThrottle"(){

        when:
        gio.setThrottle(true)
        then:
        1 * config.setThrottle(true)

        when:
        bgThread = Thread.start {
            gio.setThrottle(true)
        }
        waitComplete()
        then:
        1 * config.setThrottle(true)
        checkLog()
    }


    def "disable"(){

        when:
        gio.disable()
        then:
        1 * config.disableAll()

        when:
        bgThread = Thread.start {
            gio.disable()
        }
        waitComplete()
        then:
        1 * config.disableAll()
        checkLog()
    }


    def "setGeoLocation"(){

        when:
        gio.setGeoLocation(100, 200)
        then:
        1 * coreAppState.setLocation(100, 200)

        when:
        bgThread = Thread.start {
            gio.setGeoLocation(100, 200)
        }
        waitComplete()
        then:
        1 * coreAppState.setLocation(100, 200)
        checkLog()
    }


    def "clearGeoLocation"(){

        when:
        gio.clearGeoLocation()
        then:
        1 * coreAppState.clearLocation()

        when:
        bgThread = Thread.start {
            gio.clearGeoLocation()
        }
        waitComplete()
        then:
        1 * coreAppState.clearLocation()
        checkLog()
    }


    def "setUserId"(){

        when:
        gio.setUserId("good")
        then:
        1 * coreAppState.setUserId("good")

        when:
        bgThread = Thread.start {
            gio.setUserId("good")
        }
        waitComplete()
        then:
        1 * coreAppState.setUserId("good")
        checkLog()
    }


    def "clearUserId"(){

        when:
        gio.clearUserId()
        then:
        1 * coreAppState.clearUserId()

        when:
        bgThread = Thread.start {
            gio.clearUserId()
        }
        waitComplete()
        then:
        1 * coreAppState.clearUserId()
        checkLog()
    }


    def "setPeopleVariable"(){
        JSONObject peopleVariable = new JSONObject()
        peopleVariable.put("myName", "ldk")
        when:
        gio.setPeopleVariable(peopleVariable)
        then:
        1 * coreAppState.setPeopleVariable(_)

        when:
        bgThread = Thread.start {
            gio.setPeopleVariable(peopleVariable)
        }
        waitComplete()
        then:
        1 * coreAppState.setPeopleVariable(_)
        checkLog()
    }


    def "setPeopleVariableKeyV"(){

        when:
        gio.setPeopleVariable("myName", "ldk")
        then:
        1 * coreAppState.setPeopleVariable("myName", "ldk")

        when:
        bgThread = Thread.start {
            gio.setPeopleVariable("myName", "ldk")
        }
        waitComplete()
        then:
        1 * coreAppState.setPeopleVariable("myName", "ldk")
        checkLog()
    }


    def "setEvar"(){
        JSONObject peopleVariable = new JSONObject()
        peopleVariable.put("myName", "ldk")
        when:
        gio.setEvar(peopleVariable)
        then:
        1 * coreAppState.setConversionVariable(_)

        when:
        bgThread = Thread.start {
            gio.setEvar(peopleVariable)
        }
        waitComplete()
        then:
        1 * coreAppState.setConversionVariable(_)
        checkLog()
    }


    def "setEvarKeyV"(){

        when:
        gio.setEvar("myKey", true)
        then:
        1 * coreAppState.setConversionVariable("myKey", true)

        when:
        bgThread = Thread.start {
            gio.setEvar("myKey", true)
        }
        waitComplete()
        then:
        1 * coreAppState.setConversionVariable("myKey", true)
        checkLog()
    }


    def "setVisitorNull"(){

        when:
        gio.setVisitor(null)
        then:
        1 * coreAppState.setVisitorVariable(null)

        when:
        bgThread = Thread.start {
            gio.setVisitor(null)
        }
        waitComplete()
        then:
        1 * coreAppState.setVisitorVariable(null)
        checkLog()
    }


    def "setVisitorNonNull"(){
        JSONObject peopleVariable = new JSONObject()
        peopleVariable.put("myName", "ldk")
        when:
        gio.setVisitor(peopleVariable)
        then:
        1 * coreAppState.setVisitorVariable(_)

        when:
        bgThread = Thread.start {
            gio.setVisitor(peopleVariable)
        }
        waitComplete()
        then:
        1 * coreAppState.setVisitorVariable(_)
        checkLog()
    }


    def "setImeiEnableFalse"(){

        when:
        gio.setImeiEnable(true)
        then:

        1 * deviceUUIDFactory.setImeiEnable(true)
        1 * messageProcessor.saveVisit(false)

        when:
        bgThread = Thread.start {
            gio.setImeiEnable(true)
        }
        waitComplete()
        then:

        1 * deviceUUIDFactory.setImeiEnable(true)
        1 * messageProcessor.saveVisit(false)
        checkLog()
    }


    def "track"(){
        JSONObject peopleVariable = new JSONObject()
        peopleVariable.put("myName", "ldk")
        when:
        gio.track("test")
        then:
        1 * messageProcessor.saveCustomEvent(_)

        when:
        bgThread = Thread.start {
            gio.track("test")
        }
        waitComplete()
        then:
        1 * messageProcessor.saveCustomEvent(_)
        checkLog()
    }
}
