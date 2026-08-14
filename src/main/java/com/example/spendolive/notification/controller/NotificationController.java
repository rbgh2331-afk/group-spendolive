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
 * 알림(개인화 알림) API 컨트롤러. 벨 드롭다운(bellIcon.js) + 알림센터 탭(notice.js)이 호출함.
 * - 화면(JSP) 없이 전부 JSON만 반환하는 순수 API.
 * - 모든 메서드가 로그인 여부부터 확인하고, 비로그인이면 빈 값/401/LOGIN_REQUIRED 등으로
 *   메서드마다 다르게 응답함 (호출부마다 처리 방식이 달라서 통일은 안 되어 있음).
 */
@RestController
@RequestMapping("/spendolive/notification")
public class NotificationController {

    private final NotificationService notificationService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 NotificationService 구현체를 자동으로 넣어줌
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // 알림센터(noticeCenter.jsp) "알림" 탭 전체 목록. 비로그인이면 빈 배열 반환
    @GetMapping("/ajax/list.do")
    public List<NotificationDTO> notificationList(HttpSession session) {

        MemberVO memberInfo =
                (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo == null || memberInfo.getId() == null) {
            return Collections.emptyList();
        }

        // ⚠ 디버깅용으로 찍어보던 로그가 그대로 남아있는 것으로 보임. 동작엔 지장 없어서
        //   로직은 안 건드리고 표시만 해둠

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

    // 헤더 종모양 아이콘의 빨간 배지 숫자용. 비로그인이면 0으로 응답(에러 대신)
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

    // 알림 클릭 시 읽음 처리 (readNotification() JS가 호출)
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

    // 알림 찜(star) 토글
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