<%-- 지출 등록·수정·삭제·예산·월 이동 AJAX 영역 --%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />


<main>
    <section class="page-hero">
        <div class="container page-hero-grid">
            <div>
                <p class="eyebrow">EXPENSE MANAGER</p>
                <h1>지출관리</h1>
                <p class="hero-text">
                    고정지출, 변동지출, OTT지출을 분류별로 등록하고 월별 지출 내역을 확인합니다.
                </p>
                <div class="hero-buttons">
                    <a href="#expense-form" class="btn btn-primary btn-large">지출 등록</a>
                    <a href="#expense-list" class="btn btn-primary btn-large">월별 내역 보기</a>
                    <a href="${contextPath}/spendolive/calendar/main.do" class="btn btn-primary btn-large">캘린더</a>
                </div>
            </div>

            <div class="dashboard-preview">
                <div class="preview-header">
                    <span>월별 조회</span>
                    <strong>${selectedYearMonth.substring(5, 7)}월 요약</strong>
                </div>

                <%-- 선택한 달의 예산을 등록하거나 수정한다. --%>
                <%-- 지출 저장 기능은 AJAX 전용 주소만 사용하며 pageAjax.js가 제출을 가로챈다. --%>
                <form action="${contextPath}/spendolive/expense/budget/save.do"
                      method="post"
                      class="monthly-budget-form"
                      data-ajax-form
                      data-loading-message="예산을 저장하고 있습니다.">
                    <input type="hidden" name="budget_month" value="${selectedYearMonth}">

                    <label for="monthlyBudgetAmount">
                        ${selectedYearMonth.substring(5, 7)}월 예산
                    </label>

                    <div class="monthly-budget-input-row">
                        <div class="monthly-budget-input-wrap">
                            <input type="number"
                                   id="monthlyBudgetAmount"
                                   name="budget_amount"
                                   value="${monthlyBudget > 0 ? monthlyBudget : ''}"
                                   min="0"
                                   step="1000"
                                   placeholder="예산 금액 입력"
                                   required>
                            <span>원</span>
                        </div>

                        <button type="submit" class="btn btn-primary">
                            저장
                        </button>
                    </div>

                    <c:if test="${param.budgetSaved == 'Y'}">
                        <p class="monthly-budget-message">선택한 달의 예산이 저장되었습니다.</p>
                    </c:if>
                </form>

                <div class="category-list expense-type-summary-list">
                    <div>
                        <span class="dot fixed"></span>
                        <p>고정</p>
                        <strong>
                            <fmt:formatNumber value="${expenseTypeSummary.FIXED}" pattern="#,###" />원
                        </strong>
                    </div>
                    <div>
                        <span class="dot variable"></span>
                        <p>변동</p>
                        <strong>
                            <fmt:formatNumber value="${expenseTypeSummary.VARIABLE}" pattern="#,###" />원
                        </strong>
                    </div>
                    <div>
                        <span class="dot ott"></span>
                        <p>OTT</p>
                        <strong>
                            <fmt:formatNumber value="${expenseTypeSummary.OTT}" pattern="#,###" />원
                        </strong>
                    </div>

                </div>
            </div>
        </div>
    </section>

    <section id="expense-form" class="section compact">
        <div class="container">
            <div class="expense-layout">
                <div class="expense-form card">
                    <h3>빠른 지출 등록</h3>

                    <form action="${contextPath}/spendolive/expense/add.do" method="post" class="form-grid" data-ajax-form data-loading-message="지출을 등록하고 있습니다.">
                        <input type="hidden" name="yearMonth" value="${selectedYearMonth}">
                        <input type="hidden" id="repeat_yn" name="repeat_yn" value="N">
                        <input type="hidden" id="fixed_yn" name="fixed_yn" value="N">

                        <label>
                            분류
                            <select id="expense_type" name="expense_type" required>
                                <option value="">분류 선택</option>
                                <option value="FIXED">고정</option>
                                <option value="VARIABLE">변동</option>
                                <option value="OTT">OTT</option>
                            </select>
                        </label>

                        <label>
                            카테고리
                            <select id="category_id" name="category_id" required>
                                <option value="">먼저 분류를 선택하세요</option>
                                <c:forEach var="category" items="${categoryList}">
                                    <option value="${category.category_id}" data-type="${category.expense_type}">
                                        <c:out value="${category.category_name}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </label>

                        <label>
                            지출 제목
                            <input type="text" name="expense_title" placeholder="예: 월세, 점심 식사" required>
                        </label>

                        <label>
                            금액
                            <input type="number" name="amount" placeholder="예: 12000" min="0" required>
                        </label>

                        <label>
                            지출 날짜
                            <input type="date" name="expense_date" required>
                        </label>

                        <label>
                            결제 수단
                            <select name="payment_method">
                                <option value="">선택 안함</option>
                                <option value="CARD">카드</option>
                                <option value="CASH">현금</option>
                                <option value="TRANSFER">계좌이체</option>
                                <option value="KAKAO_PAY">카카오페이</option>
                                <option value="NAVER_PAY">네이버페이</option>
                            </select>
                        </label>

                        <label id="repeatCycleArea" class="expense-hidden">
                            반복 주기
                            <select id="repeat_cycle" name="repeat_cycle">
                                <option value="">반복 없음</option>
                                <option value="MONTHLY">매월</option>
                                <option value="WEEKLY">매주</option>
                                <option value="YEARLY">매년</option>
                            </select>
                        </label>

                        <%-- [고정지출 종료월] 고정 분류에서만 선택형 종료월을 표시한다. --%>
                        <label id="repeatEndMonthArea" class="expense-hidden">
                            고정지출 종료월
                            <input type="month" id="repeat_end_month" name="repeat_end_month">
                            <small>반복 주기를 선택한 경우 적용되며, 비워두면 계속 반복됩니다.</small>
                        </label>

                        <label>
                            메모
                            <input type="text" name="memo" placeholder="선택 입력">
                        </label>

                        <button type="submit" class="btn btn-primary full">
                            등록하기
                        </button>
                    </form>
                </div>

                <div id="expense-list" class="expense-table card">
                    <div class="row-title">
                        <div>
                            <h3>최근 지출 내역</h3>
                            <%-- [최근 지출 페이지 처리] 선택한 달의 지출을 한 페이지에 10개씩 표시한다. --%>
                            <p class="card-desc">선택한 달의 지출을 한 페이지에 10개씩 표시합니다.</p>
                        </div>

                        <%-- [공통 AJAX 로딩 적용] 사용자가 조회 월을 바꿀 때 본문만 갱신하고 지정 문구를 공통 팝업에 표시한다. --%>
                        <form action="${contextPath}/spendolive/expense/list.do" method="get" data-ajax-navigation data-loading-message="선택한 달의 지출 내역을 불러오고 있습니다.">
                            <input type="month"
                                   name="yearMonth"
                                   value="${selectedYearMonth}">
                        </form>
                    </div>
                    <div class="expense-filter-bar">
                        <select id="expenseTypeFilter">
                            <option value="">전체 분류</option>
                            <option value="FIXED">고정</option>
                            <option value="VARIABLE">변동</option>
                            <option value="OTT">OTT</option>
                        </select>

                        <select id="expenseCategoryFilter">
                            <option value="">전체 카테고리</option>

                            <c:forEach var="category" items="${categoryList}">
                                <option value="${category.category_id}" data-type="${category.expense_type}">
                                    <c:out value="${category.category_name}" />
                                </option>
                            </c:forEach>
                        </select>

                        <select id="expenseAmountSort">
                            <option value="">최신순</option>
                            <option value="DESC">금액 높은 순</option>
                            <option value="ASC">금액 낮은 순</option>
                        </select>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>날짜</th>
                                    <th>내용</th>
                                    <th>분류</th>
                                    <th>카테고리</th>
                                    <th>금액</th>
                                    <th>결제수단</th>
                                    <th>반복</th>
                                    <th>관리</th>
                                </tr>
                            </thead>

                            <tbody id="expenseRows">
                                <c:forEach var="expense" items="${expenseList}" varStatus="status">
                                    <fmt:formatDate var="expenseDateValue" value="${expense.expense_date}" pattern="yyyy-MM-dd" />
                                    <%-- [고정지출 종료월] 수정 입력창에 사용할 yyyy-MM 값을 만든다. --%>
                                    <fmt:formatDate var="repeatEndMonthValue" value="${expense.repeat_end_date}" pattern="yyyy-MM" />

                                    <%--
                                        자동 반복 내역은 같은 원본 expense_id를 공유할 수 있으므로
                                        발생 날짜를 붙여 화면 행 키를 고유하게 만든다.
                                    --%>
                                    <c:set var="expenseRowKey" value="${expense.expense_id}" />
                                    <c:if test="${expense.auto_generated_yn == 'Y'}">
                                        <c:set var="expenseRowKey" value="${expense.expense_id}_${expenseDateValue}" />
                                    </c:if>

                                    <tr class="expense-row"
                                    data-expense-id="${expenseRowKey}"
                                    data-source-expense-id="${expense.expense_id}"
                                    data-type="${expense.expense_type}"
                                    data-category="${expense.category_id}"
                                    data-amount="${expense.amount}"
                                    data-order="${status.index}">
                                        <td>
                                            <span class="view-mode">
                                                <fmt:formatDate value="${expense.expense_date}" pattern="yyyy.MM.dd" />
                                            </span>
                                            <input class="edit-mode edit-expense-date expense-hidden" form="editForm${expense.expense_id}" type="date" name="expense_date" value="${expenseDateValue}" onchange="syncEditRepeatEndMonthMin(this)" required>
                                        </td>

                                        <td>
                                            <span class="view-mode">
                                                <c:out value="${expense.expense_title}" />
                                                <c:if test="${expense.auto_generated_yn == 'Y'}">
                                                    <span class="tag">자동</span>
                                                </c:if>
                                            </span>
                                            <input class="edit-mode expense-hidden" form="editForm${expense.expense_id}" type="text" name="expense_title" value="${fn:escapeXml(expense.expense_title)}" required>
                                        </td>

                                        <td>
                                            <span class="view-mode">
                                                <c:choose>
                                                    <c:when test="${expense.expense_type == 'FIXED'}">고정</c:when>
                                                    <c:when test="${expense.expense_type == 'VARIABLE'}">변동</c:when>
                                                    <c:when test="${expense.expense_type == 'OTT'}">OTT</c:when>
                                                    <c:otherwise>${expense.expense_type}</c:otherwise>
                                                </c:choose>
                                            </span>
                                            <select class="edit-mode edit-expense-type expense-hidden" form="editForm${expense.expense_id}" name="expense_type" data-row-id="${expense.expense_id}" onchange="filterEditCategoriesFromSelect(this)" required>
                                                <option value="FIXED" ${expense.expense_type == 'FIXED' ? 'selected' : ''}>고정</option>
                                                <option value="VARIABLE" ${expense.expense_type == 'VARIABLE' ? 'selected' : ''}>변동</option>
                                                <option value="OTT" ${expense.expense_type == 'OTT' ? 'selected' : ''}>OTT</option>
                                            </select>
                                        </td>

                                        <td>
                                            <span class="view-mode"><c:out value="${expense.category_name}" /></span>
                                            <select class="edit-mode edit-category expense-hidden" form="editForm${expense.expense_id}" name="category_id" data-row-id="${expense.expense_id}" required>
                                                <c:forEach var="category" items="${categoryList}">
                                                    <option value="${category.category_id}" data-type="${category.expense_type}" ${category.category_id == expense.category_id ? 'selected' : ''}>
                                                        <c:out value="${category.category_name}" />
                                                    </option>
                                                </c:forEach>
                                            </select>
                                        </td>

                                        <td>
                                            <span class="view-mode">
                                                <fmt:formatNumber value="${expense.amount}" pattern="#,###" />원
                                            </span>
                                            <input class="edit-mode expense-hidden" form="editForm${expense.expense_id}" type="number" name="amount" value="${expense.amount}" min="0" required>
                                        </td>

                                        <td>
                                            <span class="view-mode">
                                                <c:choose>
                                                    <c:when test="${expense.payment_method == 'CARD'}">카드</c:when>
                                                    <c:when test="${expense.payment_method == 'CASH'}">현금</c:when>
                                                    <c:when test="${expense.payment_method == 'TRANSFER'}">계좌이체</c:when>
                                                    <c:when test="${expense.payment_method == 'KAKAO_PAY'}">카카오페이</c:when>
                                                    <c:when test="${expense.payment_method == 'NAVER_PAY'}">네이버페이</c:when>
                                                    <c:otherwise>-</c:otherwise>
                                                </c:choose>
                                            </span>
                                            <select class="edit-mode expense-hidden" form="editForm${expense.expense_id}" name="payment_method">
                                                <option value="" ${empty expense.payment_method ? 'selected' : ''}>선택 안함</option>
                                                <option value="CARD" ${expense.payment_method == 'CARD' ? 'selected' : ''}>카드</option>
                                                <option value="CASH" ${expense.payment_method == 'CASH' ? 'selected' : ''}>현금</option>
                                                <option value="TRANSFER" ${expense.payment_method == 'TRANSFER' ? 'selected' : ''}>계좌이체</option>
                                                <option value="KAKAO_PAY" ${expense.payment_method == 'KAKAO_PAY' ? 'selected' : ''}>카카오페이</option>
                                                <option value="NAVER_PAY" ${expense.payment_method == 'NAVER_PAY' ? 'selected' : ''}>네이버페이</option>
                                            </select>
                                        </td>

                                        <td>
                                            <span class="view-mode">
                                                <c:choose>
                                                    <c:when test="${expense.repeat_cycle == 'MONTHLY'}">매월</c:when>
                                                    <c:when test="${expense.repeat_cycle == 'WEEKLY'}">매주</c:when>
                                                    <c:when test="${expense.repeat_cycle == 'YEARLY'}">매년</c:when>
                                                    <c:otherwise>-</c:otherwise>
                                                </c:choose>
                                                <%-- [고정지출 종료월] 종료월이 있으면 반복 정보 아래에 함께 표시한다. --%>
                                                <c:if test="${expense.expense_type == 'FIXED' and not empty expense.repeat_end_date}">
                                                    <br><small>종료 ${repeatEndMonthValue}</small>
                                                </c:if>
                                            </span>
                                            <select class="edit-mode edit-repeat-cycle expense-hidden" form="editForm${expense.expense_id}" name="repeat_cycle" data-row-id="${expense.expense_id}" onchange="changeEditRepeatYnFromSelect(this)">
                                                <option value="" ${empty expense.repeat_cycle ? 'selected' : ''}>반복 없음</option>
                                                <option value="MONTHLY" ${expense.repeat_cycle == 'MONTHLY' ? 'selected' : ''}>매월</option>
                                                <option value="WEEKLY" ${expense.repeat_cycle == 'WEEKLY' ? 'selected' : ''}>매주</option>
                                                <option value="YEARLY" ${expense.repeat_cycle == 'YEARLY' ? 'selected' : ''}>매년</option>
                                            </select>
                                            <%-- [고정지출 종료월] 수정 모드에서 기존 종료월을 변경하거나 비울 수 있다. --%>
                                            <div class="edit-mode edit-repeat-end-area expense-hidden">
                                                <input type="month" class="edit-repeat-end-month" form="editForm${expense.expense_id}" name="repeat_end_month" value="${repeatEndMonthValue}">
                                            </div>
                                        </td>

                                        <td>
                                            <c:choose>
                                                <c:when test="${expense.auto_generated_yn == 'Y'}">
                                                    <%-- [고정지출 종료월] 자동 생성 행은 원본 등록월에서 종료월을 수정하도록 안내한다. --%>
                                                    <span class="tag">원본 등록월에서 수정</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <form id="editForm${expense.expense_id}" action="${contextPath}/spendolive/expense/modify.do" method="post" data-ajax-form data-loading-message="지출을 수정하고 있습니다.">
                                                        <input type="hidden" name="expense_id" value="${expense.expense_id}">
                                                        <input type="hidden" name="yearMonth" value="${selectedYearMonth}">
                                                        <input type="hidden" id="editRepeatYn${expense.expense_id}" name="repeat_yn" value="${expense.repeat_yn}">
                                                        <input type="hidden" id="editFixedYn${expense.expense_id}" name="fixed_yn" value="${expense.fixed_yn}">
                                                        <input type="hidden" form="editForm${expense.expense_id}" name="memo" value="${fn:escapeXml(expense.memo)}">
                                                    </form>

                                                    <div class="expense-action-buttons">
                                                        <button type="button" class="btn btn-primary edit-btn" onclick="changeEditMode(this)">수정</button>
                                                        <button type="submit" form="editForm${expense.expense_id}" class="btn btn-primary save-btn expense-hidden">완료</button>
                                                        <button type="button" class="btn btn-outline cancel-btn expense-hidden" onclick="cancelEditMode(this)">취소</button>

                                                        <form action="${contextPath}/spendolive/expense/delete.do" method="post" data-ajax-form data-ajax-confirm="이 지출 내역을 삭제하시겠습니까?" data-loading-message="지출을 삭제하고 있습니다.">
                                                            <input type="hidden" name="expense_id" value="${expense.expense_id}">
                                                            <input type="hidden" name="yearMonth" value="${selectedYearMonth}">
                                                            <button type="submit" class="btn btn-outline delete-btn">삭제</button>
                                                        </form>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>

                                <c:if test="${empty expenseList}">
                                    <tr>
                                        <td colspan="8" class="expense-empty-cell">
                                            선택한 달에 등록된 지출 내역이 없습니다.
                                        </td>
                                    </tr>
                                </c:if>
                                <tr id="expenseFilterEmpty" class="expense-hidden">
                                    <td colspan="8" class="expense-empty-cell">
                                        선택한 조건에 맞는 지출 내역이 없습니다.
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                    <%-- [최근 지출 페이지 처리]
                         분류·카테고리·금액 정렬 결과를 기준으로 JavaScript가 페이지 버튼을 구성한다.
                         기존 월별 전체 목록은 유지하므로 차트·요약 계산에는 영향을 주지 않는다.
                    --%>
                    <div id="expensePagination"
                         class="pagination expense-pagination expense-hidden"
                         aria-label="최근 지출 내역 페이지">
                        <button type="button"
                                class="pg-btn"
                                data-expense-page-direction="prev"
                                aria-label="이전 페이지">‹</button>

                        <div id="expensePageNumbers" class="expense-page-numbers"></div>

                        <button type="button"
                                class="pg-btn"
                                data-expense-page-direction="next"
                                aria-label="다음 페이지">›</button>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section id="expense-analytics" class="section compact">
        <div class="container">
            <div class="section-title">
                <p class="eyebrow">EXPENSE INSIGHT</p>
                <h2>지출 분석</h2>
                <p class="section-desc">
                    선택한 달을 기준으로 최근 3개월 지출 흐름, 카테고리별 지출 비중, 이번달 지출 랭킹을 확인합니다.
                </p>
            </div>

            <div class="analytics-grid">
                <div class="analytics-card card">
                    <h3>
                        월별 지출 차트
                        <small>최근 3개월</small>
                    </h3>

                    <div class="bar-chart expense-month-chart">
                        <c:forEach var="monthData" items="${monthChartList}">
                            <div class="bar-item ${monthData.month == selectedYearMonth ? 'active' : ''}">
                                <strong>
                                    <fmt:formatNumber value="${monthData.total}" pattern="#,###" />원
                                </strong>
                                <div class="bar-track">
                                    <div class="expense-chart-bar" style="--bar-height:${monthData.barPercent};"></div>
                                </div>
                                <span>${monthData.monthLabel}</span>
                            </div>
                        </c:forEach>
                    </div>
                </div>

                <div class="analytics-card card">
                    <h3>
                        카테고리별 지출 분석
                        <small>이번달 기준</small>
                    </h3>

                    <p class="card-desc">
                        총 지출 <strong><fmt:formatNumber value="${selectedMonthTotal}" pattern="#,###" />원</strong> 기준입니다.
                    </p>

                    <c:choose>
                        <c:when test="${empty categorySummaryList}">
                            <p class="empty-analytics">이번달 카테고리별 지출 데이터가 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <ul class="category-analysis-list">
                                <c:forEach var="categoryData" items="${categorySummaryList}">
                                    <li>
                                        <div class="category-analysis-head">
                                            <strong>${categoryData.category_name}</strong>
                                            <span>${categoryData.percent}%</span>
                                        </div>
                                        <div class="category-progress">
                                            <span class="category-progress-bar" style="--progress-width:${categoryData.percent};"></span>
                                        </div>
                                        <p>
                                            <fmt:formatNumber value="${categoryData.total}" pattern="#,###" />원
                                        </p>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="analytics-card card">
                    <h3>
                        이번달 지출 랭킹
                        <small>최대 10개</small>
                    </h3>

                    <c:choose>
                        <c:when test="${empty rankingList}">
                            <p class="empty-analytics">이번달 지출 랭킹 데이터가 없습니다.</p>
                        </c:when>
                        <c:otherwise>
                            <ol class="ranking-list expense-ranking-list">
                                <c:forEach var="ranking" items="${rankingList}">
                                    <li>
                                        <div>
                                            <strong><c:out value="${ranking.expense_title}" /></strong>
                                            <span><c:out value="${ranking.category_name}" /></span>
                                        </div>
                                        <em>
                                            <fmt:formatNumber value="${ranking.amount}" pattern="#,###" />원
                                        </em>
                                    </li>
                                </c:forEach>
                            </ol>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </section>

    <%-- [생필품 가격 비교] 한국소비자원 OpenAPI 조회 기능을 지출관리 하단에 별도 카드로 배치한다. --%>
    <section id="consumer-price-compare" class="section compact" data-context-path="${contextPath}">
        <div class="container">
            <div class="section-title">
                <p class="eyebrow">PUBLIC PRICE DATA</p>
                <h2>생필품 가격 비교</h2>
                <p class="section-desc">한국소비자원 조사자료를 기준으로 상품별 판매점 가격을 비교합니다.</p>
            </div>

            <div class="consumer-price-card card">
                <%-- [생필품 가격 비교] 상품명 검색 후 사용자가 정확한 상품을 선택한다. --%>
                <form id="consumerPriceSearchForm" class="consumer-price-search-form">
                    <label for="consumerProductKeyword">상품명 검색</label>
                    <div class="consumer-price-search-row">
                        <input type="search" id="consumerProductKeyword" placeholder="예: 우유, 라면, 세제" autocomplete="off" required>
                        <button type="submit" id="consumerProductSearchButton" class="btn btn-primary">조회</button>
                    </div>
                </form>

                <%-- [생필품 가격 비교] 조회 상태와 공공 API 오류를 같은 위치에서 안내한다. --%>
                <p id="consumerPriceMessage" class="consumer-price-message">상품명을 입력한 뒤 조회해주세요.</p>

                <%-- [생필품 가격 비교] 같은 검색어에 여러 상품이 있을 수 있어 선택 목록을 먼저 표시한다. --%>
                <div id="consumerProductResults" class="consumer-product-results expense-hidden" aria-live="polite"></div>

                <%-- [생필품 가격 비교] 선택 상품의 최저·평균·최고가와 판매점별 가격을 표시한다. --%>
                <div id="consumerPriceResults" class="consumer-price-results expense-hidden" aria-live="polite">
                    <div class="consumer-price-result-head">
                        <div>
                            <span id="consumerSelectedProduct" class="consumer-selected-product"></span>
                            <strong id="consumerInspectDay"></strong>
                        </div>
                        <small>자료 제공: 한국소비자원 참가격</small>
                    </div>

                    <div class="consumer-price-summary">
                        <div><span>최저가</span><strong id="consumerLowestPrice">-</strong></div>
                        <div><span>평균가</span><strong id="consumerAveragePrice">-</strong></div>
                        <div><span>최고가</span><strong id="consumerHighestPrice">-</strong></div>
                    </div>

                    <div class="consumer-price-table-wrap">
                        <table class="consumer-price-table">
                            <thead>
                                <tr>
                                    <th>판매점</th>
                                    <th>주소</th>
                                    <th>행사</th>
                                    <th>가격</th>
                                </tr>
                            </thead>
                            <tbody id="consumerStorePriceRows"></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>

</main>

<script src="${contextPath}/resources/js/expense.js" data-ajax-reload></script>
