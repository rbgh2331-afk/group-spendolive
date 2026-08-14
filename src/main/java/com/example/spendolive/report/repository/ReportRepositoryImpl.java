package com.example.spendolive.report.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.report.domain.ReportVO;
import com.example.spendolive.report.domain.WarningVO;

@Repository
public class ReportRepositoryImpl implements ReportRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReportRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final String insertReport = "INSERT INTO report_tb (room_id, reporter_id, reported_member_id, report_reason, report_status)"
                                        +" VALUES (?,?,?,?, 'WAIT') ";
    private final String selectReportAll = "SELECT REPORT_ID,REPORTER_ID,REPORTED_member_id,ROOM_ID,REPORT_REASON,REPORT_STATUS,ADMIN_COMMENT,created_at,processed_at "
                                        +"from report_tb ";
    private final String selectReport = "SELECT REPORT_ID,REPORTER_ID,REPORTED_member_id,ROOM_ID,REPORT_REASON,REPORT_STATUS,ADMIN_COMMENT,created_at,processed_at "
                                        +"from report_tb where report_status=? ";

    private final String updateComment = "UPDATE report_tb SET admin_comment =? , processed_at =SYSDATE , report_status =? WHERE report_id=? ";
    private final String completeReportIfWaiting = "UPDATE report_tb SET admin_comment =?, processed_at =SYSDATE, report_status ='COMPLETE' "
                                                 + "WHERE report_id =? AND report_status ='WAIT' ";
    private final String insertWarning = "INSERT INTO warning_tb (member_id, report_id, warning_reason, penalty_days, status, created_at)"
                                        +" VALUES (?,?,?,?,?,SYSDATE) ";

    @Override
    public void insertReport(ReportVO reportInfo) {
        jdbcTemplate.update(insertReport, reportInfo.getRoom_id(), reportInfo.getReporter_id() , reportInfo.getReported_member_id(), reportInfo.getReport_reason());
    }
    @Override
    public List<ReportVO> selectReportAll() {
        try {
            return jdbcTemplate.query(selectReportAll, (rs, rowNum) -> {
            ReportVO report = new ReportVO();
            report.setAdmin_comment(rs.getString("admin_comment"));
            report.setCreated_at(rs.getObject("created_at", LocalDateTime.class));
            report.setProcessed_at(rs.getObject("processed_at", LocalDateTime.class));
            report.setReport_id(rs.getLong("report_id"));
            report.setReport_reason(rs.getString("report_reason"));
            report.setReport_status(rs.getString("report_status"));
            report.setReported_member_id(rs.getString("reported_member_id"));
            report.setReporter_id(rs.getString("reporter_id"));
            report.setRoom_id(rs.getInt("room_id"));

            return report;
            });
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 조회 결과가 없으면 null 반환
            return null;
        }
    }
    @Override
    public List<ReportVO> selectReport(String status) {
        try {
            return jdbcTemplate.query(selectReport, (rs, rowNum) -> {
            ReportVO report = new ReportVO();
            report.setAdmin_comment(rs.getString("admin_comment"));
            report.setCreated_at(rs.getObject("created_at", LocalDateTime.class));
            report.setProcessed_at(rs.getObject("processed_at", LocalDateTime.class));
            report.setReport_id(rs.getLong("report_id"));
            report.setReport_reason(rs.getString("report_reason"));
            report.setReport_status(rs.getString("report_status"));
            report.setReported_member_id(rs.getString("reported_member_id"));
            report.setReporter_id(rs.getString("reporter_id"));
            report.setRoom_id(rs.getInt("room_id"));

            return report;
            }, status);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 조회 결과가 없으면 null 반환
            return null;
        }
    }
    @Override
    public void updateComment(String comment, int report_id) {
        jdbcTemplate.update(updateComment, comment, "COMPLETE", report_id);
    }
    @Override
    public int completeReportIfWaiting(String comment, int report_id) {
        return jdbcTemplate.update(completeReportIfWaiting, comment, report_id);
    }
    @Override
    public void insertWarning(WarningVO warning) {
        jdbcTemplate.update(insertWarning, warning.getMember_id(), warning.getReport_id(), warning.getWarning_reason(),
                warning.getPenalty_days(), warning.getStatus());
    }

}
