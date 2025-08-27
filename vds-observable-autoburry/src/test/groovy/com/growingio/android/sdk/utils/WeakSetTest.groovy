package com.growingio.android.sdk.utils

import org.junit.Assert
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/8/6.
 */
class WeakSetTest extends Specification {

    private WeakSet<InnerClass> weakSet

    def setup(){
        weakSet = new WeakSet<>()
    }

    def "Size"() {
        Assert.assertEquals(0, weakSet.size())

        when: 'add one item to WeakSet'
        weakSet.add(new InnerClass("First"))
        then:
        1 == weakSet.size()
    }

    def "Size after gc"(){
        Assert.assertTrue(weakSet.isEmpty())

        when: 'add two item, and gc'
        weakSet.add(new InnerClass("First"))
        weakSet.add(new InnerClass("Second"))
        Assert.assertEquals(2, weakSet.size())
        systemGc()

        then:
        weakSet.isEmpty()
        0 == weakSet.size()
    }

    private static void systemGc() {
        System.gc()
        System.gc()
        Thread.sleep(500)
    }

    def "Contains"() {
        when:
        weakSet.add(new InnerClass("First"))
        weakSet.add(new InnerClass("Second"))

        then:
        weakSet.contains(new InnerClass("First"))
        !weakSet.contains(new InnerClass("Third"))
    }

    def "Iterator with empty set"() {
        when: 'not after gc'
        def iterator = weakSet.iterator()
        then:
        !iterator.hasNext()

        when: 'after gc'
        weakSet.add(new InnerClass("First"))
        weakSet.add(new InnerClass("Second"))

        systemGc()

        def iterator2 = weakSet.iterator()

        then:
        !iterator2.hasNext()
    }

    def "Iterator with not empty set"(){
        when: 'just one item'
        def itemOne = new InnerClass("First")
        weakSet.add(itemOne)
        def iterator = weakSet.iterator()

        then:
        1 == weakSet.size()
        iterator.hasNext()
        'First' == (++iterator).name

        when: 'gc one item'
        weakSet.add(new InnerClass("Second"))
        systemGc()
        iterator = weakSet.iterator()

        then:
        1 == weakSet.size()
        iterator.hasNext()
        'First' == (++iterator).name
    }

    def "Remove"() {
        when: 'remove a item which is not in WeakSet'
        weakSet.remove(new InnerClass("First"))

        then:
        weakSet.isEmpty()

        when: 'remove a item which is in WeakSet'
        weakSet.add(new InnerClass("First"))
        weakSet.remove(new InnerClass("First"))

        then:
        0 == weakSet.size()
    }

    def "Clear"() {
        when:
        weakSet.add(new InnerClass("First"))
        weakSet.add(new InnerClass("Second"))
        weakSet.add(new InnerClass("Third"))

        weakSet.clear()

        then:
        weakSet.isEmpty()

        when: 'after add'
        weakSet.add(new InnerClass("Fourth"))
        then:
        1 == weakSet.size()
    }

    private static class InnerClass{
        private String name;
        InnerClass(String name){
            this.name = name;
        }

        @Override
        String toString() {
            return name;
        }

        @Override
        boolean equals(Object obj) {
            if (this.is(obj))
                return true;
            if (obj instanceof InnerClass)
                return name == obj.name
            return false;
        }

        @Override
        int hashCode() {
            return name.hashCode();
        }
    }
}
