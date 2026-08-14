package com.example.spendolive.notice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.notice.domain.NoticeDTO;
import com.example.spendolive.notice.service.NoticeService;

@Controller
@RequestMapping("/spendolive/notice")
public class NoticeController {
    private static final Logger log = LoggerFactory.getLogger(NoticeController.class);

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /* ─── 공지 센터 메인 ──────────────────────────────────── */
    @GetMapping("/center.do")

    public Object noticeCenter(
            @RequestParam(value = "tab", required = false, defaultValue = "notice") String tab,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        // 알림 탭은 로그인 필요
        if ("alert".equals(tab) && memberInfo == null) {
            redirectAttributes.addFlashAttribute("msg", "로그인이 필요한 기능 입니다. \n로그인을 해주세요 !");
            return "redirect:/member/loginForm.do?log=notice";
        }

        String id = (memberInfo != null) ? memberInfo.getId() : null;

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/notice/noticeCenter.jsp");
        mav.addObject("loginYn", memberInfo != null);
        mav.addObject("tab", tab);

        try {
            mav.addObject("noticeList",    noticeService.getNoticeList(id));
            mav.addObject("noticeCount",   noticeService.getNoticeCount());
            mav.addObject("importantCount", noticeService.getPinnedCount());
        } catch (Exception e) {
            log.error("{}", "[NoticeController.noticeCenter] 공지 로드 실패: " + e.getMessage(), e);
            mav.addObject("noticeList",    List.of());
            mav.addObject("noticeCount",   0);
            mav.addObject("importantCount", 0);
            mav.addObject("errorMsg", "공지사항을 불러오는 중 오류가 발생했습니다.");
        }
        session.removeAttribute("log");
        return mav;
    }

    /* ─── 공지 상세 ───────────────────────────────────────── */
    @GetMapping("/detail.do")
    public ModelAndView noticeDetail(
            @RequestParam(value = "notice_id", required = false, defaultValue = "0") int notice_id,
            // 목록에서 어떤 필터(전체/안읽음/중요)를 보다가 들어왔는지 - "목록으로" 링크에
            // 그대로 실어 보내서 돌아갔을 때 같은 필터가 유지되게 함
            @RequestParam(value = "filter", required = false, defaultValue = "all") String filter,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        if (notice_id <= 0) {
            ModelAndView mav = new ModelAndView("common/layout");
            mav.addObject("body_page", "/WEB-INF/views/notice/noticeCenter.jsp");
            mav.addObject("errorMsg", "잘못된 공지 번호입니다.");
            mav.addObject("loginYn", memberInfo != null);
            return mav;
        }

        NoticeDTO notice = null;
        try {
            // 찜(star_yn) 상태까지 같이 받아와서, 목록에서 찜한 공지는 상세에서도
            // 채워진 별로 보이도록 함 (기존엔 star_yn을 안 가져와서 항상 빈 별로만 보였음)
            notice = noticeService.getNoticeDetailForUser(notice_id, memberInfo != null ? memberInfo.getId() : null);
        } catch (Exception e) {
            log.error("{}", "[NoticeController.noticeDetail] 조회 실패: " + e.getMessage(), e);
        }

        if (notice == null) {
            ModelAndView mav = new ModelAndView("common/layout");
            mav.addObject("body_page", "/WEB-INF/views/notice/noticeCenter.jsp");
            mav.addObject("errorMsg", "존재하지 않는 공지사항입니다.");
            mav.addObject("loginYn", memberInfo != null);
            return mav;
        }

        // 로그인 사용자 읽음 처리
        if (memberInfo != null && memberInfo.getId() != null) {
            try {
                noticeService.readNotice(notice_id, memberInfo.getId());
            } catch (Exception e) {
                log.error("{}", "[NoticeController.noticeDetail] 읽음 처리 실패: " + e.getMessage(), e);
            }
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/notice/noticeDetail.jsp");
        mav.addObject("notice", notice);
        mav.addObject("loginYn", memberInfo != null);
        mav.addObject("filter", filter);
        return mav;
    }

    /* ─── AJAX: 전체 공지 목록 ────────────────────────────── */
    @GetMapping("/ajax/noticeList.do")
    @ResponseBody
    public List<NoticeDTO> ajaxNoticeList(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        String id = (memberInfo != null) ? memberInfo.getId() : null;

        try {
            return noticeService.getNoticeList(id);
        } catch (Exception e) {
            log.error("{}", "[NoticeController.ajaxNoticeList] 오류: " + e.getMessage(), e);
            return List.of();
        }
    }

    /* ─── AJAX: 중요 공지 목록 ────────────────────────────── */
    @GetMapping("/ajax/importantList.do")
    @ResponseBody
    public List<NoticeDTO> ajaxImportantList(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        String id = (memberInfo != null) ? memberInfo.getId() : null;

        try {
            return noticeService.getImportantList(id);
        } catch (Exception e) {
            log.error("{}", "[NoticeController.ajaxImportantList] 오류: " + e.getMessage(), e);
            return List.of();
        }
    }

    /* ─── AJAX: 안 읽은 공지 목록 ────────────────────────── */
    @GetMapping("/ajax/unreadNoticeList.do")
    @ResponseBody
    public List<NoticeDTO> ajaxUnreadNoticeList(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return List.of();
        }

        try {
            return noticeService.getUnreadNoticeList(memberInfo.getId());
        } catch (Exception e) {
            log.error("{}", "[NoticeController.ajaxUnreadNoticeList] 오류: " + e.getMessage(), e);
            return List.of();
        }
    }

    /* ─── AJAX: 찜 토글 ───────────────────────────────────── */
    @PostMapping("/ajax/star.do")
    @ResponseBody
    public Map<String, String> toggleNoticeStar(
            @RequestParam(value = "notice_id", required = false, defaultValue = "0") int notice_id,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Map.of("result", "LOGIN_REQUIRED");
        }
        if (notice_id <= 0) {
            return Map.of("result", "INVALID_PARAM");
        }

        try {
            noticeService.toggleNoticeStar(notice_id, memberInfo.getId());
            return Map.of("result", "OK");
        } catch (Exception e) {
            log.error("{}", "[NoticeController.toggleNoticeStar] 오류: " + e.getMessage(), e);
            return Map.of("result", "ERROR");
        }
    }

    /* ─── AJAX: 공지 상세(모달용) ──────────────────────────
       기존 /detail.do(페이지 이동)와 서비스/읽음 처리 로직은 동일하고,
       화면 이동 없이 JSON만 내려줌. 목록에서 모달로 띄우기 위해 추가. */
    @GetMapping("/ajax/detail.do")
    @ResponseBody
    public Map<String, Object> ajaxNoticeDetail(
            @RequestParam(value = "notice_id", required = false, defaultValue = "0") int notice_id,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        if (notice_id <= 0) {
            return Map.of("result", "INVALID_PARAM");
        }

        NoticeDTO notice;
        try {
            notice = noticeService.getNoticeDetailForUser(notice_id, memberInfo != null ? memberInfo.getId() : null);
        } catch (Exception e) {
            log.error("{}", "[NoticeController.ajaxNoticeDetail] 조회 실패: " + e.getMessage(), e);
            return Map.of("result", "ERROR");
        }

        if (notice == null) {
            return Map.of("result", "NOT_FOUND");
        }

        // 로그인 사용자 읽음 처리 (기존 detail.do와 동일한 로직)
        if (memberInfo != null && memberInfo.getId() != null) {
            try {
                noticeService.readNotice(notice_id, memberInfo.getId());
            } catch (Exception e) {
                log.error("{}", "[NoticeController.ajaxNoticeDetail] 읽음 처리 실패: " + e.getMessage(), e);
            }
        }

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("result", "OK");
        res.put("notice_id", notice.getNotice_id());
        res.put("title", notice.getTitle());
        res.put("content", notice.getContent());
        res.put("admin_id", notice.getAdmin_id());
        res.put("created_at", notice.getCreated_at());
        res.put("pinned_yn", notice.getPinned_yn());
        res.put("star_yn", notice.getStar_yn());
        return res;
    }
}
