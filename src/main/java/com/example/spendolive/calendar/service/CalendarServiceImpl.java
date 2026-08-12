package com.example.spendolive.calendar.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.spendolive.Expense.domain.ExpenseDTO;
import com.example.spendolive.Expense.repository.ExpenseRepository;

@Service
public class CalendarServiceImpl implements CalendarService {

    // 반복(고정/OTT) 지출을 대상 월에 맞게 확장해주는 selectExpenseList()를 재사용
    @Autowired
    private ExpenseRepository expenseRepository;

    @Override
    public List<Map<String, Object>> getMonthlyExpenses(long memberId, int year, int month) {

        // selectExpenseList는 "yyyy-MM" 형식을 YearMonth.parse로 받는다.
        String yearMonth = String.format("%04d-%02d", year, month);

        List<ExpenseDTO> expenses;
        try {
            expenses = expenseRepository.selectExpenseList(memberId, yearMonth);
        } catch (Exception e) {
            System.err.println("[CalendarServiceImpl.getMonthlyExpenses] 조회 실패: " + e.getMessage());
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