package com.example.spendolive.common.ajax;

/**
 * 
 * success, code, message, data, redirectUrl 필드를 모든 신규 AJAX Controller에서 함께 사용
 * 기존 AJAX Controller가 이미 사용하던 응답 구조는 강제로 변경하지 않는다.
 */
public record AjaxResponse<T>(boolean success, String code, String message, T data, String redirectUrl) {

    // 처리 성공 시 화면 갱신 주소 등 선택적인 data를 함께 전달
    public static <T> AjaxResponse<T> success(String message, T data) {
        return new AjaxResponse<>(true, "SUCCESS", message, data, null);
    }

    public static AjaxResponse<Void> success(String message) {
        return success(message, null);
    }

    // 실패 코드를 구분해 JavaScript가 세션 만료·권한·중복 요청 등을 판단할 수 있게 한다
    public static AjaxResponse<Void> failure(String code, String message) {
        return new AjaxResponse<>(false, code, message, null, null);
    }

    // 세션 만료 안내 후 이동할 로그인 주소를 redirectUrl로 전달
    public static AjaxResponse<Void> sessionExpired(String redirectUrl) {
        return new AjaxResponse<>(false, "SESSION_EXPIRED", "로그인 시간이 만료되었습니다. 다시 로그인해주세요.", null, redirectUrl);
    }

    public static AjaxResponse<Void> forbidden() {
        return failure("FORBIDDEN", "관리자 권한이 필요합니다.");
    }
}
