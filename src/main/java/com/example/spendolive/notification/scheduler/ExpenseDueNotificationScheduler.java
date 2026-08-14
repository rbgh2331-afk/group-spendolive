package com.example.spendolive.notification.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.spendolive.notification.domain.NotificationType;
import com.example.spendolive.notification.service.NotificationService;

/**
 * (캘린더) 지출 결제일 임박 알림.
 * 매일 정해진 시각에 고정/반복 지출(expense_tb.fixed_yn='Y' or repeat_yn='Y')을 훑어서
 * 다음 결제 예정일이 REMINDER_DAYS일 이내로 다가온 건에 대해 알림을 1건 생성
 * 같은 지출로 같은 날 중복 발송은 하지 않는다 (link_url + 오늘 날짜 기준으로 체크).
 *
 * Expense 패키지의 기존 조회 로직(ExpenseRepositoryImpl.selectExpenseList 등)을 건드리지 않기 위해
 * 이 스케줄러 안에서 필요한 조회만 별도 SQL로 직접 처리
 */
@Component
public class ExpenseDueNotificationScheduler {

    private static final int REMINDER_DAYS = 3; // 결제 예정일 D-3부터 알림 시작

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public ExpenseDueNotificationScheduler(JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    private static final String FIND_FIXED_OR_REPEAT_EXPENSE_SQL = """
            SELECT
                e.expense_id, m.id AS login_id, e.expense_title,
                e.expense_date, e.repeat_yn, e.repeat_cycle
            FROM expense_tb e
            JOIN member_tb m ON e.member_id = m.member_id
            WHERE e.fixed_yn = 'Y' OR e.repeat_yn = 'Y'
        """;

    // 같은 지출로 오늘 이미 알림을 보냈는지 확인 (link_url에 expenseId를 심어서 구분)
    private static final String EXISTS_TODAY_NOTIFIED_SQL = """
            SELECT COUNT(*) FROM notification_tb
            WHERE id = ? AND notification_type = ? AND link_url = ?
              AND TRUNC(created_at) = TRUNC(SYSDATE)
        """;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul") // 매일 오전 9시
    public void notifyUpcomingExpenseDue() {
        LocalDate today = LocalDate.now();

        List<ExpenseDueRow> rows = jdbcTemplate.query(FIND_FIXED_OR_REPEAT_EXPENSE_SQL, (rs, rowNum) -> {
            java.sql.Date d = rs.getDate("expense_date");
            return new ExpenseDueRow(
                    rs.getLong("expense_id"),
                    rs.getString("login_id"),
                    rs.getString("expense_title"),
                    (d != null) ? d.toLocalDate() : null,
                    rs.getString("repeat_yn"),
                    rs.getString("repeat_cycle")
            );
        });

        for (ExpenseDueRow row : rows) {
            if (row.baseDate == null || row.loginId == null || row.loginId.isBlank()) {
                continue;
            }

            LocalDate nextDue = calcNextDueDate(row.baseDate, row.repeatYn, row.repeatCycle, today);
            if (nextDue == null) {
                continue;
            }

            long daysUntil = ChronoUnit.DAYS.between(today, nextDue);
            if (daysUntil < 0 || daysUntil > REMINDER_DAYS) {
                continue;
            }

            String linkUrl = "/spendolive/calendar.do?expenseId=" + row.expenseId;

            Integer already = jdbcTemplate.queryForObject(
                    EXISTS_TODAY_NOTIFIED_SQL, Integer.class,
                    row.loginId, NotificationType.EXPENSE_DUE, linkUrl);
            if (already != null && already > 0) {
                continue;
            }

            String dueText = (daysUntil == 0) ? "오늘" : (daysUntil + "일 후(" + nextDue + ")");
            notificationService.createNotification(
                    row.loginId,
                    NotificationType.EXPENSE_DUE,
                    "지출 결제일이 다가와요",
                    "\"" + row.title + "\" 결제 예정일이 " + dueText + "입니다.",
                    linkUrl
            );
        }
    }

    /** 반복 주기를 고려해 오늘(today) 이후 가장 가까운 결제 예정일을 계산. 대상이 없으면 null. */
    private LocalDate calcNextDueDate(LocalDate baseDate, String repeatYn, String repeatCycle, LocalDate today) {
        if (!"Y".equals(repeatYn) || repeatCycle == null) {
            // 반복이 아닌 고정 지출(1회성) → 원래 날짜가 이미 지났으면 대상 아님
            return baseDate.isBefore(today) ? null : baseDate;
        }

        switch (repeatCycle) {
            case "MONTHLY": {
                LocalDate candidate = withDaySafe(today.withDayOfMonth(1), baseDate.getDayOfMonth());
                if (candidate.isBefore(today)) {
                    LocalDate nextMonth = today.plusMonths(1).withDayOfMonth(1);
                    candidate = withDaySafe(nextMonth, baseDate.getDayOfMonth());
                }
                return candidate;
            }
            case "WEEKLY": {
                LocalDate candidate = baseDate;
                while (candidate.isBefore(today)) {
                    candidate = candidate.plusWeeks(1);
                }
                return candidate;
            }
            case "YEARLY": {
                LocalDate candidate = withDaySafe(LocalDate.of(today.getYear(), baseDate.getMonth(), 1), baseDate.getDayOfMonth());
                if (candidate.isBefore(today)) {
                    candidate = withDaySafe(LocalDate.of(today.getYear() + 1, baseDate.getMonth(), 1), baseDate.getDayOfMonth());
                }
                return candidate;
            }
            default:
                return baseDate.isBefore(today) ? null : baseDate;
        }
    }

    /** 해당 월의 마지막 날을 넘지 않게 day를 보정해서 날짜를 만든다 (예: 31일 반복이 2월엔 28일로) */
    private LocalDate withDaySafe(LocalDate monthStart, int day) {
        int safeDay = Math.min(day, monthStart.lengthOfMonth());
        return monthStart.withDayOfMonth(safeDay);
    }

    private static final class ExpenseDueRow {
        private final Long expenseId;
        private final String loginId;
        private final String title;
        private final LocalDate baseDate;
        private final String repeatYn;
        private final String repeatCycle;

        private ExpenseDueRow(Long expenseId, String loginId, String title,
                               LocalDate baseDate, String repeatYn, String repeatCycle) {
            this.expenseId = expenseId;
            this.loginId = loginId;
            this.title = title;
            this.baseDate = baseDate;
            this.repeatYn = repeatYn;
            this.repeatCycle = repeatCycle;
        }
    }
}
