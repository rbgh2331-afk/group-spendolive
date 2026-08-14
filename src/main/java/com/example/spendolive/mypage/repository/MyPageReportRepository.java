package com.example.spendolive.mypage.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.spendolive.mypage.domain.MyPageReportDTO;

/**
 * 마이페이지 신고 내역 조회 저장소
 */
@Repository
public class MyPageReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public MyPageReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int selectMyReportCount(String loginId) {
        String sql = """
                SELECT COUNT(*)
                FROM report_tb
                WHERE reporter_id = ?
                """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, loginId);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    public int selectwarning_count(String loginId) {
        String sql = """
                SELECT COUNT(*)
                FROM warning_tb
                WHERE member_id = ?
                """;

        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, loginId);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    public List<MyPageReportDTO> selectMyReportList(String loginId) {
        String sql = """
                SELECT r.report_id,
                       r.reported_member_id,
                       NVL(m.member_name, r.reported_member_id) AS reported_member_name,
                       NVL(m.nickname, r.reported_member_id) AS reported_member_nickname,
                       r.report_reason,
                       r.report_status,
                       r.admin_comment,
                       TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at,
                       TO_CHAR(r.processed_at, 'YYYY-MM-DD') AS processed_at,
                       CASE
                           WHEN m.status IN ('BLOCK', 'PERM_BLOCK') THEN 'Y'
                           ELSE 'N'
                       END AS blocked_yn
                FROM report_tb r
                LEFT JOIN member_tb m ON r.reported_member_id = m.id
                WHERE r.reporter_id = ?
                ORDER BY r.created_at DESC, r.report_id DESC
                """;

        try {
            return jdbcTemplate.query(sql, reportRowMapper(), loginId);
        } catch (DataAccessException e) {
            return Collections.emptyList();
        }
    }

    private RowMapper<MyPageReportDTO> reportRowMapper() {
        return new RowMapper<>() {
            @Override
            public MyPageReportDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                MyPageReportDTO report = new MyPageReportDTO();
                report.setReport_id(rs.getLong("report_id"));
                report.setReported_member_id(rs.getString("reported_member_id"));
                report.setReported_member_name(rs.getString("reported_member_name"));
                report.setReported_member_nickname(rs.getString("reported_member_nickname"));
                report.setReport_reason(rs.getString("report_reason"));
                report.setReport_status(rs.getString("report_status"));
                report.setAdmin_comment(rs.getString("admin_comment"));
                report.setCreated_at(rs.getString("created_at"));
                report.setProcessed_at(rs.getString("processed_at"));
                report.setBlocked_yn(rs.getString("blocked_yn"));

                return report;
            }
        };
    }
}
