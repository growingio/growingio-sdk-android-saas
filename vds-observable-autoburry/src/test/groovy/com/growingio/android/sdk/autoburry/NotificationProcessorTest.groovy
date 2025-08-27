package com.growingio.android.sdk.autoburry

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import com.growingio.android.sdk.FakeIntent
import com.growingio.android.sdk.base.event.NewIntentEvent
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.GConfig
import com.growingio.android.sdk.utils.Utils
import spock.lang.Specification

import java.util.regex.Pattern

/**
 * Created by liangdengke on 2018/11/13.
 */
class NotificationProcessorTest extends Specification {

    NotificationProcessor notificationProcessor
    Context context;

    def setup(){
        GConfig.DEBUG = false
        notificationProcessor = new NotificationProcessor()
        notificationProcessor.enable = true
        notificationProcessor.coreAppState = Mock(CoreAppState)
        notificationProcessor.pushFile = File.createTempDir()
        GConfig.DEBUG = false
    }

    def "hookPendingIntent, and notify notification"(){
        def intent = new FakeIntent()

        println("pushFile: " + notificationProcessor.pushFile)

        when: 'hook pending intent create'
        notificationProcessor.hookPendingIntentCreateBefore(intent)
        def pendingIntent = Mock(PendingIntent)
        notificationProcessor.hookPendingIntentCreateAfter(intent, pendingIntent)

        then: 'intent should has gio id'
        intent.hasExtra(NotificationProcessor.GIO_ID_KEY)
        def gioId = intent.getStringExtra(NotificationProcessor.GIO_ID_KEY)
        notificationProcessor.pendingIntent2Ids.get(pendingIntent) == gioId

        when: 'notify notification'
        def notification = new Notification()
        notification.contentIntent = pendingIntent
        notificationProcessor.onNotify(null, 10, notification)
        notificationProcessor.onIntent(new NewIntentEvent(intent))

        then:
        true
    }

    def "parse double"(){
        when:
        def result = Double.parseDouble("8.1.0")
        then:
        thrown NumberFormatException

        when:
        Pattern pattern = Pattern.compile("[0-9]\\.[0-9]")
        def match = pattern.matcher("[Huawei]_8.1.0")
        def content = null
        if (match.find()){
            content = match.group()
        }
        then:
        content == '8.1'
    }

    def "generate Notification hook string"(){
        setup:
        println()
        def pendingMethods = ['getActivity', 'getBroadcast', 'getService']
        PendingIntent.getDeclaredMethods().each {
            if (pendingMethods.contains(it.name)){
                println(Utils.constructorMethodStr(it))
            }
        }
        println()
        def vdsAgentMethods = ['onPendingIntentGetActivityBefore',
                               'onPendingIntentGetActivityAfter',
                               'onPendingIntentGetActivityShortBefore',
                               'onPendingIntentGetActivityShortAfter',
                               'onPendingIntentGetBroadcastBefore',
                               'onPendingIntentGetBroadcastAfter',
                               'onPendingIntentGetServiceBefore',
                               'onPendingIntentGetServiceAfter',
                               'onPendingIntentGetForegroundServiceBefore',
                               'onPendingIntentGetForegroundServiceAfter',
                               'onNotify',
                               'onBroadcastReceiver',
                               'onServiceStart',
                               'onServiceStartCommand']
        VdsAgent.getDeclaredMethods().each {
            if (vdsAgentMethods.contains(it.name)){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()
        def notifyMethods = ['notify']
        NotificationManager.getDeclaredMethods().each {
            if (notifyMethods.contains(it.name)){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()
        Service.getDeclaredMethods().each {
            if (it.name == 'onStartCommand' || it.name == 'onStart'){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()
        BroadcastReceiver.getDeclaredMethods().each {
            if (it.name == 'onReceive'){
                println(Utils.constructorMethodStr(it))
            }
        }
    }
}
