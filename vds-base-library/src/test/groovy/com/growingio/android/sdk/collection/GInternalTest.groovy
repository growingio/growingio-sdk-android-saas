package com.growingio.android.sdk.collection

import org.json.JSONObject
import spock.lang.Specification

class GInternalTest extends Specification {

    def "test add features"(){

        when: "init status"
        GInternal.getInstance()
        then:
        null == GInternal.getInstance().getFeaturesVersionJson()

        when: "addFeatures"
        GInternal.getInstance().addFeaturesVersion("gtouch", "0.5.60", "web-circle", "2")
        def featuresJson = GInternal.getInstance().getFeaturesVersionJson()
        def json = new JSONObject(featuresJson)

        then:
        json.has("gtouch")
        "2" == json.getString("web-circle")
        println(featuresJson)
    }
}
