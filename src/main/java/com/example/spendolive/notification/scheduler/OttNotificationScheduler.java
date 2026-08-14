package com.example.spendolive.notification.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.spendolive.notification.domain.NotificationType;
import com.example.spendolive.notification.service.NotificationService;

/**
 * 
 * OTT 공유방 정산/결제 관련 알림 3종.
 *   1) 자동결제 예정일 사전 안내 (마감 10일 전 ~ 당일, 3일 이내는 강조)
 *   2) 결제 실패 + 자동 강퇴 안내
 *   3) 환불/정산취소 완료 안내
 *
 * 주의 - 이 스케줄러는 OttServiceImpl.processScheduledOttJobs()나 OttRepositoryImpl의
 * 기존 배치 SQL(EXPIRE_OVERDUE_PAYMENTS_*, CLOSE_EFFECTIVE_ROOMS_* 등)을 전혀 건드리지 않는다.
 *
 * 그 배치(OttScheduleTask, 매일 새벽 1시 정각)가 남기는 결과값(kicked_at, left_at, closed_at 같은
 * 타임스탬프 컬럼)을 "오늘 자로 바뀐 것"만 조회해서 알림만 별도로 얹는 방식.
 */
@Component
public class OttNotificationScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public OttNotificationScheduler(JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    // =========================================================
    // 1) 자동결제 예정일 사전 안내 (PAYMENT_DUE)
    // =========================================================

    private static final int PAYMENT_DUE_REMINDER_DAYS = 10; // 마감 10일 전부터 안내 시작
    private static final int PAYMENT_DUE_URGENT_DAYS = 3;     // 마감 3일 이내는 강조 문구 + 카드확인 안내

    private static final String FIND_UPCOMING_UNPAID_PAYMENTS_SQL = """
            SELECT sp.id AS member_login_id, st.room_id, r.room_name,
                   sp.total_amount, st.payment_close_date
            FROM settlement_payment_tb sp
            JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
            JOIN ott_room_tb r ON st.room_id = r.room_id
            WHERE sp.payment_status = 'UNPAID'
              AND st.status IN ('PAYMENT_OPEN', 'REQUESTED', 'READY')
              AND st.payment_close_date >= TRUNC(SYSDATE)
              AND st.payment_close_date <= TRUNC(SYSDATE) + ?
            """;

    // 같은 결제 건으로 오늘 이미 안내를 보냈는지 확인 (링크에 room_id를 심어서 구분 - 하루 1번만)
    private static final String EXISTS_TODAY_NOTIFIED_SQL = """
            SELECT COUNT(*) FROM notification_tb
            WHERE id = ? AND notification_type = ? AND link_url = ?
              AND TRUNC(created_at) = TRUNC(SYSDATE)
            """;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul") // 매일 오전 9시
    public void notifyUpcomingOttPayments() {
        List<UpcomingPaymentRow> rows = jdbcTemplate.query(
                FIND_UPCOMING_UNPAID_PAYMENTS_SQL,
                (rs, rowNum) -> new UpcomingPaymentRow(
                        rs.getString("member_login_id"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        rs.getInt("total_amount"),
                        rs.getDate("payment_close_date").toLocalDate()),
                PAYMENT_DUE_REMINDER_DAYS);

        LocalDate today = LocalDate.now();

        for (UpcomingPaymentRow row : rows) {
            if (row.memberLoginId == null || row.memberLoginId.isBlank()) {
                continue;
            }

            long daysUntil = ChronoUnit.DAYS.between(today, row.closeDate);
            String linkUrl = "/spendolive/ott/recruit.do?tab=settlement&room_id=" + row.roomId;

            Integer already = jdbcTemplate.queryForObject(
                    EXISTS_TODAY_NOTIFIED_SQL, Integer.class,
                    row.memberLoginId, NotificationType.PAYMENT_DUE, linkUrl);
            if (already != null && already > 0) {
                continue;
            }

            String message;
            if (daysUntil <= PAYMENT_DUE_URGENT_DAYS) {
                message = row.roomName + " 다음 이용분 " + row.totalAmount + "원이 "
                        + daysUntil + "일 후(" + row.closeDate + ") 자동결제됩니다. "
                        + "등록된 카드 잔액을 꼭 확인해 주세요. 결제 실패 시 " + row.closeDate + "에 방에서 자동 퇴장됩니다.";
            } else {
                message = row.roomName + " 다음 이용분 " + row.totalAmount + "원이 "
                        + daysUntil + "일 후(" + row.closeDate + ") 자동결제될 예정입니다.";
            }

            notificationService.createNotification(
                    row.memberLoginId,
                    NotificationType.PAYMENT_DUE,
                    "자동결제 예정 안내",
                    message,
                    linkUrl);
        }
    }

    // =========================================================
    // 2) 결제 실패 + 자동 강퇴 (PAYMENT_FAIL / ROOM_LEAVE_KICK)
    // =========================================================

    // =========================================================
    // 2-1) 나가기 예약 실제 처리 완료 (ROOM_LEAVE_KICK)
    // =========================================================

    // OttRepositoryImpl.PROCESS_LEAVE_RESERVATIONS_LEAVE_MEMBER_SQL이 오늘 자로 OUT 처리한 멤버를 조회
    // 방 자체가 폐쇄(CLOSED)돼서 집단으로 OUT 처리된 경우는 제외함(그건 환불완료 알림에서 따로 다룸) -
    // 방장이 방을 없애서 나간 게 아니라, 본인이 예약해서 자발적으로 나간 경우만 여기서 알림
    private static final String FIND_TODAY_SELF_LEFT_MEMBERS_SQL = """
            SELECT rm.member_login_id, rm.room_id, r.room_name
            FROM ott_room_member_tb rm
            JOIN ott_room_tb r ON rm.room_id = r.room_id
            WHERE rm.status = 'OUT'
              AND TRUNC(rm.left_at) = TRUNC(SYSDATE)
              AND r.status != 'CLOSED'
            """;

    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Seoul")
    public void notifyTodaySelfLeftMembers() {
        List<RoomMemberRow> rows = jdbcTemplate.query(FIND_TODAY_SELF_LEFT_MEMBERS_SQL,
                (rs, rowNum) -> new RoomMemberRow(
                        rs.getString("member_login_id"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        null));

        for (RoomMemberRow row : rows) {
            notificationService.createNotification(
                    row.memberLoginId,
                    NotificationType.ROOM_LEAVE_KICK,
                    "공유방 나가기 완료",
                    row.roomName + " 공유방에서 예약하신 날짜에 나가기 처리가 완료되었습니다.",
                    "/spendolive/ott/recruit.do");
        }
    }

    // =========================================================
    // 2-2) 결제 실패 + 자동 강퇴 (PAYMENT_FAIL / ROOM_LEAVE_KICK)
    // =========================================================

    // OttRepositoryImpl.EXPIRE_OVERDUE_PAYMENTS_KICK_MEMBERS_SQL이 오늘 자로 KICKED 처리한 멤버를 그대로 조회
    private static final String FIND_TODAY_KICKED_MEMBERS_SQL = """
            SELECT rm.member_login_id, rm.room_id, r.room_name, rm.kicked_reason
            FROM ott_room_member_tb rm
            JOIN ott_room_tb r ON rm.room_id = r.room_id
            WHERE rm.status = 'KICKED'
              AND TRUNC(rm.kicked_at) = TRUNC(SYSDATE)
            """;

    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Seoul") // OttScheduleTask(새벽 1시 정각) 실행 후 5분 뒤
    public void notifyTodayKickedMembers() {
        List<RoomMemberRow> rows = jdbcTemplate.query(FIND_TODAY_KICKED_MEMBERS_SQL,
                (rs, rowNum) -> new RoomMemberRow(
                        rs.getString("member_login_id"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        rs.getString("kicked_reason")));

        for (RoomMemberRow row : rows) {
            String linkUrl = "/spendolive/ott/recruit.do?tab=settlement&room_id=" + row.roomId;

            notificationService.createNotification(
                    row.memberLoginId,
                    NotificationType.PAYMENT_FAIL,
                    "결제에 실패했어요",
                    row.roomName + " 이용분 자동결제에 실패했습니다. 카드 정보를 확인해 주세요.",
                    linkUrl);

            notificationService.createNotification(
                    row.memberLoginId,
                    NotificationType.ROOM_LEAVE_KICK,
                    "공유방에서 나가졌어요",
                    row.roomName + " 공유방에서 자동 퇴장되었습니다."
                            + (row.extra != null && !row.extra.isBlank() ? " (" + row.extra + ")" : ""),
                    linkUrl);
        }
    }

    // =========================================================
    // 3) 환불 / 정산취소 완료 (REFUND_DONE)
    // =========================================================

    // OttRepositoryImpl.CLOSE_EFFECTIVE_ROOMS_*_SQL이 오늘 자로 최종 종료 처리한 방의 멤버를 조회
    private static final String FIND_TODAY_CLOSED_ROOM_MEMBERS_SQL = """
            SELECT rm.member_login_id, rm.room_id, r.room_name
            FROM ott_room_member_tb rm
            JOIN ott_room_tb r ON rm.room_id = r.room_id
            WHERE rm.status = 'OUT'
              AND TRUNC(rm.left_at) = TRUNC(SYSDATE)
              AND r.status = 'CLOSED'
              AND TRUNC(r.closed_at) = TRUNC(SYSDATE)
            """;

    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Seoul")
    public void notifyTodayRefundedMembers() {
        List<RoomMemberRow> rows = jdbcTemplate.query(FIND_TODAY_CLOSED_ROOM_MEMBERS_SQL,
                (rs, rowNum) -> new RoomMemberRow(
                        rs.getString("member_login_id"),
                        rs.getLong("room_id"),
                        rs.getString("room_name"),
                        null));

        for (RoomMemberRow row : rows) {
            notificationService.createNotification(
                    row.memberLoginId,
                    NotificationType.REFUND_DONE,
                    "환불 처리 완료",
                    row.roomName + " 공유방 종료에 따른 환불이 처리되었습니다. 마이페이지에서 확인해 주세요.",
                    "/spendolive/mypage.do");
        }
    }

    private static final class UpcomingPaymentRow {
        final String memberLoginId;
        final Long roomId;
        final String roomName;
        final int totalAmount;
        final LocalDate closeDate;

        UpcomingPaymentRow(String memberLoginId, Long roomId, String roomName, int totalAmount, LocalDate closeDate) {
            this.memberLoginId = memberLoginId;
            this.roomId = roomId;
            this.roomName = roomName;
            this.totalAmount = totalAmount;
            this.closeDate = closeDate;
        }
    }

    private static final class RoomMemberRow {
        final String memberLoginId;
        final Long roomId;
        final String roomName;
        final String extra;

        RoomMemberRow(String memberLoginId, Long roomId, String roomName, String extra) {
            this.memberLoginId = memberLoginId;
            this.roomId = roomId;
            this.roomName = roomName;
            this.extra = extra;
        }
    }
}
