package com.dji.sample.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String toKstString(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZone(ZoneId.systemDefault())
                .withZoneSameInstant(KST_ZONE)
                .format(FORMATTER);
    }
}