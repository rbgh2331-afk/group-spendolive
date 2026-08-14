package com.example.spendolive.faq.controller;

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

import com.example.spendolive.faq.domain.FaqVO;
import com.example.spendolive.faq.service.FaqService;
import com.example.spendolive.member.domain.MemberVO;

/**
 * 관리자 FAQ 관리 화면(목록/등록/수정/순서변경/삭제)을 담당하는 컨트롤러.
 * - 목록(list.do)만 화면(JSP)을 반환하고, 나머지(등록/수정/순서/삭제)는 전부
 *   AJAX 전용이라 페이지 이동 없이 JSON만 주고받음 (adminFaq.js가 호출 주체).
 * - 모든 메서드 맨 앞에서 isAdmin()으로 관리자 세션인지부터 확인함.
 */
@Controller
@RequestMapping("/spendolive/admin/faq")
public class AdminFaqController {
    private static final Logger log = LoggerFactory.getLogger(AdminFaqController.class);

    private final FaqService faqService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 FaqService 구현체를 자동으로 넣어줌
    public AdminFaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    // 세션에 저장된 memberInfo가 있고, role이 "ADMIN"인지 확인.
    // 아래 모든 요청 처리 메서드가 맨 앞에서 이걸로 관리자인지부터 검사함
    private boolean isAdmin(HttpSession session) {
        MemberVO m = (MemberVO) session.getAttribute("memberInfo");
        return m != null && "ADMIN".equals(m.getRole());
    }

    /* ─── 목록 ─────────────────────────────────────────────── */
    // GET /spendolive/admin/faq/list.do
    // 관리자가 아니면 메인으로 돌려보내고, 맞으면 전체 목록 + 카테고리별로
    // 묶은 목록(faqGroups, adminFaqList.jsp가 카테고리 헤딩별로 표 나눠 그릴 때 씀)을 같이 넘김
    @GetMapping("/list.do")
    public ModelAndView list(HttpSession session) {
        if (!isAdmin(session)) return new ModelAndView("redirect:/spendolive/main.do");

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/admin/faq_inquiry/adminFaqList.jsp");
        try {
            mav.addObject("faqList", faqService.getAllFaqList());
            mav.addObject("faqGroups", faqService.getAllFaqGroupedByCategory());
        } catch (Exception e) {
            mav.addObject("faqList", List.of());
            mav.addObject("faqGroups", Map.of());
            mav.addObject("errorMsg", "FAQ 목록을 불러오는 중 오류가 발생했습니다.");
        }
        return mav;
    }

    /* ════════════════════════════════════════════════════════════
       AJAX 전용 (페이지 이동 없이 JSON) — adminFaq.js가 호출.
       목록/작성폼/수정폼(GET)은 그대로 두고, 실제 등록/수정/순서변경/삭제만 여기서 처리.
       ════════════════════════════════════════════════════════════ */

    /** AJAX: FAQ 등록 */
    @PostMapping("/ajax/insert.do")
    @ResponseBody
    public ResponseEntity<?> ajaxInsert(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "answer",   required = false) String answer,
            @RequestParam(value = "useYn",    defaultValue = "N") String useYn,
            HttpSession session) {

        if (!isAdmin(session)) return forbidden();
        if (isBlank(category) || isBlank(question) || isBlank(answer)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "카테고리, 질문, 답변을 모두 입력해 주세요."));
        }
        if (!"Y".equals(useYn)) useYn = "N";

        FaqVO faq = new FaqVO();
        faq.setCategory(category);
        faq.setQuestion(question.strip());
        faq.setAnswer(answer.strip());
        // 새 FAQ는 항상 그 카테고리 맨 뒤 순서로 등록됨 (같은 카테고리 안에서 몇 번째인지는
        // getNextSortOrder가 계산해줌 - 관리자가 직접 순서를 안 정해도 됨)
        faq.setSort_order(faqService.getNextSortOrder(category));
        faq.setUse_yn(useYn);

        try {
            faqService.insertFaq(faq);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "FAQ가 등록되었습니다."));
        } catch (DataAccessException e) {
            log.error("{}", "[AdminFaqController.ajaxInsert] 등록 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "등록 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: FAQ 수정 */
    @PostMapping("/ajax/update.do")
    @ResponseBody
    public ResponseEntity<?> ajaxUpdate(
            @RequestParam(value = "faq_id",   defaultValue = "0") int faq_id,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "answer",   required = false) String answer,
            @RequestParam(value = "useYn",    defaultValue = "N") String useYn,
            HttpSession session) {

        if (!isAdmin(session)) return forbidden();
        if (faq_id <= 0) return badId();
        if (isBlank(category) || isBlank(question) || isBlank(answer)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "카테고리, 질문, 답변을 모두 입력해 주세요."));
        }
        if (!"Y".equals(useYn)) useYn = "N";

        FaqVO faq = new FaqVO();
        faq.setFaq_id(faq_id);
        faq.setCategory(category);
        faq.setQuestion(question.strip());
        faq.setAnswer(answer.strip());
        faq.setUse_yn(useYn);
        // 여기선 sort_order를 새로 안 세팅함 - 수정은 순서를 안 건드리고 내용만 바꾸는 거라
        // updateFaq 쪽 SQL이 sort_order 컬럼은 아예 건드리지 않는 걸로 되어있어야 함

        try {
            faqService.updateFaq(faq);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "FAQ가 수정되었습니다."));
        } catch (Exception e) {
            log.error("{}", "[AdminFaqController.ajaxUpdate] 수정 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "수정 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 순서 위로 */
    @PostMapping("/ajax/moveUp.do")
    @ResponseBody
    public ResponseEntity<?> ajaxMoveUp(
            @RequestParam(value = "faq_id", defaultValue = "0") int faq_id,
            HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (faq_id <= 0) return badId();
        try {
            faqService.moveFaqUp(faq_id);
            return ResponseEntity.ok(Map.of("result", "OK"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "순서 변경 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 순서 아래로 */
    @PostMapping("/ajax/moveDown.do")
    @ResponseBody
    public ResponseEntity<?> ajaxMoveDown(
            @RequestParam(value = "faq_id", defaultValue = "0") int faq_id,
            HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (faq_id <= 0) return badId();
        try {
            faqService.moveFaqDown(faq_id);
            return ResponseEntity.ok(Map.of("result", "OK"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "순서 변경 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: FAQ 삭제 */
    @PostMapping("/ajax/delete.do")
    @ResponseBody
    public ResponseEntity<?> ajaxDelete(
            @RequestParam(value = "faq_id", defaultValue = "0") int faq_id,
            HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        if (faq_id <= 0) return badId();
        try {
            faqService.deleteFaq(faq_id);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "FAQ가 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "삭제 중 오류가 발생했습니다."));
        }
    }

    /* ── 공통 응답 헬퍼 ── */
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    // ⚠ 관리자 아닐 때 HTTP 상태코드는 401(UNAUTHORIZED)로 내려주는데,
    //   응답 body의 result 값은 "FORBIDDEN"(원래 403 느낌)이라 이름이 좀 안 맞음.
    //   adminFaq.js 쪽이 지금 status 401만 보고 처리하는 거면 상관없는데,
    //   혹시 result 값 문자열("FORBIDDEN")을 직접 비교하는 코드가 있으면 헷갈릴 수 있음.
    //   로직은 안 건드림
    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("result", "FORBIDDEN", "message", "관리자만 접근할 수 있습니다."));
    }
    private ResponseEntity<?> badId() {
        return ResponseEntity.badRequest()
                .body(Map.of("result", "INVALID_PARAM", "message", "잘못된 FAQ 번호입니다."));
    }
}