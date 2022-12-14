package com.study.blog.blog_core.utils;

/**
 * @ClassName HexConvertUtil
 * @Description TODO
 * @Author Alex Li
 * @Date 2022/10/8 09:37
 * @Version 1.0
 */
public class HexConvertUtil {

    private static final Integer INTEGER_1 = 1;
    private static final Integer INTEGER_2 = 2;

    private HexConvertUtil() {

    }

    /**
     * 将二进制转为十六进制
     */
    public static String parseByte2HexStr(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte buff : bytes) {
            String hex = Integer.toHexString(buff & 0xFF);
            if (hex.length() == INTEGER_1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将十六进制转为二进制
     */
    public static byte[] parseHex2Byte(String hexStr) {
        if (hexStr.length() < INTEGER_1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / INTEGER_2];
        for (int i = 0, len = hexStr.length() / INTEGER_2; i < len; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

}
