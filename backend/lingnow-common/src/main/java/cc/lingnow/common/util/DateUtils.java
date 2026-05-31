package cc.lingnow.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期时间工具类
 * 提供 LocalDateTime, Date, String, Timestamp 之间的相互转换
 *
 * @author admin
 */
public class DateUtils {

    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认日期格式
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 默认时区 (系统默认)
     */
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    // ============================ 获取当前时间 ============================

    /**
     * 获取当前 LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前 Date
     */
    public static Date nowDate() {
        return new Date();
    }

    /**
     * 获取当前时间戳 (毫秒)
     */
    public static long nowTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String nowStr() {
        return format(now());
    }

    // ============================ 格式化 ============================

    /**
     * LocalDateTime -> String
     */
    public static String format(LocalDateTime localDateTime) {
        return format(localDateTime, DEFAULT_DATE_TIME_FORMAT);
    }

    /**
     * LocalDateTime -> String (指定格式)
     */
    public static String format(LocalDateTime localDateTime, String pattern) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Date -> String
     */
    public static String format(Date date) {
        return format(date, DEFAULT_DATE_TIME_FORMAT);
    }

    /**
     * Date -> String (指定格式)
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return format(dateToLocalDateTime(date), pattern);
    }

    // ============================ 解析 ============================

    /**
     * String -> LocalDateTime
     */
    public static LocalDateTime parse(String timeStr) {
        return parse(timeStr, DEFAULT_DATE_TIME_FORMAT);
    }

    /**
     * String -> LocalDateTime (指定格式)
     */
    public static LocalDateTime parse(String timeStr, String pattern) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * String -> Date
     */
    public static Date parseDate(String timeStr) {
        return parseDate(timeStr, DEFAULT_DATE_TIME_FORMAT);
    }

    /**
     * String -> Date (指定格式)
     */
    public static Date parseDate(String timeStr, String pattern) {
        LocalDateTime localDateTime = parse(timeStr, pattern);
        return localDateTimeToDate(localDateTime);
    }

    // ============================ 类型转换 ============================

    /**
     * Date -> LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * LocalDateTime -> Date
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * Timestamp -> LocalDateTime
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * LocalDateTime -> Timestamp
     */
    public static long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return 0L;
        }
        return localDateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * Date -> Timestamp
     */
    public static long dateToTimestamp(Date date) {
        if (date == null) {
            return 0L;
        }
        return date.getTime();
    }

    /**
     * Timestamp -> Date
     */
    public static Date timestampToDate(long timestamp) {
        return new Date(timestamp);
    }
}
