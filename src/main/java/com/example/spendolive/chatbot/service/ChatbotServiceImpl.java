package com.example.spendolive.chatbot.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.spendolive.chatbot.domain.ChatbotAnswerDTO;
import com.example.spendolive.faq.domain.FaqVO;
import com.example.spendolive.faq.repository.FaqRepository;

/**
 * 챗봇이 사용자 질문에 답할 FAQ를 찾아주는 서비스.
 * - 새 테이블/AI 모델 없이, 기존 FAQ 데이터를 키워드 매칭으로 검색해서 답변함
 * - 매칭 방식: 질문/답변/카테고리에 사용자 키워드가 얼마나 겹치는지 점수로 채점(scoreMatch)해서
 *   가장 점수 높은 FAQ 하나를 골라 답변으로 씀 (findAnswer가 전체 흐름 담당)
 */
@Service
public class ChatbotServiceImpl implements ChatbotService {

    // 매칭 실패 시 공통으로 내려줄 안내 문구
    private static final String FALLBACK_MESSAGE =
        "문의 주신 내용에 딱 맞는 답변을 찾지 못했어요. 1:1 문의를 남겨주시면 확인 후 답변드릴게요.";

    // 이 점수 이상이어야 "매칭 성공"으로 인정 (낮출수록 매칭은 잘 되지만 엉뚱한 FAQ가 걸릴 확률도 올라감)
    private static final int MIN_SCORE = 1;

    // FAQ 기능에서 이미 만들어둔 Repository를 그대로 재사용 (새 테이블/새 DAO 안 만듦)
    private final FaqRepository faqRepository;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 FaqRepository를 자동으로 넣어줌
    public ChatbotServiceImpl(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public ChatbotAnswerDTO findAnswer(String question) {
        // 질문이 비어있으면 바로 fallback (DB 조회할 필요도 없음)
        if (question == null || question.isBlank()) {
            return new ChatbotAnswerDTO(false, FALLBACK_MESSAGE, null, null);
        }

        // 1단계: 사용자 질문에서 검색에 쓸 키워드만 뽑아냄
        List<String> keywords = extractKeywords(question);

        // 2단계: 노출 중인(use_yn='Y') FAQ 전체를 후보로 가져옴
        List<FaqVO> candidates = faqRepository.findAllVisible();

        // 3단계: 후보 FAQ들을 하나씩 채점하면서 제일 점수 높은 걸 찾음
        FaqVO best = null;
        int bestScore = 0;

        for (FaqVO faq : candidates) {
            int score = scoreMatch(keywords, faq);
            if (score > bestScore) {
                bestScore = score;
                best = faq;
            }
        }

        // 4단계: 최고 점수가 기준(MIN_SCORE) 이상이면 그 FAQ 답변을 반환
        if (best != null && bestScore >= MIN_SCORE) {
            return new ChatbotAnswerDTO(true, best.getAnswer(), best.getFaq_id(), best.getCategory());
        }

        // 기준 미달이면 매칭 실패로 처리
        return new ChatbotAnswerDTO(false, FALLBACK_MESSAGE, null, null);
    }

    /**
     * 사용자 질문 문자열을 공백 기준으로 쪼개서,
     * 2글자 미만인 짧은 단어(조사 파편 등)는 걸러내고 키워드 리스트로 만듦
     * 예: "정산은 언제 되나요?" → ["정산은", "언제", "되나요?"]
     */
    private List<String> extractKeywords(String question) {
        return List.of(question.trim().split("\\s+")).stream()
                .filter(w -> w.length() >= 2)
                .toList();
    }

    /**
     * 하나의 FAQ에 대해 키워드가 얼마나 겹치는지 점수를 매김
     * - 질문 제목(question)에 있으면 2점  → 제목에 딱 맞는 FAQ를 최우선으로
     * - 답변 본문(answer)에 있으면 1점   → 제목엔 없어도 내용에 언급됐으면 후보로 인정
     * - 카테고리명(categoryLabel)에 있으면 1점 → "로그인"이라고만 쳐도 관련 카테고리 FAQ가 걸리게
     */
    private int scoreMatch(List<String> keywords, FaqVO faq) {
        String question = faq.getQuestion() == null ? "" : faq.getQuestion();
        String answer = faq.getAnswer() == null ? "" : faq.getAnswer();
        String categoryLabel = faq.getCategoryLabel();

        int score = 0;
        for (String kw : keywords) {
            if (question.contains(kw)) score += 2;
            if (answer.contains(kw)) score += 1;
            if (categoryLabel.contains(kw)) score += 1;
        }
        return score;
    }
}
