package com.growingio.android.sdk.utils;


public class CustomerInterface {
    public interface Encryption {
        /**
         * 加密
         *
         * @param source 被加密的String
         * @return 加密之后的值
         */
        String encrypt(String source);
    }
}