package com.example.spendolive.notice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.notice.domain.NoticeDTO;
import com.example.spendolive.notice.service.NoticeService;

@Controller
@RequestMapping("/admin/notice")
public class AdminNoticeController {
    private static final Logger log = LoggerFactory.getLogger(AdminNoticeController.class);

    private final NoticeService noticeService;

    public AdminNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    private boolean isAdmin(HttpSession session) {
        MemberVO m = (MemberVO) session.getAttribute("memberInfo");
        return m != null && "ADMIN".equals(m.getRole());
    }

    /* ─── 목록 ─────────────────────────────────────────────────────────────
       AJAX 전환 후에는 이 메서드는 "빈 골격 페이지"만 그려주고,
       실제 목록 데이터는 화면 로드 후 adminNotice.js가 /ajax/list.do를
       호출해서 채워 넣는다. (전체 새로고침 없이 페이지 이동/검색 가능)
       ────────────────────────────────────────────────────────────────── */
    @GetMapping("/list.do")
    public ModelAndView list(HttpSession session) {
        if (!isAdmin(session)) return new ModelAndView("redirect:/spendolive/main.do");

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/admin/faq_inquiry/adminNoticeList.jsp");
        return mav;
    }

    /* ════════════════════════════════════════════════════════════
       작성/수정은 이제 목록 화면(adminNoticeList.jsp)의 모달에서 처리하므로
       별도 write.do 페이지는 제거했다. 아래 ajax/* 로 CRUD를 처리한다.
       - list.do: 첫 진입 시 골격 페이지(빈 목록 + 모달)만 렌더
       - ajax/list · detail · insert · update · delete: 실제 데이터 처리(JSON)
       ════════════════════════════════════════════════════════════ */

    /** AJAX: 공지 목록 (페이지네이션 포함) 조회 */
    @GetMapping("/ajax/list.do")
    @ResponseBody
    public ResponseEntity<?> ajaxList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            HttpSession session) {

        // 관리자가 아니면 401(미인증)로 응답 → 프론트에서 로그인 페이지로 안내
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN", "message", "관리자만 접근할 수 있습니다."));
        }

        try {
            int totalPages = noticeService.getNoticeAdminTotalPages();
            int currentPage = Math.min(Math.max(page, 1), Math.max(totalPages, 1));

            Map<String, Object> body = Map.of(
                    "result", "OK",
                    "noticeList", noticeService.getNoticeListForAdmin(currentPage),
                    "currentPage", currentPage,
                    "totalPages", totalPages,
                    "totalCount", noticeService.getNoticeCount()
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("{}", "[AdminNoticeController.ajaxList] 오류: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "공지 목록을 불러오는 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 단건 조회 (수정 폼에 기존 값 채워 넣을 때 사용) */
    @GetMapping("/ajax/detail.do")
    @ResponseBody
    public ResponseEntity<?> ajaxDetail(
            @RequestParam(value = "notice_id", defaultValue = "0") int notice_id,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN"));
        }
        if (notice_id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("result", "INVALID_PARAM"));
        }

        NoticeDTO notice = null;
        try {
            notice = noticeService.getNoticeDetail(notice_id);
        } catch (Exception e) {
            log.error("{}", "[AdminNoticeController.ajaxDetail] 오류: " + e.getMessage(), e);
        }

        if (notice == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("result", "NOT_FOUND", "message", "존재하지 않는 공지사항입니다."));
        }
        return ResponseEntity.ok(Map.of("result", "OK", "notice", notice));
    }

    /** AJAX: 등록 처리 (JSON으로 결과만 반환, 페이지 이동은 프론트 JS가 담당) */
    @PostMapping("/ajax/insert.do")
    @ResponseBody
    public ResponseEntity<?> ajaxInsert(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "pinned_yn", defaultValue = "N") String pinned_yn,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN"));
        }
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "제목과 내용을 모두 입력해 주세요."));
        }
        if (!"Y".equals(pinned_yn)) pinned_yn = "N";

        MemberVO m = (MemberVO) session.getAttribute("memberInfo");
        NoticeDTO dto = new NoticeDTO();
        dto.setTitle(title.strip());
        dto.setContent(content.strip());
        dto.setPinned_yn(pinned_yn);
        dto.setAdmin_id(m.getId());

        try {
            int newId = noticeService.insertNotice(dto);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "공지사항이 등록되었습니다.", "notice_id", newId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("result", "ERROR", "message", e.getMessage()));
        } catch (DataAccessException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "등록 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 수정 처리 */
    @PostMapping("/ajax/update.do")
    @ResponseBody
    public ResponseEntity<?> ajaxUpdate(
            @RequestParam(value = "notice_id", defaultValue = "0") int notice_id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "pinned_yn", defaultValue = "N") String pinned_yn,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN"));
        }
        if (notice_id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("result", "INVALID_PARAM", "message", "잘못된 공지 번호입니다."));
        }
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("result", "INVALID_PARAM", "message", "제목과 내용을 모두 입력해 주세요."));
        }
        if (!"Y".equals(pinned_yn)) pinned_yn = "N";

        NoticeDTO dto = new NoticeDTO();
        dto.setNotice_id(notice_id);
        dto.setTitle(title.strip());
        dto.setContent(content.strip());
        dto.setPinned_yn(pinned_yn);

        try {
            noticeService.updateNotice(dto);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "공지사항이 수정되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "수정 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 삭제 처리 */
    @PostMapping("/ajax/delete.do")
    @ResponseBody
    public ResponseEntity<?> ajaxDelete(
            @RequestParam(value = "notice_id", defaultValue = "0") int notice_id,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "FORBIDDEN"));
        }
        if (notice_id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("result", "INVALID_PARAM", "message", "잘못된 공지 번호입니다."));
        }
        try {
            noticeService.deleteNotice(notice_id);
            return ResponseEntity.ok(Map.of("result", "OK", "message", "공지사항이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "삭제 중 오류가 발생했습니다."));
        }
    }
}