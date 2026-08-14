<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<main class="calendar-page">
    <section class="page-hero">
        <div class="container">
            <p class="eyebrow">
                SPENDING CALENDAR
            </p>
            <h1>
                캘린더
            </h1>
            <p class="hero-text">
                월별로 넘겨보는 큰 달력입니다. 날짜에는 지출 금액과 카테고리만 간단히 보여줍니다.
            </p>
        </div>
    </section>
    <section class="section compact">
        <div class="container">
            <div class="section-title row-title">
                <div>
                    <p class="eyebrow">
                        MONTHLY VIEW
                    </p>
                    <h2>
                        월별 지출 캘린더
                    </h2>
                </div>
            </div>
            <div class="calendar-page-layout">

    <div class="calendar">
        <div class="calendar-header">
            <span id="calendarTitle"></span>
            <button class="detail-link" id="detailBtn">자세히보기</button>
        </div>
        <div id="calendar"></div>

        <%-- "자세히보기" 누르면 위 달력(#calendar)은 숨기고 이걸 대신 보여줌.
             FullCalendar listMonth뷰는 페이지네이션이 없어서, 사이드 패널(이번달 주요 지출)과
             같은 방식(직접 페이지 잘라서 보여주기)으로 자체 구현함 --%>
        <div id="calendarDetailList" class="calendar-detail-list is-hidden"></div>
    </div>

    <div class="side-column">
        <div class="calendar-controls">
            <button class="btn btn-light" onclick="changeMonth(-1)">
                ‹ 이전달
            </button>
            <button class="btn btn-primary" onclick="location.href='${contextPath}/spendolive/expense/list.do#expense-form'">
                + 지출등록
            </button>
            <button class="btn btn-light" onclick="changeMonth(1)">
                다음달 ›
            </button>
        </div>

        <aside class="card side-panel">
            <h3>오늘 할 일</h3>
            <div id="todayTodoList">
                <!-- calendar.js의 renderTodayTodo()가 이 안에 오늘 지출 예정 건을 채워줌 -->
            </div>
        </aside>

        <aside class="card side-panel">
            <h3>이번 달 주요 지출</h3>
            <div id="sideEventList">
                <!-- calendar.js의 renderSidePanel()이 이 안에 지출 목록을 채워줌 -->
            </div>
            <!-- 3개 넘으면 calendar.js가 이 아래에 1 2 3 숫자 페이지네이션 자동 생성 -->

            <button class="btn btn-primary full" onclick="location.href='${contextPath}/spendolive/expense/list.do'">
                지출관리에서 보기
            </button>
        </aside>
    </div>

</div>

        </div>
    </section>
</main>

<script>
    // isLogOn은 CommonModelAdvice에서 모든 페이지에 전역으로 내려주는 값
    const isLoggedIn = ${isLogOn};
</script>

<script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.21/index.global.min.js"></script>
<script src="${contextPath}/resources/js/calendar.js"></script>
