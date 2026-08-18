package com.example.spendolive.expense.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.spendolive.expense.domain.ExpenseCategoryDTO;
import com.example.spendolive.expense.domain.ExpenseDTO;

@Repository
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final String selectExpenseListByMonthSql = """
            SELECT
                e.expense_id,
                e.member_id,
                e.category_id,
                c.category_name,
                c.expense_type,
                e.expense_title,
                e.amount,
                e.expense_date,
                e.payment_method,
                e.memo,
                e.repeat_yn,
                e.repeat_cycle,
                e.repeat_end_date,
                e.fixed_yn,
                e.created_at,
                e.updated_at
            FROM expense_tb e
            JOIN expense_category_tb c
              ON e.category_id = c.category_id
            WHERE e.member_id = ?
              AND e.expense_date >= ?
              AND e.expense_date < ?
            ORDER BY e.expense_date DESC, e.expense_id DESC
            """;

    // 조회 월이 종료일을 지난 반복 원본은 자동 생성 대상에서 제외
    private final String selectRepeatBaseExpenseSql = """
        SELECT
            e.expense_id,
            e.member_id,
            e.category_id,
            c.category_name,
            c.expense_type,
            e.expense_title,
            e.amount,
            e.expense_date,
            e.payment_method,
            e.memo,
            e.repeat_yn,
            e.repeat_cycle,
            e.repeat_end_date,
            e.fixed_yn,
            e.created_at,
            e.updated_at
        FROM expense_tb e
        JOIN expense_category_tb c
          ON e.category_id = c.category_id
        WHERE e.member_id = ?
          AND c.expense_type IN ('FIXED', 'OTT')
          AND e.repeat_yn = 'Y'
          AND e.repeat_cycle IN ('MONTHLY', 'WEEKLY', 'YEARLY')
          AND e.expense_date < ?
          AND (e.repeat_end_date IS NULL OR e.repeat_end_date >= ?)
        ORDER BY e.expense_date ASC, e.expense_id ASC
        """;

    private final String selectExpenseSql = """
            SELECT
                e.expense_id,
                e.member_id,
                e.category_id,
                c.category_name,
                c.expense_type,
                e.expense_title,
                e.amount,
                e.expense_date,
                e.payment_method,
                e.memo,
                e.repeat_yn,
                e.repeat_cycle,
                e.repeat_end_date,
                e.fixed_yn,
                e.created_at,
                e.updated_at
            FROM expense_tb e
            JOIN expense_category_tb c
              ON e.category_id = c.category_id
            WHERE e.expense_id = ?
            """;

    // 등록 시 선택한 종료일을 함께 저장
    private final String insertExpenseSql = """
            INSERT INTO expense_tb (
                member_id,
                category_id,
                expense_title,
                amount,
                expense_date,
                payment_method,
                memo,
                repeat_yn,
                repeat_cycle,
                repeat_end_date,
                fixed_yn
            ) VALUES (?, ?, ?, ?, ?, ?, ?, NVL(?, 'N'), ?, ?, NVL(?, 'N'))
            """;

    // 수정 시 종료일 변경 또는 NULL 해제를 반영한다
    private final String updateExpenseSql = """
            UPDATE expense_tb
            SET
                category_id = ?,
                expense_title = ?,
                amount = ?,
                expense_date = ?,
                payment_method = ?,
                memo = ?,
                repeat_yn = NVL(?, 'N'),
                repeat_cycle = ?,
                repeat_end_date = ?,
                fixed_yn = NVL(?, 'N'),
                updated_at = SYSDATE
            WHERE expense_id = ?
              AND member_id = ?
            """;

    private final String deleteExpenseSql = """
            DELETE FROM expense_tb
            WHERE expense_id = ?
              AND member_id = ?
            """;

    private final String selectCategoryListSql = """
            SELECT
                category_id,
                category_name,
                expense_type,
                sort_order
            FROM expense_category_tb
            ORDER BY expense_type ASC, sort_order ASC, category_id ASC
            """;

    private final String selectCategoryListByTypeSql = """
            SELECT
                category_id,
                category_name,
                expense_type,
                sort_order
            FROM expense_category_tb
            WHERE expense_type = ?
            ORDER BY sort_order ASC, category_id ASC
            """;

    // 회원과 연월을 기준으로 월 예산을 조회
    private final String selectMonthlyBudgetSql = """
            SELECT NVL(MAX(budget_amount), 0)
            FROM monthly_budget_tb
            WHERE member_id = ?
              AND budget_month = ?
            """;

    // 같은 달의 예산이 있으면 수정하고 없으면 새로 등록
    private final String saveMonthlyBudgetSql = """
            MERGE INTO monthly_budget_tb budget
            USING (
                SELECT
                    ? AS member_id,
                    ? AS budget_month,
                    ? AS budget_amount
                FROM dual
            ) input
            ON (
                budget.member_id = input.member_id
                AND budget.budget_month = input.budget_month
            )
            WHEN MATCHED THEN
                UPDATE SET
                    budget.budget_amount = input.budget_amount,
                    budget.updated_at = SYSDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    member_id,
                    budget_month,
                    budget_amount,
                    created_at,
                    updated_at
                )
                VALUES (
                    input.member_id,
                    input.budget_month,
                    input.budget_amount,
                    SYSDATE,
                    SYSDATE
                )
            """;

    @Override
    public List<ExpenseDTO> selectExpenseList(Long member_id, String yearMonth) {
        YearMonth targetMonth = YearMonth.parse(yearMonth);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.plusMonths(1).atDay(1);

        List<ExpenseDTO> result = jdbcTemplate.query(
                selectExpenseListByMonthSql,
                expenseRowMapper(),
                member_id,
                Date.valueOf(startDate),
                Date.valueOf(endDate)
        );

        for (ExpenseDTO expense : result) {
            expense.setAuto_generated_yn("N");
        }

        List<ExpenseDTO> repeatBaseList = jdbcTemplate.query(
                selectRepeatBaseExpenseSql,
                expenseRowMapper(),
                member_id,
                Date.valueOf(startDate),
                Date.valueOf(startDate)
        );

        result.addAll(makeRepeatedExpenses(repeatBaseList, targetMonth, result));

        result.sort(
                Comparator.comparing(ExpenseDTO::getExpense_date, Comparator.nullsLast(Comparator.reverseOrder()))
                          .thenComparing(ExpenseDTO::getExpense_id, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        return result;
    }

    @Override
    public ExpenseDTO selectExpense(Long expense_id) {
        try {
            return jdbcTemplate.queryForObject(selectExpenseSql, expenseRowMapper(), expense_id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void insertExpense(ExpenseDTO expenseDTO) {
        jdbcTemplate.update(
                insertExpenseSql,
                expenseDTO.getMember_id(),

                expenseDTO.getCategory_id(),
                expenseDTO.getExpense_title(),

                expenseDTO.getAmount(),
                toSqlDate(expenseDTO.getExpense_date()),
                expenseDTO.getPayment_method(),
                expenseDTO.getMemo(),
                expenseDTO.getRepeat_yn(),
                expenseDTO.getRepeat_cycle(),
                toSqlDate(expenseDTO.getRepeat_end_date()),
                expenseDTO.getFixed_yn()
        );
    }

    @Override
    public void updateExpense(ExpenseDTO expenseDTO) {
        jdbcTemplate.update(
                updateExpenseSql,
                expenseDTO.getCategory_id(),
                expenseDTO.getExpense_title(),
                expenseDTO.getAmount(),
                toSqlDate(expenseDTO.getExpense_date()),
                expenseDTO.getPayment_method(),
                expenseDTO.getMemo(),
                expenseDTO.getRepeat_yn(),
                expenseDTO.getRepeat_cycle(),
                toSqlDate(expenseDTO.getRepeat_end_date()),
                expenseDTO.getFixed_yn(),
                expenseDTO.getExpense_id(),
                expenseDTO.getMember_id()
        );
    }

    @Override

    public void deleteExpense(Long expense_id, Long member_id) {
        jdbcTemplate.update(deleteExpenseSql, expense_id, member_id);

    }

    @Override
    public List<ExpenseCategoryDTO> selectCategoryList() {
        return jdbcTemplate.query(selectCategoryListSql, categoryRowMapper());
    }

    @Override
    public List<ExpenseCategoryDTO> selectCategoryListByType(String expense_type) {
        return jdbcTemplate.query(selectCategoryListByTypeSql, categoryRowMapper(), expense_type);
    }

    // 조회 결과가 없으면 예산 0원으로 반환
    @Override
    public int selectMonthlyBudget(Long member_id, String budget_month) {
        Integer budgetAmount = jdbcTemplate.queryForObject(
                selectMonthlyBudgetSql,
                Integer.class,
                member_id,
                budget_month
        );

        return budgetAmount == null ? 0 : budgetAmount;
    }

    // 예산 저장 시 수정일은 SQL에서 현재 날짜로 갱신
    @Override
    public void saveMonthlyBudget(Long member_id, String budget_month, int budget_amount) {
        jdbcTemplate.update(
                saveMonthlyBudgetSql,
                member_id,
                budget_month,
                budget_amount
        );
    }

    /**
     * 반복 설정으로 생성될 내역을 만든다.
     *
     * 같은 날짜에 같은 카테고리·제목·금액으로 실제 지출이 등록되어 있으면
     * 사용자가 해당 회차를 직접 입력한 것으로 보고
     * 자동 반복 내역을 추가하지 않는다.
     */
    private List<ExpenseDTO> makeRepeatedExpenses(
            List<ExpenseDTO> repeatBaseList,
            YearMonth targetMonth,
            List<ExpenseDTO> actualExpenseList) {

        List<ExpenseDTO> repeatedList = new ArrayList<>();
        Set<String> actualOccurrenceKeys = new HashSet<>();
        Set<String> generatedBaseDateKeys = new HashSet<>();

        for (ExpenseDTO actual : actualExpenseList) {
            LocalDate actualDate = toLocalDate(actual.getExpense_date());
            if (actualDate != null) {
                actualOccurrenceKeys.add(makeOccurrenceKey(actual, actualDate));
            }
        }

        LocalDate targetStart = targetMonth.atDay(1);
        LocalDate targetEnd = targetMonth.plusMonths(1).atDay(1);

        for (ExpenseDTO base : repeatBaseList) {
            LocalDate baseDate = toLocalDate(base.getExpense_date());

            if (baseDate == null) {
                continue;
            }

            String repeat_cycle = base.getRepeat_cycle();

            if ("MONTHLY".equals(repeat_cycle)) {
                int day = Math.min(baseDate.getDayOfMonth(), targetMonth.lengthOfMonth());
                LocalDate repeatedDate = targetMonth.atDay(day);

                if (!repeatedDate.isBefore(targetStart)
                        && repeatedDate.isBefore(targetEnd)
                        && repeatedDate.isAfter(baseDate)) {
                    addRepeatedExpenseIfNeeded(
                            repeatedList,
                            actualOccurrenceKeys,
                            generatedBaseDateKeys,
                            base,
                            repeatedDate);
                }
            }

            if ("WEEKLY".equals(repeat_cycle)) {
                LocalDate repeatedDate = baseDate.plusWeeks(1);

                while (repeatedDate.isBefore(targetStart)) {
                    repeatedDate = repeatedDate.plusWeeks(1);
                }

                while (repeatedDate.isBefore(targetEnd)) {
                    if (repeatedDate.isAfter(baseDate)) {
                        addRepeatedExpenseIfNeeded(
                                repeatedList,
                                actualOccurrenceKeys,
                                generatedBaseDateKeys,
                                base,
                                repeatedDate);
                    }
                    repeatedDate = repeatedDate.plusWeeks(1);
                }
            }

            if ("YEARLY".equals(repeat_cycle)
                    && baseDate.getMonth() == targetMonth.getMonth()) {

                int day = Math.min(baseDate.getDayOfMonth(), targetMonth.lengthOfMonth());
                LocalDate repeatedDate = targetMonth.atDay(day);

                if (repeatedDate.isAfter(baseDate)) {
                    addRepeatedExpenseIfNeeded(
                            repeatedList,
                            actualOccurrenceKeys,
                            generatedBaseDateKeys,
                            base,
                            repeatedDate);
                }
            }
        }

        return repeatedList;
    }

    /**
     * 실제 등록 내역과 겹치지 않고, 같은 원본의 같은 날짜가 아직 생성되지
     * 않았을 때만 자동 반복 내역을 추가한다.
     */
    private void addRepeatedExpenseIfNeeded(
            List<ExpenseDTO> repeatedList,
            Set<String> actualOccurrenceKeys,
            Set<String> generatedBaseDateKeys,
            ExpenseDTO base,
            LocalDate repeatedDate) {

        // 종료일을 지난 자동 반복 내역은 생성하지 않는다
        LocalDate repeatEndDate = toLocalDate(base.getRepeat_end_date());
        if (repeatEndDate != null && repeatedDate.isAfter(repeatEndDate)) {
            return;
        }

        String occurrenceKey = makeOccurrenceKey(base, repeatedDate);
        String baseDateKey = String.valueOf(base.getExpense_id()) + "|" + repeatedDate;

        if (actualOccurrenceKeys.contains(occurrenceKey)) {
            return;
        }

        if (!generatedBaseDateKeys.add(baseDateKey)) {
            return;
        }

        repeatedList.add(copyAsRepeatedExpense(base, repeatedDate));
    }

    /**
     * 수동 등록 내역과 자동 반복 내역이 같은 회차인지 비교하는 키다.
     * 제목의 앞뒤 공백과 대소문자 차이는 같은 값으로 처리
     */
    private String makeOccurrenceKey(ExpenseDTO expense, LocalDate expenseDate) {
        return String.valueOf(expense.getCategory_id())
                + "|" + normalizeOccurrenceText(expense.getExpense_title())
                + "|" + (expense.getAmount() == null ? 0 : expense.getAmount())
                + "|" + expenseDate;
    }

    private String normalizeOccurrenceText(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private ExpenseDTO copyAsRepeatedExpense(ExpenseDTO base, LocalDate repeatedDate) {
        ExpenseDTO repeated = new ExpenseDTO();

        repeated.setExpense_id(base.getExpense_id());
        repeated.setMember_id(base.getMember_id());
        repeated.setCategory_id(base.getCategory_id());
        repeated.setCategory_name(base.getCategory_name());
        repeated.setExpense_type(base.getExpense_type());
        repeated.setExpense_title(base.getExpense_title());

        repeated.setAmount(base.getAmount());
        repeated.setExpense_date(Date.valueOf(repeatedDate));
        repeated.setPayment_method(base.getPayment_method());
        repeated.setMemo(base.getMemo());

        repeated.setRepeat_yn(base.getRepeat_yn());
        repeated.setRepeat_cycle(base.getRepeat_cycle());
        // 자동 생성 행에도 원본 종료일을 유지
        repeated.setRepeat_end_date(base.getRepeat_end_date());
        repeated.setFixed_yn(base.getFixed_yn());
        repeated.setAuto_generated_yn("Y");
        repeated.setCreated_at(base.getCreated_at());
        repeated.setUpdated_at(base.getUpdated_at());

        return repeated;
    }

    private RowMapper<ExpenseDTO> expenseRowMapper() {
        return new RowMapper<>() {
            @Override
            public ExpenseDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                ExpenseDTO expense = new ExpenseDTO();

                expense.setExpense_id(rs.getLong("expense_id"));
                expense.setMember_id(rs.getLong("member_id"));
                expense.setCategory_id(rs.getLong("category_id"));
                expense.setCategory_name(rs.getString("category_name"));
                expense.setExpense_type(rs.getString("expense_type"));
                expense.setExpense_title(rs.getString("expense_title"));

                expense.setAmount(rs.getInt("amount"));
                expense.setExpense_date(rs.getDate("expense_date"));
                expense.setPayment_method(rs.getString("payment_method"));
                expense.setMemo(rs.getString("memo"));

                expense.setRepeat_yn(rs.getString("repeat_yn"));
                expense.setRepeat_cycle(rs.getString("repeat_cycle"));
                // DB 종료일을 DTO에 매핑한다
                expense.setRepeat_end_date(rs.getDate("repeat_end_date"));
                expense.setFixed_yn(rs.getString("fixed_yn"));
                expense.setAuto_generated_yn("N");
                expense.setCreated_at(rs.getTimestamp("created_at"));
                expense.setUpdated_at(rs.getTimestamp("updated_at"));

                return expense;
            }
        };
    }

    private RowMapper<ExpenseCategoryDTO> categoryRowMapper() {
        return new RowMapper<>() {
            @Override
            public ExpenseCategoryDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                ExpenseCategoryDTO category = new ExpenseCategoryDTO();

                category.setCategory_id(rs.getLong("category_id"));
                category.setCategory_name(rs.getString("category_name"));
                category.setExpense_type(rs.getString("expense_type"));
                category.setSort_order(rs.getInt("sort_order"));

                return category;
            }
        };
    }

    private Date toSqlDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime());
    }

    private LocalDate toLocalDate(java.util.Date date) {

        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        return date.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDate();
    }
}
