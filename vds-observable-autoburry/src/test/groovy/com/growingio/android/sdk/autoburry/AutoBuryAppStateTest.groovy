package com.growingio.android.sdk.autoburry

import android.app.Activity
import android.app.Dialog
import android.app.Fragment
import android.content.Context
import android.view.View
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.GConfig
import spock.lang.Specification

import java.lang.ref.WeakReference

/**
 * Created by liangdengke on 2018/8/6.
 */
class AutoBuryAppStateTest extends Specification {

    private CoreAppState coreAppState;
    private AutoBuryAppState autoBuryAppState;
    private GConfig gConfig;

    def setup(){
        // just for OnGIODialogShow and hideGIODialog
        gConfig = Mock(GConfig)
        GConfig.isReplace = false
        coreAppState = new CoreAppState(gConfig, Mock(Context))
        autoBuryAppState = new AutoBuryAppState(coreAppState, gConfig)
        GConfig.DEBUG = false
    }

    def "OnGIODialogShow"() {
        def activity = Mock(Activity)
        def dialog = Mock(Dialog)

        setup:
        coreAppState.mActivitiesWithGioDialogs = null

        when:
        coreAppState.onGIODialogShow(activity, dialog)

        then:
        1 == coreAppState.mActivitiesWithGioDialogs.get(activity).size()

        when: 'add two dialog'
        coreAppState.mActivitiesWithGioDialogs = null
        coreAppState.onGIODialogShow(activity, dialog)
        coreAppState.onGIODialogShow(activity, Mock(Dialog))
        then:
        2 == coreAppState.mActivitiesWithGioDialogs.get(activity).size()

        when: 'add two dialog, but gc one'
        coreAppState.mActivitiesWithGioDialogs = null
        def mockDialog = new WeakReference<>(Mock(Dialog))
        coreAppState.onGIODialogShow(activity, dialog)
        coreAppState.onGIODialogShow(activity, mockDialog.get())
        System.gc()
        System.gc()
        Thread.sleep(500)

        then:
        coreAppState.mActivitiesWithGioDialogs.get(activity).size() != 0
    }

    def "test weakRef"(){
        when:
        def weakRef = new WeakReference(new Date())
        System.gc()
        System.gc()
        Thread.sleep(500)

        then:
        weakRef.get() == null
    }

    def "hideGIODialog"(){
        setup:
        coreAppState.mActivitiesWithGioDialogs = null
        def activity = Mock(Activity)
        def dialog = Mock(Dialog)
        def dialog2 = Mock(Dialog)

        when:
        coreAppState.onGIODialogShow(activity, dialog)
        coreAppState.onGIODialogShow(activity, dialog2)
        coreAppState.hideGIODialog(activity)
        then:
        1 * dialog.dismiss()
        0 * dialog2.dismiss()
        null == coreAppState.mActivitiesWithGioDialogs.get(activity)
        1 * dialog2.isShowing() >> false
        1* dialog.isShowing() >> true

        when:
        coreAppState.mActivitiesWithGioDialogs = null
        coreAppState.hideGIODialog(activity)
        then:
        null == coreAppState.mActivitiesWithGioDialogs
    }

    def "trackAllFragment without ignore"(){
        def currentActivity = Mock(Activity)
        def mockSuperFragment = Mock(SuperFragment)
        def mockView = Mock(View)
        def mockFragment = Mock(Fragment){
            getView() >> mockView
        }

        when: 'without track'
        def result = autoBuryAppState.isFragmentView(currentActivity, mockView)
        then:
        !result

        when: 'with track'
        autoBuryAppState.trackFragment(currentActivity, mockFragment)
        then:
        thrown(ExceptionInInitializerError)

        when:
        result = autoBuryAppState.isFragmentView(currentActivity, mockView)
        then:
        result
    }

    def "shouldTrackFragment"(){
        def currentActivity = Mock(Activity)
        def mockFragment = Mock(Fragment)
        def mockSuperFragment = Mock(SuperFragment){
            getFragment() >> mockFragment
            isBelongActivity(currentActivity) >> true
        }

        when: 'with trackAllFragment'
        def result = autoBuryAppState.shouldTrackFragment(currentActivity, mockSuperFragment)
        then:
        result
        gConfig.shouldTrackAllFragment() >> true

        when: 'without trackAllFragment'
        result = autoBuryAppState.shouldTrackFragment(currentActivity, mockSuperFragment)
        then:
        !result
        gConfig.shouldTrackAllFragment() >> false

        when: 'with trackAllFragment ignore'
        autoBuryAppState.ignoreFragment(currentActivity, mockFragment)
        then:
        thrown(Throwable)

        when:
        result = autoBuryAppState.shouldTrackFragment(currentActivity, mockSuperFragment)
        then:
        !result
        gConfig.shouldTrackAllFragment() >> true

        when: 'without trackAllFragment and track'
        autoBuryAppState.trackFragment(currentActivity, mockFragment)
        then:
        thrown(NoClassDefFoundError)

        when:
        result = autoBuryAppState.shouldTrackFragment(currentActivity, mockSuperFragment)
        then:
        result
        gConfig.shouldTrackAllFragment() >> false

    }

    def "shouldTrackAllFragment"(){
        def currentActivity = Mock(Activity)

        when: 'with default true'
        def shouldTrackAllFragment = autoBuryAppState.shouldTrackAllFragment(currentActivity)
        then:
        shouldTrackAllFragment
        gConfig.shouldTrackAllFragment() >> true

        when: 'with default false'
        shouldTrackAllFragment = autoBuryAppState.shouldTrackAllFragment(currentActivity)
        then:
        !shouldTrackAllFragment
        gConfig.shouldTrackAllFragment() >> false

        when: 'with default true, and setTrackAllFragment false'
        autoBuryAppState.setTrackAllFragment(currentActivity, false)
        shouldTrackAllFragment = autoBuryAppState.shouldTrackAllFragment(currentActivity)
        then:
        !shouldTrackAllFragment
        gConfig.shouldTrackAllFragment() >> true

        when: 'with default false, and setTrackAllFragment true'
        autoBuryAppState.mTrackAllFragmentSpecialActivities.clear()
        autoBuryAppState.setTrackAllFragment(currentActivity, true)
        shouldTrackAllFragment = autoBuryAppState.shouldTrackAllFragment(currentActivity)
        then:
        shouldTrackAllFragment
        gConfig.shouldTrackAllFragment() >> false

        when: 'with default true, and setTrackAllFragment true'
        autoBuryAppState.setTrackAllFragment(currentActivity, true)
        then:
        gConfig.shouldTrackAllFragment() >> true
        def ex = thrown(Exception)
        ex.message.contains("mocked")

        when: 'with default false, and setTrackAllFragment false'
        autoBuryAppState.setTrackAllFragment(currentActivity, false)
        then:
        gConfig.shouldTrackAllFragment() >> false
        ex = thrown(Exception)
        ex.message.contains("mocked")
    }
}
