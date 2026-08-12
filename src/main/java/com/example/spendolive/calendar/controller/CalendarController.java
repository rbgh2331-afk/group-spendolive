package com.example.spendolive.calendar.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.example.spendolive.calendar.service.CalendarService;
import com.example.spendolive.member.domain.MemberVO;

/**
 * 캘린더 페이지 컨트롤러.
 * - 화면(JSP) 반환 + 월별 지출 조회 API를 한 컨트롤러에서 같이 담당한다.
 * - 실제 조회/가공 로직은 CalendarService로 위임 (Repository 직접 호출 X)
 */
@Controller
@RequestMapping("/spendolive")
public class CalendarController {

    @Autowired
    private CalendarService calendarService;

    /**
     * 캘린더 페이지(calendar.jsp) 화면을 반환.
     * GET 또는 POST /spendolive/calendar.do
     */
    @RequestMapping(value = "/calendar.do", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView calendar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/calendar/calendar.jsp");
        return mav;
    }

    /**
     * 특정 연/월의 지출 내역을 JSON으로 반환.
     * GET /spendolive/calendar/expenses.do?year=2026&month=7
     *
     * 클래스 레벨 매핑이 "/spendolive"라서, 예전 CalendarApiController 때와 같은
     * 주소(/spendolive/calendar/expenses.do)를 유지하려고 경로에 "/calendar"를
     * 직접 붙여씀 (calendar.js가 이 주소로 이미 fetch하고 있어서 URL을 안 바꿈).
     */
    @GetMapping("/calendar/expenses.do")
    @ResponseBody
    public List<Map<String, Object>> getMonthlyExpenses(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            HttpSession session) {

        // 로그인 확인 (로그인 시 memberInfo가 세션에 저장됨)
        // 비로그인이면 에러 던지지 않고 빈 목록으로 정상 응답
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return List.of();
        }

        return calendarService.getMonthlyExpenses(memberInfo.getMember_id(), year, month);
    }
}