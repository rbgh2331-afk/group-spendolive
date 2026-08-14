package com.example.spendolive.notification.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.notification.domain.NotificationDTO;

@Repository
public class NotificationRepository {
    private static final Logger log = LoggerFactory.getLogger(NotificationRepository.class);

    // ────────────────────────────────────────────────────────────
    // SQL 정의
    // ────────────────────────────────────────────────────────────

    // 목록 조회 (찜 우선 → 안읽은거 우선 → 최신순)
    private static final String FIND_BY_ID_SQL = """
            SELECT
                notification_id, id, notification_type,
                title, message, link_url,
                read_yn, star_yn,
                TO_CHAR(created_at, 'YYYY.MM.DD') AS created_at
            FROM notification_tb
            WHERE id = ?
            ORDER BY star_yn DESC, read_yn ASC, created_at DESC, notification_id DESC
        """;

    // 벨 드롭다운 전용: 안읽은 알림만 (읽으면 목록에서 사라져야 하므로)
    private static final String FIND_UNREAD_BY_ID_SQL = """
            SELECT
                notification_id, id, notification_type,
                title, message, link_url,
                read_yn, star_yn,
                TO_CHAR(created_at, 'YYYY.MM.DD') AS created_at
            FROM notification_tb
            WHERE id = ? AND read_yn = 'N'
            ORDER BY star_yn DESC, created_at DESC, notification_id DESC
        """;

    private static final String COUNT_UNREAD_SQL = """
            SELECT COUNT(*)
            FROM notification_tb
            WHERE id = ? AND read_yn = 'N'
        """;

    private static final String UPDATE_READ_YN_SQL = """
            UPDATE notification_tb
            SET read_yn = 'Y'
            WHERE notification_id = ? AND id = ?
        """;

    // 목적지 페이지(link_url)에 직접 들어왔을 때 자동 읽음 처리용
    // (예: INQUIRY_REPLY 알림은 link_url="/spendolive/inquiry/list.do" 이고,
    //  사용자가 벨 알림 클릭이 아니라 메뉴 등으로 이 페이지에 바로 들어와도 읽음 처리되어야 함)
    private static final String UPDATE_READ_BY_LINK_URL_SQL = """
            UPDATE notification_tb
            SET read_yn = 'Y'
            WHERE id = ? AND link_url = ? AND read_yn = 'N'
        """;

    // 단건 조회
    private static final String FIND_BY_NOTIFICATION_ID_SQL = """
            SELECT notification_id, id, notification_type,
                   title, message, link_url, read_yn, star_yn,
                   TO_CHAR(created_at, 'YYYY.MM.DD') AS created_at
            FROM notification_tb
            WHERE notification_id = ? AND id = ?
        """;

    // 찜 토글
    private static final String TOGGLE_STAR_SQL = """
            UPDATE notification_tb
            SET star_yn = CASE WHEN star_yn = 'Y' THEN 'N' ELSE 'Y' END
            WHERE notification_id = ? AND id = ?
        """;

    // 알림 1건 생성 (채팅/결제/정산/문의답변 등 모든 이벤트가 공용으로 쓰는 발송 창구)
    private static final String INSERT_NOTIFICATION_SQL = """
            INSERT INTO notification_tb
                (id, notification_type, title, message, link_url, read_yn, star_yn, created_at)
            VALUES (?, ?, ?, ?, ?, 'N', 'N', SYSDATE)
        """;

    // ────────────────────────────────────────────────────────────
    // 필드 / 생성자 / RowMapper
    // ────────────────────────────────────────────────────────────

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ─── 공통 RowMapper. findById/findByNotificationId 공용 ─── */
    private NotificationDTO mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        NotificationDTO dto = new NotificationDTO();
        dto.setNotification_id(rs.getInt("notification_id"));
        dto.setId(rs.getString("id"));
        dto.setNotification_type(rs.getString("notification_type"));
        dto.setTitle(rs.getString("title"));
        dto.setMessage(rs.getString("message"));
        dto.setLink_url(rs.getString("link_url"));
        dto.setRead_yn(rs.getString("read_yn"));
        dto.setStar_yn(rs.getString("star_yn"));
        dto.setCreated_at(rs.getString("created_at"));
        return dto;
    }

    // ────────────────────────────────────────────────────────────
    // 조회 / 등록 / 수정 메서드
    // ────────────────────────────────────────────────────────────

    /* ─── 목록 조회 ───────────────────────────────────────── */
    public List<NotificationDTO> findById(String id) {
        if (id == null || id.isBlank()) return Collections.emptyList();
        try {
            return jdbcTemplate.query(FIND_BY_ID_SQL, (rs, rowNum) -> mapRow(rs), id);
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.findById] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 목록 조회 (안읽은 것만, 벨 드롭다운 전용) ───────── */
    public List<NotificationDTO> findUnreadById(String id) {
        if (id == null || id.isBlank()) return Collections.emptyList();
        try {
            return jdbcTemplate.query(FIND_UNREAD_BY_ID_SQL, (rs, rowNum) -> mapRow(rs), id);
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.findUnreadById] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 안 읽은 개수 ────────────────────────────────────── */
    public int countUnread(String id) {
        if (id == null || id.isBlank()) return 0;
        try {
            Integer count = jdbcTemplate.queryForObject(COUNT_UNREAD_SQL, Integer.class, id);
            return (count != null) ? count : 0;
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.countUnread] DB 오류: " + e.getMessage(), e);
            return 0;
        }
    }

    /* ─── 읽음 처리 ───────────────────────────────────────── */
    public void updateReadYn(int notificationId, String id) {
        if (notificationId <= 0 || id == null || id.isBlank()) return;
        try {
            int rows = jdbcTemplate.update(UPDATE_READ_YN_SQL, notificationId, id);
            if (rows == 0) {
                log.warn("{}", "[NotificationRepository.updateReadYn] 대상 없음: notification_id=" + notificationId);
            }
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.updateReadYn] DB 오류: " + e.getMessage(), e);
        }
    }

    /* ─── 목적지 페이지(link_url) 진입 시 자동 읽음 처리 ─────
       해당 회원의 해당 link_url을 향한 안읽은 알림을 전부 읽음 처리.
       (알림이 여러 개 쌓여 있어도 그 페이지에 들어왔다는 건 다 확인한 것으로 간주) ─── */
    public void updateReadByLinkUrl(String id, String linkUrl) {
        if (id == null || id.isBlank() || linkUrl == null || linkUrl.isBlank()) return;
        try {
            jdbcTemplate.update(UPDATE_READ_BY_LINK_URL_SQL, id, linkUrl);
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.updateReadByLinkUrl] DB 오류: " + e.getMessage(), e);
        }
    }

    /* ─── 단건 조회 ──────────────────────────────────────── */
    public NotificationDTO findByNotificationId(int notificationId, String id) {
        if (notificationId <= 0 || id == null || id.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(FIND_BY_NOTIFICATION_ID_SQL, (rs, rowNum) -> mapRow(rs), notificationId, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.findByNotificationId] 오류: " + e.getMessage(), e);
            return null;
        }
    }

    /* ─── 찜 토글 ─────────────────────────────────────────── */
    public void toggleStar(int notificationId, String id) {
        if (notificationId <= 0 || id == null || id.isBlank()) return;
        try {
            int rows = jdbcTemplate.update(TOGGLE_STAR_SQL, notificationId, id);
            if (rows == 0) {
                log.warn("{}", "[NotificationRepository.toggleStar] 대상 없음: notification_id=" + notificationId);
            }
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.toggleStar] DB 오류: " + e.getMessage(), e);
        }
    }


    /* ─── 알림 1건 생성 (모든 기능이 공용으로 쓰는 발송 창구) ─── */
    public void insertNotification(String id, String type, String title, String message, String linkUrl) {
        if (id == null || id.isBlank() || type == null || type.isBlank()
                || title == null || title.isBlank() || message == null || message.isBlank()) {
            log.warn("{}", "[NotificationRepository.insertNotification] 필수 값 누락으로 생성 건너뜀 (id=" + id + ", type=" + type + ")");
            return;
        }
        try {
            jdbcTemplate.update(INSERT_NOTIFICATION_SQL, id, type, title, message, linkUrl);
        } catch (DataAccessException e) {
            log.error("{}", "[NotificationRepository.insertNotification] DB 오류: " + e.getMessage(), e);
        }
    }
}