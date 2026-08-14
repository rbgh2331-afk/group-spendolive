package com.example.spendolive.expense.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.spendolive.expense.domain.ExpenseCategoryDTO;
import com.example.spendolive.expense.domain.ExpenseDTO;
import com.example.spendolive.expense.repository.ExpenseRepository;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public List<ExpenseDTO> getExpenseList(Long member_id, String yearMonth) {
        return expenseRepository.selectExpenseList(member_id, yearMonth);
    }

    @Override
    public ExpenseDTO getExpense(Long expense_id) {
        return expenseRepository.selectExpense(expense_id);
    }

    @Override
    public void addExpense(ExpenseDTO expenseDTO) {
        expenseRepository.insertExpense(expenseDTO);
    }

    @Override
    public void modifyExpense(ExpenseDTO expenseDTO) {
        expenseRepository.updateExpense(expenseDTO);
    }

    @Override

    public void removeExpense(Long expense_id, Long member_id) {
        expenseRepository.deleteExpense(expense_id, member_id);

    }

    @Override
    public List<ExpenseCategoryDTO> getCategoryList() {
        return expenseRepository.selectCategoryList();
    }

    @Override
    public List<ExpenseCategoryDTO> getCategoryListByType(String expense_type) {
        return expenseRepository.selectCategoryListByType(expense_type);
    }

    // Repository에서 선택 월 예산을 조회한다.
    @Override
    public int getMonthlyBudget(Long member_id, String budget_month) {
        return expenseRepository.selectMonthlyBudget(member_id, budget_month);
    }

    // Repository에 월 예산 등록·수정을 요청한다.
    @Override
    public void saveMonthlyBudget(Long member_id, String budget_month, int budget_amount) {
        expenseRepository.saveMonthlyBudget(member_id, budget_month, budget_amount);
    }
}
