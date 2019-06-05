package com.ddlab.rnd.utils;

import org.apache.tomcat.util.codec.binary.Base64;

/**
 * 功能说明 ：编码解码工具类
 *
 * @ version Rversion 1.0.0
 * 修改时间       | 修改内容
 */
public class EncodeUtil {

    /**
     * Base64编码.
     */
    public static String encodeBase64(byte[] input) {
        return Base64.encodeBase64String(input);
    }
}
