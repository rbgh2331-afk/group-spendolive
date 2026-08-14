package com.example.spendolive.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.repository.MemberRepository;
import com.example.spendolive.report.domain.ReportVO;
import com.example.spendolive.report.domain.WarningVO;
import com.example.spendolive.report.exception.ReportProcessException;
import com.example.spendolive.report.repository.ReportRepository;
import java.util.List;
@Service
public class ReportServiceImpl implements ReportService{
    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;

    public ReportServiceImpl(ReportRepository reportRepository, MemberRepository memberRepository) {
        this.reportRepository = reportRepository;
        this.memberRepository = memberRepository;
    }
    @Override
    @Transactional
    public void insertReport( String reported_member_id, String room_id, String chat_text, MemberVO memberInfo) throws Exception{
        ReportVO reportInfo =new ReportVO();
        String reporter = memberInfo.getId();
        int parsed_room_id = Integer.parseInt(room_id);
        try{
        reportInfo.setReport_reason(chat_text);
        reportInfo.setReported_member_id(reported_member_id);
        reportInfo.setReporter_id(reporter);
        reportInfo.setRoom_id(parsed_room_id);
        reportRepository.insertReport(reportInfo);
        }catch (Exception e) {
            throw new ReportProcessException("REPORT_FAILED", "이미 신고가 완료된 건 입니다.");
        }
    }
    @Override
    @Transactional
    public List<ReportVO> selectReport(String status) throws Exception{
        return reportRepository.selectReport(status);
    }
    @Override
    @Transactional
    public List<ReportVO> selectReportAll() throws Exception{
        return reportRepository.selectReportAll();
    }
    @Override
    @Transactional
    public void updateComment(String comment, int report_id) throws Exception{
        reportRepository.updateComment(comment, report_id);
    }
    @Override
    @Transactional
    public void insertWarning(String comment, String userId, int reportId, String result) throws Exception{
        MemberVO user = memberRepository.selectMemberById(userId);
        if (result.equals("1")) {
            // 경고 횟수는 실제 누적값을 유지하고, 패널티 기간만 1회 7일 / 2회 14일 / 3회 이상 30일로 고정한다
            int nextWarningCount = user.getWarning_count() + 1;
            int penaltyDays = resolvePenaltyDays(nextWarningCount);

            WarningVO wVo = new WarningVO();
            wVo.setMember_id(userId);
            wVo.setWarning_reason(comment);
            wVo.setPenalty_days(penaltyDays);
            wVo.setStatus("Y");
            wVo.setReport_id(reportId);
            // WAIT 상태인 신고만 최초 1회 처리한다. 동시에 같은 신고가 들어와도 한 요청만 성공한다
            int completedRows = reportRepository.completeReportIfWaiting(comment, reportId);
            if (completedRows == 0) {
                throw new ReportProcessException("REPORT_ALREADY_PROCESSED", "이미 처리된 신고입니다.");
            }

            try{
                reportRepository.insertWarning(wVo);
                memberRepository.applyWarningPenalty(userId, penaltyDays);
            }catch (Exception e) {
                throw new ReportProcessException("REPORT_FAILED", "경고 처리 중 문제가 생겼습니다. 다시 시도 해주세요", e);
            }
        }else if (result.equals("2")) {
            // 퇴출 처리는 정책 논의 후 별도로 구현한다
        }
    }

    private int resolvePenaltyDays(int warningCount) {
        if (warningCount == 1) {
            return 7;
        }
        if (warningCount == 2) {
            return 14;
        }
        return 30;
    }
}
