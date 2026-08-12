package com.example.spendolive.calendar.service;

import java.util.List;
import java.util.Map;

public interface CalendarService {

    /**
     * 특정 회원의 특정 연/월 지출 내역을 캘린더 JS가 쓰는 형태(Map)로 반환.
     * 반복(고정/OTT) 지출은 대상 월에 맞춰 확장된 상태로 내려온다.
     */
    List<Map<String, Object>> getMonthlyExpenses(long memberId, int year, int month);
}