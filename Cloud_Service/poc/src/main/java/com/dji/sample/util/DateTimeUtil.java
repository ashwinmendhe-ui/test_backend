package com.dji.sample.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatKst(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZoneSameInstant(KST_ZONE)
                .format(FORMATTER);
    }

    public static String formatKst(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.format(FORMATTER);
    }
}