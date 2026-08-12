package com.example.spendolive.notification.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.notification.domain.NotificationDTO;
import com.example.spendolive.notification.service.NotificationService;

/**
 * 알림 상세를 별도 페이지(JSP)로 보여주는 컨트롤러.
 */
@Controller
@RequestMapping("/spendolive/notification")
public class NotificationPageController {

    private final NotificationService notificationService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 NotificationService 구현체를 자동으로 넣어줌
    public NotificationPageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /* ─── 알림 상세 페이지 ────────────────────────────────── */
    // GET /spendolive/notification/detail.do?notification_id=
    // 비로그인이면 로그인 페이지로, 잘못된 번호거나 존재하지 않으면 errorMsg 담아서 같은 화면 반환
    @GetMapping("/detail.do")
    public ModelAndView notificationDetail(
            @RequestParam(value = "notification_id", defaultValue = "0") int notification_id,
            HttpSession session) {

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        // 비로그인 → 로그인 페이지로
        if (memberInfo == null) {
            return new ModelAndView("redirect:/member/loginForm.do");
        }

        if (notification_id <= 0) {
            ModelAndView mav = new ModelAndView("common/layout");
            mav.addObject("body_page", "/WEB-INF/views/notification/notificationDetail.jsp");
            mav.addObject("errorMsg", "잘못된 알림 번호입니다.");
            return mav;
        }

        NotificationDTO notification = null;
        try {
            notification = notificationService.getNotificationDetail(notification_id, memberInfo.getId());
        } catch (Exception e) {
            System.err.println("[NotificationPageController.detail] 조회 실패: " + e.getMessage());
        }

        if (notification == null) {
            ModelAndView mav = new ModelAndView("common/layout");
            mav.addObject("body_page", "/WEB-INF/views/notification/notificationDetail.jsp");
            mav.addObject("errorMsg", "존재하지 않는 알림입니다.");
            return mav;
        }

        // 이 페이지에 직접 들어온 것 자체가 그 알림을 확인한 거라 읽음 처리.
        // (bellIcon.js의 readNotificationFromBell()이 이동 전에 이미 read.do를 한 번
        //  호출하긴 하지만, 여기서 또 한 번 처리해도 무해함 - 이미 읽음이면 그냥 갱신 없이 넘어감)
        try {
            notificationService.readNotification(notification_id, memberInfo.getId());
        } catch (Exception e) {
            System.err.println("[NotificationPageController.detail] 읽음 처리 실패: " + e.getMessage());
        }

        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/notification/notificationDetail.jsp");
        mav.addObject("notification", notification);
        return mav;
    }
}