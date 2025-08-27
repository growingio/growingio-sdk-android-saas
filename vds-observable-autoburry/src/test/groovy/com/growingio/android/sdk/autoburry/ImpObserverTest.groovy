package com.growingio.android.sdk.autoburry

import android.app.Activity
import android.view.View
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.ImpressionMark
import com.growingio.android.sdk.utils.ActivityUtil
import com.growingio.android.sdk.utils.TimerToggler
import com.growingio.android.sdk.utils.ViewHelper
import org.json.JSONObject
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

@RunWith(PowerMockRunner)
@PowerMockRunnerDelegate(Sputnik)
@PrepareForTest([ActivityUtil, ViewHelper])
class ImpObserverTest extends Specification {

    private CoreAppState coreAppState
    private ImpObserver impObserver

    def setup(){
        coreAppState = Mock(CoreAppState)
        impObserver = new ImpObserver(coreAppState)
    }

    def "stamp a normal view, without globalId"(){
        setup:
        def view = Mock(View)
        def json = new JSONObject()

        when:
        impObserver.markViewImpression(new ImpressionMark(view, "no action").setNum(10).setVariable(json).setDelayTimeMills(10))

        then:
        1 * view.getContext() >> null
        1 * coreAppState.getForegroundActivity() >> null
        null == impObserver.mActivityScopes
    }

    def 'stamp a normal view, without globalid, and with action'(){
        setup:
        def activity = new Activity()
        def view = Mock(View)
        def json = new JSONObject()
        def anotherView = Mock(View)

        view.getContext() >> activity
        anotherView.getContext() >> activity

        when:
        impObserver.markViewImpression(new ImpressionMark(view, 'with action').setNum(10).setVariable(json).setDelayTimeMills(10L))
        def scope = impObserver.mActivityScopes.get(activity)

        then:
        1 == impObserver.mActivityScopes.size()
        1 == scope.togglerWithViewsList.size()
        10L == scope.togglerWithViewsList.get(0).delayTime
        0 == scope.globalIdToImpEvent.size()
        0 == scope.nextPassInvisible.size()
        1 == scope.viewToTogglerWithViews.size()
        scope.viewToTogglerWithViews.get(view) == scope.togglerWithViewsList.get(0)

        when: 'add a delay time same again'
        impObserver.markViewImpression(new ImpressionMark(anotherView, 'with action').setNum(10).setVariable(json).setDelayTimeMills(10))

        then:
        1 == impObserver.mActivityScopes.size()
        1 == scope.togglerWithViewsList.size()

        when: 'add same view again, and delay time same'
        impObserver.markViewImpression(new ImpressionMark(view, 'with action').setNum(100).setVariable(json).setDelayTimeMills(10))

        then:
        1 == scope.togglerWithViewsList.size()
        scope.getImpEvent(view).mark.num == 100

        when: 'add same view again, but not same delay time'
        impObserver.markViewImpression(new ImpressionMark(view, 'with action').setNum(300).setVariable(json).setDelayTimeMills(100L))
        then:
        2 == scope.togglerWithViewsList.size()
        scope.getImpEvent(view).mark.num == 300
        scope.viewToTogglerWithViews.get(view).delayTime == 100L
    }

    def 'stamp view with globalId'(){
        setup:
        def activity = new Activity()
        def view = Mock(View)
        def json = new JSONObject()
        def anotherView = Mock(View)

        view.getContext() >> activity
        anotherView.getContext() >> activity

        when: 'stamp first'
        impObserver.markViewImpression(new ImpressionMark(view, 'view_id').setGlobalId("view").setNum(10).setVariable(json))
        def scope = impObserver.mActivityScopes.get(activity)

        then:
        1 == scope.viewToTogglerWithViews.size()
        1 == scope.globalIdToImpEvent.size()
        scope.globalIdToImpEvent.get("view").mark.delayTimeMills == 0

        then: 'stamp again'
        impObserver.markViewImpression(new ImpressionMark(view, "view_id").setGlobalId("view").setNum(10).setVariable(json))
        then:
        1 == scope.viewToTogglerWithViews.size()

        then: 'stamp again num changed'
        impObserver.markViewImpression(new ImpressionMark(view, "view_id").setGlobalId("view").setNum(13).setVariable(json))
        then:
        scope.globalIdToImpEvent.get("view").mark.num == 13

        then: 'stamp anotherview with another id'
        impObserver.markViewImpression(new ImpressionMark(anotherView, "view_id").setGlobalId("anotherview").setNum(14).setVariable(json))
        then:
        scope.globalIdToImpEvent.size() == 2
        scope.globalIdToImpEvent.get('anotherview').mark.num == 14

        then: 'stamp anotherview again with view id'
        def oldImpEvent = scope.getImpEvent(anotherView)
        impObserver.markViewImpression(new ImpressionMark(anotherView, "view_id").setGlobalId("view").setNum(15).setVariable(json))
        then:
        scope.nextPassInvisible.size() == 1
        scope.nextPassInvisible.contains(oldImpEvent)
        scope.viewToTogglerWithViews.size() == 2

    }

    def 'checkImp'(){
        setup:
        def visibleView = Mock(View)
        def inVisibleView =Mock(View)

        PowerMockito.mockStatic(ViewHelper)
        PowerMockito.when(ViewHelper.viewVisibilityInParents(visibleView)).thenReturn(true)
        PowerMockito.when(ViewHelper.viewVisibilityInParents(inVisibleView)).thenReturn(false)

        ImpObserver.ActivityScope scope = new ImpObserver.ActivityScope(null)
        ImpObserver.TogglerWithViews togglerWithViews = new ImpObserver.TogglerWithViews(0)
        ImpObserver.ImpEvent impEvent = new ImpObserver.ImpEvent()
        impEvent.mark = new ImpressionMark(visibleView, "visibleView").setCollectContent(false)
        impEvent.lastVisible = true
        togglerWithViews.addView(visibleView, impEvent, scope)

        ImpObserver.ImpEvent inVisibleImpEvent = new ImpObserver.ImpEvent()
        inVisibleImpEvent.mark = new ImpressionMark(inVisibleView, "inVisibleView")
        togglerWithViews.addView(inVisibleView, inVisibleImpEvent, scope)

        def activity = new Activity()
        coreAppState.setResumedActivity(activity)
        impObserver.mActivityScopes = new WeakHashMap<>()
        impObserver.mActivityScopes.put(activity, scope)
        impObserver.viewTreeChangeTimerToggler = Mock(TimerToggler)

        when:
        impObserver.checkImp(togglerWithViews, togglerWithViews.impViews)
        then:
        coreAppState.getResumedActivity() >> activity

        when:
        impEvent.lastVisible = false
        impObserver.checkImp(togglerWithViews, togglerWithViews.impViews)

        then:
        coreAppState.getResumedActivity() >>  activity
        def e = thrown(RuntimeException)
        e.message.contains('Method i in android.util.Log')
    }
}
