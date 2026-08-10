package com.example.spendolive.weather.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 단기예보 API에 넘길 base_date/base_time을 계산하는 유틸.
 *
 * 기상청은 하루에 8번(2,5,8,11,14,17,20,23시) 예보를 발표하고, 각 발표시각으로부터
 * 약 10분 뒤에 데이터가 실제로 열람 가능해짐. 그래서 "지금 몇 시니까 base_time은 몇 시"를
 * 그대로 쓰면 아직 안 열린 데이터를 요청하게 될 수 있어서, 현재 시각에서 10분을 뺀 뒤
 * 그 기준으로 "가장 최근에 이미 발표됐을 시각"을 역산해서 씀.
 */
public class WeatherTimeUtil {

    // 기상청 발표시각(하루 8회). 오름차순으로 정렬돼 있어야 아래 계산 로직이 맞게 동작함
    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};

    /**
     * 지금 이 순간 기준으로 사용할 base_date(yyyyMMdd)와 base_time(HHmm)을 계산해서 반환.
     * 반환값: [0]=base_date, [1]=base_time (예: {"20260810", "1400"})
     */
    public static String[] getBaseDateTime() {
        // 발표 후 데이터가 열리기까지의 지연(10분)을 감안해서 현재 시각을 10분 당겨서 계산
        LocalDateTime now = LocalDateTime.now().minusMinutes(10);
        int hour = now.getHour();

        // BASE_HOURS를 앞에서부터 훑으면서, 지금 시각(hour) 이하인 발표시각으로 계속 갱신함
        // → 반복이 끝나면 baseHour는 "지금 시각을 넘지 않는 발표시각 중 가장 늦은 것"이 됨
        // 예: 지금 15시면 2,5,8,11,14까지는 다 15 이하라 계속 덮어써지고 14에서 멈춤(17은 15보다 커서 스킵)
        int baseHour = BASE_HOURS[0];
        for (int h : BASE_HOURS) {
            if (hour >= h) {
                baseHour = h;
            }
        }

        LocalDate baseDate = now.toLocalDate();
        // 지금 시각이 그날 첫 발표시각(2시)보다도 이르면(예: 새벽 1시),
        // 오늘 발표분이 아직 하나도 안 나온 거라 어제 마지막 발표(23시) 걸 씀
        if (hour < BASE_HOURS[0]) {
            baseDate = baseDate.minusDays(1);
            baseHour = 23;
        }

        String date = baseDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = String.format("%02d00", baseHour); // 14 -> "1400"

        return new String[]{date, time};
    }
}