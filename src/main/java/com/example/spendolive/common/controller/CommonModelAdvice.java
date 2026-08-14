package com.example.spendolive.common.controller;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.service.OttService;

@ControllerAdvice
public class CommonModelAdvice {

    private final OttService ottService;

    public CommonModelAdvice(OttService ottService) {
        this.ottService = ottService;
    }

    @ModelAttribute
    public void addCommonModel(Model model, HttpSession session, HttpServletRequest request) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");


        // ── 로그인 여부 + memberInfo 항상 model에 주입 ──────────────
        // header.jsp 등 공통 레이아웃에서 ${isLogOn}, ${memberInfo} 사용
        boolean isLogOn = (memberInfo != null);
        model.addAttribute("isLogOn", isLogOn);
        model.addAttribute("memberInfo", memberInfo);

        
        if (memberInfo == null) {
            model.addAttribute("chatRoomSummaryList", Collections.emptyList());
            model.addAttribute("chatTotalUnreadCount", 0);
            return;
        }

        // AJAX/관리자 요청은 사용자 공통 채팅 위젯을 렌더링하지 않으므로 OTT 조회를 생략한다.
        // 페이지 이동과 무관한 요청마다 채팅방/미읽음 DB 조회가 반복되는 것을 방지한다.
        if (isChatWidgetLookupUnnecessary(request)) {
            model.addAttribute("chatRoomSummaryList", Collections.emptyList());
            model.addAttribute("chatTotalUnreadCount", 0);
            return;
        }

        String loginId = memberInfo.getId();
        if (loginId == null || loginId.isBlank()) {
            loginId = String.valueOf(memberInfo.getMember_id());
        }

        try {
            model.addAttribute("chatRoomSummaryList", ottService.getMyChatRooms(loginId));
            model.addAttribute("chatTotalUnreadCount", ottService.getUnreadChatCount(loginId));
        } catch (DataAccessException e) {
            // OTT 관련 테이블이 아직 생성되지 않았거나 SQL 스키마가 정리되지 않은 상태여도
            // 다른 페이지까지 깨지지 않도록 공통 채팅 위젯은 빈 값으로 둔다.
            model.addAttribute("chatRoomSummaryList", Collections.emptyList());
            model.addAttribute("chatTotalUnreadCount", 0);
        }
    }
    private boolean isChatWidgetLookupUnnecessary(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        boolean ajaxRequest = "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || uri.contains("/ajax/")
                || uri.endsWith("/status.do");
        boolean adminRequest = uri.contains("/admin/") || uri.contains("/spendolive/admin/");
        return ajaxRequest || adminRequest;
    }

}
