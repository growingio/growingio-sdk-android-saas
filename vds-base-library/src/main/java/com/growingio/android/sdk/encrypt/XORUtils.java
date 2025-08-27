package com.growingio.android.sdk.encrypt;

public class XORUtils {
    /**
     * XOR异或算法加密
     *
     * @param data 数据
     * @param key  密钥
     * @return 返回加密后的数据
     */
    public static byte[] encrypt(byte[] data, int key) {
        if (data == null || data.length == 0) {
            return data;
        }

        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key);
        }
        return result;
    }
}
