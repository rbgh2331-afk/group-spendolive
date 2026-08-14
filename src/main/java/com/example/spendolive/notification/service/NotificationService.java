package com.example.spendolive.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.spendolive.notification.domain.NotificationDTO;
import org.springframework.dao.DataAccessException;
import com.example.spendolive.notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<NotificationDTO> getNotificationList(String id) {
        return notificationRepository.findById(id);
    }

    /** 벨 드롭다운 전용: 안읽은 알림만 반환 (읽으면 목록에서 사라짐) */
    public List<NotificationDTO> getUnreadNotificationList(String id) {
        return notificationRepository.findUnreadById(id);
    }

    /**
     * 알림이 가리키는 목적지 페이지(link_url)에 사용자가 "직접" 들어왔을 때 호출.
     * 벨 드롭다운/알림센터를 거치지 않고 메뉴 등으로 바로 들어와도,
     * 그 페이지 내용을 확인한 것이므로 관련 알림을 읽음 처리
     *
     * @param id      로그인 회원 ID
     * @param linkUrl 알림 생성 시 저장했던 것과 동일한 경로 (예: "/spendolive/inquiry/list.do")
     */
    public void markAsReadByLinkUrl(String id, String linkUrl) {
        notificationRepository.updateReadByLinkUrl(id, linkUrl);
    }

    public int getUnread_count(String id) {
        return notificationRepository.countUnread(id);
    }

    public void readNotification(int notification_id, String id) {
        notificationRepository.updateReadYn(notification_id, id);
    }

    public void toggleStar(int notification_id, String id) {
        notificationRepository.toggleStar(notification_id, id);
    }

    public NotificationDTO getNotificationDetail(int notification_id, String id) {
        return notificationRepository.findByNotificationId(notification_id, id);
    }

    /**
     * 알림 1건 생성 (공용 발송 창구).
     * 채팅/결제/정산/문의답변/캘린더 등 어떤 기능이든 알림을 보내야 하면 이 메서드를 호출하면 된다.
     * type은 NotificationType 상수를 사용할 것.
     *
     * @param id       수신자 회원 ID
     * @param type     NotificationType 상수 (예: NotificationType.INQUIRY_REPLY)
     * @param title    알림 제목
     * @param message  알림 본문
     * @param linkUrl  클릭 시 이동할 경로 (없으면 null)
     */
    public void createNotification(String id, String type, String title, String message, String linkUrl) {
        notificationRepository.insertNotification(id, type, title, message, linkUrl);
    }
}
