package com.example.spendolive.chatbot.service;

import com.example.spendolive.chatbot.domain.ChatbotAnswerDTO;

/**
 * 챗봇 질문에 대한 답변 조회 기능을 정의하는 서비스 인터페이스
 */
public interface ChatbotService {
    ChatbotAnswerDTO findAnswer(String question);
}
