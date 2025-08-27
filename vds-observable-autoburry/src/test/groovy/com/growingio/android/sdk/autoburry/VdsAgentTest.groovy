package com.growingio.android.sdk.autoburry

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import com.growingio.android.sdk.collection.DeviceUUIDFactory
import com.growingio.android.sdk.utils.Utils
import spock.lang.Specification

class VdsAgentTest extends Specification{

    def "print showDialogFragment signature"(){
        setup:
        VdsAgent.methods.each {
            if ("showDialogFragment" == it.name){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()
        DialogFragment.methods.each {
            if ("show" == it.name){
                println(Utils.constructorMethodStr(it))
            }
        }
    }

    def "print androidX FragmentTransaction and FragmentManager"(){
        setup:
        FragmentTransaction.methods.each {
            if ("add" == it.name){
                println(Utils.constructorMethodStr(it))
            }else if ("replace" == it.name){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()

        VdsAgent.methods.each {
            if ("onFragmentTransactionAdd" == it.name){
                println(Utils.constructorMethodStr(it))
            }
        }
    }

    def "print androidX FragmentTransaction show and attach"(){
        setup:
        FragmentTransaction.methods.each {
            if ("show" == it.name){
                println(Utils.constructorMethodStr(it))
            }else if ("attach" == it.name){
                println(Utils.constructorMethodStr(it))
            }
        }
    }

    def "print setVisibility hook"(){
        setup:
        View.methods.each {
            if (it.name == 'setVisibility'){
                println(Utils.constructorMethodStr(it))
            }
        }

        println()
        VdsAgent.methods.each {
            if (it.name == 'onSetViewVisibility'){
                println(Utils.constructorMethodStr(it))
            }
        }
    }

    def "print DeviceUUIDFactory initImei method"(){
        setup:
        def names = ['initIMEI', 'initAndroidID', 'initGoogleAdId']
        DeviceUUIDFactory.declaredMethods.each {
            if (names.contains(it.name)){
                println(Utils.constructorMethodStr(it))
            }
        }
    }
}