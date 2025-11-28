package com.qs.qs5502demo.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日期工具类
 */
public class DateUtil {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat TASK_NO_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());

    /**
     * 获取当前日期字符串 yyyy-MM-dd
     */
    public static String getCurrentDate() {
        return DATE_FORMAT.format(new Date());
    }

    /**
     * 获取当前日期时间字符串 yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentDateTime() {
        return DATETIME_FORMAT.format(new Date());
    }

    /**
     * 生成任务编号
     * @param prefix 前缀：R/S/H/C
     */
    public static String generateTaskNo(String prefix) {
        return prefix + TASK_NO_FORMAT.format(new Date());
    }

    /**
     * 格式化日期
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }
}

