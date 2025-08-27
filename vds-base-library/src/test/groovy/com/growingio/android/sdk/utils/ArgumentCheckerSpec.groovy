package com.growingio.android.sdk.utils

import android.util.Log
import com.growingio.android.sdk.collection.GConfig
import org.json.JSONArray
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

import java.lang.reflect.Constructor

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([Log.class])
class ArgumentCheckerSpec extends Specification{

    ArgumentChecker gio

    def setup(){
        PowerMockito.mockStatic(Log.class)
        PowerMockito.when(Log.i(Mockito.anyString(), Mockito.anyString())).thenReturn(1)
        Constructor initMethod = GConfig.getDeclaredConstructor()
        initMethod.setAccessible(true)
        def config = initMethod.newInstance()
        gio = new com.growingio.android.sdk.utils.ArgumentChecker(config)
    }

    def "isIllegalEventName: with right syntax"(){

        def rightEventNames = ["aHello", "_aHello", "a", "中Hello", "Hel中文lo", "23jkjk", ":haha"]

        for (String it: rightEventNames){
            def result = gio.isIllegalEventName(it)
            assert !result
        }
        cleanup:
        println("OK")
    }

    def "isIllegalEventName: empty or too long"(){
        when:
        def result = gio.isIllegalEventName(null)
        then:
        result

        when:
        result = gio.isIllegalEventName("")
        then:
        result

        when:
        result = gio.isIllegalEventName(" ")
        then:
        result

        when:
        result = gio.isIllegalEventName("    ")
        then:
        result

        when:
        result = gio.isIllegalEventName("  \n   ")
        then:
        result

        when:
        result = gio.isIllegalEventName("a" * 51)
        then:
        result
    }

    def "validJSONObject: no error"(){
        JSONObject obj = new JSONObject();
        obj.put("key1", 10)
        obj.put("key2", "String")
        obj.put("key3", "世界")
        when:
        def result = gio.validJSONObject(obj)

        then:
        result.length() == 3
        result.getString("key2") == "String"
    }

    def "validJSONObject: with JSONObject and JSONArray"(){
        JSONObject obj = new JSONObject()
        JSONObject param = new JSONObject()
        obj.put("obj", param)
        JSONArray array = new JSONArray();
        obj.put("array", array)

        when:
        def result = gio.validJSONObject(obj)
        then:
        result == null


        JSONObject obj2 = new JSONObject()
        obj2.put("key1","names")
        when:
        result = gio.validJSONObject(obj2)
        then:
        result.length() == 1
    }

    def "validJSONObject: with value error"(){
        JSONObject obj = new JSONObject()
        obj.put("empty", null)

        when:
        def result = gio.validJSONObject(obj)
        then:
        0 == result.length()

        obj.put("max_long", "a" * 1001)
        when:
        result = gio.validJSONObject(obj)
        then:
        result == null

        when:
        obj = new JSONObject()
        obj.put("empty_string", "")
        obj.put("chinese_empty", " ")
        result = gio.validJSONObject(obj)
        then:
        2 == result.length()
        "" == result.getString("empty_string")
        "" == result.getString("chinese_empty")
    }
}