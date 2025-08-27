package com.growingio.android.sdk.crash

import com.growingio.android.sdk.monitor.analysis.Analysed
import spock.lang.Specification

class CrashManagerTest extends Specification {
    def "handle sdk exception"() {
        given:
        Analysed analysed = new Analysed(throwable)
        analysed.setIsFindTarget(findTarget)
        analysed.setFirstTargetElement(new StackTraceElement(className, "fake", "fake", 0))
        when:
        def result = CrashManager.isSdkException(analysed)
        then:
        result == expectedResult

        where:
        throwable | findTarget | className               || expectedResult
        null      | true       | 'com.growingio.xxx'     || true
        null      | true       | 'com.xxxx.eventcenter'  || false
        null      | true       | ''                      || false
        null      | false      | 'com.growingio.xxx'     || false
        null      | false      | 'com.xxxx.eventcenter'  || false
        null      | false      | ''                      || false
    }
}