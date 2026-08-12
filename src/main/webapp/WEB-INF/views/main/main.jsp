<%-- [AJAX 변경 주석] 메인 월 선택은 전체 페이지 이동 대신 공통 부분 갱신 대상으로 표시했다. --%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />


    <main>
        <%--
            메인 대시보드 데이터
            - 로그인 상태이면 선택한 달의 지출과 예산을 사용한다.
            - 비로그인 상태이면 JS에서 랜덤 금액을 보여준다.
        --%>
        <div id="mainDashboardData"
             data-login="${mainLoggedIn}"
             data-fixed="${mainFixedTotal}"
             data-variable="${mainVariableTotal}"
             data-ott="${mainOttTotal}"
             data-ott-settlement-count="${mainOttSettlementCount}"
             data-budget="${mainBudget}">
        </div>

        <section class="hero">
            <div class="container page-hero-grid">
                <div>
                    <p class="eyebrow">
                        EXPENSE · CALENDAR · OTT MANAGEMENT
                    </p>
                    <h1>
                        SpendOlive로
                        <br>
                        지출을 한눈에 관리하세요
                    </h1>
                    <p class="hero-text">
                        메인에서는 지출관리, 캘린더, OTT관리, 마이페이지를 간단히 확인하고, 각 메뉴에서 더 자세한 기능을 사용할 수 있습니다.
                    </p>
                    <div class="hero-buttons">
                        <a href="${contextPath}/spendolive/expense/list.do" onclick="return loginYn('expense', ${isLogOn})" class="btn btn-primary btn-large">
                            지출관리 바로가기
                        </a>
                        <a href="${contextPath}/spendolive/calendar.do" class="btn btn-primary btn-large">
                            캘린더 보기
                        </a>
                        </div>
                    <div class="hero-stats">
                        <div>
                            <strong>
                            <span id="mainTotalStat">1,284,000원</span>
                        </strong>
                        <span>
                            ${mainSelectedMonthLabel} 총 지출
                        </span>
                    </div>
                    <div>
                        <strong>
                        3개
                    </strong>
                    <span>
                        지출 구분
                    </span>
                </div>
                <div>
                    <strong>
                    <span id="mainOttSettlementStat">4건</span>
                </strong>
                <span>
                    OTT 정산
                </span>
            </div>
        </div>
    </div>
    <div class="dashboard-preview">
        <div class="preview-header">
            <div class="main-preview-title">
                <span>
                    ${mainSelectedMonthLabel} 요약
                </span>
                <strong>
                    <span id="dashboardTotalAmount">₩1,284,000</span>
                </strong>
            </div>

            <%-- 메인 그래프에 표시할 연월을 선택한다. --%>
            <c:if test="${mainLoggedIn}">
                <%-- 월 변경 시 전체 layout 대신 지정된 본문 영역만 다시 조회한다. --%>
                <%-- [공통 AJAX 로딩 적용] 월 선택은 전체 페이지 이동 대신 메인 콘텐츠만 갱신하며 공통 팝업을 사용한다. --%>
                <form action="${contextPath}/spendolive/main.do"
                      method="get"
                      class="main-month-select-form"
                      data-ajax-navigation
                      data-loading-message="선택한 달의 지출 현황을 불러오고 있습니다.">
                    <label for="mainYearMonth">조회 월</label>
                    <div>
                        <input type="month"
                               id="mainYearMonth"
                               name="yearMonth"
                               value="${mainSelectedYearMonth}">
                        <button type="submit" class="btn btn-primary">조회</button>
                    </div>
                </form>
            </c:if>
        </div>
    <div class="spend-ring" id="spendRing">
        <div class="ring-center">
            <span>
                예산 사용
            </span>
            <strong>
            <span id="budgetPercentText">72%</span>
        </strong>
    </div>
</div>
<div class="category-list">
    <div>
        <span class="dot fixed">
        </span>
        <p>
            고정지출
        </p>
        <strong>
        <span id="fixedExpenseAmount">620,000원</span>
    </strong>
</div>
<div>
    <span class="dot variable">
    </span>
    <p>
        변동지출
    </p>
    <strong>
    <span id="variableExpenseAmount">578,000원</span>
</strong>
</div>
<div>
    <span class="dot ott">
    </span>
    <p>
        OTT지출
    </p>
    <strong>
    <span id="ottExpenseAmount">86,000원</span>
</strong>
</div>
</div>
</div>
</div>
</section>
<section class="section compact">
    <div class="container">
        <div class="section-title">
            <p class="eyebrow">
                OVERVIEW
            </p>
            <h2>
                기능 한눈에 보기
            </h2>
        </div>
        <div class="grid-4">
            <article class="summary-card">
                <div class="icon">
                    💳
                </div>
                <h3>
                    지출관리
                </h3>
                <p>
                    고정지출, 변동지출, OTT지출을 나누어 등록하고 통계와 랭킹을 확인합니다.
                </p>
                <a href="${contextPath}/spendolive/expense/list.do" onclick="return loginYn('expense', ${isLogOn})" class="btn btn-primary full">
                    자세히 보기
                </a>
            </article>
            <article class="summary-card">
                <div class="icon">
                    📅
                </div>
                <h3>
                    캘린더
                </h3>
                <p>
                    월별 달력에서 날짜별 지출 금액과 카테고리, 지출관리를 편하게 확인합니다.
                </p>
                <a href="${contextPath}/spendolive/calendar.do" class="btn btn-primary full">
                    자세히 보기
                </a>
            </article>
            <article class="summary-card">
                <div class="icon">
                    🎬
                </div>
                <h3>
                    OTT관리
                </h3>
                <p>
                    지인과의 공유방, 다른 사람들과의 모집 게시판, 정산 요청을 관리합니다.
                </p>
                <a href="${contextPath}/spendolive/ott.do" onclick="return loginYn('ott', ${isLogOn})" class="btn btn-primary full">
                    자세히 보기
                </a>
            </article>
            <article class="summary-card">
                <div class="icon">
                    👤
                </div>
                <h3>
                    마이페이지
                </h3>
                <p>
                    나의 지출 현황, 정산 상태, 회원정보를 한 화면에서 확인합니다.
                </p>
                <a href="${contextPath}/spendolive/mypage.do" onclick="return loginYn('mypage', ${isLogOn})" class="btn btn-primary full">
                    자세히 보기
                </a>
            </article>
        </div>
    </div>
</section>
</main>


<script src="${contextPath}/resources/js/main.js" data-ajax-reload></script>
