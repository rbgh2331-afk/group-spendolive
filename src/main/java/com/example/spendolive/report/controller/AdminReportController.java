package com.example.spendolive.report.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.report.domain.ReportAjaxResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 관리자 신고 관리 요청 처리 컨트롤러
 */
public interface AdminReportController {
    public ModelAndView listUpReport(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<ReportAjaxResponse> comment(@RequestParam("admin_comment") String admin_comment, @RequestParam("reported_member_id") String reported_member_id, @RequestParam("report_id") int report_id, @RequestParam("result") String result,  HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
}
