package com.example.spendolive.common.ajax;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.spendolive.member.domain.MemberVO;

import jakarta.servlet.http.HttpSession;

/**
 * 
 * AJAX Controller가 기존 세션의 memberInfo를 동일한 방식으로 확인하도록 모은 유틸리티다.
 * 로그인 만료와 관리자 권한 부족 응답을 공통 AjaxResponse 형식으로 반환
 */
public final class AjaxAuthSupport {

    private AjaxAuthSupport() {
    }

    // 세션이 없거나 memberInfo 타입이 올바르지 않으면 로그인하지 않은 상태로 처리
    public static MemberVO member(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("memberInfo");
        return value instanceof MemberVO ? (MemberVO) value : null;
    }

    // role 값이 정확히 ADMIN인 회원만 관리자 AJAX 요청을 실행할 수 있다
    public static boolean isAdmin(MemberVO member) {
        return member != null && "ADMIN".equals(member.getRole());
    }

    // 세션 만료를 401과 로그인 이동 주소로 반환해 공통 JavaScript가 처리하게 한다
    public static ResponseEntity<AjaxResponse<Void>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AjaxResponse.sessionExpired("/member/loginForm.do"));
    }

    // 로그인은 되어 있지만 관리자 권한이 없을 때 403 JSON을 반환
    public static ResponseEntity<AjaxResponse<Void>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(AjaxResponse.forbidden());
    }
}
