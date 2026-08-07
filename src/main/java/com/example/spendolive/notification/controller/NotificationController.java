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

@RestController
@RequestMapping("/spendolive/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/ajax/list.do")
    public List<NotificationDTO> notificationList(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Collections.emptyList();
        }

        System.out.println("로그인 ID = " + memberInfo.getId());

        return notificationService.getNotificationList(memberInfo.getId());
    }

    // 벨 드롭다운 전용: 안읽은 알림만 반환. 읽음 처리되면 다음 호출부터 목록에서 사라짐.
    // (전체 내역이 필요한 알림센터 페이지는 기존 /ajax/list.do 그대로 사용)
    @GetMapping("/ajax/unread_list.do")
    public ResponseEntity<List<NotificationDTO>> unreadNotificationList(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            // 비로그인 - 빈 배열이 아니라 401로 구분해서 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        return ResponseEntity.ok(notificationService.getUnreadNotificationList(memberInfo.getId()));
}

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