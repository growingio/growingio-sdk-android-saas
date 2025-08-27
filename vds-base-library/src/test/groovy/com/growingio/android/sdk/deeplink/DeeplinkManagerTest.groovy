package com.growingio.android.sdk.deeplink

import android.app.Activity
import android.content.*
import android.net.Uri
import android.text.TextUtils
import com.growingio.android.sdk.base.event.ValidUrlEvent
import com.growingio.android.sdk.collection.CoreAppState
import com.growingio.android.sdk.collection.CoreInitialize
import com.growingio.android.sdk.collection.DeviceUUIDFactory
import com.growingio.android.sdk.collection.GConfig
import com.growingio.android.sdk.utils.EventBusUtil
import com.growingio.android.sdk.utils.PowerMockUtils
import com.growingio.eventcenter.bus.EventBus
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification
import spock.lang.Unroll

@RunWith(PowerMockRunner)
@PowerMockRunnerDelegate(Sputnik)
@PrepareForTest([CoreInitialize, DeviceUUIDFactory, TextUtils])
class DeeplinkManagerTest extends Specification {

    DeeplinkManager manager
    Activity activity
    Context context
    EventBus eventBus
    GConfig config

    void setup() {
        config = Mock(GConfig)
        context = Mock(Context)
        manager = new DeeplinkManager(config, context)
        activity = Mock(Activity)
        eventBus = Mock(EventBus)
        EventBusUtil.mockEventBus(eventBus)
    }

    def "test handle null intent"() {


        setup:
        def intent = Mock(Intent)
        when:
        def result = manager.handleIntent(intent, activity)

        then:
        assert !result
    }


    def "test handle invalid intent"() {
        setup:
        def uri = Mock(Uri)
        def intent = Mock(Intent)

        when:
        def result = manager.handleIntent(intent, activity)

        then:
        !result
        intent.getData() >> uri
        intent.getAction() >> Intent.ACTION_VIEW
        1 * uri.getScheme()
    }

    def "handle valid depplink intent"() {
        setup:
        def intent = Mock(Intent)
        def uri = Mock(Uri)
        when:
        def result = manager.handleIntent(intent, activity)

        then:
        result
        intent.getData() >> uri
        uri.getScheme() >> "growing.xxxx"
    }

    def "handle valid applink intent"() {
        setup:
        def intent = Mock(Intent)
        def uri = Mock(Uri)
        when:
        def result = manager.handleIntent(intent, activity)

        then:
        result
        intent.getData() >> uri
        uri.getScheme() >> "https"
        uri.getHost() >> "gio.ren"
    }


    def getData(String data) {
        String result = ""
        for (int i = 0; i < data.length(); i++) {
            result += data.charAt(i).toString((char) 8204)
        }

        System.out.print("--->" + result)
        return result
    }

    def "test get data"() {
        when:
        def result = getData("hhhhhhhhhhh")
        then:
        result
    }

    def "check ClipBoard Invalid JSONObject"() {
        setup:
        def deeplinkInfo = Mock(DeeplinkInfo)
        def clipData = Mock(ClipData)
        def cm = Mock(ClipboardManager)
        def item = Mock(ClipData.Item)
        def clipDesc = Mock(ClipDescription)
        def charSequence = Mock(CharSequence)
        when:
        manager.cm = cm
        def result = manager.checkClipBoard(deeplinkInfo)
        then:
        !result
        cm.getPrimaryClip() >> clipData
        clipData.getItemAt(0) >> item
        clipData.getItemCount() >> 1
        clipData.getDescription() >> clipDesc
        clipDesc.getMimeType(0) >> ClipDescription.MIMETYPE_TEXT_HTML
        item.coerceToText(context) >> charSequence
        charSequence.length() >> 2000 * 16
        charSequence.charAt(0) >> 1
    }


    def "test onValidSchemeUrlIntent method"() {
        setup:
        PowerMockUtils.mockTextUtil()
        def uri = Mock(Uri)
        def validEvent = new ValidUrlEvent(uri, activity, ValidUrlEvent.APPLINK)
        def coreAppState = Mock(CoreAppState)
        def deviceFactory = Mock(DeviceUUIDFactory)
        PowerMockito.mockStatic(CoreInitialize)
        PowerMockito.mockStatic(DeviceUUIDFactory)
        PowerMockito.when(CoreInitialize.coreAppState()).thenReturn(coreAppState)
        PowerMockito.when(CoreInitialize.deviceUUIDFactory()).thenReturn(deviceFactory)

        when:
        deviceFactory.initUserAgent(context)
        manager = new DeeplinkManager(config, activity) {
            @Override
            def void handleAppLink(String trackId, boolean sendReengage, boolean isInApp, DeeplinkCallback callback) {
                assert trackId == "hhhhhh?nimeide#fangjiaba"
            }
        }
        manager.onValidSchemaUrlIntent(validEvent)

        then:
        coreAppState.getForegroundActivity() >> activity
        validEvent.data.getPath() >> "hhh"
        validEvent.data.toString() >> "https://tiantiankaixin:445/hhhhhh?nimeide#fangjiaba"
    }

    /**
     * 这里只测试处理 trackId 代码块。。。
     * @return
     */
    @Unroll
    def "test processTrackId code block"() {
        when:
        def result = processTrackIdCodeBlock(clickedLink)
        then:
        result == trackIdDetail
        where:
        trackIdDetail              | clickedLink
        "hhhhhh?nimeide#fangjiaba" | "https://tiantiankaixin:445/hhhhhh?nimeide#fangjiaba"
        "today?nimeide"            | "https://tiantiankaixin:445/today?nimeide"
        "hhhhhh/uuuuuu"            | "https://tiantiankaixin:445/hhhhhh/uuuuuu"
        "omg#hi"                   | "https://tiantiankaixin:445/omg#hi"
        "omg#hi"                   | "https://tiantiankaixin/omg#hi"
        "omg#hi"                   | "https://tiantiankaixin.com/omg#hi"
        "omg#hi"                   | "https://tiantiankaixin.com.cn/omg#hi"
        "omg#hi"                   | "https://tiantiankaixin.com.cn:9002/omg#hi"
    }

    def "processTrackIdCodeBlock"(String clickedUri) {
        return clickedUri.substring(clickedUri.indexOf("/", "https://".length()) + 1)
    }

}
