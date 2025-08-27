package com.growingio.android.sdk.utils;

import android.content.Context;
import android.util.Log;

import com.bun.miitmdid.core.ErrorCode;
import com.bun.miitmdid.core.MdidSdkHelper;
import com.bun.supplier.IIdentifierListener;
import com.bun.supplier.IdSupplier;


/**
 * <p>
 *
 * @author cpacm 2021/3/15
 */
public class OaidHelper1013 implements IIdentifierListener {

    private static final String TAG = "GIO.oaid";
    private String oaid;
    private volatile boolean complete = false;

    public String getOaid(Context context) {
        int ret;
        try {
            ret = MdidSdkHelper.InitSdk(context, true, this);
        } catch (Throwable e) {
            Log.e(TAG, "InitSdkError: ", e);
            return null;
        }
        switch (ret) {
            case ErrorCode.INIT_ERROR_DEVICE_NOSUPPORT:
            case ErrorCode.INIT_ERROR_LOAD_CONFIGFILE:
            case ErrorCode.INIT_ERROR_MANUFACTURER_NOSUPPORT:
            case ErrorCode.INIT_HELPER_CALL_ERROR:
                Log.e(TAG, "MdidSdkHelper.InitSdk failed, and returnCode: " + ret);
                return null;
            case 0:
            case ErrorCode.INIT_ERROR_RESULT_DELAY:
            case ErrorCode.INIT_ERROR_BEGIN:
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

    @Override
    public void OnSupport(boolean isSupport, IdSupplier idSupplier) {
        if (!isSupport || idSupplier == null) {
        } else {
            try {
                oaid = idSupplier.getOAID();
            } catch (Throwable e) {
                Log.e(TAG, "getOAID failed: ", e);
            }
        }
        synchronized (this) {
            complete = true;
            notifyAll();
        }
    }
}

