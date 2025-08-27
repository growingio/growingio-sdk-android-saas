package com.growingio.android.sdk.data.net

import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class AsyncTimeoutTest extends Specification {

    def "just one request, normal"(){
        setup:
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection connection = Mock(HttpURLConnection)

        def out = new AsyncTimeout.Timeout(connection, 500l)
        when:
        timeout.enter(out)
        timeout.exit(out)
        Thread.sleep(1000)

        then:
        0 * connection.disconnect()
    }

    def "just on request, timeout"(){
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection connection = Mock(HttpURLConnection)

        def out = new AsyncTimeout.Timeout(connection, 500l)
        when:
        timeout.enter(out)
        Thread.sleep(1000)
        timeout.exit(out)

        then:
        1 * connection.disconnect()
    }

    def "multiple requests, normal"(){
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection connection = Mock(HttpURLConnection)
        AtomicInteger completeCount = new AtomicInteger(0)

        when:
        (0..20).each {
            Thread.start{
                def out = new AsyncTimeout.Timeout(connection, 500l)
                timeout.enter(out)
                timeout.exit(out)
                completeCount.addAndGet(1)
            }
        }
        Thread.sleep(1000)
        then:
        0 * connection.disconnect()
        21 == completeCount.toInteger()
    }

    def "multiple requests, all timeout"(){
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection connection = Mock(HttpURLConnection)
        AtomicInteger completeCount = new AtomicInteger(0)

        when:
        (0..20).each {
            Thread.start {
                def out = new AsyncTimeout.Timeout(connection, 200l)
                timeout.enter(out)
                Thread.sleep(300)
                timeout.exit(out)
                completeCount.addAndGet(1)
            }
        }
        Thread.sleep(2000)
        then:
        21 * connection.disconnect()
        21 == completeCount.toInteger()
    }

    def "multiple requests, half timeout"(){
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection connection = Mock(HttpURLConnection)
        AtomicInteger completeCount = new AtomicInteger(0)

        when:
        (0..20).each {
            int i = it
            Thread.start {
                def out = new AsyncTimeout.Timeout(connection, 200l)
                timeout.enter(out)
                if (i % 2 == 0){
                    Thread.sleep(300)
                }
                timeout.exit(out)
                completeCount.addAndGet(1)
            }
        }
        Thread.sleep(2000)
        then:
        21 == completeCount.toInteger()
        11 * connection.disconnect()
    }

    def "two requests, late request, timeout short"(){
        AsyncTimeout timeout = new AsyncTimeout()
        HttpURLConnection firstConnection = Mock(HttpURLConnection)
        HttpURLConnection secondConnection = Mock(HttpURLConnection)

        when:
        def first = new AsyncTimeout.Timeout(firstConnection, 600l)
        timeout.enter(first)
        Thread.sleep(100)
        def second = new AsyncTimeout.Timeout(secondConnection, 300l)
        timeout.enter(second)
        Thread.sleep(300)
        timeout.exit(first)
        timeout.exit(second)

        then:
        1 * secondConnection.disconnect()
        0 * firstConnection.disconnect()
    }

    @Ignore
    def "test HttpUrlConnection disconnect"(){
        setup:
        AsyncTimeout timeout = new AsyncTimeout()
        URL url = new URL("http://192.168.55.32:8080")
        HttpURLConnection connection = url.openConnection()
        connection.setConnectTimeout(10_000)
        connection.setReadTimeout(5_000)
        connection.setRequestMethod("POST")
        connection.setDoOutput(true)
        connection.setChunkedStreamingMode(4096)
        OutputStream outputStream = connection.getOutputStream()
        println "output stream 已获取， 请断开连接"
        def out = new AsyncTimeout.Timeout(connection, 20_000)
        timeout.enter(out)
        (1..10000).each {
            println("write: $it")
            outputStream.write(new byte[4 * 1024])
            outputStream.flush()
        }
        timeout.exit(out)
        outputStream.close()
        println("outputStream close")
        Thread.sleep(5000)
        println("start get responseCode")
        println connection.getResponseCode()
    }
}
