package com.example.spendolive.inquiry.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.spendolive.inquiry.domain.InquiryFileVO;
import com.example.spendolive.inquiry.domain.InquiryVO;
import com.example.spendolive.inquiry.service.InquiryService;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.notification.service.NotificationService;

@Controller
@RequestMapping("/spendolive/inquiry")
public class InquiryController {
    private static final Logger log = LoggerFactory.getLogger(InquiryController.class);

    private final InquiryService inquiryService;
    private final NotificationService notificationService;

    public InquiryController(InquiryService inquiryService, NotificationService notificationService) {
        this.inquiryService = inquiryService;
        this.notificationService = notificationService;
    }

    /* ─── 내 문의 조회 ────────────────────────────────────── */
    @GetMapping("/list.do")
    public ModelAndView inquiryList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "status", defaultValue = "all") String status,
            HttpSession session, RedirectAttributes ra) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            ra.addFlashAttribute("msg", "로그인이 필요한 기능입니다. \n로그인 후 이용해 주세요.");
            return new ModelAndView("redirect:/member/loginForm.do");
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/inquiry/inquiryList.jsp");

        // 화면 필터 코드(all/wait/done/review) → DB 저장값(WAIT/DONE/REVIEW, all은 필터 없음=null)
        String normalizedStatus = normalizeStatusFilter(status);

        try {
            int totalPages = inquiryService.getMyInquiryTotalPages(memberInfo.getId(), normalizedStatus);
            int currentPage = Math.min(Math.max(page, 1), totalPages);

            mav.addObject("inquiryList",
                    inquiryService.getMyInquiryList(memberInfo.getId(), currentPage, normalizedStatus));
            mav.addObject("currentPage", currentPage);
            mav.addObject("totalPages", totalPages);
            mav.addObject("currentStatus", status.toLowerCase()); // 필터 버튼 active 표시 + 페이지네이션 링크 유지용
        } catch (Exception e) {
            log.error("{}", "[InquiryController.inquiryList] 목록 로드 실패: " + e.getMessage(), e);
            mav.addObject("inquiryList", List.of());
            mav.addObject("currentPage", 1);
            mav.addObject("totalPages", 1);
            mav.addObject("currentStatus", "all");
            mav.addObject("errorMsg", "문의 목록을 불러오는 중 오류가 발생했습니다.");
        }
        return mav;
    }

    /** 화면 필터 코드를 DB status 컬럼값으로 변환. all이거나 알 수 없는 값이면 null(필터 없음). */
    private String normalizeStatusFilter(String status) {
        if (status == null) return null;
        switch (status.toLowerCase()) {
            case "wait": return "WAIT";
            case "done": return "DONE";
            case "review": return "REVIEW";
            default: return null; // "all" 포함
        }
    }

    /* ─── 문의 작성 폼 ────────────────────────────────────── */
    @GetMapping("/write.do")
    public ModelAndView inquiryWriteForm(HttpSession session, RedirectAttributes ra) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            ra.addFlashAttribute("msg", "로그인이 필요한 기능입니다. \n로그인 후 이용해 주세요.");
            return new ModelAndView("redirect:/member/loginForm.do");
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/inquiry/inquiryWrite.jsp");
        return mav;
    }

    /* ─── 문의 상세 ───────────────────────────────────────── */
    @GetMapping("/detail.do")
    public ModelAndView inquiryDetail(
            @RequestParam(value = "inquiryNo", defaultValue = "0") int inquiryNo,
            HttpSession session, RedirectAttributes ra) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            ra.addFlashAttribute("msg", "로그인이 필요한 기능입니다. \n로그인 후 이용해 주세요.");
            return new ModelAndView("redirect:/member/loginForm.do");
        }

        InquiryVO inquiry = null;
        try {
            inquiry = inquiryService.getInquiryDetail(inquiryNo);
        } catch (Exception e) {
            log.error("{}", "[InquiryController.inquiryDetail] 조회 실패: " + e.getMessage(), e);
        }

        // 존재하지 않거나 본인 문의가 아니면 목록으로
        if (inquiry == null || !inquiry.getId().equals(memberInfo.getId())) {
            ra.addFlashAttribute("errorMsg", "존재하지 않거나 접근할 수 없는 문의입니다.");
            return new ModelAndView("redirect:/spendolive/inquiry/list.do");
        }

        // 문의 상세 화면 구성
        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/inquiry/inquiryDetail.jsp");
        mav.addObject("inquiry", inquiry);
        return mav;

    }

    /* ─── 문의 수정 폼 ─────────────────────────────────────── */
    @GetMapping("/edit.do")
    public ModelAndView inquiryEditForm(
            @RequestParam(value = "inquiryNo", defaultValue = "0") int inquiryNo,
            HttpSession session, RedirectAttributes ra) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            ra.addFlashAttribute("msg", "로그인이 필요한 기능입니다. \n로그인 후 이용해 주세요.");
            return new ModelAndView("redirect:/member/loginForm.do");
        }

        InquiryVO inquiry = inquiryService.getInquiryDetail(inquiryNo);

        // 본인 문의가 아니거나, 답변이 이미 달려서 더 이상 수정할 수 없는 상태면 목록으로
        if (inquiry == null || !inquiry.getId().equals(memberInfo.getId()) || !"WAIT".equals(inquiry.getStatus())) {
            ra.addFlashAttribute("errorMsg", "수정할 수 없는 문의입니다. (답변 대기 상태의 본인 문의만 수정 가능합니다)");
            return new ModelAndView("redirect:/spendolive/inquiry/list.do");
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/inquiry/inquiryEdit.jsp");
        mav.addObject("inquiry", inquiry);
        return mav;
    }

    /* ════════════════════════════════════════════════════════════
       AJAX 전용 엔드포인트 (페이지 이동 없이 JSON으로 결과만 반환)
       - 목록 조회/필터/페이지네이션은 기존 list.do(GET)를 그대로 재사용해서
         프론트(inquiry.js)가 그 페이지의 #inqBoardArea 조각만 갈아끼운다.
         (관리자 문의/FAQ의 admin.js swapBoardArea와 동일한 방식)
       - 작성/수정/삭제만 아래 ajax/* 로 JSON 처리한다.
       ════════════════════════════════════════════════════════════ */

    /** AJAX: 문의 등록 (첨부파일 포함, FormData로 전송받음) */
    @PostMapping("/ajax/write.do")
    @ResponseBody
    public ResponseEntity<?> ajaxWrite(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "inquiry_type", required = false) String inquiry_type,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "LOGIN_REQUIRED", "message", "로그인이 필요합니다."));
        }
        if (category == null || category.isBlank()
                || inquiry_type == null || inquiry_type.isBlank()
                || title == null || title.isBlank()
                || content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "카테고리, 유형, 제목, 내용을 모두 입력해 주세요."));
        }

        InquiryVO inquiry = new InquiryVO();
        inquiry.setId(memberInfo.getId());
        inquiry.setCategory(category);
        inquiry.setInquiry_type(inquiry_type);
        inquiry.setTitle(title.strip());
        inquiry.setContent(content.strip());

        try {
            inquiryService.writeInquiry(inquiry, attachments);
            return ResponseEntity.ok(Map.of("result", "OK",
                    "message", "문의가 접수되었습니다. 답변까지 영업일 기준 1~2일 소요됩니다."));
        } catch (IllegalArgumentException e) {
            // 첨부파일 검증 실패(용량/확장자/개수 초과 등)
            return ResponseEntity.badRequest().body(Map.of("result", "ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("{}", "[InquiryController.ajaxWrite] 등록 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "문의 접수 중 오류가 발생했습니다. 다시 시도해 주세요."));
        }
    }

    /** AJAX: 문의 수정 (첨부파일은 수정 대상 아님 - 기존 정책 유지) */
    @PostMapping("/ajax/edit.do")
    @ResponseBody
    public ResponseEntity<?> ajaxEdit(
            @RequestParam(value = "inquiryNo", defaultValue = "0") int inquiryNo,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "inquiry_type", required = false) String inquiry_type,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "LOGIN_REQUIRED", "message", "로그인이 필요합니다."));
        }
        if (category == null || category.isBlank()
                || inquiry_type == null || inquiry_type.isBlank()
                || title == null || title.isBlank()
                || content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("result", "INVALID_PARAM", "message", "카테고리, 유형, 제목, 내용을 모두 입력해 주세요."));
        }

        InquiryVO inquiry = new InquiryVO();
        inquiry.setInquiry_id(inquiryNo);
        inquiry.setId(memberInfo.getId());
        inquiry.setCategory(category);
        inquiry.setInquiry_type(inquiry_type);
        inquiry.setTitle(title.strip());
        inquiry.setContent(content.strip());

        try {
            // updateInquiry가 false를 반환하는 경우 = 대상이 없거나, 본인 것이 아니거나,
            // 이미 답변이 달려서(WAIT 아님) 수정 조건에 안 맞는 경우 (Service 쪽에서 판단)
            boolean updated = inquiryService.updateInquiry(inquiry);
            if (!updated) {
                return ResponseEntity.badRequest().body(Map.of("result", "ERROR",
                        "message", "수정할 수 없는 문의입니다. (답변 대기 상태의 본인 문의만 수정 가능합니다)"));
            }
            return ResponseEntity.ok(Map.of("result", "OK", "message", "문의가 수정되었습니다."));
        } catch (Exception e) {
            log.error("{}", "[InquiryController.ajaxEdit] 수정 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "수정 중 오류가 발생했습니다."));
        }
    }

    /** AJAX: 문의 삭제 */
    @PostMapping("/ajax/delete.do")
    @ResponseBody
    public ResponseEntity<?> ajaxDelete(
            @RequestParam(value = "inquiryNo", defaultValue = "0") int inquiryNo,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "LOGIN_REQUIRED", "message", "로그인이 필요합니다."));
        }

        try {
            boolean deleted = inquiryService.deleteInquiry(inquiryNo, memberInfo.getId());
            if (!deleted) {
                return ResponseEntity.badRequest().body(Map.of("result", "ERROR",
                        "message", "삭제할 수 없는 문의입니다. (답변 대기 상태의 본인 문의만 삭제 가능합니다)"));
            }
            return ResponseEntity.ok(Map.of("result", "OK", "message", "문의가 삭제되었습니다."));
        } catch (Exception e) {
            log.error("{}", "[InquiryController.ajaxDelete] 삭제 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("result", "ERROR", "message", "삭제 중 오류가 발생했습니다."));
        }
    }

    /* ─── 첨부파일 미리보기/다운로드 ──────────────────────── */
    @GetMapping("/file/{file_id}")
    public ResponseEntity<Resource> viewInquiryFile(
            @PathVariable("file_id") int file_id, HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return ResponseEntity.status(401).build();
        }

        // 본인 문의에 달린 첨부파일이 맞는지 확인 (관리자는 전체 열람 가능)
        boolean isAdmin = "ADMIN".equals(memberInfo.getRole());
        InquiryFileVO file = inquiryService.getInquiryFile(file_id, memberInfo.getId(), isAdmin);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path path = Path.of(file.getFile_path());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOrigin_name() + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("{}", "[InquiryController.viewInquiryFile] 파일 읽기 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
