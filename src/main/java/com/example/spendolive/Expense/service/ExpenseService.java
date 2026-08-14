package com.example.spendolive.expense.service;

import java.util.List;

import com.example.spendolive.expense.domain.ExpenseCategoryDTO;
import com.example.spendolive.expense.domain.ExpenseDTO;

public interface ExpenseService {

    List<ExpenseDTO> getExpenseList(Long member_id, String yearMonth);

    ExpenseDTO getExpense(Long expense_id);

    void addExpense(ExpenseDTO expenseDTO);

    void modifyExpense(ExpenseDTO expenseDTO);

    void removeExpense(Long expense_id, Long member_id);

    List<ExpenseCategoryDTO> getCategoryList();

    List<ExpenseCategoryDTO> getCategoryListByType(String expense_type);

    // 선택 월 예산 조회
    int getMonthlyBudget(Long member_id, String budget_month);

    // 선택 월 예산 저장
    void saveMonthlyBudget(Long member_id, String budget_month, int budget_amount);
}
