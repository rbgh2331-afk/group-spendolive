package com.example.spendolive.report.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.report.domain.ReportAjaxResponse;
import com.example.spendolive.report.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@Controller("ReportController")
@RequestMapping(value="/report")
public class ReportControllerImpl implements ReportController{
    private static final Logger log = LoggerFactory.getLogger(ReportControllerImpl.class);

    private final ReportService reportService;

    public ReportControllerImpl(ReportService reportService) {
        this.reportService = reportService;
    }
    @Override
    @PostMapping("/report.do")
    public ResponseEntity<ReportAjaxResponse> insertReport(@RequestParam("reported_member_id") String reported_member_id, @RequestParam("room_id") String room_id, @RequestParam("chat_text")   String chat_text, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        try {
            reportService.insertReport(reported_member_id, room_id, chat_text, memberInfo);
            return ResponseEntity.ok(new ReportAjaxResponse(
                    true,
                    "REPORTED_COMPLETED",
                    "신고에 성공하였습니다..",
                    "SUCCESS",
                    null,
                    "/spendolive/main.do"));

        } catch (Exception e) {
            // 신고 대상/방/채팅 내용은 개인정보 보호를 위해 서버 로그로 출력하지 않는다
            log.error("{}", "🚨 [신고 저장 오류]: " + e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ReportAjaxResponse(
                false,
                "REPORTED_FAILED",
                e.getMessage(),
                "FAIL",
                null,
                "/spendolive/main.do"));
        }
    }
}
