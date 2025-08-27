package com.growingio.android.sdk.utils

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.view.menu.ListMenuItemView
import android.support.v7.widget.RecyclerView
import com.bun.supplier.IIdentifierListener
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.tencent.smtt.sdk.WebView
import spock.lang.Specification

/**
 * Created by liangdengke on 2018/11/30.
 */
class ClassExistHelperTest extends Specification {

    def "print filed name to classname"(){
        setup:
        def filedNames2Class =
                ["sHasX5WebView": WebView,
                 "sHasUcWebView":com.uc.webview.export.WebView,
                 "sHasSupportRecyclerView": RecyclerView,
                 "sHasSupportViewPager": ViewPager,
                 "sHasSupportSwipeRefreshLayoutView": SwipeRefreshLayout,
                 "sHasSupportFragment": Fragment,
                 "sHasSupportFragmentActivity": FragmentActivity,
                 "sHasSupportAlertDialog": AlertDialog,
                 "sHasSupportListMenuItemView": ListMenuItemView,

                 "sHasAndroidXRecyclerView": androidx.recyclerview.widget.RecyclerView,
                 "sHasAndroidXViewPager": androidx.viewpager.widget.ViewPager,
                 "sHasAndroidXSwipeRefreshLayoutView": androidx.swiperefreshlayout.widget.SwipeRefreshLayout,
                 "sHasAndroidXFragment": androidx.fragment.app.Fragment,
                 "sHasAndroidXFragmentActivity": androidx.fragment.app.FragmentActivity,
                 "sHasAndroidXAlertDialog": androidx.appcompat.app.AlertDialog,
                 "sHasAndroidXListMenuItemView": androidx.appcompat.view.menu.ListMenuItemView,
                 "sHasAdvertisingIdClient": AdvertisingIdClient,
                 "sHasMSA1013": IIdentifierListener,
                 "sHasMSA1010": com.bun.miitmdid.core.IIdentifierListener,
                 "sHasMSA1025": com.bun.miitmdid.interfaces.IIdentifierListener,
                ]

        filedNames2Class.each {
            println("filedName2ClassName.put(\"${it.key}\", \"${it.value.name}\");");
        }

        println()

        filedNames2Class.each {
            println("${it.key} = hasClass(\"${it.value.name}\");")
        }
    }
}
