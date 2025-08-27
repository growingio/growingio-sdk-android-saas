package com.growingio.android.sdk.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import com.growingio.android.sdk.collection.CoreInitialize;
import com.growingio.android.sdk.collection.GConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * author CliffLeopard
 * time   2017/11/22:上午11:39
 * email  gaoguanling@growingio.com
 */

public class EncryptionUtil {
    public static String encodeRules = null;

    public static String AESDecode(String content) {
        try {

            return ecbDecrypt(content, getDecodeKey());

        } catch (Exception ignore) {
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String ecbDecrypt(String base64Encode, String sKey) throws Exception {
        try {
            byte[] content = Base64.decode(base64Encode, Base64.URL_SAFE);
            // 判断Key是否正确
            if (sKey == null) {
                System.out.print("Key为空null");
                return null;
            }
            // 判断Key是否为16倍数
            if (sKey.length() % 16 != 0) {
                System.out.print("Key长度不是16倍数");
                return null;
            }

            byte[] raw = sKey.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            try {
                byte[] bytes = cipher.doFinal(content);
                return new String(bytes, "UTF-8");
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    private static String getDecodeKey() {

        if (TextUtils.isEmpty(encodeRules)) {
            String version = CoreInitialize.config().getAppVersion();
            if (TextUtils.isEmpty(version)) {
                return null;
            }
            int times = 16 / version.length() + 1;
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < times; i++)
                builder.append(version);
            encodeRules = builder.toString().substring(0, 16);
        }

//        LogUtil.i("GIO: decodeRules:",encodeRules);
        return encodeRules;
    }
}
