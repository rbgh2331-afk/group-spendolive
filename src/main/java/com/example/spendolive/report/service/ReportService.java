package com.example.spendolive.report.service;

import java.util.List;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.report.domain.ReportVO;

/**
 * 신고 처리 서비스
 */
public interface ReportService {
    public void insertReport( String reported_member_id, String room_id, String chat_text, MemberVO memberInfo)throws Exception;
    public List<ReportVO> selectReport(String status)throws Exception;
    public List<ReportVO> selectReportAll()throws Exception;
    public void updateComment(String comment, int report_id) throws Exception;
    public void insertWarning(String comment, String userId, int reportId, String result) throws Exception;

}
