package com.example.spendolive.common.ajax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = AjaxEndpoint.class)
/**
 * 
 * 파라미터 누락, 타입 오류, 잘못된 요청 방식, 서버 오류를 JSON 응답으로 통일한다.
 * AJAX 요청 중 Whitelabel HTML이 브라우저에 그대로 표시되는 상황을 방지
 */
public class AjaxExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AjaxExceptionHandler.class);

    // Controller 메서드 진입 전 발생하는 요청값 오류도 공통 400 JSON으로 변환
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<AjaxResponse<Void>> invalidRequest(Exception exception) {
        log.warn("잘못된 AJAX 요청: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(AjaxResponse.failure("INVALID_REQUEST", "요청값이 올바르지 않습니다. 입력 내용을 확인해주세요."));
    }

    // 지원하지 않는 GET/POST 방식 호출은 405 JSON으로 반환
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<AjaxResponse<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        log.warn("지원하지 않는 AJAX 요청 방식: {}", exception.getMethod());
        return ResponseEntity.status(405)
                .body(AjaxResponse.failure("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다."));
    }

    // 위에서 분류되지 않은 예외는 상세 내부 정보 대신 공통 서버 오류 문구를 반환
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResponse<Void>> serverError(Exception exception) {
        log.error("AJAX 처리 중 예상하지 못한 서버 오류가 발생했습니다.", exception);
        return ResponseEntity.internalServerError()
                .body(AjaxResponse.failure("SERVER_ERROR", "처리 중 서버 오류가 발생했습니다."));
    }
}
