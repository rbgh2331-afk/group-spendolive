package com.example.spendolive.expense.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseCategoryDTO {

    private Long category_id;
    private String category_name;
    private String expense_type;
    private Integer sort_order;
}