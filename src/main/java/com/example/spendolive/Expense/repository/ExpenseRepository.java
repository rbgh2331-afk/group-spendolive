package com.example.spendolive.expense.repository;

import java.util.List;

import com.example.spendolive.expense.domain.ExpenseCategoryDTO;
import com.example.spendolive.expense.domain.ExpenseDTO;

public interface ExpenseRepository {

    List<ExpenseDTO> selectExpenseList(Long member_id, String yearMonth);

    ExpenseDTO selectExpense(Long expense_id);

    void insertExpense(ExpenseDTO expenseDTO);

    void updateExpense(ExpenseDTO expenseDTO);

    void deleteExpense(Long expense_id, Long member_id);

    List<ExpenseCategoryDTO> selectCategoryList();

    List<ExpenseCategoryDTO> selectCategoryListByType(String expense_type);

    // 회원의 선택 월 예산 조회
    int selectMonthlyBudget(Long member_id, String budget_month);

    // 선택 월 예산 등록 또는 수정
    void saveMonthlyBudget(Long member_id, String budget_month, int budget_amount);
}
