package com.example.spendolive.common.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.spendolive.member.domain.MemberVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 로그인 필수 URL의 서버 측 세션 검사를 한 곳에서 처리한다.
 * 화면 요청은 로그인 페이지로 이동시키고, AJAX/JSON 요청은 401 JSON을 반환한다.
 */
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        Object sessionMember = session == null ? null : session.getAttribute("memberInfo");
        if (sessionMember instanceof MemberVO member && member.getId() != null && !member.getId().isBlank()) {
            return true;
        }

        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"code\":\"SESSION_EXPIRED\",\"message\":\"로그인이 필요합니다.\",\"data\":null,\"redirectUrl\":\"/member/loginForm.do\"}");
            return false;
        }

        String loginTarget = resolveLoginTarget(request.getRequestURI());
        response.sendRedirect(request.getContextPath() + "/member/loginForm.do?log=" + loginTarget);
        return false;
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || request.getRequestURI().contains("/ajax/");
    }

    private String resolveLoginTarget(String uri) {
        if (uri != null && uri.contains("/expense/")) return "expense";
        if (uri != null && uri.contains("/mypage")) return "mypage";
        return "ott";
    }
}
