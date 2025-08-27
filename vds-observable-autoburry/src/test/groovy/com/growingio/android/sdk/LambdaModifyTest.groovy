package com.growingio.android.sdk

import android.content.DialogInterface
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.growingio.android.sdk.autoburry.VdsAgent
import com.growingio.android.sdk.utils.Utils
import spock.lang.Specification

import java.lang.reflect.Method

public class LambdaModifyTest extends Specification{

    def "generate lambda data code"(){
        setup:
        def lambdaListeners = new HashMap()
        lambdaListeners.put(View.OnClickListener, ["lambdaOnClick", 1]) // 1表示参数个数， 可选
        lambdaListeners.put(RadioGroup.OnCheckedChangeListener, ['lambdaOnCheckedChangeRadioGroup'])
        lambdaListeners.put(CompoundButton.OnCheckedChangeListener, ['lambdaOnCheckedChangedCompoundButton'])
        lambdaListeners.put(DialogInterface.OnClickListener, ['lambdaOnDialogClick'])
        lambdaListeners.put(AdapterView.OnItemClickListener, ['lambdaOnItemClick'])
        lambdaListeners.put(RatingBar.OnRatingBarChangeListener, ['lambdaOnRatingChanged'])
        lambdaListeners.put(ExpandableListView.OnGroupClickListener, ['lambdaOnGroupClick'])
        lambdaListeners.put(ExpandableListView.OnChildClickListener, ['lambdaOnChildClick'])
        lambdaListeners.put(MenuItem.OnMenuItemClickListener, ['lambdaOnMenuItemClick'])
        lambdaListeners.put(ActionMenuView.OnMenuItemClickListener, ['lambdaOnMenuItemClick'])
        lambdaListeners.put(Toolbar.OnMenuItemClickListener, ['lambdaOnMenuItemClick'])
        lambdaListeners.put(PopupMenu.OnMenuItemClickListener, ['lambdaOnMenuItemClick'])
        StringBuilder builder = new StringBuilder()
        lambdaListeners.each {
            String className = it.key.name.replace('.', '/')
            String methodName = it.value[0]
            List value = it.value
            def result = VdsAgent.methods.findAll {
                def eq = it.name == methodName
                if (eq && value.size() > 1){
                    eq = it.parameterTypes.size() == value[1]
                }
                return eq
            }
            if (result.size() != 1)
                throw new IllegalStateException("result not valid: ${result}")
            Method method = result[0]
            String methodDesc = Utils.constructorMethodStr(method)
            println("lambdaDesc2AgentDesc.put(\"L${className};\", \"${methodDesc}\");")
            Method interfaceMethod = it.key.methods[0]
            String interfaceMethodDesc = Utils.constructorMethodStr(interfaceMethod)
            interfaceMethodDesc = interfaceMethodDesc.substring(0, interfaceMethodDesc.length() - 1)
            int index = methodDesc.lastIndexOf("(");
            methodDesc = methodDesc.substring(index, methodDesc.length() - 1);
            if (!interfaceMethodDesc.endsWith(methodDesc)){
                builder.append("interface: ").append(interfaceMethodDesc).append('\n')
                        .append('vdsAgent: ').append(methodDesc).append("\n\n");
            }
        }

        if (builder.length() != 0){
            println()
            println(builder.toString())
            throw new RuntimeException(builder.toString())
        }
    }
}