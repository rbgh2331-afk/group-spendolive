package com.example.spendolive.weather.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherTimeUtil {

    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    public static String[] getBaseDateTime() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(10);
        int hour = now.getHour();

        int baseHour = BASE_HOURS[0];
        for (int h : BASE_HOURS) {
            if (hour >= h) {
                baseHour = h;
            }
        }

        LocalDate baseDate = now.toLocalDate();
        if (hour < BASE_HOURS[0]) {
            baseDate = baseDate.minusDays(1);
            baseHour = 23;
        }

        String date = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = String.format("%02d00", baseHour);

        return new String[]{date, time};
    }
}