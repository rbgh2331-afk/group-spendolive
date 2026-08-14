package com.example.spendolive.report.repository;

import java.util.List;

import com.example.spendolive.report.domain.ReportVO;
import com.example.spendolive.report.domain.WarningVO;

/**
 * 신고 데이터 조회 및 저장 저장소
 */
public interface ReportRepository {
    public void insertReport(ReportVO reportInfo) throws Exception;
    public List<ReportVO> selectReport(String status) throws Exception;
    public void updateComment(String comment, int report_id) throws Exception;
    public int completeReportIfWaiting(String comment, int report_id) throws Exception;
    public void insertWarning(WarningVO warning) throws Exception;
    public List<ReportVO> selectReportAll()throws Exception;

}
