package com.example.spendolive.ott.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.ott.domain.OttChatMessageDTO;
import com.example.spendolive.ott.domain.OttChatRoomDTO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttServiceDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;

// 사용자 OTT DB 처리 - JdbcTemplate으로 방, 정산, 채팅 기능 수행
@Repository
public class OttRepositoryImpl implements OttRepository {

    // INSERT문 ================================================================
    private static final String INSERT_ACTIVE_ROOM_MEMBER_SQL = "INSERT INTO ott_room_member_tb (room_member_id, room_id, member_login_id, member_role, share_amount, fee_rate, fee_amount, pay_amount, status, pay_day) "
    +" SELECT ?, ?, ?, 'MEMBER', ?, ?, ?, ?, 'ACTIVE', ? from ott_room_tb where room_id =? AND (SELECT COUNT(*) FROM ott_room_member_tb WHERE room_id = ? AND status = 'ACTIVE') < member_limit";

    private static final String INSERT_CHAT_MESSAGE_SQL = "INSERT INTO ott_chat_message_tb (message_id, room_id, sender_id, message_content) VALUES (?, ?, ?, ?)";

    private static final String INSERT_HOST_MEMBER_SQL = "INSERT INTO ott_room_member_tb (room_member_id, room_id, member_login_id, member_role, share_amount, fee_rate, fee_amount, pay_amount, status) VALUES (?, ?, ?, 'HOST', 0, 0, 0, 0, 'ACTIVE')";
 
    private static final String INSERT_OTT_NOTIFICATION_SQL = "INSERT INTO notification_tb (id, notification_type, title, message, link_url, read_yn, star_yn) VALUES (?, 'OTT', ?, ?, ?, 'N', 'N')";

    private static final String INSERT_REFUNDS_FOR_ROOM_CLOSE_INSERT_REFUND_SQL = """
                INSERT INTO settlement_refund_tb (
                    refund_id,
                    payment_id,
                    settlement_id,
                    member_login_id,
                    refund_amount,
                    refund_reason,
                    refund_status,
                    completed_at,
                    memo
                )
                SELECT seq_settlement_refund.NEXTVAL,
                       sp.payment_id,
                       st.settlement_id,
                       sp.id,
                       sp.total_amount,
                       'ROOM_CLOSE',
                       'COMPLETED',
                       SYSDATE,
                       '방 삭제 요청으로 자동 환불 처리'
                FROM settlement_payment_tb sp
                JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
                WHERE st.room_id = ?
                  AND (st.service_start_date >= ?
                       OR (st.service_start_date IS NULL AND st.settlement_month >= ?))
                  AND sp.payment_status IN ('PAID', 'CONFIRMED')
                  AND NOT EXISTS (
                        SELECT 1
                        FROM settlement_refund_tb rf
                        WHERE rf.payment_id = sp.payment_id
                  )
                """;

    private static final String INSERT_ROOM_SQL = "INSERT INTO ott_room_tb (room_id, host_login_id, ott_service_id, room_name, plan_name, total_price, billing_day, member_limit, room_mode, status, invite_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_SETTLEMENT_PAYMENT_IF_ABSENT_SQL = """
                INSERT INTO settlement_payment_tb (
                    payment_id,
                    settlement_id,
                    id,
                    base_amount,
                    fee_rate,
                    fee_amount,
                    total_amount,
                    payment_status,
                    memo
                )
                SELECT ?, ?, ?, ?, ?, ?, ?, 'UNPAID', ?
                FROM dual
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM settlement_payment_tb
                    WHERE settlement_id = ?
                      AND id = ?
                )
                """;

    private static final String INSERT_SETTLEMENT_SQL = """
                INSERT INTO settlement_tb (
                    settlement_id,
                    room_id,
                    settlement_month,
                    total_price,
                    total_fee,
                    total_pay_amount,
                    due_date,
                    payment_start_date,
                    payment_close_date,
                    service_start_date,
                    service_end_date,
                    replace_start_date,
                    replace_end_date,
                    status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

    // SELECT문 ================================================================
    private static final String CAN_USE_CHAT_ROOM_SQL = """
                SELECT COUNT(*)
                FROM ott_room_tb r
                WHERE r.room_id = ?
                  AND r.status <> 'CLOSED'
                  AND (
                        r.host_login_id = ?
                        OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.member_login_id = ?
                              AND rm.status = 'ACTIVE'
                        )
                  )
                """;

    private static final String COUNT_ACTIVE_ROOM_MEMBERS_SQL = "SELECT COUNT(*) FROM ott_room_member_tb WHERE room_id = ? AND status = 'ACTIVE'";

    private static final String COUNT_MY_ROOMS_SQL = """
                SELECT COUNT(*)
                FROM ott_room_tb r
                WHERE r.status <> 'CLOSED'
                  AND (
                       r.host_login_id = ?
                       OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.member_login_id = ?
                              AND rm.status = 'ACTIVE'
                       )
                  )
                """;

    private static final String COUNT_RECRUIT_ROOMS_SQL = """
                SELECT COUNT(*)
                FROM ott_room_tb r
                JOIN member_tb m
                  ON r.host_login_id = m.id
                 AND m.status NOT IN ('LEAVE', 'PERM_BLOCK')
                WHERE NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                  AND r.status IN ('RECRUITING', 'REPLACE_RECRUITING')
                """;

    private static final String COUNT_FILTERED_RECRUIT_ROOMS_SQL = """
                SELECT COUNT(*)
                FROM ott_room_tb r
                JOIN member_tb m
                  ON r.host_login_id = m.id
                 AND m.status NOT IN ('LEAVE', 'PERM_BLOCK')
                WHERE NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                  AND r.status <> 'CLOSED'
                  AND (? IS NULL OR r.ott_service_id = ?)
                  AND (? IS NULL OR LOWER(r.room_name) LIKE '%' || LOWER(?) || '%')
                """;

    // 메인 화면에는 금액 추정치가 아니라 사용자가 실제로 관련된 정산 회차 수를 표시한다.
    private static final String COUNT_MY_SETTLEMENTS_SQL = """
                SELECT COUNT(DISTINCT st.settlement_id)
                FROM settlement_tb st
                JOIN ott_room_tb r ON st.room_id = r.room_id
                WHERE st.settlement_month = ?
                  AND NVL(st.status, 'READY') <> 'CANCELLED'
                  AND (
                       r.host_login_id = ?
                       OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.member_login_id = ?
                       )
                  )
                """;

    private static final String COUNT_UNREAD_CHAT_MESSAGES_SQL = """
                SELECT COUNT(*)
                FROM ott_chat_message_tb cm
                JOIN ott_room_tb r ON cm.room_id = r.room_id
                WHERE cm.sender_id <> ?
                  AND r.status <> 'CLOSED'
                  AND (
                        r.host_login_id = ?
                        OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.member_login_id = ?
                              AND rm.status = 'ACTIVE'
                        )
                  )
                  AND cm.created_at > NVL((
                        SELECT cr.last_read_at
                        FROM ott_chat_read_tb cr
                        WHERE cr.room_id = cm.room_id
                          AND cr.member_login_id = ?
                  ), TO_DATE('1900-01-01', 'YYYY-MM-DD'))
                """;

    private static final String EXISTS_SETTLEMENT_SQL = "SELECT COUNT(*) FROM settlement_tb WHERE room_id = ? AND settlement_month = ?";

    private static final String HAS_PAID_UPCOMING_PAYMENT_SQL = """
                SELECT COUNT(*)
                FROM settlement_payment_tb sp
                JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
                WHERE st.room_id = ?
                  AND sp.id = ?
                  AND sp.payment_status IN ('PAID', 'CONFIRMED')
                  AND st.service_start_date >= ?
                """;

    private static final String HAS_RESERVED_LEAVE_SQL = "SELECT COUNT(*) FROM ott_room_member_tb WHERE room_id = ? AND member_login_id = ? AND member_role = 'MEMBER' AND status = 'ACTIVE' AND leave_reserved_yn = 'Y'";

    private static final String IS_ACTIVE_NORMAL_MEMBER_SQL = "SELECT COUNT(*) FROM ott_room_member_tb WHERE room_id = ? AND member_login_id = ? AND member_role = 'MEMBER' AND status = 'ACTIVE'";

    private static final String SELECT_ACTIVE_MEMBERS_SQL = """
                SELECT rm.room_member_id,
                       rm.room_id,
                       r.room_name,
                       s.service_name,
                       rm.member_login_id AS member_login_id,
                       NVL(m.nickname, rm.member_login_id) AS member_nickname,
                       NVL(m.member_name, rm.member_login_id) AS member_name,
                       rm.member_role,
                       rm.share_amount,
                       rm.fee_rate,
                       rm.fee_amount,
                       rm.pay_amount,
                       TO_CHAR(rm.joined_at, 'YYYY-MM-DD') AS joined_at,
                       rm.status,
                       NVL(rm.leave_reserved_yn, 'N') AS leave_reserved_yn,
                       TO_CHAR(rm.leave_requested_at, 'YYYY-MM-DD') AS leave_requested_at,
                       TO_CHAR(rm.leave_scheduled_date, 'YYYY-MM-DD') AS leave_scheduled_date,
                       TO_CHAR(rm.leave_cancelled_at, 'YYYY-MM-DD') AS leave_cancelled_at,
                       rm.leave_reason
                FROM ott_room_member_tb rm
                JOIN ott_room_tb r ON rm.room_id = r.room_id
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON rm.member_login_id = m.id
                WHERE rm.room_id = ?
                  AND rm.status = 'ACTIVE'
                ORDER BY rm.member_role DESC, rm.joined_at
                """;

    private static final String SELECT_ACTIVE_ROOM_MEMBER_IDS_SQL = "SELECT member_login_id FROM ott_room_member_tb WHERE room_id = ? AND status = 'ACTIVE'";

    private static final String SELECT_CHAT_MESSAGES_SQL = """
                SELECT *
                FROM (
                    SELECT latest.*
                    FROM (
                        SELECT cm.message_id,
                               cm.room_id,
                               cm.sender_id,
                               CASE
                                   WHEN cm.message_content LIKE '[SYSTEM] %'
                                        OR cm.message_content LIKE '%공유방에 참여했습니다.%'
                                   THEN '시스템 알림'
                                   ELSE NVL(m.member_name, cm.sender_id)
                               END AS sender_name,
                               CASE
                                   WHEN cm.message_content LIKE '[SYSTEM] %' THEN SUBSTR(cm.message_content, 10)
                                   ELSE cm.message_content
                               END AS message_content,
                               TO_CHAR(cm.created_at, 'YYYY-MM-DD HH24:MI') AS created_at,
                               CASE
                                   WHEN cm.message_content LIKE '[SYSTEM] %'
                                        OR cm.message_content LIKE '%공유방에 참여했습니다.%'
                                   THEN 'N'
                                   WHEN cm.sender_id = ? THEN 'Y'
                                   ELSE 'N'
                               END AS mine_yn,
                               CASE
                                   WHEN cm.message_content LIKE '[SYSTEM] %'
                                        OR cm.message_content LIKE '%공유방에 참여했습니다.%'
                                   THEN 'Y'
                                   ELSE 'N'
                               END AS system_yn
                        FROM ott_chat_message_tb cm
                        LEFT JOIN member_tb m ON cm.sender_id = m.id
                        WHERE cm.room_id = ?
                        ORDER BY cm.created_at DESC, cm.message_id DESC
                    ) latest
                    WHERE ROWNUM <= 100
                )
                ORDER BY message_id
                """;

    private static final String SELECT_CHAT_MESSAGE_SEQUENCE_SQL = "SELECT seq_ott_chat_message.NEXTVAL FROM dual";

    private static final String SELECT_HOSTED_ROOMS_BY_MODE_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL(COUNT(CASE WHEN rm.status = 'ACTIVE' THEN 1 END), 0) AS current_member_count
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON r.host_login_id = m.id
                LEFT JOIN ott_room_member_tb rm ON r.room_id = rm.room_id
                WHERE r.host_login_id = ?
                  AND r.status <> 'CLOSED'
                  AND (? IS NULL OR NVL(r.room_mode, 'RECRUIT') = ?)
                GROUP BY r.room_id,
                         r.host_login_id,
                         NVL(m.nickname, r.host_login_id),
                         r.ott_service_id,
                         s.service_name,
                         r.room_name,
                         r.plan_name,
                         r.total_price,
                         r.billing_day,
                         r.member_limit,
                         NVL(r.room_mode, 'RECRUIT'),
                         r.status,
                         r.invite_code,
                         TO_CHAR(r.close_requested_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.close_effective_date, 'YYYY-MM-DD'),
                         r.close_reason,
                         r.close_notice,
                         TO_CHAR(r.closed_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.created_at, 'YYYY-MM-DD')
                ORDER BY r.room_id DESC
                """;

    private static final String SELECT_HOSTED_ROOM_MEMBERS_SQL = """
                SELECT rm.room_member_id,
                       rm.room_id,
                       r.room_name,
                       s.service_name,
                       rm.member_login_id AS member_login_id,
                       NVL(m.nickname, rm.member_login_id) AS member_nickname,
                       NVL(m.member_name, rm.member_login_id) AS member_name,
                       rm.member_role,
                       rm.share_amount,
                       rm.fee_rate,
                       rm.fee_amount,
                       rm.pay_amount,
                       TO_CHAR(rm.joined_at, 'YYYY-MM-DD') AS joined_at,
                       rm.status,
                       NVL(rm.leave_reserved_yn, 'N') AS leave_reserved_yn,
                       TO_CHAR(rm.leave_requested_at, 'YYYY-MM-DD') AS leave_requested_at,
                       TO_CHAR(rm.leave_scheduled_date, 'YYYY-MM-DD') AS leave_scheduled_date,
                       TO_CHAR(rm.leave_cancelled_at, 'YYYY-MM-DD') AS leave_cancelled_at,
                       rm.leave_reason
                FROM ott_room_member_tb rm
                JOIN ott_room_tb r ON rm.room_id = r.room_id
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON rm.member_login_id = m.id
                WHERE r.host_login_id = ?
                  AND NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                  AND rm.member_role = 'MEMBER'
                  AND rm.status = 'ACTIVE'
                  AND r.status <> 'CLOSED'
                ORDER BY r.room_id DESC,
                         rm.joined_at DESC
                """;

    private static final String SELECT_HOSTED_SETTLEMENT_PAYMENTS_SQL = """
                SELECT st.settlement_id,
                       st.room_id,
                       r.room_name,
                       s.service_name,
                       st.settlement_month,
                       st.status,
                       TO_CHAR(st.payment_start_date, 'YYYY-MM-DD') AS payment_start_date,
                       TO_CHAR(st.payment_close_date, 'YYYY-MM-DD') AS payment_close_date,
                       TO_CHAR(st.service_start_date, 'YYYY-MM-DD') AS service_start_date,
                       TO_CHAR(st.service_end_date, 'YYYY-MM-DD') AS service_end_date,
                       sp.payment_id,
                       sp.id AS member_login_id,
                       NVL(m.member_name, sp.id) AS member_name,
                       NVL(m.nickname, sp.id) AS member_nickname,
                       sp.base_amount,
                       sp.fee_amount,
                       sp.total_amount,
                       sp.payment_status,
                       TO_CHAR(sp.paid_at, 'YYYY-MM-DD') AS paid_at
                FROM settlement_tb st
                JOIN ott_room_tb r ON st.room_id = r.room_id
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                JOIN settlement_payment_tb sp ON st.settlement_id = sp.settlement_id
                LEFT JOIN member_tb m ON sp.id = m.id
                WHERE r.host_login_id = ?
                  AND (? IS NULL OR NVL(r.room_mode, 'RECRUIT') = ?)
                ORDER BY st.settlement_month DESC,
                         st.settlement_id DESC,
                         r.room_id DESC,
                         sp.payment_status,
                         sp.id
                """;

    private static final String SELECT_JOINED_ROOMS_BY_MODE_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL(COUNT(CASE WHEN rm_all.status = 'ACTIVE' THEN 1 END), 0) AS current_member_count,
                       mine.status AS my_application_status,
                       NVL(mine.leave_reserved_yn, 'N') AS leave_reserved_yn,
                       TO_CHAR(mine.leave_requested_at, 'YYYY-MM-DD') AS leave_requested_at,
                       TO_CHAR(mine.leave_scheduled_date, 'YYYY-MM-DD') AS leave_scheduled_date,
                       TO_CHAR(mine.leave_cancelled_at, 'YYYY-MM-DD') AS leave_cancelled_at,
                       mine.leave_reason
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                JOIN ott_room_member_tb mine
                  ON mine.room_id = r.room_id
                 AND mine.member_login_id = ?
                 AND mine.member_role = 'MEMBER'
                 AND mine.status = 'ACTIVE'
                LEFT JOIN member_tb m ON r.host_login_id = m.id
                LEFT JOIN ott_room_member_tb rm_all ON r.room_id = rm_all.room_id
                WHERE r.status <> 'CLOSED'
                  AND r.host_login_id <> ?
                  AND (? IS NULL OR NVL(r.room_mode, 'RECRUIT') = ?)
                GROUP BY r.room_id,
                         r.host_login_id,
                         NVL(m.nickname, r.host_login_id),
                         r.ott_service_id,
                         s.service_name,
                         r.room_name,
                         r.plan_name,
                         r.total_price,
                         r.billing_day,
                         r.member_limit,
                         NVL(r.room_mode, 'RECRUIT'),
                         r.status,
                         r.invite_code,
                         TO_CHAR(r.close_requested_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.close_effective_date, 'YYYY-MM-DD'),
                         r.close_reason,
                         r.close_notice,
                         TO_CHAR(r.closed_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.created_at, 'YYYY-MM-DD'),
                         mine.status,
                         NVL(mine.leave_reserved_yn, 'N'),
                         TO_CHAR(mine.leave_requested_at, 'YYYY-MM-DD'),
                         TO_CHAR(mine.leave_scheduled_date, 'YYYY-MM-DD'),
                         TO_CHAR(mine.leave_cancelled_at, 'YYYY-MM-DD'),
                         mine.leave_reason
                ORDER BY r.room_id DESC
                """;

    private static final String SELECT_MEMBER_DISPLAY_NAME_SQL = "SELECT NVL(member_name, NVL(nickname, id)) FROM member_tb WHERE id = ?";

    private static final String SELECT_MY_CHAT_ROOMS_SQL = """
                SELECT r.room_id,
                       r.room_name,
                       s.service_name,
                       NVL((
                            SELECT MAX(cm.message_content) KEEP (DENSE_RANK LAST ORDER BY cm.created_at, cm.message_id)
                            FROM ott_chat_message_tb cm
                            WHERE cm.room_id = r.room_id
                       ), '아직 메시지가 없습니다.') AS last_message,
                       NVL((
                            SELECT COUNT(*)
                            FROM ott_chat_message_tb cm
                            WHERE cm.room_id = r.room_id
                              AND cm.sender_id <> ?
                              AND cm.created_at > NVL((
                                    SELECT cr.last_read_at
                                    FROM ott_chat_read_tb cr
                                    WHERE cr.room_id = r.room_id
                                      AND cr.member_login_id = ?
                              ), TO_DATE('1900-01-01', 'YYYY-MM-DD'))
                       ), 0) AS unread_count
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                WHERE r.status <> 'CLOSED'
                  AND (
                       r.host_login_id = ?
                       OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.member_login_id = ?
                              AND rm.status = 'ACTIVE'
                       )
                  )
                ORDER BY r.room_id DESC
                """;

    private static final String SELECT_MY_ROOMS_BY_MODE_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL(COUNT(CASE WHEN rm_all.status = 'ACTIVE' THEN 1 END), 0) AS current_member_count,
                       NVL(MAX(rm_mine.leave_reserved_yn), 'N') AS leave_reserved_yn,
                       TO_CHAR(MAX(rm_mine.leave_requested_at), 'YYYY-MM-DD') AS leave_requested_at,
                       TO_CHAR(MAX(rm_mine.leave_scheduled_date), 'YYYY-MM-DD') AS leave_scheduled_date,
                       TO_CHAR(MAX(rm_mine.leave_cancelled_at), 'YYYY-MM-DD') AS leave_cancelled_at,
                       MAX(rm_mine.leave_reason) AS leave_reason
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON r.host_login_id = m.id
                LEFT JOIN ott_room_member_tb rm_all ON r.room_id = rm_all.room_id
                LEFT JOIN ott_room_member_tb rm_mine ON r.room_id = rm_mine.room_id AND rm_mine.member_login_id = ?
                WHERE r.status <> 'CLOSED'
                  AND (? IS NULL OR NVL(r.room_mode, 'RECRUIT') = ?)
                  AND (
                       r.host_login_id = ?
                       OR EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb mine
                            WHERE mine.room_id = r.room_id
                              AND mine.member_login_id = ?
                              AND mine.status = 'ACTIVE'
                       )
                  )
                GROUP BY r.room_id,
                         r.host_login_id,
                         NVL(m.nickname, r.host_login_id),
                         r.ott_service_id,
                         s.service_name,
                         r.room_name,
                         r.plan_name,
                         r.total_price,
                         r.billing_day,
                         r.member_limit,
                         NVL(r.room_mode, 'RECRUIT'),
                         r.status,
                         r.invite_code,
                         TO_CHAR(r.close_requested_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.close_effective_date, 'YYYY-MM-DD'),
                         r.close_reason,
                         r.close_notice,
                         TO_CHAR(r.closed_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.created_at, 'YYYY-MM-DD')
                ORDER BY r.room_id DESC
                """;

    private static final String SELECT_MY_SETTLEMENTS_SQL = """
            SELECT st.settlement_id,
                   st.room_id,
                   r.room_name,
                   s.service_name,
                   st.settlement_month,
                   st.total_price,
                   st.total_fee,
                   st.total_pay_amount,
                   TO_CHAR(st.due_date, 'YYYY-MM-DD') AS due_date,
                   TO_CHAR(st.payment_start_date, 'YYYY-MM-DD') AS payment_start_date,
                   TO_CHAR(st.payment_close_date, 'YYYY-MM-DD') AS payment_close_date,
                   TO_CHAR(st.service_start_date, 'YYYY-MM-DD') AS service_start_date,
                   TO_CHAR(st.service_end_date, 'YYYY-MM-DD') AS service_end_date,
                   TO_CHAR(st.replace_start_date, 'YYYY-MM-DD') AS replace_start_date,
                   TO_CHAR(st.replace_end_date, 'YYYY-MM-DD') AS replace_end_date,
                   st.status,
                   TO_CHAR(st.created_at, 'YYYY-MM-DD') AS created_at,
                   CASE WHEN r.host_login_id = ? THEN 'HOST' ELSE 'MEMBER' END AS my_role,
                   sp.payment_id AS my_payment_id,
                   sp.payment_status AS my_payment_status,
                   sp.total_amount AS my_total_amount
            FROM settlement_tb st
            JOIN ott_room_tb r ON st.room_id = r.room_id
            JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
            LEFT JOIN settlement_payment_tb sp
                   ON st.settlement_id = sp.settlement_id
                  AND sp.id = ?
            WHERE (? IS NULL OR NVL(r.room_mode, 'RECRUIT') = ?)
              AND (
                   r.host_login_id = ?
                   OR EXISTS (
                        SELECT 1
                        FROM ott_room_member_tb rm
                        WHERE rm.room_id = r.room_id
                          AND rm.member_login_id = ?
                   )
              )
            ORDER BY st.settlement_month DESC, st.settlement_id DESC
            """;

    private static final String SELECT_OLDEST_AVAILABLE_RECRUIT_ROOM_ID_SQL = """
                SELECT room_id
                FROM (
                    SELECT r.room_id
                    FROM ott_room_tb r
                    WHERE NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                    AND r.ott_service_id = ?
                    AND r.status IN ('RECRUITING', 'REPLACE_RECRUITING')
                    AND EXISTS (
                            SELECT 1
                            FROM member_tb host_member
                            WHERE host_member.id = r.host_login_id
                              AND host_member.status NOT IN ('LEAVE', 'PERM_BLOCK')
                    )
                    AND r.host_login_id <> ?
                    AND NOT EXISTS (
                            SELECT 1
                            FROM ott_room_member_tb mine
                            WHERE mine.room_id = r.room_id
                            AND mine.member_login_id = ?
                            AND mine.status = 'ACTIVE'
                    )
                    AND (
                            SELECT COUNT(*)
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                            AND rm.status = 'ACTIVE'
                    ) < r.member_limit
                    ORDER BY r.created_at ASC, r.room_id ASC
                )
                WHERE ROWNUM = 1
                """;

    private static final String SELECT_OTT_SERVICE_RULE_SQL = """
                SELECT ott_service_id,
                       service_name,
                       default_price,
                       share_yn,
                       risk_level,
                       block_reason,
                       fixed_plan_name,
                       base_price,
                       extra_member_fee,
                       extra_member_count,
                       max_member_limit,
                       platform_fee_rate
                FROM ott_service_tb
                WHERE ott_service_id = ?
                  AND share_yn = 'Y'
                """;

    private static final String SELECT_PAYMENT_MAP_SQL = """
                SELECT sp.payment_id,
                       sp.id AS id,
                       sp.payment_status,
                       sp.total_amount,
                       st.settlement_id,
                       st.room_id,
                       st.settlement_month,
                       r.host_login_id AS host_login_id,
                       r.room_name,
                       r.status AS room_status
                FROM settlement_payment_tb sp
                JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
                JOIN ott_room_tb r ON st.room_id = r.room_id
                WHERE sp.payment_id = ?
                """;

    private static final String SELECT_RECRUIT_ROOMS_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL(COUNT(CASE WHEN rm.status = 'ACTIVE' THEN 1 END), 0) AS current_member_count,
                       NVL((
                            SELECT MAX(mine.status)
                            FROM ott_room_member_tb mine
                            WHERE mine.room_id = r.room_id
                              AND mine.member_login_id = ?
                       ), 'NONE') AS my_application_status
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                JOIN member_tb m
                  ON r.host_login_id = m.id
                 AND m.status NOT IN ('LEAVE', 'PERM_BLOCK')
                LEFT JOIN ott_room_member_tb rm ON r.room_id = rm.room_id
                WHERE NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                  AND r.status <> 'CLOSED'
                  AND (? IS NULL OR r.ott_service_id = ?)
                  AND (? IS NULL OR LOWER(r.room_name) LIKE '%' || LOWER(?) || '%')
                GROUP BY r.room_id,
                         r.host_login_id,
                         NVL(m.nickname, r.host_login_id),
                         r.ott_service_id,
                         s.service_name,
                         r.room_name,
                         r.plan_name,
                         r.total_price,
                         r.billing_day,
                         r.member_limit,
                         NVL(r.room_mode, 'RECRUIT'),
                         r.status,
                         r.invite_code,
                         TO_CHAR(r.close_requested_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.close_effective_date, 'YYYY-MM-DD'),
                         r.close_reason,
                         r.close_notice,
                         TO_CHAR(r.closed_at, 'YYYY-MM-DD'),
                         TO_CHAR(r.created_at, 'YYYY-MM-DD')
                ORDER BY r.room_id DESC
                """;

    private static final String SELECT_RECRUIT_ROOMS_PAGE_SQL =
            SELECT_RECRUIT_ROOMS_SQL + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    private static final String SELECT_ROOM_BY_INVITE_CODE_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL((
                            SELECT COUNT(*)
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.status = 'ACTIVE'
                       ), 0) AS current_member_count
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON r.host_login_id = m.id
                WHERE r.invite_code = ?
                  AND r.status <> 'CLOSED'
                """;

    private static final String SELECT_ROOM_MEMBER_SEQUENCE_SQL = "SELECT seq_ott_room_member.NEXTVAL FROM dual";

    private static final String SELECT_ROOM_MEMBER_STATUS_SQL = "SELECT status FROM ott_room_member_tb WHERE room_id = ? AND member_login_id = ?";

    private static final String SELECT_ROOM_SEQUENCE_SQL = "SELECT seq_ott_room.NEXTVAL FROM dual";

    private static final String SELECT_ROOM_SQL = """
                SELECT r.room_id,
                       r.host_login_id,
                       NVL(m.nickname, r.host_login_id) AS host_nickname,
                       r.ott_service_id,
                       s.service_name,
                       r.room_name,
                       r.plan_name,
                       r.total_price,
                       r.billing_day,
                       r.member_limit,
                       NVL(r.room_mode, 'RECRUIT') AS room_mode,
                       r.status,
                       r.invite_code,
                       TO_CHAR(r.close_requested_at, 'YYYY-MM-DD') AS close_requested_at,
                       TO_CHAR(r.close_effective_date, 'YYYY-MM-DD') AS close_effective_date,
                       r.close_reason,
                       r.close_notice,
                       TO_CHAR(r.closed_at, 'YYYY-MM-DD') AS closed_at,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       NVL((
                            SELECT COUNT(*)
                            FROM ott_room_member_tb rm
                            WHERE rm.room_id = r.room_id
                              AND rm.status = 'ACTIVE'
                       ), 0) AS current_member_count
                FROM ott_room_tb r
                JOIN ott_service_tb s ON r.ott_service_id = s.ott_service_id
                LEFT JOIN member_tb m ON r.host_login_id = m.id
                WHERE r.room_id = ?
                """;

    private static final String SELECT_SERVICE_NAME_SQL = "SELECT service_name FROM ott_service_tb WHERE ott_service_id = ?";

    private static final String SELECT_SETTLEMENT_ID_SQL = "SELECT settlement_id FROM settlement_tb WHERE room_id = ? AND settlement_month = ?";

    private static final String SELECT_SETTLEMENT_PAYMENT_SEQUENCE_SQL = "SELECT seq_settlement_payment.NEXTVAL FROM dual";

    private static final String SELECT_SETTLEMENT_SEQUENCE_SQL = "SELECT seq_settlement.NEXTVAL FROM dual";

    private static final String SELECT_SHAREABLE_SERVICES_SQL = """
                SELECT ott_service_id,
                       service_name,
                       default_price,
                       share_yn,
                       risk_level,
                       block_reason,
                       fixed_plan_name,
                       base_price,
                       extra_member_fee,
                       extra_member_count,
                       max_member_limit,
                       platform_fee_rate
                FROM ott_service_tb
                WHERE share_yn = 'Y'
                ORDER BY service_name
                """;

    // UPDATE문 ================================================================
    private static final String CANCEL_ROOM_LEAVE_SQL = "UPDATE ott_room_member_tb SET leave_reserved_yn = 'N', leave_cancelled_at = SYSDATE, leave_reason = NVL(leave_reason, '') || ' / 예약 취소' WHERE room_id = ? AND member_login_id = ? AND member_role = 'MEMBER' AND status = 'ACTIVE' AND leave_reserved_yn = 'Y'";

    private static final String CANCEL_UNPAID_FUTURE_PAYMENTS_SQL = """
                UPDATE settlement_payment_tb sp
                SET payment_status = 'CANCELLED',
                    cancelled_at = SYSDATE,
                    memo = NVL(sp.memo, '') || ' / 방 삭제 요청으로 결제 취소'
                WHERE sp.payment_status = 'UNPAID'
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        WHERE st.settlement_id = sp.settlement_id
                          AND st.room_id = ?
                          AND (st.service_start_date >= ?
                               OR (st.service_start_date IS NULL AND st.settlement_month >= ?))
                  )
                """;

    private static final String CLOSE_EFFECTIVE_ROOMS_CLOSE_MEMBER_SQL = """
                UPDATE ott_room_member_tb rm
                SET status = 'OUT',
                    left_at = SYSDATE
                WHERE rm.status = 'ACTIVE'
                  AND EXISTS (
                        SELECT 1
                        FROM ott_room_tb r
                        WHERE r.room_id = rm.room_id
                          AND r.status = 'CLOSE_REQUESTED'
                          AND r.close_effective_date <= TRUNC(SYSDATE)
                  )
                """;

    private static final String CLOSE_EFFECTIVE_ROOMS_CLOSE_ROOM_SQL = "UPDATE ott_room_tb SET status = 'CLOSED', closed_at = SYSDATE, updated_at = SYSDATE WHERE status = 'CLOSE_REQUESTED' AND close_effective_date <= TRUNC(SYSDATE)";

    private static final String EXPIRE_OVERDUE_PAYMENTS_EXPIRE_PAYMENTS_SQL = """
                UPDATE settlement_payment_tb sp
                SET payment_status = 'EXPIRED',
                    expired_at = SYSDATE,
                    memo = NVL(sp.memo, '') || ' / 결제 마감일 초과'
                WHERE sp.payment_status = 'UNPAID'
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        WHERE st.settlement_id = sp.settlement_id
                          AND st.status IN ('PAYMENT_OPEN', 'REQUESTED', 'READY')
                          AND st.payment_close_date < TRUNC(SYSDATE)
                  )
                """;

    private static final String EXPIRE_OVERDUE_PAYMENTS_KICK_MEMBERS_SQL = """
                UPDATE ott_room_member_tb rm
                SET status = 'KICKED',
                    kicked_at = SYSDATE,
                    kicked_reason = '다음 이용분 결제 마감일까지 결제하지 않아 자동 추방되었습니다.'
                WHERE rm.member_role = 'MEMBER'
                  AND rm.status = 'ACTIVE'
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_payment_tb sp
                        JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
                        WHERE st.room_id = rm.room_id
                          AND sp.id = rm.member_login_id
                          AND sp.payment_status = 'EXPIRED'
                          AND st.payment_close_date < TRUNC(SYSDATE)
                  )
                """;

    private static final String EXPIRE_OVERDUE_PAYMENTS_UPDATE_ROOM_SQL = """
                UPDATE ott_room_tb r
                SET status = 'REPLACE_RECRUITING',
                    updated_at = SYSDATE
                WHERE r.status IN ('ACTIVE', 'PAYMENT_OPEN', 'RECRUITING')
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        WHERE st.room_id = r.room_id
                          AND st.status = 'REPLACE_RECRUITING'
                          AND st.replace_end_date >= TRUNC(SYSDATE)
                  )
                """;

    private static final String EXPIRE_OVERDUE_PAYMENTS_UPDATE_SETTLEMENT_SQL = """
                UPDATE settlement_tb st
                SET status = 'REPLACE_RECRUITING'
                WHERE st.status IN ('PAYMENT_OPEN', 'REQUESTED', 'READY')
                  AND st.payment_close_date < TRUNC(SYSDATE)
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_payment_tb sp
                        WHERE sp.settlement_id = st.settlement_id
                          AND sp.payment_status = 'EXPIRED'
                  )
                """;

    private static final String INSERT_REFUNDS_FOR_ROOM_CLOSE_UPDATE_PAYMENT_SQL = """
                UPDATE settlement_payment_tb sp
                SET payment_status = 'REFUNDED',
                    cancelled_at = SYSDATE,
                    memo = NVL(sp.memo, '') || ' / 방 삭제 요청으로 자동 환불 완료'
                WHERE sp.payment_status IN ('PAID', 'CONFIRMED')
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        WHERE st.settlement_id = sp.settlement_id
                          AND st.room_id = ?
                          AND (st.service_start_date >= ?
                               OR (st.service_start_date IS NULL AND st.settlement_month >= ?))
                  )
                """;

    private static final String MARK_FUTURE_SETTLEMENTS_CANCELLED_SQL = "UPDATE settlement_tb SET status = 'CANCELLED', closed_at = SYSDATE WHERE room_id = ? AND status IN ('PAYMENT_OPEN', 'REQUESTED', 'READY', 'REPLACE_RECRUITING') AND (service_start_date >= ? OR (service_start_date IS NULL AND settlement_month >= ?))";

    private static final String PROCESS_LEAVE_RESERVATIONS_CANCEL_UNPAID_PAYMENT_SQL = """
                UPDATE settlement_payment_tb sp
                SET payment_status = 'CANCELLED',
                    cancelled_at = SYSDATE,
                    memo = NVL(sp.memo, '') || ' / 나가기 예약으로 다음 이용분 결제 제외'
                WHERE sp.payment_status = 'UNPAID'
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        JOIN ott_room_member_tb rm ON rm.room_id = st.room_id
                        WHERE st.settlement_id = sp.settlement_id
                          AND st.room_id = rm.room_id
                          AND sp.id = rm.member_login_id
                          AND rm.member_role = 'MEMBER'
                          AND rm.status = 'ACTIVE'
                          AND rm.leave_reserved_yn = 'Y'
                          AND rm.leave_scheduled_date <= TRUNC(SYSDATE)
                          AND st.service_start_date >= TRUNC(SYSDATE)
                  )
                """;

    private static final String PROCESS_LEAVE_RESERVATIONS_LEAVE_MEMBER_SQL = "UPDATE ott_room_member_tb rm SET status = 'OUT', left_at = SYSDATE, leave_reserved_yn = 'N', leave_reason = NVL(rm.leave_reason, '') || ' / 예약일 자동 퇴장 완료' WHERE rm.member_role = 'MEMBER' AND rm.status = 'ACTIVE' AND rm.leave_reserved_yn = 'Y' AND rm.leave_scheduled_date <= TRUNC(SYSDATE)";

    private static final String PROCESS_LEAVE_RESERVATIONS_UPDATE_RECRUIT_ROOM_SQL = """
                UPDATE ott_room_tb r
                SET status = 'REPLACE_RECRUITING',
                    updated_at = SYSDATE
                WHERE NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
                  AND r.status NOT IN ('CLOSED', 'CLOSE_REQUESTED')
                  AND EXISTS (
                        SELECT 1
                        FROM ott_room_member_tb rm
                        WHERE rm.room_id = r.room_id
                          AND rm.member_role = 'MEMBER'
                          AND rm.status = 'ACTIVE'
                          AND rm.leave_reserved_yn = 'Y'
                          AND rm.leave_scheduled_date <= TRUNC(SYSDATE)
                  )
                """;

    // 재입장 시 과거 탈퇴 예약 정보까지 초기화한다.
    // 이 값이 남아 있으면 스케줄러가 재입장한 사용자를 다시 자동 퇴장시킬 수 있다.
    private static final String REACTIVATE_ROOM_MEMBER_SQL = "UPDATE ott_room_member_tb SET member_role = 'MEMBER', share_amount = ?, fee_rate = ?, fee_amount = ?, pay_amount = ?, pay_day = ?, status = 'ACTIVE', joined_at = SYSDATE, kicked_at = NULL, kicked_reason = NULL, left_at = NULL, leave_reserved_yn = 'N', leave_requested_at = NULL, leave_scheduled_date = NULL, leave_cancelled_at = NULL, leave_reason = NULL WHERE room_id = ? AND member_login_id = ?";

    private static final String RESERVE_ROOM_LEAVE_SQL = "UPDATE ott_room_member_tb SET leave_reserved_yn = 'Y', leave_requested_at = SYSDATE, leave_scheduled_date = ?, leave_cancelled_at = NULL, leave_reason = '다음 결제일 전 나가기 예약' WHERE room_id = ? AND member_login_id = ? AND member_role = 'MEMBER' AND status = 'ACTIVE'";

    private static final String UPDATE_PAYMENT_PAID_SQL = """
                UPDATE settlement_payment_tb sp
                SET payment_status = 'PAID',
                    paid_at = SYSDATE,
                    memo = NVL(sp.memo, '') || ' / 사용자 결제완료 처리'
                WHERE sp.payment_id = ?
                  AND sp.id = ?
                  AND sp.payment_status = 'UNPAID'
                  AND EXISTS (
                        SELECT 1
                        FROM settlement_tb st
                        JOIN ott_room_tb r ON st.room_id = r.room_id
                        WHERE st.settlement_id = sp.settlement_id
                          AND r.status NOT IN ('CLOSE_REQUESTED', 'CLOSED')
                          AND st.status IN ('PAYMENT_OPEN', 'REQUESTED', 'READY')
                          AND (st.payment_start_date IS NULL OR TRUNC(SYSDATE) >= st.payment_start_date)
                          AND (st.payment_close_date IS NULL OR TRUNC(SYSDATE) <= st.payment_close_date)
                  )
                """;

    private static final String UPDATE_ROOM_CLOSE_REQUEST_SQL = "UPDATE ott_room_tb SET status = 'CLOSE_REQUESTED', close_requested_at = SYSDATE, close_effective_date = ?, close_reason = ?, close_notice = ?, updated_at = SYSDATE WHERE room_id = ? AND host_login_id = ? AND status NOT IN ('CLOSE_REQUESTED', 'CLOSED')";

    private static final String UPDATE_ROOM_STATUS_SQL = "UPDATE ott_room_tb SET status = ?, updated_at = SYSDATE WHERE room_id = ? AND status NOT IN ('CLOSE_REQUESTED', 'CLOSED')";

    // 정산 회차가 열릴 때 실제 결제 마감일의 일자로 ACTIVE 멤버 값을 맞춘다.
    private static final String UPDATE_ACTIVE_MEMBER_PAY_DAY_SQL =
            "UPDATE ott_room_member_tb SET pay_day = ? WHERE room_id = ? AND status = 'ACTIVE' AND member_role = 'MEMBER'";

    private static final String UPDATE_SETTLEMENT_SQL = "UPDATE settlement_tb SET total_price = ?, total_fee = ?, total_pay_amount = ?, due_date = ?, payment_start_date = ?, payment_close_date = ?, service_start_date = ?, service_end_date = ?, replace_start_date = ?, replace_end_date = ?, status = ? WHERE settlement_id = ?";

    // DELETE문 ================================================================
    // 현재 사용 중인 SQL 없음

    // MERGE문 ================================================================
    private static final String MARK_CHAT_ROOM_AS_READ_SQL = """
                MERGE INTO ott_chat_read_tb cr
                USING (
                    SELECT ? AS room_id,
                           ? AS member_login_id
                    FROM dual
                ) src
                ON (cr.room_id = src.room_id AND cr.member_login_id = src.member_login_id)
                WHEN MATCHED THEN UPDATE SET cr.last_read_at = SYSDATE
                WHEN NOT MATCHED THEN INSERT (room_id, member_login_id, last_read_at)
                VALUES (src.room_id, src.member_login_id, SYSDATE)
                """;


    // OTT 관련 SQL 실행을 담당하는 JdbcTemplate
    private final JdbcTemplate jdbcTemplate;

    // JdbcTemplate을 주입받아 OTT 관련 SQL을 실행
    public OttRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================================
    // 1. OTT 서비스/요금제 기준 조회
    // =========================================================

    // 공유 가능한 OTT 서비스와 고정 요금 규칙 조회
    @Override
    public List<OttServiceDTO> selectShareableServices() {

        return jdbcTemplate.query(SELECT_SHAREABLE_SERVICES_SQL, (rs, rowNum) -> mapOttService(rs));
    }

    // 선택한 OTT 서비스의 요금 및 공유 규칙 조회
    @Override
    public OttServiceDTO selectOttServiceRule(Long ott_service_id) {
        if (ott_service_id == null) {
            return null;
        }


        try {
            return jdbcTemplate.queryForObject(SELECT_OTT_SERVICE_RULE_SQL, (rs, rowNum) -> mapOttService(rs), ott_service_id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // =========================================================
    // 2. 공유방/모집방 조회
    // =========================================================

    // 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> selectRecruitRooms(String loginId) {
        return selectRecruitRooms(loginId, null, null);
    }

    // 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> selectRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword) {

        return jdbcTemplate.query(SELECT_RECRUIT_ROOMS_SQL,
                (rs, rowNum) -> mapRoom(rs, true),
                loginId,
                ott_service_id,
                ott_service_id,
                roomNameKeyword,
                roomNameKeyword);
    }

    // 검색 조건과 페이지 범위를 적용한 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> selectRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword,
            int offset, int pageSize) {
        return jdbcTemplate.query(SELECT_RECRUIT_ROOMS_PAGE_SQL,
                (rs, rowNum) -> mapRoom(rs, true),
                loginId,
                ott_service_id,
                ott_service_id,
                roomNameKeyword,
                roomNameKeyword,
                offset,
                pageSize);
    }

    // 빠른 참가에서 실제로 roomId를 찾는 SQL이다.
    @Override
    public Long selectOldestAvailableRecruitRoomId(Long ott_service_id, String loginId) {
        /*
        * 빠른 참가에서 실제로 roomId를 찾는 SQL이다.
        *
        * 일반 신청하기는 JSP에서 roomId가 바로 넘어오지만,
        * 빠른 참가는 ottServiceId만 넘어오기 때문에
        * 여기서 조건에 맞는 roomId를 직접 찾아야 한다.
        */

        // 1. 값이 없으면 조회할 수 없으므로 null 반환
        if (ott_service_id == null || loginId == null || loginId.isBlank()) {
            return null;
        }

        /*
        * SQL 설명:
        *
        * FROM ott_room_tb r
        * - 모집방 정보가 들어있는 테이블에서 찾는다.
        *
        * NVL(r.room_mode, 'RECRUIT') = 'RECRUIT'
        * - 가족방이 아니라 외부인 모집방만 찾는다.
        *
        * r.ott_service_id = ?
        * - 사용자가 선택한 OTT 종류와 같은 방만 찾는다.
        *
        * r.status IN ('RECRUITING', 'REPLACE_RECRUITING')
        * - 현재 모집중인 방만 찾는다.
        *
        * r.host_login_id <> ?
        * - 내가 만든 방에는 빠른 참가하지 못하게 한다.
        *
        * NOT EXISTS (...)
        * - 내가 이미 ACTIVE 상태로 참여 중인 방은 제외한다.
        *
        * ACTIVE 인원 수 < r.member_limit
        * - 아직 자리가 남아있는 방만 찾는다.
        *
        * ORDER BY r.created_at ASC, r.room_id ASC
        * - 가장 오래된 방부터 채우기 위해 생성일 오래된 순으로 정렬한다.
        * - 생성일이 같으면 room_id가 낮은 방을 먼저 선택한다.
        *
        * WHERE ROWNUM = 1
        * - 조건에 맞는 방 중 가장 첫 번째 방 하나만 가져온다.
        */

        try {
            /*
            * 파라미터 순서:
            * 1. ott_service_id → 선택한 OTT
            * 2. loginId      → 내가 만든 방 제외
            * 3. loginId      → 내가 이미 참여한 방 제외
            */
            return jdbcTemplate.queryForObject(SELECT_OLDEST_AVAILABLE_RECRUIT_ROOM_ID_SQL, Long.class, ott_service_id, loginId, loginId);
        } catch (EmptyResultDataAccessException e) {
            /*
            * 조건에 맞는 방이 하나도 없으면 queryForObject가 예외를 던진다.
            * 이 경우 빠른 참가 실패로 보고 null을 반환한다.
            */
            return null;
        }
    }

    // 내가 참여 중인 가족·지인 공유방 목록 조회
    @Override
    public List<OttRoomDTO> selectFriendRooms(String loginId) {
        return selectMyRoomsByMode(loginId, "FRIEND");
    }

    // 내가 방장인 가족·지인 공유방 목록 조회
    @Override
    public List<OttRoomDTO> selectHostedFriendRooms(String loginId) {
        return selectHostedRoomsByMode(loginId, "FRIEND");
    }

    // 내가 방장인 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> selectHostedRecruitRooms(String loginId) {
        return selectHostedRoomsByMode(loginId, "RECRUIT");
    }

    // 내가 일반 멤버로 참여 중인 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> selectJoinedRecruitRooms(String loginId) {
        return selectJoinedRoomsByMode(loginId, "RECRUIT");
    }

    // 내가 참여 중인 전체 OTT 방 목록 조회
    @Override
    public List<OttRoomDTO> selectMyRooms(String loginId) {
        return selectMyRoomsByMode(loginId, null);
    }

    // 내가 방장인 전체 OTT 방 목록 조회
    @Override
    public List<OttRoomDTO> selectHostedRooms(String loginId) {
        return selectHostedRoomsByMode(loginId, null);
    }


    // 방 유형에 따라 내가 참여 중인 방 목록 조회
    private List<OttRoomDTO> selectMyRoomsByMode(String loginId, String room_mode) {

        return jdbcTemplate.query(SELECT_MY_ROOMS_BY_MODE_SQL, (rs, rowNum) -> mapRoom(rs, false), loginId, room_mode, room_mode, loginId, loginId);
    }

    // 방 유형에 따라 내가 방장인 방 목록 조회
    private List<OttRoomDTO> selectHostedRoomsByMode(String loginId, String room_mode) {

        return jdbcTemplate.query(SELECT_HOSTED_ROOMS_BY_MODE_SQL, (rs, rowNum) -> mapRoom(rs, false), loginId, room_mode, room_mode);
    }

    // 방 유형에 따라 내가 일반 멤버로 참여 중인 방 목록 조회
    private List<OttRoomDTO> selectJoinedRoomsByMode(String loginId, String room_mode) {

        return jdbcTemplate.query(SELECT_JOINED_ROOMS_BY_MODE_SQL, (rs, rowNum) -> mapRoom(rs, true), loginId, loginId, room_mode, room_mode);
    }

    // =========================================================
    // 3. 참여자 조회
    // =========================================================

    // 내가 방장인 방의 참여자 목록 조회
    @Override
    public List<OttRoomMemberDTO> selectHostedRoomMembers(String loginId) {

        return jdbcTemplate.query(SELECT_HOSTED_ROOM_MEMBERS_SQL, (rs, rowNum) -> mapRoomMember(rs), loginId);
    }

    // =========================================================
    // 4. 정산/결제 조회
    // =========================================================

    // 내 OTT 정산 내역 조회
    @Override
    public List<OttSettlementDTO> selectMySettlements(String loginId) {
        return selectMySettlements(loginId, null);
    }

    // 내 OTT 정산 내역 조회
    @Override
    public List<OttSettlementDTO> selectMySettlements(String loginId, String room_mode) {

        return jdbcTemplate.query(SELECT_MY_SETTLEMENTS_SQL, (rs, rowNum) -> mapMySettlement(rs), loginId, loginId, room_mode, room_mode, loginId, loginId);
    }

    // 내가 방장인 방의 멤버별 정산 결제 내역 조회
    @Override
    public List<OttSettlementDTO> selectHostedSettlementPayments(String loginId, String room_mode) {

        return jdbcTemplate.query(SELECT_HOSTED_SETTLEMENT_PAYMENTS_SQL, (rs, rowNum) -> {
            OttSettlementDTO settlement = new OttSettlementDTO();
            settlement.setSettlement_id(rs.getLong("settlement_id"));
            settlement.setRoom_id(rs.getLong("room_id"));
            settlement.setRoom_name(rs.getString("room_name"));
            settlement.setService_name(rs.getString("service_name"));
            settlement.setSettlement_month(rs.getString("settlement_month"));
            settlement.setStatus(rs.getString("status"));
            settlement.setPayment_start_date(rs.getString("payment_start_date"));
            settlement.setPayment_close_date(rs.getString("payment_close_date"));
            settlement.setService_start_date(rs.getString("service_start_date"));
            settlement.setService_end_date(rs.getString("service_end_date"));
            settlement.setPayment_id(getNullableLong(rs, "payment_id"));
            settlement.setMember_login_id(rs.getString("member_login_id"));
            settlement.setMember_name(rs.getString("member_name"));
            settlement.setMember_nickname(rs.getString("member_nickname"));
            settlement.setBase_amount(rs.getInt("base_amount"));
            settlement.setFee_amount(rs.getInt("fee_amount"));
            settlement.setTotal_amount(rs.getInt("total_amount"));
            settlement.setPayment_status(rs.getString("payment_status"));
            settlement.setPaid_at(rs.getString("paid_at"));
            return settlement;
        }, loginId, room_mode, room_mode);
    }

    // 정산 조회 결과를 OttSettlementDTO로 변환
    private OttSettlementDTO mapMySettlement(java.sql.ResultSet rs) throws java.sql.SQLException {
        OttSettlementDTO settlement = new OttSettlementDTO();
        settlement.setSettlement_id(rs.getLong("settlement_id"));
        settlement.setRoom_id(rs.getLong("room_id"));
        settlement.setRoom_name(rs.getString("room_name"));
        settlement.setService_name(rs.getString("service_name"));
        settlement.setSettlement_month(rs.getString("settlement_month"));
        settlement.setTotal_price(rs.getInt("total_price"));
        settlement.setTotal_fee(rs.getInt("total_fee"));
        settlement.setTotal_pay_amount(rs.getInt("total_pay_amount"));
        settlement.setDue_date(rs.getString("due_date"));
        settlement.setPayment_start_date(rs.getString("payment_start_date"));
        settlement.setPayment_close_date(rs.getString("payment_close_date"));
        settlement.setService_start_date(rs.getString("service_start_date"));
        settlement.setService_end_date(rs.getString("service_end_date"));
        settlement.setReplace_start_date(rs.getString("replace_start_date"));
        settlement.setReplace_end_date(rs.getString("replace_end_date"));
        settlement.setStatus(rs.getString("status"));
        settlement.setCreated_at(rs.getString("created_at"));
        settlement.setMy_role(rs.getString("my_role"));
        settlement.setPayment_id(getNullableLong(rs, "my_payment_id"));
        settlement.setMy_payment_status(rs.getString("my_payment_status"));
        settlement.setMy_total_amount(rs.getInt("my_total_amount"));
        return settlement;
    }

    // =========================================================
    // 5. 채팅 조회
    // =========================================================

    // 내가 참여 중인 OTT 채팅방 목록 조회
    @Override
    public List<OttChatRoomDTO> selectMyChatRooms(String loginId) {

        return jdbcTemplate.query(SELECT_MY_CHAT_ROOMS_SQL, (rs, rowNum) -> {
            OttChatRoomDTO chatRoom = new OttChatRoomDTO();
            chatRoom.setRoom_id(rs.getLong("room_id"));
            chatRoom.setRoom_name(rs.getString("room_name"));
            chatRoom.setService_name(rs.getString("service_name"));
            chatRoom.setUnread_count(rs.getInt("unread_count"));
            chatRoom.setLast_message(rs.getString("last_message"));
            return chatRoom;
        }, loginId, loginId, loginId, loginId);
    }

    // 선택한 OTT 채팅방의 메시지 목록 조회
    @Override
    public List<OttChatMessageDTO> selectChatMessages(Long room_id, String loginId) {

        return jdbcTemplate.query(SELECT_CHAT_MESSAGES_SQL, (rs, rowNum) -> {
            OttChatMessageDTO message = new OttChatMessageDTO();
            message.setMessage_id(rs.getLong("message_id"));
            message.setRoom_id(rs.getLong("room_id"));
            message.setSender_id(rs.getString("sender_id"));
            message.setSender_name(rs.getString("sender_name"));
            message.setMessage_content(rs.getString("message_content"));
            message.setCreated_at(rs.getString("created_at"));
            message.setMine_yn(rs.getString("mine_yn"));
            message.setSystem_yn(rs.getString("system_yn"));
            return message;
        }, loginId, room_id);
    }

    // 채팅방 입장에 필요한 OTT 방 정보 조회
    @Override
    public OttRoomDTO selectChatRoom(Long room_id, String loginId) {
        return selectRoom(room_id);
    }

    // 현재 모집 중인 외부인 방 개수 조회
    @Override
    public int countRecruitRooms() {
        Integer count = jdbcTemplate.queryForObject(COUNT_RECRUIT_ROOMS_SQL, Integer.class);
        return count == null ? 0 : count;
    }

    // 검색 조건에 맞는 종료되지 않은 외부인 모집방 개수 조회
    @Override
    public int countRecruitRooms(Long ott_service_id, String roomNameKeyword) {
        Integer count = jdbcTemplate.queryForObject(
                COUNT_FILTERED_RECRUIT_ROOMS_SQL,
                Integer.class,
                ott_service_id,
                ott_service_id,
                roomNameKeyword,
                roomNameKeyword);
        return count == null ? 0 : count;
    }

    // 내가 참여 중인 OTT 방 개수 조회
    @Override
    public int countMyRooms(String loginId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_MY_ROOMS_SQL, Integer.class, loginId, loginId);
        return count == null ? 0 : count;
    }

    // 내가 읽지 않은 OTT 채팅 메시지 개수 조회
    @Override
    public int countUnreadChatMessages(String loginId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_UNREAD_CHAT_MESSAGES_SQL, Integer.class, loginId, loginId, loginId, loginId);
        return count == null ? 0 : count;
    }

    // 선택 월에 사용자가 실제로 관련된 정산 회차 수 조회
    @Override
    public int countMySettlements(String loginId, String settlement_month) {
        Integer count = jdbcTemplate.queryForObject(
                COUNT_MY_SETTLEMENTS_SQL,
                Integer.class,
                settlement_month,
                loginId,
                loginId);
        return count == null ? 0 : count;
    }

    // =========================================================
    // 6. 방·정산·멤버 저장 SQL
    // =========================================================

    // 방 한 건 저장 - 방 이름, 초대 코드, 상태 판단은 Service에서 완료한다.
    @Override
    public Long insertRoom(OttRoomDTO roomDTO, String loginId, String status) {
        Long room_id = jdbcTemplate.queryForObject(SELECT_ROOM_SEQUENCE_SQL, Long.class);

        jdbcTemplate.update(INSERT_ROOM_SQL,
                room_id,
                loginId,
                roomDTO.getOtt_service_id(),
                roomDTO.getRoom_name(),
                roomDTO.getPlan_name(),
                roomDTO.getTotal_price(),
                roomDTO.getBilling_day(),
                roomDTO.getMember_limit(),
                roomDTO.getRoom_mode(),
                status,
                roomDTO.getInvite_code());
        return room_id;
    }

    // 방 생성자를 HOST이자 ACTIVE 멤버로 등록
    @Override
    public void insertHostMember(Long room_id, String loginId) {
        Long room_member_id = jdbcTemplate.queryForObject(SELECT_ROOM_MEMBER_SEQUENCE_SQL, Long.class);
        jdbcTemplate.update(INSERT_HOST_MEMBER_SQL, room_member_id, room_id, loginId);
    }

    // OTT 방의 현재 상태 변경
    @Override
    public void updateRoomStatus(Long room_id, String status) {
        jdbcTemplate.update(UPDATE_ROOM_STATUS_SQL, status, room_id);
    }

    // 방 종료 요청 정보와 종료 예정일 저장
    @Override
    public int updateRoomCloseRequest(Long room_id, String hostId, LocalDate close_effective_date,
            String close_reason, String close_notice) {
        return jdbcTemplate.update(UPDATE_ROOM_CLOSE_REQUEST_SQL, Date.valueOf(close_effective_date), close_reason,
                close_notice, room_id, hostId);
    }

    // OTT 정산 회차 기본 정보 등록
    @Override
    public Long insertSettlement(OttSettlementDTO settlementDTO) {
        Long settlement_id = jdbcTemplate.queryForObject(SELECT_SETTLEMENT_SEQUENCE_SQL, Long.class);
        jdbcTemplate.update(INSERT_SETTLEMENT_SQL,
                settlement_id,
                settlementDTO.getRoom_id(),
                settlementDTO.getSettlement_month(),
                settlementDTO.getTotal_price(),
                settlementDTO.getTotal_fee(),
                settlementDTO.getTotal_pay_amount(),
                toSqlDate(settlementDTO.getDue_date()),
                toSqlDate(settlementDTO.getPayment_start_date()),
                toSqlDate(settlementDTO.getPayment_close_date()),
                toSqlDate(settlementDTO.getService_start_date()),
                toSqlDate(settlementDTO.getService_end_date()),
                toSqlDate(settlementDTO.getReplace_start_date()),
                toSqlDate(settlementDTO.getReplace_end_date()),
                settlementDTO.getStatus());
        return settlement_id;
    }

    // 기존 OTT 정산 회차 정보와 상태 변경
    @Override
    public void updateSettlement(OttSettlementDTO settlementDTO) {
        jdbcTemplate.update(UPDATE_SETTLEMENT_SQL,
                settlementDTO.getTotal_price(),
                settlementDTO.getTotal_fee(),
                settlementDTO.getTotal_pay_amount(),
                toSqlDate(settlementDTO.getDue_date()),
                toSqlDate(settlementDTO.getPayment_start_date()),
                toSqlDate(settlementDTO.getPayment_close_date()),
                toSqlDate(settlementDTO.getService_start_date()),
                toSqlDate(settlementDTO.getService_end_date()),
                toSqlDate(settlementDTO.getReplace_start_date()),
                toSqlDate(settlementDTO.getReplace_end_date()),
                settlementDTO.getStatus(),
                settlementDTO.getSettlement_id());
    }

    // 정산 멤버별 결제 건을 중복 없이 등록
    @Override
    public void insertSettlementPaymentIfAbsent(Long settlement_id, OttRoomMemberDTO member, String memo) {
        Long payment_id = jdbcTemplate.queryForObject(SELECT_SETTLEMENT_PAYMENT_SEQUENCE_SQL, Long.class);
        jdbcTemplate.update(INSERT_SETTLEMENT_PAYMENT_IF_ABSENT_SQL,
                payment_id,
                settlement_id,
                member.getMember_login_id(),
                nullToZero(member.getShare_amount()),
                member.getFee_rate() == null ? 0.0 : member.getFee_rate(),
                nullToZero(member.getFee_amount()),
                nullToZero(member.getPay_amount()),
                memo,
                settlement_id,
                member.getMember_login_id());
    }

    // 로그인 사용자의 정산 결제 건을 PAID 상태로 변경
    @Override
    public int updatePaymentPaid(Long payment_id, String loginId) {
        return jdbcTemplate.update(UPDATE_PAYMENT_PAID_SQL, payment_id, loginId);
    }

    // 결제 완료 사용자를 ACTIVE 방 멤버로 신규 등록
    @Override
    public void insertActiveRoomMember(Long room_id, String loginId, int share_amount,
            double fee_rate, int fee_amount, int pay_amount, int pay_day) {
        Long room_member_id = jdbcTemplate.queryForObject(SELECT_ROOM_MEMBER_SEQUENCE_SQL, Long.class);
        jdbcTemplate.update(INSERT_ACTIVE_ROOM_MEMBER_SQL, room_member_id, room_id, loginId,
                share_amount, fee_rate, fee_amount, pay_amount, pay_day,room_id,room_id);
    }

    // 기존 방 멤버를 ACTIVE 상태로 재입장 처리
    @Override
    public void reactivateRoomMember(Long room_id, String loginId, int share_amount,
            double fee_rate, int fee_amount, int pay_amount, int pay_day) {
        jdbcTemplate.update(
                REACTIVATE_ROOM_MEMBER_SQL,
                share_amount,
                fee_rate,
                fee_amount,
                pay_amount,
                pay_day,
                room_id,
                loginId);
    }

    // 해당 정산 회차의 실제 결제 마감일에 맞춰 ACTIVE 멤버 결제일 갱신
    @Override
    public void updateActiveMemberPayDay(Long room_id, int pay_day) {
        jdbcTemplate.update(UPDATE_ACTIVE_MEMBER_PAY_DAY_SQL, pay_day, room_id);
    }

    // 일반 멤버의 방 나가기 예약 정보 저장
    @Override
    public int reserveRoomLeave(Long room_id, String loginId, LocalDate leave_scheduled_date) {
        return jdbcTemplate.update(RESERVE_ROOM_LEAVE_SQL, Date.valueOf(leave_scheduled_date), room_id, loginId);
    }

    // 아직 처리되지 않은 방 나가기 예약 취소
    @Override
    public int cancelRoomLeave(Long room_id, String loginId) {
        return jdbcTemplate.update(CANCEL_ROOM_LEAVE_SQL, room_id, loginId);
    }

    // 방 종료일 이후 이용분의 환불 내역 등록
    @Override
    public void insertRefundsForRoomClose(Long room_id, LocalDate close_effective_date, String targetMonth) {
        jdbcTemplate.update(INSERT_REFUNDS_FOR_ROOM_CLOSE_INSERT_REFUND_SQL, room_id, Date.valueOf(close_effective_date), targetMonth);

        jdbcTemplate.update(INSERT_REFUNDS_FOR_ROOM_CLOSE_UPDATE_PAYMENT_SQL, room_id, Date.valueOf(close_effective_date), targetMonth);
    }

    // 방 종료일 이후 미결제 결제 건 취소
    @Override
    public void cancelUnpaidFuturePayments(Long room_id, LocalDate close_effective_date, String targetMonth) {
        jdbcTemplate.update(CANCEL_UNPAID_FUTURE_PAYMENTS_SQL, room_id, Date.valueOf(close_effective_date), targetMonth);
    }

    // 방 종료일 이후 미래 정산 회차 취소
    @Override
    public void markFutureSettlementsCancelled(Long room_id, LocalDate close_effective_date, String targetMonth) {
        jdbcTemplate.update(MARK_FUTURE_SETTLEMENTS_CANCELLED_SQL, room_id, Date.valueOf(close_effective_date), targetMonth);
    }

    // 예약일이 도래한 방 멤버를 OUT 상태로 변경
    @Override
    public void processLeaveReservations() {
        jdbcTemplate.update(PROCESS_LEAVE_RESERVATIONS_CANCEL_UNPAID_PAYMENT_SQL);

        jdbcTemplate.update(PROCESS_LEAVE_RESERVATIONS_UPDATE_RECRUIT_ROOM_SQL);

        jdbcTemplate.update(PROCESS_LEAVE_RESERVATIONS_LEAVE_MEMBER_SQL);
    }

    // 결제 기한이 지난 미결제 건 만료 처리
    @Override
    public void expireOverduePayments() {
        jdbcTemplate.update(EXPIRE_OVERDUE_PAYMENTS_EXPIRE_PAYMENTS_SQL);

        jdbcTemplate.update(EXPIRE_OVERDUE_PAYMENTS_KICK_MEMBERS_SQL);

        jdbcTemplate.update(EXPIRE_OVERDUE_PAYMENTS_UPDATE_SETTLEMENT_SQL);

        jdbcTemplate.update(EXPIRE_OVERDUE_PAYMENTS_UPDATE_ROOM_SQL);
    }

    // 종료 예정일이 도래한 방과 멤버를 최종 종료 처리
    @Override
    public void closeEffectiveRooms() {
        jdbcTemplate.update(CLOSE_EFFECTIVE_ROOMS_CLOSE_MEMBER_SQL);

        jdbcTemplate.update(CLOSE_EFFECTIVE_ROOMS_CLOSE_ROOM_SQL);
    }

    // OTT 관련 사용자 알림 등록
    @Override
    public void insertOttNotification(String member_login_id, String title, String message, String link_url) {
        jdbcTemplate.update(INSERT_OTT_NOTIFICATION_SQL, member_login_id, title, message, link_url);
    }

    // OTT 채팅 메시지 등록
    @Override
    public void insertChatMessage(Long room_id, String sender_id, String message_content) {
        Long message_id = jdbcTemplate.queryForObject(SELECT_CHAT_MESSAGE_SEQUENCE_SQL, Long.class);
        jdbcTemplate.update(INSERT_CHAT_MESSAGE_SQL, message_id, room_id, sender_id, message_content);
    }

    // 채팅방의 마지막 읽은 시각 저장 또는 갱신
    @Override
    public void markChatRoomAsRead(Long room_id, String loginId) {
        jdbcTemplate.update(MARK_CHAT_ROOM_AS_READ_SQL, room_id, loginId);
    }

    // =========================================================
    // 10. RowMapper / 내부 유틸
    // =========================================================

    // OTT 서비스 조회 결과를 OttServiceDTO로 변환
    private OttServiceDTO mapOttService(java.sql.ResultSet rs) throws java.sql.SQLException {
        OttServiceDTO service = new OttServiceDTO();
        service.setOtt_service_id(rs.getLong("ott_service_id"));
        service.setService_name(rs.getString("service_name"));
        service.setDefault_price(rs.getInt("default_price"));
        service.setShare_yn(rs.getString("share_yn"));
        service.setRisk_level(rs.getString("risk_level"));
        service.setBlock_reason(rs.getString("block_reason"));
        service.setFixed_plan_name(rs.getString("fixed_plan_name"));
        service.setBase_price(rs.getInt("base_price"));
        service.setExtra_member_fee(rs.getInt("extra_member_fee"));
        service.setExtra_member_count(rs.getInt("extra_member_count"));
        service.setMax_member_limit(rs.getInt("max_member_limit"));
        service.setPlatform_fee_rate(rs.getDouble("platform_fee_rate"));

        return service;
    }

    // OTT 방 조회 결과를 OttRoomDTO로 변환
    private OttRoomDTO mapRoom(java.sql.ResultSet rs, boolean hasMyStatus) throws java.sql.SQLException {
        OttRoomDTO room = new OttRoomDTO();
        room.setRoom_id(rs.getLong("room_id"));
        room.setHost_login_id(rs.getString("host_login_id"));
        room.setHost_nickname(rs.getString("host_nickname"));
        room.setOtt_service_id(rs.getLong("ott_service_id"));
        room.setService_name(rs.getString("service_name"));
        room.setRoom_name(rs.getString("room_name"));
        room.setPlan_name(rs.getString("plan_name"));
        room.setTotal_price(rs.getInt("total_price"));
        room.setBilling_day(rs.getInt("billing_day"));
        room.setMember_limit(rs.getInt("member_limit"));
        room.setRoom_mode(rs.getString("room_mode"));
        room.setCurrent_member_count(rs.getInt("current_member_count"));
        room.setStatus(rs.getString("status"));
        room.setInvite_code(rs.getString("invite_code"));
        room.setClose_requested_at(rs.getString("close_requested_at"));
        room.setClose_effective_date(rs.getString("close_effective_date"));
        room.setClose_reason(rs.getString("close_reason"));
        room.setClose_notice(rs.getString("close_notice"));
        room.setClosed_at(rs.getString("closed_at"));
        room.setCreated_at(rs.getString("created_at"));
        if (hasMyStatus) {
            room.setMy_application_status(rs.getString("my_application_status"));
        }
        room.setLeave_reserved_yn(getOptionalString(rs, "leave_reserved_yn"));
        room.setLeave_requested_at(getOptionalString(rs, "leave_requested_at"));
        room.setLeave_scheduled_date(getOptionalString(rs, "leave_scheduled_date"));
        room.setLeave_cancelled_at(getOptionalString(rs, "leave_cancelled_at"));
        room.setLeave_reason(getOptionalString(rs, "leave_reason"));
        return room;
    }

    // OTT 방 멤버 조회 결과를 OttRoomMemberDTO로 변환
    private OttRoomMemberDTO mapRoomMember(java.sql.ResultSet rs) throws java.sql.SQLException {
        OttRoomMemberDTO member = new OttRoomMemberDTO();
        member.setRoom_member_id(rs.getLong("room_member_id"));
        member.setRoom_id(rs.getLong("room_id"));
        member.setRoom_name(rs.getString("room_name"));
        member.setService_name(rs.getString("service_name"));
        member.setMember_login_id(rs.getString("member_login_id"));
        member.setMember_nickname(rs.getString("member_nickname"));
        member.setMember_name(rs.getString("member_name"));
        member.setMember_role(rs.getString("member_role"));
        member.setShare_amount(rs.getInt("share_amount"));
        member.setFee_rate(rs.getDouble("fee_rate"));
        member.setFee_amount(rs.getInt("fee_amount"));
        member.setPay_amount(rs.getInt("pay_amount"));
        member.setJoined_at(rs.getString("joined_at"));
        member.setStatus(rs.getString("status"));
        member.setLeave_reserved_yn(getOptionalString(rs, "leave_reserved_yn"));
        member.setLeave_requested_at(getOptionalString(rs, "leave_requested_at"));
        member.setLeave_scheduled_date(getOptionalString(rs, "leave_scheduled_date"));
        member.setLeave_cancelled_at(getOptionalString(rs, "leave_cancelled_at"));
        member.setLeave_reason(getOptionalString(rs, "leave_reason"));
        return member;
    }

    // 초대 코드에 해당하는 가족·지인 공유방 조회
    @Override
    public OttRoomDTO selectRoomByInviteCode(String invite_code) {
        if (invite_code == null || invite_code.isBlank()) {
            return null;
        }


        try {
            return jdbcTemplate.queryForObject(SELECT_ROOM_BY_INVITE_CODE_SQL, (rs, rowNum) -> mapRoom(rs, false), invite_code.trim().toUpperCase());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 방 ID에 해당하는 OTT 방 한 건 조회
    @Override
    public OttRoomDTO selectRoom(Long room_id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_ROOM_SQL, (rs, rowNum) -> mapRoom(rs, false), room_id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 선택한 방의 ACTIVE 멤버 목록 조회
    @Override
    public List<OttRoomMemberDTO> selectActiveMembers(Long room_id) {
        return jdbcTemplate.query(SELECT_ACTIVE_MEMBERS_SQL, (rs, rowNum) -> mapRoomMember(rs), room_id);
    }

    // 로그인 사용자의 채팅방 사용 권한 확인
    @Override
    public boolean canUseChatRoom(Long room_id, String loginId) {
        Integer count = jdbcTemplate.queryForObject(CAN_USE_CHAT_ROOM_SQL, Integer.class, room_id, loginId, loginId);
        return count != null && count > 0;
    }

    // 선택한 방에서 로그인 사용자의 멤버 상태 조회
    @Override
    public String selectRoomMemberStatus(Long room_id, String loginId) {
        try {
            return jdbcTemplate.queryForObject(SELECT_ROOM_MEMBER_STATUS_SQL, String.class, room_id, loginId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 선택한 방의 현재 ACTIVE 인원 수 조회
    @Override
    public int countActiveRoomMembers(Long room_id) {
        Integer count = jdbcTemplate.queryForObject(COUNT_ACTIVE_ROOM_MEMBERS_SQL, Integer.class, room_id);
        return count == null ? 0 : count;
    }

    // 선택한 방과 정산 월의 정산 존재 여부 확인
    @Override
    public boolean existsSettlement(Long room_id, String settlement_month) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_SETTLEMENT_SQL, Integer.class, room_id, settlement_month);
        return count != null && count > 0;
    }

    // 선택한 방과 정산 월의 정산 ID 조회
    @Override
    public Long selectSettlementId(Long room_id, String settlement_month) {
        try {
            return jdbcTemplate.queryForObject(SELECT_SETTLEMENT_ID_SQL, Long.class, room_id, settlement_month);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 결제 처리에 필요한 정산 결제 정보 조회
    @Override
    public Map<String, Object> selectPaymentMap(Long payment_id) {
        try {
            return jdbcTemplate.queryForMap(SELECT_PAYMENT_MAP_SQL, payment_id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // 회원 ID에 해당하는 화면 표시 이름 조회
    @Override
    public String selectMemberDisplayName(String loginId) {
        try {
            return jdbcTemplate.queryForObject(SELECT_MEMBER_DISPLAY_NAME_SQL, String.class, loginId);
        } catch (EmptyResultDataAccessException e) {
            return loginId;
        }
    }

    // OTT 서비스 ID에 해당하는 서비스명 조회
    @Override
    public String selectServiceName(Long ott_service_id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_SERVICE_NAME_SQL, String.class, ott_service_id);
        } catch (EmptyResultDataAccessException e) {
            return "OTT";
        }
    }

    // 사용자가 해당 방의 ACTIVE 일반 멤버인지 확인
    @Override
    public boolean isActiveNormalMember(Long room_id, String loginId) {
        Integer count = jdbcTemplate.queryForObject(IS_ACTIVE_NORMAL_MEMBER_SQL, Integer.class, room_id, loginId);
        return count != null && count > 0;
    }

    // 사용자의 미처리 나가기 예약 존재 여부 확인
    @Override
    public boolean hasReservedLeave(Long room_id, String loginId) {
        Integer count = jdbcTemplate.queryForObject(HAS_RESERVED_LEAVE_SQL, Integer.class, room_id, loginId);
        return count != null && count > 0;
    }

    // 나가기 예정일까지 결제 완료된 이용 회차 존재 여부 확인
    @Override
    public boolean hasPaidUpcomingPayment(Long room_id, String loginId, LocalDate leave_scheduled_date) {
        Integer count = jdbcTemplate.queryForObject(HAS_PAID_UPCOMING_PAYMENT_SQL, Integer.class,
                room_id, loginId, Date.valueOf(leave_scheduled_date));
        return count != null && count > 0;
    }

    // 선택한 방의 ACTIVE 멤버 ID 목록 조회
    @Override
    public List<String> selectActiveRoomMemberIds(Long room_id) {
        return jdbcTemplate.queryForList(SELECT_ACTIVE_ROOM_MEMBER_IDS_SQL, String.class, room_id);
    }

    // 조회 결과에 선택 컬럼이 존재할 때만 문자열 값 반환
    private String getOptionalString(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        java.sql.ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return rs.getString(columnName);
            }
        }
        return null;
    }

    // 문자열 날짜를 SQL Date로 변환
    private Date toSqlDate(String value) {
        return value == null || value.isBlank() ? null : Date.valueOf(value);
    }

    // null 정수 값을 0으로 변환
    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    // NULL 여부를 유지하며 숫자 컬럼을 Long으로 변환
    private Long getNullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
