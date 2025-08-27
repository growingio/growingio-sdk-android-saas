package com.growingio.android.sdk.collection

import android.text.TextUtils
import com.growingio.android.sdk.ipc.GrowingIOIPC
import com.growingio.android.sdk.utils.PowerMockUtils
import com.growingio.android.sdk.utils.ThreadUtils
import com.growingio.eventcenter.EventCenter
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([TextUtils.class])
class SessionManagerTest extends Specification {

    def setup(){
        PowerMockUtils.mockTextUtil()
    }

    def "OnActivityResume"() {
        setup:
        def msgProcessor = Mock(MessageProcessor)
        def gioIPC = Mock(GrowingIOIPC)
        def config = Mock(GConfig)

        def sessionManager = new SessionManager(msgProcessor, gioIPC, config)

        when: "next must send visit"
        sessionManager.getSessionIdInner()
        sessionManager.onActivityResume()
        then:
        gioIPC.getSessionId() >> null
        1 * msgProcessor.saveVisit(true)


        when: "后台超过30s"
        sessionManager.onActivityResume()

        then:
        gioIPC.getLastPauseTime() >> System.currentTimeMillis() - 50_000L
        config.getSessionInterval() >> 30_000L
        1 * gioIPC.setSessionId(_)
        1 * msgProcessor.saveVisit(true)

        when: "后台更新location， 下次Resume更新一个visit但不更新session"
        sessionManager.nextResumeResendVisit()
        sessionManager.onActivityResume()

        then:
        gioIPC.getLastPauseTime() >> System.currentTimeMillis() - 10_000L
        config.getSessionInterval() >> 30_000L
        1 * msgProcessor.saveVisit(true)
        0 * gioIPC.setSessionId(_)

        when: "不应该发送visit"
        sessionManager.onActivityResume()

        then:
        gioIPC.getLastPauseTime() >> System.currentTimeMillis() - 10_000L
        config.getSessionInterval() >> 30_000L
        0 * msgProcessor.saveVisit(true)
        0 * gioIPC.setSessionId(_)

        when: "后台更新location, 后台时间超过30s, 更新sessionId， 重发visit"
        sessionManager.onActivityResume()

        then:
        gioIPC.getLastPauseTime() >> System.currentTimeMillis() - 50_000L
        config.getSessionInterval() >> 30_000L
        1 * msgProcessor.saveVisit(true)
        1 * gioIPC.setSessionId(_)
    }
}
