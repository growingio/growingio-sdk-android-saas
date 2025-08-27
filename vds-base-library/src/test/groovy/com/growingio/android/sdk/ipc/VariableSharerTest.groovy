package com.growingio.android.sdk.ipc

import com.growingio.android.sdk.collection.GConfig
import spock.lang.Specification

import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by liangdengke on 2018/8/13.
 */
class VariableSharerTest extends Specification {

    private VariableSharer variableSharer;

    def setup(){
        GConfig.DEBUG = false
        GConfig.isReplace = true
        File file = new File("/tmp/test.sid");
        if (file.exists())
            file.delete()
        variableSharer = new VariableSharer(file, true, 10)
    }

    public void makeEntityChanged(){
        for (VariableEntity entity : variableSharer.entityList){
            entity.changed = true
        }
    }

    def "test check and prepare magic"(){
        setup:
        variableSharer.completeMetaData([].toSet());

        when: 'first check magic'
        variableSharer.checkOrPrepareMagic()
        then:
        variableSharer.isUsingMultiProcess()

        when: 'wrong magic num'
        def originalMagic = variableSharer.MAGIC_NUM
        variableSharer.MAGIC_NUM = 0x34
        variableSharer.checkOrPrepareMagic()
        variableSharer.MAGIC_NUM = originalMagic
        then:
        !variableSharer.isUsingMultiProcess()
    }

    def "test get int"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var2"))
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var3"))
        variableSharer.completeMetaData([].toSet())

        when: 'original value is zero'
        def zero = variableSharer.getIntByIndex(1)
        then:
        0 == zero

        when: 'non zero value'
        variableSharer.putIntByIndex(1, 200)
        makeEntityChanged()
        def nonZero = variableSharer.getIntByIndex(1)
        then:
        200 == nonZero
    }

    def "test save and restore string"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var2"))
        def str = "MyName is LDK: 中文"
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("string", str.length()))
        variableSharer.completeMetaData([].toSet())

        when:
        variableSharer.putStringByIndex(2, str)
        makeEntityChanged()
        def restore = variableSharer.getStringByIndex(2)
        then:
        str == restore
        variableSharer.usingMultiProcess
    }

    def "test save and restore long"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createLongVariable("var1"))
        variableSharer.completeMetaData([].toSet())

        when:
        variableSharer.putLongByIndex(0, 1000L)
        makeEntityChanged()
        variableSharer.entityList.get(0).changed = true
        def restore = variableSharer.getLongByIndex(0)

        then:
        1000L == restore
    }


    def "test compare and set int "(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.completeMetaData([].toSet())

        when:
        variableSharer.putIntByIndex(0, 10)
        def shouldFalse = variableSharer.compareAndSetIntByIndex(0, 32, 44)
        variableSharer.entityList.get(0).changed = true
        def oldValue = variableSharer.getIntByIndex(0)

        then:
        !shouldFalse
        10 == oldValue

        when:
        def shouldTrue = variableSharer.compareAndSetIntByIndex(0, 10, 100)
        variableSharer.entityList.get(0).changed = true
        def newValue = variableSharer.getIntByIndex(0)

        then:
        shouldTrue
        100 == newValue
    }

    def "test put null"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("var1", 20))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("var2", 20))
        variableSharer.completeMetaData([].toSet())

        when:
        variableSharer.putStringByIndex(0, null)
        variableSharer.mByteBuffer.position(variableSharer.metaBaseAddress)
        variableSharer.mByteBuffer.putInt(400)
        variableSharer.mByteBuffer.putInt(20)
        then:
        null == variableSharer.getStringByIndex(0)
    }

    def "test repair pid"(){
        variableSharer.mByteBuffer = ByteBuffer.allocate(2 + 2 + 4 * 5)
        variableSharer.mPid = 100

        when:
        variableSharer.mByteBuffer.rewind()
        variableSharer.mByteBuffer.putShort((short)3)
        variableSharer.mByteBuffer.putShort((short)4)
        variableSharer.mByteBuffer.putInt(10)
        variableSharer.mByteBuffer.putInt(11)
        variableSharer.mByteBuffer.putInt(12)
        variableSharer.mByteBuffer.putInt(13)
        variableSharer.repairPid([10, 11, 12, 13].toSet())
        then:
        3 == variableSharer.mByteBuffer.getShort(0)
        5 == variableSharer.mByteBuffer.getShort(2)
        100 == variableSharer.mByteBuffer.getInt(4 + 4 * 4)
    }

    def "test repair pid with all died"(){
        variableSharer.mByteBuffer = ByteBuffer.allocate(2 + 2 + 4 * 5)
        variableSharer.mPid = 100

        when:
        variableSharer.mByteBuffer.rewind()
        variableSharer.mByteBuffer.putShort((short)3)
        variableSharer.mByteBuffer.putShort((short)4)
        variableSharer.mByteBuffer.putInt(10)
        variableSharer.mByteBuffer.putInt(11)
        variableSharer.mByteBuffer.putInt(12)
        variableSharer.mByteBuffer.putInt(13)
        variableSharer.repairPid([].toSet())
        then:
        3 == variableSharer.mByteBuffer.getShort(0)
        1 == variableSharer.mByteBuffer.getShort(2)
        100 == variableSharer.mByteBuffer.getInt(4)
    }

    def "test checkEntityChanged"(){
        def byteBuffer = ByteBuffer.allocate(4 * 6)
        variableSharer.mByteBuffer = byteBuffer

        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("var2", 20))
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var3"))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("var4", 10))
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var5"))
        variableSharer.mPid = 100
        variableSharer.metaBaseAddress = 0

        def init = {
            0.upto(4){
                variableSharer.entityList.get(it).changed = false
            }
        }

        when: 'with no change'
        variableSharer.checkEntityChanged()
        then:
        variableSharer.entityList.get(0).changed
        variableSharer.entityList.get(4).changed

        when: 'with total mod change'
        init.run()
        byteBuffer.rewind()
        byteBuffer.putInt(200)
        variableSharer.checkEntityChanged()
        then:
        !variableSharer.entityList.get(0).changed
        !variableSharer.entityList.get(4).changed

        when: 'with total not change'
        init.run()
        variableSharer.checkEntityChanged()
        then:
        !variableSharer.entityList.get(0).changed
        !variableSharer.entityList.get(4).changed

        when: 'with change first'
        init.run()
        byteBuffer.rewind()
        byteBuffer.putInt(32)
        byteBuffer.putInt(45)
        variableSharer.checkEntityChanged()
        then:
        variableSharer.entityList.get(0).changed
        !variableSharer.entityList.get(4).changed

        when: 'with change last'
        init.run()
        byteBuffer.position(4 * 5)
        byteBuffer.putInt(32)
        byteBuffer.rewind()
        byteBuffer.putInt(68)
        variableSharer.checkEntityChanged()
        then:
        !variableSharer.entityList.get(0).changed
        variableSharer.entityList.get(4).changed
    }

    def "test time consume for init"(){
        when:
        def startTime = System.currentTimeMillis()
        def sharer = new VariableSharer(new File("/tmp/test.init"), true, 300)
        sharer.addVariableEntity(VariableEntity.createIntVariable("age"))
        sharer.addVariableEntity(VariableEntity.createStringVariable("name", 20))
        sharer.addVariableEntity(VariableEntity.createIntVariable("weight"))
        sharer.addVariableEntity(VariableEntity.createStringVariable("country", 50))
        sharer.completeMetaData([].toSet())

        then:
        println "first init time: ${System.currentTimeMillis() - startTime}"
    }

    def "test time consume one time"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testName", 20))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testAge", 5))
        variableSharer.completeMetaData([].toSet())

        when:
        def startTime = System.currentTimeMillis()
        variableSharer.putIntByIndex(0, 10)
        def result = variableSharer.getIntByIndex(0)
        then:
        10 == result
        println "单次, 读写int: ${System.currentTimeMillis() - startTime}"

        when:
        startTime = System.currentTimeMillis()
        variableSharer.putStringByIndex(1, "我的世界")
        result = variableSharer.getStringByIndex(1)
        then:
        "我的世界" == result
        println "单次, 读写String: ${System.currentTimeMillis() - startTime}"
    }

    def "test time consume"(){

        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testName", 20))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testAge", 5))
        variableSharer.completeMetaData([].toSet())

        when:
        def startTime = System.currentTimeMillis()
        0.upto(10000){
            variableSharer.putIntByIndex(0, it)
            makeEntityChanged()
            def variable = variableSharer.getIntByIndex(0)
            assert variable == it
        }

        then:
        println "10000次读写int   值耗时: ${System.currentTimeMillis() - startTime}"

        when:
        startTime = System.currentTimeMillis()
        0.upto(10000){
            variableSharer.putStringByIndex(1, "value: $it")
            def variable = variableSharer.getStringByIndex(1)
            assert  "value: $it" == variable
        }

        then:
        println "10000次读写String值耗时: ${System.currentTimeMillis() - startTime}"
    }

    def "test multi process read write"(){
        setup:
        variableSharer.addVariableEntity(VariableEntity.createIntVariable("var1"))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testName", 20))
        variableSharer.addVariableEntity(VariableEntity.createStringVariable("testAge", 5))
        variableSharer.completeMetaData([].toSet())

        when:
        def startTime = System.currentTimeMillis()
        def threadPool = Executors.newFixedThreadPool(5)
        0.upto(100000){
            threadPool.submit(new Runnable() {
                @Override
                void run() {
                    variableSharer.putStringByIndex(1, "value: $it")
                    def variable = variableSharer.getStringByIndex(1)
                    assert it == variable
                }
            })
        }
        threadPool.shutdown()
        threadPool.awaitTermination(5, TimeUnit.MINUTES)

        then:
        println "线程池, 100000次 String读写, 耗时：${System.currentTimeMillis() - startTime}"

        when:
        startTime = System.currentTimeMillis()
        threadPool = Executors.newFixedThreadPool(5)
        0.upto(100000){
            threadPool.submit(new Runnable() {
                @Override
                void run() {
                    variableSharer.putIntByIndex(0, it)
                    def variable = variableSharer.getIntByIndex(0)
                    assert it == variable
                }
            })
        }
        threadPool.shutdown()
        threadPool.awaitTermination(5, TimeUnit.MINUTES)

        then:
        println "线程池, 100000次 int读写: 耗时: ${System.currentTimeMillis() - startTime}"
    }

    def "test shared lock"(){
        when:
        def max = Integer.MAX_VALUE + 10
        then:
        println max
    }
}
