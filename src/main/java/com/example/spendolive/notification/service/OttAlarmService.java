package com.example.spendolive.notification.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import com.example.spendolive.notification.domain.NotificationType;

/**
 * OTT 공유방(모집/정산/나가기/종료) 알림
 */
@Service
public class OttAlarmService {

    private final NotificationService notificationService;

    public OttAlarmService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String roomLink(Long roomId) { return "/spendolive/ott/chat/room.do?room_id=" + roomId; }
    private String settlementLink(Long roomId) { return "/spendolive/ott/recruit.do?tab=settlement&room_id=" + roomId; }

    /** 다음 달 정산 결제 요청 (매 멤버) */
    public void notifySettlementOpened(String memberId, String roomName, String targetMonth,
                                        int payAmount, LocalDate paymentCloseDate, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.OTT,
                "OTT 다음 달 이용분 결제 요청",
                roomName + " " + targetMonth + " 이용분 " + payAmount + "원을 " + paymentCloseDate
                        + "까지 결제해 주세요. 마감 후 미결제자는 자동 추방됩니다.",
                settlementLink(roomId));
    }

    /** 결제 완료 - 방장에게 "누가 결제했다" 알림 */
    public void notifyPaymentCompletedToHost(String hostId, String payerId, String roomName,
                                              String settlementMonth, int totalAmount, Long roomId) {
        notificationService.createNotification(hostId, NotificationType.OTT,
                "OTT 결제 완료 알림",
                payerId + "님이 " + roomName + " " + settlementMonth + " 이용분 "
                        + totalAmount + "원 결제를 완료했습니다.",
                settlementLink(roomId));
    }

    /** 결제 완료 - 결제한 본인에게 알림 */
    public void notifySettlementDoneToSelf(String payerId, String roomName,
                                            String settlementMonth, int totalAmount, Long roomId) {
        notificationService.createNotification(payerId, NotificationType.SETTLEMENT_DONE,
                "정산 완료",
                roomName + " " + settlementMonth + " 이용분 " + totalAmount + "원 결제가 완료되었습니다.",
                settlementLink(roomId));
    }

    /** 모집 마감 (정원 참) */
    public void notifyRoomFull(String memberId, String roomName, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.ROOM_FULL,
                "모집이 마감되었습니다", roomName + " 공유방 모집 인원이 다 찼어요.", roomLink(roomId));
    }

    /** 결제 완료 후 입장 - 본인에게 "참여 완료" */
    public void notifyRoomJoinedSelf(String memberId, String roomName, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.ROOM_FULL,
                "공유방 참여 완료", roomName + " 공유방에 참여 완료되었습니다.", roomLink(roomId));
    }

    /** 결제 완료 후 입장 - 기존 멤버들에게 "새 이용자 입장" */
    public void notifyRoomMemberJoined(String memberId, String newMemberName, String roomName, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.ROOM_FULL,
                "공유방 입장 알림", newMemberName + "님이 " + roomName + " 공유방에 입장했습니다.", roomLink(roomId));
    }

    /** 방장이 종료 신청 - 나머지 멤버에게 종료 예정 안내 */
    public void notifyRoomCloseScheduled(String memberId, String roomName, LocalDate closeDate,
                                          String notice, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.OTT,
                "OTT 공유방 종료 예정",
                roomName + " 공유방이 " + closeDate + "에 종료될 예정입니다. " + notice, roomLink(roomId));
    }

    /** 나가기(방장 종료신청 포함) 신청 완료 - 신청 당사자 본인에게. 이용자/방장 공통 문구 */
    public void notifySelfLeaveRequested(String memberId, String roomName, LocalDate scheduledDate, Long roomId) {
        notificationService.createNotification(memberId, NotificationType.OTT,
                "공유방 나가기 신청 완료",
                roomName + " 공유방에서 다음 결제 전까지 방을 나가게 됩니다. (" + scheduledDate + ")", roomLink(roomId));
    }

    /** 이용자가 나가기 신청 - 방장에게 알림 */
    public void notifyMemberLeaveRequestedToHost(String hostId, String memberId, String roomName,
                                                  LocalDate scheduledDate, Long roomId) {
        notificationService.createNotification(hostId, NotificationType.OTT,
                "OTT 참여자 나가기 예약",
                memberId + "님이 " + roomName + " 방에서 " + scheduledDate + " 나가기 예약을 했습니다.", roomLink(roomId));
    }
}
