package com.growingio.android.sdk.autoburry.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by liangdengke on 2018/11/14.
 */
public class FileUtil {

    /**
     * Write content to outFile
     * @param outFile  The outFile, whose parent file should exits
     * @param content  The Content, will be converted to data by using default encode
     */
    public static void writeToFile(File outFile, String content) throws IOException {
        FileOutputStream outputStream = null;
        try{
            outputStream = new FileOutputStream(outFile);
            outputStream.write(content.getBytes());
        }finally {
            if (outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * Read from file to String
     * TODO: The file's max size is 2048
     */
    public static String readFromFile(File inFile) throws IOException {
        FileInputStream inputStream = null;
        try {
            byte[] buff = new byte[2048];
            inputStream = new FileInputStream(inFile);
            int size = inputStream.read(buff);
            if (size < 0 || size >= 2048) {
                System.err.println("readFromFile, but size is " + size + ", please check");
                return null;
            }
            return new String(buff, 0, size);
        }finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
}
