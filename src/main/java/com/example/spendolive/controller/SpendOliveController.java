package com.example.spendolive.controller;

import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.example.spendolive.expense.domain.ExpenseDTO;
import com.example.spendolive.admin.dashboard.domain.AdminDashboardDTO;
import com.example.spendolive.admin.dashboard.service.AdminDashboardService;
import com.example.spendolive.expense.service.ExpenseService;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.service.OttService;

@Controller
@RequestMapping("/spendolive")
public class SpendOliveController {

    private final ExpenseService expenseService;
    private final OttService ottService;
    private final AdminDashboardService adminDashboardService;

    public SpendOliveController(ExpenseService expenseService,
                                OttService ottService,
                                AdminDashboardService adminDashboardService) {
        this.expenseService = expenseService;
        this.ottService = ottService;
        this.adminDashboardService = adminDashboardService;
    }

    @RequestMapping(value = {"/", "/main.do"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView main(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = layout("/WEB-INF/views/main/main.jsp");

        // 메인 그래프에서 선택한 달의 지출과 예산을 조회
        addMainDashboardData(
                mav,
                request.getSession(),
                request.getParameter("yearMonth")
        );

        return mav;
    }

    @RequestMapping(value = {"/admin/main.do"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView adminmain(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ModelAndView mav = layout("/WEB-INF/views/admin/main/main.jsp");

        // 관리자 메인 화면에 실제 DB 기준 운영 현황을 표시
        // 통계 조회에 문제가 생겨도 관리자 메뉴 전체가 500 오류로 막히지 않도록 0건 화면을 유지
        try {
            AdminDashboardDTO dashboard = adminDashboardService.getDashboardSummary();
            mav.addObject("adminDashboard", dashboard);
        } catch (Exception e) {
            mav.addObject("adminDashboard", new AdminDashboardDTO());
            mav.addObject("adminDashboardError", "대시보드 통계를 불러오지 못했습니다. DB 테이블 상태를 확인해주세요.");
        }

        return mav;
    }
    @GetMapping("/text.do")
    public String showTextView() {
        // /WEB-INF/views/text.jsp 파일을 열어주라는 의미입니다
        return "text";
    }
    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
    /**
     * 메인 페이지 대시보드 데이터 세팅
     * - 선택한 달의 고정·변동·OTT 지출과 월 예산을 조회
     * - 비로그인 상태에서는 기존 랜덤 대시보드를 사용
     */
    private void addMainDashboardData(ModelAndView mav,
                                      HttpSession session,
                                      String requestedYearMonth) {

        String selectedYearMonth = normalizeYearMonth(requestedYearMonth);
        YearMonth selectedMonth = YearMonth.parse(selectedYearMonth);

        // JSP의 달 선택 입력창과 제목에 사용할 값이다
        mav.addObject("mainSelectedYearMonth", selectedYearMonth);
        mav.addObject("mainSelectedMonthLabel", selectedMonth.getMonthValue() + "월");

        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        // 비로그인 사용자는 DB 조회 없이 기존 랜덤 값으로 보여준다
        if (memberInfo == null) {
            mav.addObject("mainLoggedIn", false);
            mav.addObject("mainFixedTotal", 0);
            mav.addObject("mainVariableTotal", 0);
            mav.addObject("mainOttTotal", 0);
            mav.addObject("mainOttSettlementCount", 0);
            mav.addObject("mainBudget", 1700000);
            return;
        }

        long member_id = memberInfo.getMember_id();
        int fixedTotal = 0;
        int variableTotal = 0;
        int ottTotal = 0;

        // 선택한 달의 예산과 지출 목록을 조회
        int monthlyBudget = expenseService.getMonthlyBudget(member_id, selectedYearMonth);
        List<ExpenseDTO> expenseList = expenseService.getExpenseList(member_id, selectedYearMonth);

        for (ExpenseDTO expense : expenseList) {
            int amount = expense.getAmount() == null ? 0 : expense.getAmount();
            String expense_type = expense.getExpense_type();

            if ("FIXED".equals(expense_type)) {
                fixedTotal += amount;
            } else if ("VARIABLE".equals(expense_type)) {
                variableTotal += amount;
            } else if ("OTT".equals(expense_type)) {
                ottTotal += amount;
            }
        }

        mav.addObject("mainLoggedIn", true);
        mav.addObject("mainFixedTotal", fixedTotal);
        mav.addObject("mainVariableTotal", variableTotal);
        mav.addObject("mainOttTotal", ottTotal);

        // 금액으로 추정하지 않고 선택 월에 사용자가 실제로 관련된 정산 회차를 센다
        mav.addObject(
                "mainOttSettlementCount",
                ottService.getMySettlementCount(memberInfo.getId(), selectedYearMonth));

        mav.addObject("mainBudget", monthlyBudget);
    }

    // 잘못된 연월 값은 현재 달로 바꿔서 조회 오류를 막는다
    private String normalizeYearMonth(String yearMonth) {
        if (yearMonth == null || yearMonth.isBlank()) {
            return YearMonth.now().toString();
        }

        try {
            return YearMonth.parse(yearMonth).toString();
        } catch (Exception e) {
            return YearMonth.now().toString();
        }
    }
}
