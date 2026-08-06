package com.example.spendolive.calendar.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.example.spendolive.Expense.domain.ExpenseDTO;
import com.example.spendolive.Expense.repository.ExpenseRepository;
import com.example.spendolive.member.domain.MemberVO;

/**
 * 캘린더 페이지에서 쓰는 월별 지출 조회 API.
 *
 * ▸ 예전에는 이 컨트롤러가 직접 expense_tb를 "그 달 범위(BETWEEN)"로만 조회해서,
 *   고정/OTT 반복 지출이 등록한 달에만 보이고 다음 달엔 안 보였다.
 * ▸ 이제 지출(Expense) 도메인의 selectExpenseList() 를 재사용한다.
 *   이 메서드는 repeat_yn='Y'인 반복 지출(MONTHLY/WEEKLY/YEARLY)을 대상 월에 맞춰
 *   자동으로 확장해 주므로, 고정/OTT 지출이 "등록한 달 이후 매달(매주/매년)" 계속 표시된다.
 *   → 지출관리/마이페이지 화면과 완전히 동일한 데이터를 캘린더도 보게 됨(일관성).
 */
@RestController
@RequestMapping("/spendolive/calendar")
public class CalendarApiController {

    @Autowired
    private ExpenseRepository expenseRepository;

    /**
     * 특정 연/월의 지출 내역을 JSON으로 반환.
     * GET /spendolive/calendar/expenses.do?year=2026&month=7
     * 고정/반복 지출은 등록 월 이후에도 매달 자동 포함된다.
     */
    @GetMapping("/calendar.do")
    public ModelAndView calendar(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return layout("/WEB-INF/views/calendar/calendar.jsp");
    }
    @GetMapping("/expenses.do")
    @ResponseBody
    public List<Map<String, Object>> getMonthlyExpenses(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            HttpSession session) {

        // 로그인 확인 (로그인 시 memberInfo가 세션에 저장됨)
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        if (memberInfo == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        long member_id = memberInfo.getMember_id();

        // selectExpenseList는 "yyyy-MM" 형식을 YearMonth.parse로 받는다.
        String yearMonth = String.format("%04d-%02d", year, month);

        // 지출 도메인 조회: 반복(고정/OTT) 지출을 이 달에 맞게 확장한 결과를 돌려준다.
        List<ExpenseDTO> expenses;
        try {
            expenses = expenseRepository.selectExpenseList(member_id, yearMonth);
        } catch (Exception e) {
            System.err.println("[CalendarApiController.getMonthlyExpenses] 조회 실패: " + e.getMessage());
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
    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
}