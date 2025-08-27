package com.growingio.android.sdk.utils

import junit.framework.Assert
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/12/18.
 */
class LinkedStringTest extends Specification {

    def "test normal append"(){
        def builder = new StringBuilder()
        def lString = new LinkedString()

        when:
        builder.append("GOOD")
        lString.append("GOOD")
        builder.append("STUDY")
        lString.append("STUDY")

        then:
        builder.length() == lString.length()
        builder.toString() == lString.toStringValue()
    }

    def "test LinkedString copy"(){
        def builderHead = new StringBuilder()
        def lStringHead = new LinkedString()
        builderHead.append("head1")
        lStringHead.append("head1")

        when:
        def builder = new StringBuilder()
        builder.append(builderHead.toString())
        def lString = LinkedString.copy(lStringHead)
        builder.append("content1")
        builder.append("content2")
        lString.append("content1")
        lString.append("content2")

        then:
        builder.length() == lString.length()
        builder.toString() == lString.toStringValue()
    }

    def "test iterator"(){
        def builder = new StringBuilder()
        def lString = new LinkedString()

        builder.append("head1")
        lString.append("head1")
        builder.append("head2")
        lString.append("head2")

        when:
        LinkedString.LinkedStringIterator iterator = lString.iterator()
        builder.toString().getChars().each {
            Assert.assertTrue(iterator.hasNext())
            Assert.assertEquals(it, iterator.next())
        }

        then:
        true
    }

    def "test equal"(){
        when:
        def lString1 = new LinkedString()
        lString1.append("GOOD")
        lString1.append(" S")
        lString1.append("TUDY")

        def lString2 = LinkedString.fromString("GOOD STUDY")

        then:
        lString1 == lString2

        when:
        def lString3 = new LinkedString()
        lString3.append("GOOD STUAY")

        then:
        lString1 != lString3
    }

    def "test ends with something"(){
        when:
        def lString = new LinkedString()
        lString.append("GOOD")
        lString.append("STUDY")
        lString.append("FINE")
        lString.append("OK")

        then:
        !lString.endsWith("XXXxxxxxxxxxxxxxxxxxxxxx")
        lString.endsWith("EOK")

        !new LinkedString().endsWith("jkjk")
    }

    def "test hash same"(){
        when:
        def lString = new LinkedString()
        lString.append("GOOD")
        lString.append("STUDY")

        then:
        lString.hashCode() == lString.hashCode()
        "GOODSTUDY".hashCode() == lString.hashCode()
    }
}
