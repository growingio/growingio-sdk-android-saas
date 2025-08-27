package com.growingio.android.sdk.collection

import com.growingio.android.sdk.models.ActionEvent
import com.growingio.android.sdk.models.ActionStruct
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/12/18.
 */
class ActionCalculatorTest extends Specification {

    def "test makeAction"(){
        ActionCalculator actionCalculator = new ActionCalculator("pageName", 23334, null, "prefix")
        def actions = new ArrayList<>()
        (1..3000).each{
            ActionStruct struct = new ActionStruct()
            struct.index = it
            actions.add(struct)
        }
        actionCalculator.mNewImpressViews = actions

        when:
        ArrayList<ActionEvent> events = new ArrayList<>()
        def lastAction = actionCalculator.makeActionEvent(events)
        int x = 0;
        events.each {
            x += it.elems.size()
        }

        then:
        3000 == x
        actions.first() == events.first().elems.first()
        actions.last() == lastAction.elems.last()
    }
}
