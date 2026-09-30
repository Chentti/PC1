package com.example.demo.common;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeUtil {
    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    private TimeUtil() {}

    public static ZonedDateTime now() {
        return ZonedDateTime.now(LIMA).withNano(0);
    }

    public static LocalDate today() {
        return LocalDate.now(LIMA);
    }
}
