// util/IdUtil.java
package com.example.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/** 简单业务号生成器（生产可换分布式ID） */
public class IdUtil {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmss");
    public static String next(String prefix) {
        return prefix + SDF.format(new Date()) + (100000 + ThreadLocalRandom.current().nextInt(900000));
    }
    public static String formatCode(String prefix, Long id) {
        if (id == null) return prefix + "000000";
        String num = String.valueOf(id);
        if (num.length() < 6) {
            num = String.format("%06d", id);
        }
        return prefix + num;
    }
}
