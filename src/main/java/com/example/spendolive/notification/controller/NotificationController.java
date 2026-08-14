package com.example.spendolive.notification.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.notification.domain.NotificationDTO;
import com.example.spendolive.notification.service.NotificationService;

/**
 * 개인 알림 목록, 읽음 처리, 찜 상태 및 미확인 개수 요청을 처리하는 API 컨트롤러
 */
@RestController
@RequestMapping("/spendolive/notification")
public class NotificationController {

    private final NotificationService notificationService;

    // 알림 서비스 생성자 주입
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 알림센터 전체 알림 목록 조회
    @GetMapping("/ajax/list.do")
    public List<NotificationDTO> notificationList(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Collections.emptyList();
        }

        return notificationService.getNotificationList(memberInfo.getId());
    }

    // 벨 드롭다운 미확인 알림 목록 조회
    @GetMapping("/ajax/unread_list.do")
    public ResponseEntity<List<NotificationDTO>> unreadNotificationList(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            // 비로그인 요청 401 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        return ResponseEntity.ok(notificationService.getUnreadNotificationList(memberInfo.getId()));
}

    // 헤더 알림 배지용 미확인 알림 개수 조회
    @GetMapping("/ajax/unread_count.do")
    public Map<String, Integer> unread_count(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Map.of("unread_count", 0);
        }

        int unread_count =
                notificationService.getUnread_count(memberInfo.getId());

        return Map.of("unread_count", unread_count);
    }

    // 알림 읽음 상태 처리
    @PostMapping("/ajax/read.do")
    public Map<String, String> readNotification(
            @RequestParam("notification_id") int notification_id,
            HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Map.of("result", "LOGIN_REQUIRED");
        }

        notificationService.readNotification(
                notification_id,
                memberInfo.getId());

        return Map.of("result", "OK");
    }

    // 알림 찜 상태 전환
    @PostMapping("/ajax/star.do")
    public Map<String, String> toggleStar(
            @RequestParam("notification_id") int notification_id,
            HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Map.of("result", "LOGIN_REQUIRED");
        }

        notificationService.toggleStar(
                notification_id,
                memberInfo.getId());

        return Map.of("result", "OK");
    }
}
