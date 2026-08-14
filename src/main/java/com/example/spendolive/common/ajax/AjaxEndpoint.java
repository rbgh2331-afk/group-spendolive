package com.example.spendolive.common.ajax;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/**
 * 
 * 이 애너테이션이 붙은 Controller에만 AjaxExceptionHandler를 적용
 * 기존 일반 화면 Controller의 오류 처리 방식에는 영향을 주지 않기 위한 구분자다.
 */
public @interface AjaxEndpoint {
}
