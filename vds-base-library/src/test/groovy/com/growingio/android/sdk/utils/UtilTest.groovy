package com.growingio.android.sdk.utils

import spock.lang.Specification

/**
 * Created by liangdengke on 2019/1/17.
 */
class UtilTest extends Specification {

    def "check pattern server xpath same"(){

        when: 'string same'
        def filterXPath = 'OK'
        def elemXpath = 'OK'
        then:
        Util.isIdentifyPatternServerXPath(filterXPath, elemXpath)

        when: 'not same string'
        elemXpath = 'F'
        then:
        !Util.isIdentifyPatternServerXPath(filterXPath, elemXpath)

        when: 'pattern same'
        filterXPath = '/MainActivity[10]/Fragment[*]'
        elemXpath = '/MainActivity[10]/Fragment[32]'
        then:
        Util.isIdentifyPatternServerXPath(filterXPath,elemXpath)

        when: 'pattern not same'
        elemXpath = '/MainActivity[10]/Fragment[32'
        then:
        !Util.isIdentifyPatternServerXPath(filterXPath, elemXpath)

        when: 'pattern contain -'
        elemXpath = '/MainActivity[10]/Fragment[-]'
        then:
        Util.isIdentifyPatternServerXPath(filterXPath, elemXpath)

        when: 'pattern contain -, and not same'
        elemXpath = '/MainActivity[10]/Fragment[-]/Fine'
        then:
        !Util.isIdentifyPatternServerXPath(filterXPath, elemXpath)
    }
}
