package com.growingio.android.sdk.debugger

import android.app.Activity
import android.net.Uri
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.CoreInitialize
import com.growingio.android.sdk.collection.GConfig
import com.growingio.android.sdk.ipc.GrowingIOIPC
import com.growingio.android.sdk.utils.ScreenshotHelper
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

/**
 * Created by liangdengke on 2019/1/7.
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([Uri.class, ScreenshotHelper.class, CoreInitialize.class])
class DebuggerManagerTest extends Specification {

    DebuggerManager debuggerManager
    CoreAppState coreAppState

    def debuggerMainListener = Mock(DebuggerEventListener)
    def circleListener = Mock(DebuggerEventListener)
    def webCircleListener = Mock(DebuggerEventListener)

    def growingIOIPC = Mock(GrowingIOIPC)

    def setup(){
        GConfig.DEBUG = false
        coreAppState = Mock(CoreAppState)
        debuggerManager = new DebuggerManager(coreAppState)

        debuggerManager.registerDebuggerEventListener("mobile-debugger-main", debuggerMainListener)
        debuggerManager.registerDebuggerEventListener("app-circle-main", circleListener)
        PowerMockito.mockStatic(CoreInitialize)
        PowerMockito.when(CoreInitialize.growingIOIPC()).thenReturn(growingIOIPC)
    }

    def "test launchFloatViewIfNeed, app circle"(){
        def uri = Mock(Uri)
        def activity = Mock(Activity)

        when: 'type app'
        debuggerManager.launchFloatViewIfNeed(uri, activity)

        then:
        uri.getQueryParameter("circleType") >> "app"
        1 * growingIOIPC.setSpecialModel(1)
        1 * circleListener.onFirstLaunch(uri)
    }

    def "test launchFloatViewIfNeed, debugger"(){
        def uri = Mock(Uri)
        def activity = Mock(Activity)

        when:
        debuggerManager.launchFloatViewIfNeed(uri, activity)

        then:
        uri.getQueryParameter("circleType") >> "debugger"
        1 * growingIOIPC.setSpecialModel(10)
        1 * debuggerMainListener.onFirstLaunch(uri)
    }

    def "test launchFloatViewIfNeed, data-check"(){
        def uri = Mock(Uri)
        def activity = Mock(Activity)

        when:
        debuggerManager.launchFloatViewIfNeed(uri, activity)

        then:
        1 * uri.getQueryParameter("circleType") >> null
        1 * uri.getQueryParameter("dataCheckRoomNumber") >> "dataCheckRoomNumber"
        1 * debuggerMainListener.onFirstLaunch(uri)
    }
}
