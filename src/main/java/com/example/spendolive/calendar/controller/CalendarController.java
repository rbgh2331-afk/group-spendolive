package com.example.spendolive.calendar.controller;

import java.util.ArrayList;
import java.util.HashMap;
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

import com.example.spendolive.Expense.domain.ExpenseDTO;
import com.example.spendolive.Expense.repository.ExpenseRepository;
import com.example.spendolive.member.domain.MemberVO;

/**
 * 캘린더 페이지 컨트롤러.
 * - 화면(JSP) 반환 + 월별 지출 조회 API를 한 컨트롤러에서 같이 담당한다.
 *
 * 
 *
 * ▸ /calendar/expenses.do는 지출(Expense) 도메인의 selectExpenseList()를 재사용한다.
 *   이 메서드는 repeat_yn='Y'인 반복 지출(MONTHLY/WEEKLY/YEARLY)을 대상 월에 맞춰
 *   자동으로 확장해 주므로, 고정/OTT 지출이 "등록한 달 이후 매달(매주/매년)" 계속 표시된다.
 *   → 지출관리/마이페이지 화면과 완전히 동일한 데이터를 캘린더도 보게 됨(일관성).
 */
@Controller
@RequestMapping("/spendolive")
public class CalendarController {

    // 반복(고정/OTT) 지출을 대상 월에 맞게 확장해주는 selectExpenseList()를 여기서 재사용함
    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * 캘린더 페이지(calendar.jsp) 화면을 반환.
     * GET 또는 POST /spendolive/calendar.do
     * request/response는 받아만 두고 실제로는 안 씀 (세션이나 파라미터 처리가
     * 필요 없는 단순 화면 반환이라서). 지출 데이터는 여기서 안 채워주고,
     * calendar.js가 별도로 아래 /calendar/expenses.do를 fetch해서 채움.
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
     * 고정/반복 지출은 등록 월 이후에도 매달 자동 포함된다.
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
        // 비로그인이면 에러 던지지 않고 빈 목록으로 정상 응답 - 프론트(calendar.js)가
        // "로그인 시 확인 가능" 안내 문구를 정상적으로 그릴 수 있게 함
        // (예전엔 여기서 예외를 던져서 fetch가 500으로 실패했고, 그러면 calendar.js의
        //  .then() 렌더링 자체가 통째로 안 불려서 화면이 완전히 텅 비어버렸음)
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            return List.of();
        }
        long member_id = memberInfo.getMember_id();

        // selectExpenseList는 "yyyy-MM" 형식을 YearMonth.parse로 받는다.
        String yearMonth = String.format("%04d-%02d", year, month);

        // 지출 도메인 조회: 반복(고정/OTT) 지출을 이 달에 맞게 확장한 결과를 돌려준다.
        List<ExpenseDTO> expenses;
        try {
            expenses = expenseRepository.selectExpenseList(member_id, yearMonth);
        } catch (Exception e) {
            System.err.println("[CalendarController.getMonthlyExpenses] 조회 실패: " + e.getMessage());
            expenses = List.of();
        }

        // 캘린더 JS가 쓰는 필드만 골라, 기존과 동일한 JSON 형태(Map)로 매핑
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExpenseDTO e : expenses) {
            Map<String, Object> row = new HashMap<>();
            row.put("expense_id", e.getExpense_id());
            row.put("expense_title", e.getExpense_title());
            row.put("amount", e.getAmount());
            java.util.Date d = e.getExpense_date();
            row.put("expense_date", d == null
                    ? null
                    : new java.sql.Date(d.getTime()).toLocalDate().toString());  // "yyyy-MM-dd"
            row.put("category_name", e.getCategory_name());
            row.put("expense_type", e.getExpense_type());
            result.add(row);
        }
        return result;
    }
}