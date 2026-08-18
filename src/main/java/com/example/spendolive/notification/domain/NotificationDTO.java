package com.example.spendolive.notification.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDTO {
    private int notification_id;           // 알림 고유 번호(PK)
    private String id;                    // 수신자 회원 ID(FK)
    private String notification_type;      // 알림 종류 구분값
    private String title;                 // 알림 제목
    private String message;               // 알림 본문 메시지
    private String link_url;               // 클릭 시 이동할 경로
    private String read_yn;                // 읽음 Y/N (자체 컬럼)
    private String star_yn;                // 찜 Y/N   (자체 컬럼)
    private String created_at;             // 알림 생성일
}
