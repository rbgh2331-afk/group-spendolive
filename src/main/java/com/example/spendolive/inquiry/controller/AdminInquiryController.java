package com.example.spendolive.inquiry.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.inquiry.domain.InquiryVO;
import com.example.spendolive.inquiry.service.InquiryService;
import com.example.spendolive.member.domain.MemberVO;

/**
 * 관리자 문의(1:1 문의) 관리 화면(목록/상세/답변)을 담당하는 컨트롤러.
 * - 목록/상세는 화면(JSP)을 반환하고, 답변 등록만 AJAX(JSON)로 처리함
 *   (adminInquiryList.jsp가 상세를 모달로 띄우고, adminInquiry.js가 답변 폼 제출을 가로챔).
 *
 */

@Controller
@RequestMapping("/admin/inquiry")
public class AdminInquiryController {
    private static final Logger log = LoggerFactory.getLogger(AdminInquiryController.class);

    private final InquiryService inquiryService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 InquiryService 구현체를 자동으로 넣어줌
    public AdminInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    // 세션에 저장된 memberInfo가 있고, role이 "ADMIN"인지 확인
    // 아래 모든 요청 처리 메서드가 맨 앞에서 이걸로 관리자인지부터 검사함
    private boolean isAdmin(HttpSession session) {
        MemberVO m = (MemberVO) session.getAttribute("memberInfo");
        return m != null && "ADMIN".equals(m.getRole());
    }

    /** 화면 필터 코드(all/wait/done/review) → DB status 값(WAIT/DONE/REVIEW, all은 null=필터 없음) */
    private String normalizeStatusFilter(String status) {
        if (status == null) return null;
        switch (status.toLowerCase()) {
            case "wait": return "WAIT";
            case "done": return "DONE";
            case "review": return "REVIEW";
            default: return null; // "all" 포함
        }
    }

    /* ─── 전체 문의 목록 ──────────────────────────────────── */
    // GET /admin/inquiry/list.do?page=&status=
    // 페이지네이션 + 상태 필터(전체/대기/완료/검토중)를 같이 처리
    // startNumber 계산이 핵심: 목록이 최신순(내림차순)으로 나오는데 번호는
    // "오래된 문의부터 1번"으로 매기고 싶어서, 전체 개수에서 거꾸로 세어 내려가는 방식으로 구함
    // (예: 전체 20건, 1페이지(최신 10건)면 맨 위 줄이 20번, 아래로 내려갈수록 감소)
    @GetMapping("/list.do")
    public ModelAndView list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "status", defaultValue = "all") String status,
            HttpSession session) {

        if (!isAdmin(session)) return new ModelAndView("redirect:/spendolive/main.do");

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/admin/faq_inquiry/adminInquiryList.jsp");

        String normalizedStatus = normalizeStatusFilter(status);
        try {
            int totalPages = inquiryService.getAdminInquiryTotalPages(normalizedStatus);
            // 요청받은 page가 범위를 벗어나면(너무 크거나 0 이하) 유효 범위 안으로 보정
            int currentPage = Math.min(Math.max(page, 1), totalPages);
            int totalCount = inquiryService.getAdminInquiryTotalCount(normalizedStatus);
            int pageSize = inquiryService.getAdminPageSize();

            mav.addObject("inquiryList", inquiryService.getAllInquiriesForAdmin(normalizedStatus, currentPage));
            mav.addObject("currentPage", currentPage);
            mav.addObject("totalPages", totalPages);
            mav.addObject("currentStatus", status.toLowerCase());
            // 목록은 최신순(내림차순)이라, 화면 맨 위 줄이 startNumber, 그 아래로 1씩 감소하며 매김 (오래된 문의=1)
            mav.addObject("startNumber", totalCount - (currentPage - 1) * pageSize);
        } catch (Exception e) {
            log.error("{}", "[AdminInquiryController.list] 목록 로드 실패: " + e.getMessage(), e);
            mav.addObject("inquiryList", List.of());
            mav.addObject("currentPage", 1);
            mav.addObject("totalPages", 1);
            mav.addObject("currentStatus", "all");
            mav.addObject("startNumber", 0);
            mav.addObject("errorMsg", "문의 목록을 불러오는 중 오류가 발생했습니다.");
        }
        return mav;
    }

    /* ─── 문의 상세 + 답변 폼 ────────────────────────────── */
    @GetMapping("/detail.do")
    public ModelAndView detail(
            @RequestParam(value = "inquiryNo", defaultValue = "0") int inquiryNo,
            HttpSession session, RedirectAttributes ra) {

        if (!isAdmin(session)) return new ModelAndView("redirect:/spendolive/main.do");
        if (inquiryNo <= 0) {
            ra.addFlashAttribute("errorMsg", "잘못된 문의 번호입니다.");
            return new ModelAndView("redirect:/admin/inquiry/list.do");
        }

        InquiryVO inquiry = null;
        try {
            inquiry = inquiryService.getInquiryDetail(inquiryNo);
        } catch (Exception e) {
            log.error("{}", "[AdminInquiryController.detail] 조회 실패: " + e.getMessage(), e);
        }

        if (inquiry == null) {
            ra.addFlashAttribute("errorMsg", "존재하지 않는 문의입니다.");
            return new ModelAndView("redirect:/admin/inquiry/list.do");
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/admin/faq_inquiry/adminInquiryDetail.jsp");
        mav.addObject("inquiry", inquiry);
        return mav;
    }

    /* ─── 답변 등록/수정 (AJAX) ───────────────────────────
       목록이 팝업(모달)+AJAX 방식이므로, 답변도 페이지 이동 없이 JSON으로 처리.
       성공 후에는 adminInquiry.js가 모달을 닫고 목록 조각(#adminBoardArea)만
       다시 불러와 상태 배지를 갱신한다. */
    @PostMapping("/ajax/reply.do")
    @ResponseBody
    public ResponseEntity<?> ajaxReply(
            @RequestParam(value = "inquiry_id", defaultValue = "0") int inquiry_id,
            @RequestParam(value = "reply_content", required = false) String reply_content,
            @RequestParam(value = "status", defaultValue = "DONE") String status,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN", "message", "관리자만 접근할 수 있습니다."));
        }
        if (inquiry_id <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "잘못된 문의 번호입니다."));
        }
        if (reply_content == null || reply_content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "답변 내용을 입력해 주세요."));
        }
        // 관리자가 고를 수 있는 상태는 DONE(답변완료)/REVIEW(검토중) 둘 중 하나로 제한
        if (!"DONE".equals(status) && !"REVIEW".equals(status)) {
            status = "DONE";
        }

        try {
            inquiryService.replyToInquiry(inquiry_id, reply_content.strip(), status);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "답변이 등록되었습니다."));
        } catch (DataAccessException e) {
            log.error("{}", "[AdminInquiryController.ajaxReply] 답변 등록 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "답변 등록 중 오류가 발생했습니다."));
        }
    }
}
