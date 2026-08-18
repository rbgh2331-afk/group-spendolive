package com.example.spendolive.expense.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.spendolive.expense.domain.ExpenseDTO;
import com.example.spendolive.expense.service.ExpenseService;
import com.example.spendolive.member.domain.MemberVO;

import jakarta.servlet.http.HttpSession;

/**
 * 지출관리 화면 조회 Controller.
 *
 * SSR 방식으로 지출 목록 화면과 차트·요약 데이터를 구성
 * 등록·수정·삭제·예산 저장 AJAX 요청은 ExpenseAjaxController가 담당한다.
 */
@Controller
@RequestMapping("/spendolive/expense")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // -------------------------------------------------------------------------
    // SSR 화면 조회
    // -------------------------------------------------------------------------

    @GetMapping("/list.do")
    public String expenseList(@RequestParam(value = "yearMonth", required = false) String yearMonth,
                              @RequestParam(value = "date", required = false) String date,
                              Model model,
                              HttpSession session) {

        // 화면 진입 로그인 안내는 공통 JS에서 처리하므로 Controller의 중복 redirect 검사는 제거한다
        Long memberId = getLoginMemberId(session);

        String selectedDate = normalizeDate(date);
        String selectedYearMonth = normalizeYearMonth(yearMonth, selectedDate);
        YearMonth selectedMonth = YearMonth.parse(selectedYearMonth);

        List<ExpenseDTO> monthExpenseList = expenseService.getExpenseList(memberId, selectedYearMonth);
        List<ExpenseDTO> tableExpenseList = monthExpenseList;

        if (selectedDate != null) {
            tableExpenseList = monthExpenseList.stream()
                    .filter(expense -> selectedDate.equals(formatDate(expense.getExpense_date())))
                    .toList();
        }

        model.addAttribute("selectedYearMonth", selectedYearMonth);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("monthList", makeMonthList(selectedYearMonth));
        model.addAttribute("expenseList", tableExpenseList);
        model.addAttribute("categoryList", expenseService.getCategoryList());
        model.addAttribute("monthlyBudget", expenseService.getMonthlyBudget(memberId, selectedYearMonth));

        model.addAttribute("expenseTypeSummary", makeExpenseTypeSummary(monthExpenseList));
        model.addAttribute("selectedMonthTotal", sumAmount(monthExpenseList));
        model.addAttribute("monthChartList", makeMonthChartList(memberId, selectedMonth));
        model.addAttribute("categorySummaryList", makeCategorySummaryList(monthExpenseList));
        model.addAttribute("rankingList", makeRankingList(monthExpenseList));

        model.addAttribute("body_page", "/WEB-INF/views/expense/expense.jsp");
        session.removeAttribute("log");

        return "common/layout";
    }

    // -------------------------------------------------------------------------
    // 화면 요청값 보정 및 화면 데이터 구성
    // -------------------------------------------------------------------------

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return null;
        }

        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String normalizeYearMonth(String yearMonth, String date) {
        if (yearMonth != null && !yearMonth.isBlank()) {
            try {
                return YearMonth.parse(yearMonth).toString();
            } catch (Exception ignored) {
                // 잘못된 연월은 아래 날짜 또는 현재 연월로 보정한다
            }
        }

        if (date != null) {
            try {
                return YearMonth.from(LocalDate.parse(date)).toString();
            } catch (Exception ignored) {
                // 잘못된 날짜는 아래 현재 연월로 보정한다
            }
        }

        return YearMonth.now().toString();
    }

    private String normalizeDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(date).toString();
        } catch (Exception exception) {
            return null;
        }
    }

    // 로그인 여부 판단은 화면 공통 함수에 맡기고 세션 회원번호만 꺼낸다
    private Long getLoginMemberId(HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        return Long.valueOf(member.getMember_id());
    }

    private List<String> makeMonthList(String selectedYearMonth) {
        YearMonth selected = YearMonth.parse(selectedYearMonth);
        List<String> monthList = new ArrayList<>();

        for (int i = -2; i <= 4; i++) {
            monthList.add(selected.plusMonths(i).toString());
        }

        return monthList;
    }

    private Map<String, Integer> makeExpenseTypeSummary(List<ExpenseDTO> expenseList) {
        Map<String, Integer> typeSummary = new LinkedHashMap<>();
        typeSummary.put("FIXED", 0);
        typeSummary.put("VARIABLE", 0);
        typeSummary.put("OTT", 0);

        for (ExpenseDTO expense : expenseList) {
            String expenseType = expense.getExpense_type();

            if (expenseType == null || !typeSummary.containsKey(expenseType)) {
                continue;
            }

            typeSummary.put(expenseType, typeSummary.get(expenseType) + safeAmount(expense));
        }

        return typeSummary;
    }

    private List<Map<String, Object>> makeMonthChartList(Long memberId, YearMonth selectedMonth) {
        List<Map<String, Object>> monthChartList = new ArrayList<>();
        List<Integer> totalList = new ArrayList<>();

        for (int i = -2; i <= 0; i++) {
            YearMonth month = selectedMonth.plusMonths(i);
            int total = sumAmount(expenseService.getExpenseList(memberId, month.toString()));
            totalList.add(total);
        }

        int maxTotal = totalList.stream().max(Integer::compareTo).orElse(0);

        for (int i = -2; i <= 0; i++) {
            YearMonth month = selectedMonth.plusMonths(i);
            int total = totalList.get(i + 2);
            int barPercent = 0;

            if (maxTotal > 0 && total > 0) {
                barPercent = (int) Math.round((total * 100.0) / maxTotal);
            }

            Map<String, Object> monthData = new LinkedHashMap<>();
            monthData.put("month", month.toString());
            monthData.put("monthLabel", month.getMonthValue() + "월");
            monthData.put("total", total);
            monthData.put("barPercent", barPercent);
            monthChartList.add(monthData);
        }

        return monthChartList;
    }

    private List<Map<String, Object>> makeCategorySummaryList(List<ExpenseDTO> expenseList) {
        Map<String, Integer> categoryTotalMap = new LinkedHashMap<>();
        int totalAmount = sumAmount(expenseList);

        for (ExpenseDTO expense : expenseList) {
            String categoryName = expense.getCategory_name();

            if (categoryName == null || categoryName.isBlank()) {
                categoryName = "기타";
            }

            categoryTotalMap.put(
                    categoryName,
                    categoryTotalMap.getOrDefault(categoryName, 0) + safeAmount(expense)
            );
        }

        List<Map<String, Object>> categorySummaryList = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : categoryTotalMap.entrySet()) {
            int percent = 0;

            if (totalAmount > 0) {
                percent = (int) Math.round((entry.getValue() * 100.0) / totalAmount);
            }

            Map<String, Object> categoryData = new LinkedHashMap<>();
            categoryData.put("category_name", entry.getKey());
            categoryData.put("total", entry.getValue());
            categoryData.put("percent", percent);
            categorySummaryList.add(categoryData);
        }

        categorySummaryList.sort(
                (first, second) -> Integer.compare(
                        (Integer) second.get("total"),
                        (Integer) first.get("total")
                )
        );

        return categorySummaryList;
    }

    private List<ExpenseDTO> makeRankingList(List<ExpenseDTO> expenseList) {
        return expenseList.stream()
                .sorted(Comparator.comparing(this::safeAmount).reversed())
                .limit(10)
                .toList();
    }

    private int sumAmount(List<ExpenseDTO> expenseList) {
        int total = 0;

        for (ExpenseDTO expense : expenseList) {
            total += safeAmount(expense);
        }

        return total;
    }

    private int safeAmount(ExpenseDTO expense) {
        if (expense == null || expense.getAmount() == null) {
            return 0;
        }

        return expense.getAmount();
    }
}
