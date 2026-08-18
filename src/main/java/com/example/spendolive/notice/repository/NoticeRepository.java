package com.example.spendolive.notice.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.spendolive.notice.domain.NoticeDTO;

@Repository
public class NoticeRepository {
    private static final Logger log = LoggerFactory.getLogger(NoticeRepository.class);

    // ────────────────────────────────────────────────────────────
    // SQL 정의
    // ────────────────────────────────────────────────────────────

    // 전체 목록: 로그인 회원의 읽음(read_yn)/찜(star_yn) 여부를 LEFT JOIN으로 계산
    private static final String FIND_ALL_SQL = """
            SELECT
                n.notice_id, n.admin_id, n.title, n.content, n.pinned_yn,
                CASE WHEN nr.notice_id IS NULL THEN 'N' ELSE 'Y' END AS read_yn,
                CASE WHEN nf.notice_id IS NULL THEN 'N' ELSE 'Y' END AS star_yn,
                TO_CHAR(n.created_at, 'YYYY.MM.DD') AS created_at,
                TO_CHAR(n.updated_at, 'YYYY.MM.DD') AS updated_at
            FROM notice_tb n
            LEFT JOIN notice_read_tb nr ON n.notice_id = nr.notice_id AND nr.id = ?
            LEFT JOIN notice_favorite_tb nf ON n.notice_id = nf.notice_id AND nf.id = ?
            ORDER BY n.pinned_yn DESC, star_yn DESC, n.notice_id DESC
        """;

    // 관리자 공지 목록 전용: 위 FIND_ALL_SQL과 같은 정렬 기준에 OFFSET/LIMIT만 추가
    // (관리자 화면은 read_yn/star_yn을 안 쓰지만, mapRowWithReadStar를 그대로 재사용하려고
    //  같은 컬럼 구조를 유지함 — 파라미터로 넘기는 id는 항상 빈 문자열이라 항상 read_yn/star_yn='N')
    private static final String FIND_ALL_PAGED_SQL = FIND_ALL_SQL + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

    // 단건 조회
    private static final String FIND_BY_ID_SQL = """
            SELECT
                notice_id, admin_id, title, content, pinned_yn,
                TO_CHAR(created_at, 'YYYY.MM.DD') AS created_at,
                TO_CHAR(updated_at, 'YYYY.MM.DD') AS updated_at
            FROM notice_tb
            WHERE notice_id = ?
        """;

    // 사용자 상세 페이지 전용: 위 FIND_BY_ID_SQL과 같은 단건 조회에
    // 로그인 회원 기준 읽음(read_yn)/찜(star_yn) 여부까지 같이 계산해서 붙여줌
    // (관리자 수정 화면은 read_yn/star_yn이 필요 없어서 기존 FIND_BY_ID_SQL/findById를 그대로 씀)
    private static final String FIND_BY_ID_WITH_STAR_SQL = """
            SELECT
                n.notice_id, n.admin_id, n.title, n.content, n.pinned_yn,
                CASE WHEN nr.notice_id IS NULL THEN 'N' ELSE 'Y' END AS read_yn,
                CASE WHEN nf.notice_id IS NULL THEN 'N' ELSE 'Y' END AS star_yn,
                TO_CHAR(n.created_at, 'YYYY.MM.DD') AS created_at,
                TO_CHAR(n.updated_at, 'YYYY.MM.DD') AS updated_at
            FROM notice_tb n
            LEFT JOIN notice_read_tb nr ON n.notice_id = nr.notice_id AND nr.id = ?
            LEFT JOIN notice_favorite_tb nf ON n.notice_id = nf.notice_id AND nf.id = ?
            WHERE n.notice_id = ?
        """;

    private static final String COUNT_ALL_SQL = "SELECT COUNT(*) FROM notice_tb";
    private static final String COUNT_PINNED_SQL = "SELECT COUNT(*) FROM notice_tb WHERE pinned_yn = 'Y'";

    // 중요 공지 목록 (고정 공지만)
    private static final String FIND_IMPORTANT_LIST_SQL = """
            SELECT
                n.notice_id, n.admin_id, n.title, n.content, n.pinned_yn,
                CASE WHEN nr.notice_id IS NULL THEN 'N' ELSE 'Y' END AS read_yn,
                CASE WHEN nf.notice_id IS NULL THEN 'N' ELSE 'Y' END AS star_yn,
                TO_CHAR(n.created_at, 'YYYY.MM.DD') AS created_at,
                TO_CHAR(n.updated_at, 'YYYY.MM.DD') AS updated_at
            FROM notice_tb n
            LEFT JOIN notice_read_tb nr ON n.notice_id = nr.notice_id AND nr.id = ?
            LEFT JOIN notice_favorite_tb nf ON n.notice_id = nf.notice_id AND nf.id = ?
            WHERE n.pinned_yn = 'Y'
            ORDER BY n.notice_id DESC
        """;

    // 읽음 처리 (이미 읽었으면 무시)
    private static final String INSERT_NOTICE_READ_SQL = """
            MERGE INTO notice_read_tb nr
            USING dual
            ON (nr.notice_id = ? AND nr.id = ?)
            WHEN NOT MATCHED THEN
                INSERT (notice_id, id) VALUES (?, ?)
        """;

    // 안 읽은 목록
    private static final String FIND_UNREAD_SQL = """
            SELECT
                n.notice_id, n.admin_id, n.title, n.content, n.pinned_yn,
                TO_CHAR(n.created_at, 'YYYY.MM.DD') AS created_at,
                TO_CHAR(n.updated_at, 'YYYY.MM.DD') AS updated_at
            FROM notice_tb n
            WHERE NOT EXISTS (
                SELECT 1 FROM notice_read_tb nr
                WHERE nr.notice_id = n.notice_id AND nr.id = ?
            )
            ORDER BY n.pinned_yn DESC, n.created_at DESC
        """;

    // 찜 토글 (Oracle은 MERGE WHEN MATCHED THEN DELETE 단독 불가 → 존재 확인 후 DELETE/INSERT)
    private static final String CHECK_STAR_SQL = "SELECT COUNT(*) FROM notice_favorite_tb WHERE notice_id = ? AND id = ?";
    private static final String DELETE_STAR_SQL = "DELETE FROM notice_favorite_tb WHERE notice_id = ? AND id = ?";
    private static final String INSERT_STAR_SQL = "INSERT INTO notice_favorite_tb (notice_id, id) VALUES (?, ?)";

    private static final String FIND_ALL_MEMBER_IDS_SQL = "SELECT id FROM member_tb WHERE status = 'ACTIVE'";

    private static final String INSERT_NOTICE_ALERT_SQL = """
            INSERT INTO notification_tb
                (id, notification_type, title, message, link_url, read_yn, star_yn, created_at)
            VALUES (?, 'HOME', ?, ?, ?, 'N', 'N', SYSDATE)
        """;

    private static final String INSERT_NOTICE_SQL =
            "INSERT INTO notice_tb (admin_id, title, content, pinned_yn, created_at) VALUES (?, ?, ?, ?, SYSDATE)";

    private static final String UPDATE_NOTICE_SQL = """
            UPDATE notice_tb
            SET title = ?, content = ?, pinned_yn = ?, updated_at = SYSDATE
            WHERE notice_id = ?
        """;

    private static final String DELETE_FAVORITE_SQL = "DELETE FROM notice_favorite_tb WHERE notice_id = ?";
    private static final String DELETE_READ_SQL = "DELETE FROM notice_read_tb WHERE notice_id = ?";
    private static final String DELETE_NOTICE_SQL = "DELETE FROM notice_tb WHERE notice_id = ?";

    // ────────────────────────────────────────────────────────────
    // 필드 / 생성자 / RowMapper
    // ────────────────────────────────────────────────────────────

    private final JdbcTemplate jdbcTemplate;

    public NoticeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* ─── 공통 RowMapper ──────────────────────────────────── */
    private NoticeDTO mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        NoticeDTO notice = new NoticeDTO();
        notice.setNotice_id(rs.getInt("notice_id"));
        notice.setAdmin_id(rs.getString("admin_id"));
        notice.setTitle(rs.getString("title"));
        notice.setContent(rs.getString("content"));
        notice.setPinned_yn(rs.getString("pinned_yn"));
        notice.setCreated_at(rs.getString("created_at"));
        notice.setUpdated_at(rs.getString("updated_at"));
        return notice;
    }

    private NoticeDTO mapRowWithReadStar(java.sql.ResultSet rs) throws java.sql.SQLException {
        NoticeDTO notice = mapRow(rs);
        notice.setRead_yn(rs.getString("read_yn"));
        notice.setStar_yn(rs.getString("star_yn"));
        return notice;
    }

    // ────────────────────────────────────────────────────────────
    // 조회 / 등록 / 수정 / 삭제 메서드
    // ────────────────────────────────────────────────────────────

    /* ─── 전체 목록 ───────────────────────────────────────── */
    public List<NoticeDTO> findAll(String id) {
        try {
            String safeId = (id != null) ? id : "";
            return jdbcTemplate.query(FIND_ALL_SQL, (rs, rowNum) -> mapRowWithReadStar(rs), safeId, safeId);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findAll] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 관리자 목록 전용: 페이지네이션(OFFSET/LIMIT) 조회 ──── */
    public List<NoticeDTO> findAllPaged(int offset, int limit) {
        try {
            return jdbcTemplate.query(FIND_ALL_PAGED_SQL, (rs, rowNum) -> mapRowWithReadStar(rs), "", "", offset, limit);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findAllPaged] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 단건 조회 ───────────────────────────────────────── */
    public NoticeDTO findById(int noticeId) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_SQL, (rs, rowNum) -> mapRow(rs), noticeId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("{}", "[NoticeRepository.findById] notice_id=" + noticeId + " 존재하지 않음");
            return null;
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findById] DB 오류: " + e.getMessage(), e);
            return null;
        }
    }

    /* ─── 단건 조회(사용자 상세 페이지 전용, 찜 상태 포함) ────── */
    public NoticeDTO findByIdWithStar(int noticeId, String id) {
        try {
            String safeId = (id != null) ? id : "";
            return jdbcTemplate.queryForObject(FIND_BY_ID_WITH_STAR_SQL, (rs, rowNum) -> mapRowWithReadStar(rs), safeId, safeId, noticeId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("{}", "[NoticeRepository.findByIdWithStar] notice_id=" + noticeId + " 존재하지 않음");
            return null;
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findByIdWithStar] DB 오류: " + e.getMessage(), e);
            return null;
        }
    }

    /* ─── 카운트 ──────────────────────────────────────────── */
    public int countAll() {
        try {
            Integer count = jdbcTemplate.queryForObject(COUNT_ALL_SQL, Integer.class);
            return (count != null) ? count : 0;
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.countAll] DB 오류: " + e.getMessage(), e);
            return 0;
        }
    }

    public int countPinned() {
        try {
            Integer count = jdbcTemplate.queryForObject(COUNT_PINNED_SQL, Integer.class);
            return (count != null) ? count : 0;
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.countPinned] DB 오류: " + e.getMessage(), e);
            return 0;
        }
    }

    /* ─── 중요 공지 목록 ──────────────────────────────────── */
    public List<NoticeDTO> findImportantList(String id) {
        try {
            String safeId = (id != null) ? id : "";
            return jdbcTemplate.query(FIND_IMPORTANT_LIST_SQL, (rs, rowNum) -> mapRowWithReadStar(rs), safeId, safeId);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findImportantList] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 읽음 처리 ───────────────────────────────────────── */
    public void insertNoticeRead(int notice_id, String id) {
        if (id == null || id.isBlank()) return;
        try {
            jdbcTemplate.update(INSERT_NOTICE_READ_SQL, notice_id, id, notice_id, id);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.insertNoticeRead] DB 오류: " + e.getMessage(), e);
        }
    }

    /* ─── 안 읽은 목록 ────────────────────────────────────── */
    public List<NoticeDTO> findUnreadBymember_id(String id) {
        if (id == null || id.isBlank()) return Collections.emptyList();
        try {
            return jdbcTemplate.query(FIND_UNREAD_SQL, (rs, rowNum) -> {
                NoticeDTO notice = mapRow(rs);
                notice.setRead_yn("N"); // 안 읽은 목록 전용 조회라 무조건 N으로 채움
                return notice;
            }, id);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findUnreadBymember_id] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 찜 토글 ─────────────────────────────────────────── */
    public void toggleNoticeStar(int notice_id, String id) {
        if (id == null || id.isBlank()) return;
        try {
            Integer count = jdbcTemplate.queryForObject(CHECK_STAR_SQL, Integer.class, notice_id, id);
            if (count != null && count > 0) {
                jdbcTemplate.update(DELETE_STAR_SQL, notice_id, id);
            } else {
                jdbcTemplate.update(INSERT_STAR_SQL, notice_id, id);
            }
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.toggleNoticeStar] DB 오류: " + e.getMessage(), e);
        }
    }

    /* ─── 전체 회원 ID 조회 (알림 발송용) ─────────────────── */
    public List<String> findAllmember_ids() {
        try {
            return jdbcTemplate.queryForList(FIND_ALL_MEMBER_IDS_SQL, String.class);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.findAllmember_ids] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 공지 알림 전체 회원 발송 (batchUpdate로 한 번에 전송) ─── */
    public void insertNoticeAlertForAll(String title, String noticeId) {
        List<String> memberIds = findAllmember_ids();
        if (memberIds.isEmpty()) return;

        String message = "새 공지사항이 등록되었습니다.";
        String linkUrl = "/spendolive/notice/detail.do?notice_id=" + noticeId;

        try {
            jdbcTemplate.batchUpdate(INSERT_NOTICE_ALERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    ps.setString(1, memberIds.get(i));
                    ps.setString(2, title);
                    ps.setString(3, message);
                    ps.setString(4, linkUrl);
                }

                @Override
                public int getBatchSize() {
                    return memberIds.size();
                }
            });
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.insertNoticeAlertForAll] 배치 발송 실패: " + e.getMessage(), e);
        }
    }

    /* ─── 관리자: 공지 등록 (notice_id 반환) ──────────────── */
    public int insertNotice(NoticeDTO notice) {
        if (notice == null
                || notice.getTitle() == null || notice.getTitle().isBlank()
                || notice.getContent() == null || notice.getContent().isBlank()
                || notice.getAdmin_id() == null || notice.getAdmin_id().isBlank()) {
            throw new IllegalArgumentException("공지 등록: 필수 항목이 비어 있습니다.");
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbcTemplate.update(conn -> {
                java.sql.PreparedStatement ps = conn.prepareStatement(INSERT_NOTICE_SQL, new String[]{"notice_id"});
                ps.setString(1, notice.getAdmin_id());
                ps.setString(2, notice.getTitle());
                ps.setString(3, notice.getContent());
                ps.setString(4, notice.getPinned_yn() != null ? notice.getPinned_yn() : "N");
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            return (key != null) ? key.intValue() : -1;
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.insertNotice] DB 오류: " + e.getMessage(), e);
            throw e;
        }
    }

    public void updateNotice(NoticeDTO notice) {
        if (notice == null || notice.getNotice_id() <= 0)
            throw new IllegalArgumentException("공지 수정: 유효하지 않은 notice_id.");
        if (notice.getTitle() == null || notice.getTitle().isBlank()
                || notice.getContent() == null || notice.getContent().isBlank())
            throw new IllegalArgumentException("공지 수정: 제목/내용은 필수입니다.");

        try {
            int rows = jdbcTemplate.update(UPDATE_NOTICE_SQL,
                notice.getTitle(), notice.getContent(),
                notice.getPinned_yn() != null ? notice.getPinned_yn() : "N",
                notice.getNotice_id());
            if (rows == 0)
                log.warn("{}", "[NoticeRepository.updateNotice] 대상 없음: " + notice.getNotice_id());
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.updateNotice] DB 오류: " + e.getMessage(), e);
            throw e;
        }
    }

    /* ─── 관리자: 공지 삭제 ────────────────────────────────── */
    public void deleteNotice(int notice_id) {
        if (notice_id <= 0)
            throw new IllegalArgumentException("공지 삭제: 유효하지 않은 notice_id.");
        try {
            jdbcTemplate.update(DELETE_FAVORITE_SQL, notice_id);
            jdbcTemplate.update(DELETE_READ_SQL, notice_id);
            int rows = jdbcTemplate.update(DELETE_NOTICE_SQL, notice_id);
            if (rows == 0)
                log.warn("{}", "[NoticeRepository.deleteNotice] 대상 없음: " + notice_id);
        } catch (DataAccessException e) {
            log.error("{}", "[NoticeRepository.deleteNotice] DB 오류: " + e.getMessage(), e);
            throw e;
        }
    }
}
