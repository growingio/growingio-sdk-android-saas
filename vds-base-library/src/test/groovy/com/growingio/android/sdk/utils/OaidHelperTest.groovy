package com.growingio.android.sdk.utils

import android.content.Context
import android.net.Uri
import com.bun.miitmdid.core.ErrorCode
import com.bun.miitmdid.core.MdidSdkHelper
import com.bun.supplier.IdSupplier
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.MockGateway
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.core.transformers.impl.AbstractMainMockTransformer
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
// TODO: 插入代码后重新构建stackmaptable时引发异常，暂时关闭对应测试
//@PrepareForTest([MdidSdkHelper])
class OaidHelperTest extends Specification {

    // bug now
    def "testjava6"() {
        setup:
        ClassPool pool = ClassPool.getDefault()
        CtClass cc = pool.get("com.bun.miitmdid.core.MdidSdkHelper")
        for (CtMethod m : cc.declaredMethods) {
            if (m.name == "InitSdk") {
                m.insertBefore('{ Object value = org.powermock.core.MockGateway.methodCall($class, "InitSdk", $args, $sig, "int");if (value != org.powermock.core.MockGateway.PROCEED) return ((java.lang.Number)value).intValue();  }')
                println 'initsdk'
            }
        }

        cc.instrument(new ExprEditor() {
            @Override
            void edit(MethodCall m) throws CannotCompileException {
                if (m.methodName == 'intValue') {
                    m.replace('{Object classOrInstance = null; if($0!=null){classOrInstance = $0;} else { classOrInstance = $class;}Object value =  org.powermock.core.MockGateway.methodCall(classOrInstance,"intValue",$args, $sig,"int");if(value == org.powermock.core.MockGateway.PROCEED) {\t$_ = $proceed($$);} else {\t$_ = ((java.lang.Number)value).intValue();}}')
                }
            }
        })
    }

//    def "test getOaid async"(){
//        setup:
//        PowerMockito.mockStatic(MdidSdkHelper)
//        OaidHelper helper = new OaidHelper()
//
//        IdSupplier supplier = Mock(IdSupplier)
//        Context context = Mock(Context)
//
//        PowerMockito.when(MdidSdkHelper.InitSdk(context, true, helper))
//                .thenAnswer(new Answer<Object>() {
//                    @Override
//                    Object answer(InvocationOnMock invocation) throws Throwable {
//                        new Thread(){
//                            @Override
//                            void run() {
//                                helper.OnSupport(true, supplier)
//                            }
//                        }.start()
//                        return 0
//                    }
//                })
//        when:
//        def result = helper.getOaid(context)
//        then:
//        supplier.getOAID() >> "ldk-oaid"
//        "ldk-oaid" == result
//    }
//
//
//    def "test getOaid sync"(){
//        setup:
//        PowerMockito.mockStatic(MdidSdkHelper)
//        OaidHelper helper = new OaidHelper()
//        IdSupplier supplier = Mock(IdSupplier)
//        Context context = Mock(Context)
//
//        PowerMockito.when(MdidSdkHelper.InitSdk(context, true, helper)).thenAnswer(new Answer<Object>() {
//            @Override
//            Object answer(InvocationOnMock invocation) throws Throwable {
//                helper.OnSupport(true, supplier)
//                return 0
//            }
//        });
//        when:
//        def result = helper.getOaid(context)
//        then:
//        supplier.getOAID() >> "ldk-oaid"
//        "ldk-oaid" == result
//    }
//
//    def "test getOaid too long time"(){
//        PowerMockito.mockStatic(MdidSdkHelper)
//        OaidHelper helper = new OaidHelper()
//        IdSupplier supplier = Mock(IdSupplier)
//        Context context = Mock(Context)
//
//        PowerMockito.when(MdidSdkHelper.InitSdk(context, true, helper)).thenReturn(0)
//        when:
//        def result = helper.getOaid(context)
//        then:
//        0 * supplier.getOAID()
//        result == null
//    }
//
//    def "test InitSdk errorCode"(){
//        PowerMockito.mockStatic(MdidSdkHelper)
//        OaidHelper helper = new OaidHelper()
//        Context context = Mock(Context)
//
//        PowerMockito.when(MdidSdkHelper.InitSdk(context, true, helper)).thenReturn(ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT)
//        when:
//        def result = helper.getOaid(context)
//        then:
//        result == null
//    }
}
