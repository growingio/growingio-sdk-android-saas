package com.growingio.android.sdk.data.helpers

import com.growingio.android.sdk.utils.GJSONStringer
import com.growingio.android.sdk.utils.LinkedString
import org.json.JSONObject
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/12/18.
 */
class GJSONStringerTest extends Specification {

    def "test normal json"(){
        JSONObject jsonObject = new JSONObject()
        jsonObject.put("key1", "value1")
        jsonObject.put("key2", 2)
        jsonObject.put("key3", 3.4)

        when:
        def stringer = new GJSONStringer()
        def result = stringer.convertToString(jsonObject)
        def resultJSON = new JSONObject(result)

        then:
        "value1" == resultJSON.get("key1")
        2 == resultJSON.get("key2")
        3.4 == resultJSON.get("key3")
    }

    def "test json with linked string"(){

        LinkedString linkedString = new LinkedString()
        linkedString.append("GOOD").append(" ").append("STUDY")

        JSONObject jsonObject = new JSONObject()
        jsonObject.put("key1", linkedString)

        when:
        def resultJSON = new JSONObject(new GJSONStringer().convertToString(jsonObject))

        then:
        "GOOD STUDY" == resultJSON.get("key1")
    }
}
