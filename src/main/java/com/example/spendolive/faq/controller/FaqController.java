package com.example.spendolive.faq.controller;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.example.spendolive.faq.service.FaqService;

/**
 * 사용자 화면(자주 묻는 질문)용 FAQ 컨트롤러.
 * - 관리자 쪽 AdminFaqController와 달리 여기는 목록 조회 하나뿐이고, 로그인 여부와
 *   상관없이 누구나 볼 수 있음 (isAdmin 같은 권한 체크가 없음).
 * - 노출 중인(use_yn='Y') FAQ만 가져오는 getVisibleFaqGroupedByCategory()를 써서,
 *   관리자가 "숨김" 처리한 FAQ는 이 화면엔 안 보임.
 */
@Controller
@RequestMapping("/spendolive/faq")
public class FaqController {

    private final FaqService faqService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 FaqService 구현체를 자동으로 넣어줌
    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    // GET /spendolive/faq/list.do
    // 카테고리별로 묶은 FAQ 목록(faqGroups)을 faqList.jsp에 넘겨줌.
    // 조회 실패해도 에러 페이지로 안 보내고 빈 목록 + 안내 메시지로 화면은 정상 렌더링함
    @GetMapping("/list.do")
    public ModelAndView faqList() {
        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/faq/faqList.jsp");

        try {
            mav.addObject("faqGroups", faqService.getVisibleFaqGroupedByCategory());
        } catch (DataAccessException e) {
            System.err.println("[FaqController.faqList] FAQ 목록 로드 실패: " + e.getMessage());
            mav.addObject("faqGroups", Map.of());
            mav.addObject("errorMsg", "FAQ를 불러오는 중 오류가 발생했습니다.");
        }
        return mav;
    }
}