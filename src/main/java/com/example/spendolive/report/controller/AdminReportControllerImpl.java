package com.example.spendolive.report.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.report.domain.ReportAjaxResponse;
import com.example.spendolive.report.domain.ReportVO;
import com.example.spendolive.report.exception.ReportProcessException;
import com.example.spendolive.report.service.ReportService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@Controller("AdminReportController")
@RequestMapping(value="/admin/report")
public class AdminReportControllerImpl implements AdminReportController{
    private final ReportService reportService;

    public AdminReportControllerImpl(ReportService reportService) {
        this.reportService = reportService;
    }
    @Override
    @GetMapping("/list.do")
    public ModelAndView listUpReport(@RequestParam(value = "status", required = false) String status,HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        
        try {
            List<ReportVO> reportList = new ArrayList<ReportVO>();
            if(status == null){reportList = reportService.selectReportAll();}
            else{reportList = reportService.selectReport(status);}
            
            ModelAndView mav = layout("/WEB-INF/views/admin/report/report.jsp");
            mav.addObject("reportList", reportList);

            return mav;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "리스트업에 실패 하였습니다. ");
            return layout("/WEB-INF/views/admin//main/main.jsp");
        }
    }
    @Override
    @PostMapping("/comment.do")
    public ResponseEntity<ReportAjaxResponse> comment(@RequestParam("admin_comment") String admin_comment,@RequestParam("reported_member_id") String reported_member_id,@RequestParam("report_id") int report_id,@RequestParam("result") String result,  HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        
        
        try {
            reportService.insertWarning(admin_comment, reported_member_id, report_id, result);
            return ResponseEntity.ok(new ReportAjaxResponse(
                    true,
                    "WARINGED_COMPLETED",
                    "경고 처리에 성공하였습니다..",
                    "SUCCESS",
                    null,
                    "/admin/report/list.do"));
        } catch (ReportProcessException e) {
            HttpStatus status = "REPORT_ALREADY_PROCESSED".equals(e.getCode())
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
            .body(new ReportAjaxResponse(
                false,
                e.getCode(),
                e.getMessage(),
                "FAIL",
                null,
                "/admin/report/list.do"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ReportAjaxResponse(
                false,
                "WARINGED_FAILED",
                "신고 처리 중 문제가 발생했습니다.",
                "FAIL",
                null,
                "/admin/report/list.do"));
        }
    }
    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
    
}