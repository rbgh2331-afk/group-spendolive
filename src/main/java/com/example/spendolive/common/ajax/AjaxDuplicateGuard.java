package com.example.spendolive.common.ajax;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
/**
 * 
 * 사용자가 등록·결제·정산 버튼을 연속으로 눌러 같은 요청이 중복 실행되는 것을 막는다.
 * DB 구조는 변경하지 않고, 짧은 시간 동안 요청 식별 키를 메모리에 보관하는 보조 장치다.
 */
public class AjaxDuplicateGuard {

    private final ConcurrentHashMap<String, Long> requestExpirations = new ConcurrentHashMap<>();

    // 같은 식별 키가 보관 중이면 false를 반환하고, 처음 요청이면 만료 시각을 기록한다
    public boolean tryAcquire(String key, Duration holdDuration) {
        long now = System.currentTimeMillis();
        long holdMillis = Math.max(1_000L, holdDuration.toMillis());
        requestExpirations.entrySet().removeIf(entry -> entry.getValue() <= now);
        return requestExpirations.putIfAbsent(key, now + holdMillis) == null;
    }

    // 요청이 실패해 사용자가 다시 시도할 수 있어야 할 때 잠금을 즉시 해제
    public void release(String key) {
        requestExpirations.remove(key);
    }
}
