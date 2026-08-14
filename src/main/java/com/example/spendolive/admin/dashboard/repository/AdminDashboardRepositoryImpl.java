package com.example.spendolive.admin.dashboard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.admin.dashboard.domain.AdminDashboardDTO;

@Repository
public class AdminDashboardRepositoryImpl implements AdminDashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 관리자 대시보드에 필요한 다섯 개 통계를 한 번의 DB 조회로 가져온다.
     * - 전체 회원 수는 회원 상태와 권한에 관계없이 member_tb 전체를 계산
     * - 공개 모집 중 파티는 외부 모집방 중 현재 모집 가능한 상태만 계산
     * - 신고와 문의는 WAIT 상태만 처리 대기로 계산
     */
    private static final String SELECT_DASHBOARD_SUMMARY = """
            SELECT (SELECT COUNT(*)
                      FROM member_tb
                     WHERE NVL(role, 'USER') <> 'ADMIN') AS total_member_count,
                   (SELECT COUNT(*)
                      FROM ott_room_tb
                     WHERE room_mode = 'RECRUIT'
                       AND status IN ('RECRUITING', 'REPLACE_RECRUITING')) AS recruiting_party_count,
                   (SELECT COUNT(*)
                      FROM ott_service_tb) AS ott_service_count,
                   (SELECT COUNT(*)
                      FROM report_tb
                     WHERE report_status = 'WAIT') AS pending_report_count,
                   (SELECT COUNT(*)
                      FROM inquiry_tb
                     WHERE status = 'WAIT') AS pending_inquiry_count
              FROM dual
            """;

    public AdminDashboardRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminDashboardDTO selectDashboardSummary() {
        return jdbcTemplate.queryForObject(SELECT_DASHBOARD_SUMMARY, (rs, rowNum) -> {
            AdminDashboardDTO dashboard = new AdminDashboardDTO();
            dashboard.setTotalMemberCount(rs.getInt("total_member_count"));
            dashboard.setRecruitingPartyCount(rs.getInt("recruiting_party_count"));
            dashboard.setOttServiceCount(rs.getInt("ott_service_count"));
            dashboard.setPendingReportCount(rs.getInt("pending_report_count"));
            dashboard.setPendingInquiryCount(rs.getInt("pending_inquiry_count"));
            return dashboard;
        });
    }
}
