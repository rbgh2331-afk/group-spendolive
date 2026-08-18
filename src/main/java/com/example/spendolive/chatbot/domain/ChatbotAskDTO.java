package com.example.spendolive.chatbot.domain;

import lombok.Getter;
import lombok.Setter;

/*
 * 챗봇 API 요청 바디를 담는 DTO
 * 프론트(chatbot.js)에서 JSON { "question": "..." } 형태로 보내면
 * Spring이 이 클래스로 자동 변환해줌 (@RequestBody가 이 변환을 담당)
 */

@Getter
@Setter
public class ChatbotAskDTO {
    private String question;   // 사용자가 입력한 질문 원문
}
