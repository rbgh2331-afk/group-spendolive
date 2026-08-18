package com.example.spendolive.notification.service;

import org.springframework.stereotype.Service;
import com.example.spendolive.notification.domain.NotificationType;

/**
 * 회원 알림 생성 및 상태 처리 서비스
 */
@Service
public class MemberAlarmService {

    private final NotificationService notificationService;

    public MemberAlarmService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void notifySignup(String memberId) {
        notificationService.createNotification(memberId, NotificationType.SIGNUP,
                "회원가입을 축하합니다!",
                "SpendOlive 가입을 환영합니다. 지출관리, 캘린더, OTT 공유방 기능을 이용해보세요.",
                "/spendolive/main.do");
    }
}
