package com.example.spendolive.chatbot.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 챗봇 API 응답 바디를 담는 DTO
 * 이 객체를 리턴하면 Spring이 자동으로 JSON으로 바꿔서 프론트에 응답함
 */
@Getter
@AllArgsConstructor
public class ChatbotAnswerDTO {
    private boolean matched;     // FAQ 매칭에 성공했는지 여부 (true/false)
    private String answer;       // 실제로 보여줄 답변 텍스트 (매칭 실패 시엔 안내 문구)
    private Integer faq_id;       // 매칭된 FAQ의 번호 (매칭 실패 시 null)
    private String category;     // 매칭된 FAQ의 카테고리 (매칭 실패 시 null)
}
