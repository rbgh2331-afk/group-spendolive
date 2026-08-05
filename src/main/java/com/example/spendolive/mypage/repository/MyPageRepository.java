package com.example.spendolive.mypage.repository;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MyPageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MyPageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 마이페이지 지출 요약 전용 조회.
     * 회원/신고/OTT 조회는 각각 MemberRepository, MyPageReportRepository, OttRepository 쪽에서 처리한다.
     */
    public int selectThisMonthExpenseTotal(int member_id) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.plusMonths(1).atDay(1);

        String sql = """
                SELECT NVL(SUM(amount), 0)
                FROM expense_tb
                WHERE member_id = ?
                  AND expense_date >= ?
                  AND expense_date < ?
                """;

        try {
            Integer total = jdbcTemplate.queryForObject(sql, Integer.class, member_id, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
            return total == null ? 0 : total;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    /**
     * 관리자 강제탈퇴에서 사용하는 기존 처리.
     * 관리자 제재 정책은 이번 자진 회원탈퇴 개선 범위에서 변경하지 않는다.
     */
    public int withdrawMember(String loginId) {
        jdbcTemplate.update("DELETE FROM member_account_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM member_card_tb WHERE id = ?", loginId);

        String sql = """
                UPDATE member_tb
                SET status = 'LEAVE',
                    password = 'LEAVE_' || RAWTOHEX(SYS_GUID()),
                    account_status = 'NO',
                    card_status = 'NO',
                    updated_at = SYSDATE
                WHERE id = ?
                  AND status <> 'LEAVE'
                """;

        return jdbcTemplate.update(sql, loginId);
    }

    // [회원탈퇴 개선] 자진탈퇴 대상 회원 행을 잠가 중복 요청을 막고 기존 member_id를 조회한다.
    public int selectActiveMemberIdForUpdate(String loginId) {
        String sql = "SELECT member_id FROM member_tb WHERE id = ? AND status = 'ACTIVE' FOR UPDATE";
        try {
            Integer memberId = jdbcTemplate.queryForObject(sql, Integer.class, loginId);
            if (memberId == null) {
                throw new IllegalStateException("존재하지 않거나 이미 탈퇴한 회원입니다.");
            }
            return memberId;
        } catch (EmptyResultDataAccessException e) {
            throw new IllegalStateException("존재하지 않거나 이미 탈퇴한 회원입니다.", e);
        }
    }

    // [회원탈퇴 개선] REQUESTED 상태의 환불만 아직 처리 중인 환불로 계산한다.
    public int selectPendingRefundCount(String loginId) {
        String sql = "SELECT COUNT(*) FROM settlement_refund_tb WHERE member_login_id = ? AND refund_status = 'REQUESTED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, loginId);
        return count == null ? 0 : count;
    }

    // [회원탈퇴 개선] 개인 데이터 삭제와 보존 데이터 익명 아이디 이동을 한 트랜잭션 안에서 순서대로 수행한다.
    public void withdrawSelfMember(int memberId, String loginId, String temporaryId, String anonymousId, String anonymousEmail) {
        insertTemporaryMember(loginId, temporaryId);
        deletePrivateData(memberId, loginId);
        movePreservedMemberReferences(loginId, temporaryId);
        anonymizeMember(loginId, anonymousId, anonymousEmail);
        movePreservedMemberReferences(temporaryId, anonymousId);
        deleteTemporaryMember(temporaryId);
    }

    // [회원탈퇴 개선] 외래키가 잠시 참조할 임시 부모 회원을 기존 회원의 필수 가입정보 기준으로 생성한다.
    private void insertTemporaryMember(String loginId, String temporaryId) {
        String sql = """
                INSERT INTO member_tb (id, email, password, member_name, nickname, phone, login_type, verify_type, role, status, warning_count, created_at, updated_at, account_status, card_status)
                SELECT ?, LOWER(?) || '@spendolive.local', 'TEMP_' || RAWTOHEX(SYS_GUID()), '탈퇴 처리용', '탈퇴 처리용', NULL, login_type, verify_type, role, 'LEAVE', 0, SYSDATE, SYSDATE, 'NO', 'NO'
                FROM member_tb
                WHERE id = ?
                  AND status = 'ACTIVE'
                """;

        int insertedCount = jdbcTemplate.update(sql, temporaryId, temporaryId, loginId);
        if (insertedCount != 1) {
            throw new IllegalStateException("회원탈퇴 임시 정보 생성에 실패했습니다.");
        }
    }

    // [회원탈퇴 개선] 회원 개인 전용 데이터는 보존하지 않고 실제 행을 삭제한다.
    private void deletePrivateData(int memberId, String loginId) {
        jdbcTemplate.update("DELETE FROM expense_tb WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM monthly_budget_tb WHERE member_id = ?", memberId);
        jdbcTemplate.update("DELETE FROM member_tran_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM member_account_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM member_card_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM seller_account_tb WHERE member_id = ?", loginId);
        jdbcTemplate.update("DELETE FROM ott_chat_read_tb WHERE member_login_id = ?", loginId);
        jdbcTemplate.update("DELETE FROM notification_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM notice_read_tb WHERE id = ?", loginId);
        jdbcTemplate.update("DELETE FROM notice_favorite_tb WHERE id = ?", loginId);
    }

    // [회원탈퇴 개선] 거래·정산·채팅·문의·신고 이력은 삭제하지 않고 동일한 익명 아이디로 연결한다.
    private void movePreservedMemberReferences(String beforeId, String afterId) {
        // [회원탈퇴 개선] OTT 방, 과거 참여 내역, 채팅 발신자 이력을 유지한다.
        jdbcTemplate.update("UPDATE ott_room_tb SET host_login_id = ? WHERE host_login_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE ott_room_member_tb SET member_login_id = ? WHERE member_login_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE ott_chat_message_tb SET sender_id = ? WHERE sender_id = ?", afterId, beforeId);

        // [회원탈퇴 개선] 결제, 환불, 에스크로, 플랫폼 수익 이력을 유지한다.
        jdbcTemplate.update("UPDATE settlement_payment_tb SET id = ? WHERE id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE settlement_refund_tb SET member_login_id = ? WHERE member_login_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE escrow_payout_tb SET payer_id = ? WHERE payer_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE escrow_payout_tb SET host_id = ? WHERE host_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE platform_revenue_tb SET payer_id = ? WHERE payer_id = ?", afterId, beforeId);

        // [회원탈퇴 개선] 문의, 신고, 경고와 관리자 공지 작성 이력을 유지한다.
        jdbcTemplate.update("UPDATE inquiry_tb SET id = ? WHERE id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE report_tb SET reporter_id = ? WHERE reporter_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE report_tb SET reported_member_id = ? WHERE reported_member_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE warning_tb SET member_id = ? WHERE member_id = ?", afterId, beforeId);
        jdbcTemplate.update("UPDATE notice_tb SET admin_id = ? WHERE admin_id = ?", afterId, beforeId);
    }

    // [회원탈퇴 개선] 기존 member_id 행은 유지하고 로그인·개인 식별 정보만 익명값으로 변경한다.
    private void anonymizeMember(String loginId, String anonymousId, String anonymousEmail) {
        String sql = """
                UPDATE member_tb
                SET id = ?,
                    email = ?,
                    password = 'LEAVE_' || RAWTOHEX(SYS_GUID()),
                    member_name = '탈퇴한 회원',
                    nickname = '탈퇴한 회원',
                    phone = NULL,
                    status = 'LEAVE',
                    blocked_until = NULL,
                    last_login_at = NULL,
                    account_status = 'NO',
                    card_status = 'NO',
                    updated_at = SYSDATE
                WHERE id = ?
                  AND status = 'ACTIVE'
                """;

        int updatedCount = jdbcTemplate.update(sql, anonymousId, anonymousEmail, loginId);
        if (updatedCount != 1) {
            throw new IllegalStateException("회원 정보 익명화에 실패했습니다.");
        }
    }

    // [회원탈퇴 개선] 보존 데이터가 최종 익명 아이디로 이동한 뒤 임시 회원 행만 삭제한다.
    private void deleteTemporaryMember(String temporaryId) {
        int deletedCount = jdbcTemplate.update("DELETE FROM member_tb WHERE id = ? AND status = 'LEAVE'", temporaryId);
        if (deletedCount != 1) {
            throw new IllegalStateException("회원탈퇴 임시 정보 정리에 실패했습니다.");
        }
    }
}
