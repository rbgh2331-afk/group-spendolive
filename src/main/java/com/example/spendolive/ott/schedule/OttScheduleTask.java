package com.example.spendolive.ott.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.spendolive.ott.service.OttService;

/**
 * OTT 정산 및 예약 상태를 주기적으로 처리하는 스케줄 작업
 */
@Component
public class OttScheduleTask {

    private final OttService ottService;

    public OttScheduleTask(OttService ottService) {
        this.ottService = ottService;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void processOttPaymentAndCloseJobs() {
        ottService.processScheduledOttJobs();
    }
}
