package com.example.spendolive.chatbot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.spendolive.chatbot.domain.ChatbotAnswerDTO;
import com.example.spendolive.chatbot.domain.ChatbotAskDTO;
import com.example.spendolive.chatbot.service.ChatbotService;

/**
 * 챗봇 위젯(chatbot.js)이 fetch()로 호출하는 API의 진입점.
 * @RestController = 여기서 리턴하는 객체를 JSP 화면이 아니라 JSON으로 바로 응답함
 */
@RestController
@RequestMapping("/spendolive/chatbot")
public class ChatbotController {
    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    // 생성자 주입(필드에 @Autowired 안 붙이고 생성자로 받는 방식) - 스프링이
    // 빈 등록할 때 이 생성자를 보고 ChatbotService 구현체를 자동으로 넣어줌
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * POST /spendolive/chatbot/ask.do
     * 요청 body 예시: { "question": "정산은 언제 되나요?" }
     * 응답 예시: { "matched": true, "answer": "...", "faq_id": 12, "category": "expense" }
     *
     * 컨트롤러는 요청을 받아서 서비스에 그대로 넘기고, 서비스가 만든 결과를 그대로 돌려줄 뿐
     * → 실제 "생각하는" 로직은 전부 ChatbotServiceImpl에 있음 (관심사 분리)
     */
    @PostMapping("/ask.do")
    @ResponseBody
    public ChatbotAnswerDTO ask(@RequestBody ChatbotAskDTO request) {
        try {
            return chatbotService.findAnswer(request.getQuestion());
        } catch (Exception e) {
            log.error("{}", "[ChatbotController.ask] 답변 조회 실패: " + e.getMessage(), e);
            return new ChatbotAnswerDTO(
                    false,
                    "일시적인 오류로 답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.",
                    null,
                    null
            );
        }
    }
}
