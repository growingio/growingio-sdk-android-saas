package com.growingio.android.sdk.api

import com.growingio.android.sdk.models.Tag
import com.growingio.android.sdk.models.ViewAttrs
import spock.lang.Specification

/**
 * Created by liangdengke on 2019/1/8.
 */
class TagAPITest extends Specification {

    def "compatible with pattern server"(){

        TagAPI api = new TagAPI()

        when:
        api.tags.clear()
        Tag tag = new Tag()
        tag.filter = new ViewAttrs()
        tag.filter.xpath = "/MainWindows/Lineage/jkjkjk/"
        api.onReceiveTag(tag)

        then:
        1 == api.tags.size()
        "/MainWindows/Lineage/jkjkjk/" == api.tags[0].filter.xpath


        when: 'xpath with comma'
        api.tags.clear()
        Tag commaTag = new Tag()
        commaTag.filter = new ViewAttrs()
        commaTag.filter.xpath = "/MainWindow/Xyz,/MainWindow/ZZX"
        api.onReceiveTag(commaTag)

        then:
        2 == api.tags.size()
        "/MainWindow/Xyz" == api.tags[0].filter.xpath
        "/MainWindow/ZZX" == api.tags[1].filter.xpath
    }

    def "compatible with pattern server with start"(){
        TagAPI api = new TagAPI()

        when:
        api.tags.clear()
        Tag tag = new Tag()
        tag.filter = new ViewAttrs()
        tag.filter.xpath= "/MainWindows/Fragment[10]/LinearLayout[20],/MainWindows/Fragment[*]/LinearLayout[20]"
        api.onReceiveTag(tag)

        then:
        1 == api.tags.size()
        "/MainWindows/Fragment[*]/LinearLayout[20]" == api.tags[0].filter.xpath
    }
}
