package com.growingio.android.sdk.utils;

import android.content.Context;
import android.util.Log;

import com.bun.miitmdid.core.InfoCode;
import com.bun.miitmdid.core.MdidSdkHelper;
import com.bun.miitmdid.interfaces.IIdentifierListener;
import com.bun.miitmdid.interfaces.IdSupplier;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OaidHelper1100 implements IIdentifierListener {
    private static final String TAG = "GIO.oaid";
    private String oaid;
    private volatile boolean complete = false;
    private boolean isCertInit = false;
    private final String cert;

    static {
        //1.1.0的oaid_sdk
        loadLibrary("msaoaidsec");
        // 适配1.0.29的oaid_sdk
        loadLibrary("nllvm1632808251147706677");
        // 适配1.0.27的oaid_sdk
        loadLibrary("nllvm1630571663641560568");
        // 适配1.0.26的oaid_sdk
        loadLibrary("nllvm1623827671");
    }

    public OaidHelper1100(String cert){
        this.cert = cert;
    }

    public String getOaid(Context context) {
        int ret;
        try {
            // 初始化SDK证书
            if (!this.isCertInit) { // 证书只需初始化一次
                // 证书为PEM文件中的所有文本内容（包括首尾行、换行符）
                this.isCertInit = MdidSdkHelper.InitCert(context, this.cert);
                if (!this.isCertInit) {
                    Log.w("GIO.oaid", "getDeviceIds: cert init failed");
                }
            }

            ret = MdidSdkHelper.InitSdk(context, true, this);
        } catch (Throwable throwable) {
            Log.e("GIO.oaid", "InitSdkError: ", throwable);
            return null;
        }

        switch (ret) {
            case InfoCode.INIT_ERROR_CERT_ERROR:
            case InfoCode.INIT_ERROR_DEVICE_NOSUPPORT:
            case InfoCode.INIT_ERROR_LOAD_CONFIGFILE:
            case InfoCode.INIT_ERROR_MANUFACTURER_NOSUPPORT:
            case InfoCode.INIT_ERROR_SDK_CALL_ERROR:
                Log.e(TAG, "MdidSdkHelper.InitSdk failed, and returnCode: " + ret);
                return null;
            case InfoCode.INIT_INFO_RESULT_DELAY:
            case InfoCode.INIT_INFO_RESULT_OK:
                break;
            default:
        }

        if (complete) {
            return oaid;
        }
        synchronized (this) {
            if (complete) {
                return oaid;
            }
            long start = System.currentTimeMillis();
            while (!complete) {
                try {
                    wait(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!complete && (System.currentTimeMillis() - start > 30_000L)) {
                    break;
                }
            }
        }
        return oaid;
    }

    public void onSupport(IdSupplier idSupplier) {
        if (idSupplier != null && idSupplier.isSupported() && !idSupplier.isLimited()) {

            try {
                this.oaid = idSupplier.getOAID();
            } catch (Throwable throwable) {
                Log.e("GIO.oaid", "getOAID failed: ", throwable);
            }

            synchronized(this) {
                this.complete = true;
                this.notifyAll();
            }
        }
    }

    private static void loadLibrary(String libName) {
        try {
            System.loadLibrary(libName);
        } catch (Throwable ignored) {
        }
    }
}