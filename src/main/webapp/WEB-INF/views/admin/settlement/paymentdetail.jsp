<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="settlement" data-admin-title="정산관리">
    <section class="hero">
        <div>
            <div class="hero-kicker">Settlement Management</div>
            <h1>정산관리</h1>
            <p>방장 정산, 참여자 자동결제, 결제·환불 내역을 관리합니다. 각 세부 메뉴는 기존 페이지 이동 방식으로 열립니다.</p>
        </div>
    </section>

    <c:if test="${not empty msg}"><div class="flash-ok"><c:out value="${msg}" /></div></c:if>

    <nav class="admin-related-nav" aria-label="정산관리 세부 메뉴">
        <a href="${contextPath}/admin/settlement/list.do">방장 정산</a>
        <a href="${contextPath}/admin/settlement/paymentlist.do">참여자 결제</a>
        <a class="active" href="${contextPath}/admin/settlement/paymentdetaillist.do">결제·환불 내역</a>
    </nav>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <div class="section-kicker">Payment History</div>
                <h2>결제·환불 내역 (<span id="paymentDetailVisibleCount">${empty paymentdetailList ? 0 : paymentdetailList.size()}</span>건)</h2>
                <p>결제 ID, 회원 ID, 카드번호, 주문번호와 상태를 검색할 수 있습니다.</p>
            </div>
        </div>

        <div class="admin-filter-toolbar">
            <div class="filter-pills" aria-label="결제 상태 안내">
                <span class="badge green">결제 완료</span>
                <span class="badge gray">취소·환불</span>
            </div>
            <input id="paymentDetailSearchInput" class="admin-search-input" type="search"
                   placeholder="결제 ID, 회원 ID, 카드번호, 주문번호, 상태 검색"
                   aria-label="결제 및 환불 내역 검색"
                   data-search-table="#adminPaymentDetailTable">
        </div>

        <c:choose>
            <c:when test="${empty paymentdetailList}"><div class="admin-empty-filter">저장된 결제 내역이 없습니다.</div></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table id="adminPaymentDetailTable" class="admin-table"
                           data-admin-filter-table="true"
                           data-search-input="paymentDetailSearchInput"
                           data-empty-target="#paymentDetailFilterEmpty"
                           data-count-target="#paymentDetailVisibleCount">
                        <thead><tr><th>번호</th><th>결제 ID</th><th>회원 ID</th><th>카드번호</th><th>결제 금액</th><th>주문번호</th><th>상태</th><th>처리</th></tr></thead>
                        <tbody>
                        <c:forEach var="payment" items="${paymentdetailList}" varStatus="status">
                            <tr data-row-status="${payment.payment_status}">
                                <td>${status.count}</td>
                                <td>${payment.payment_id}</td>
                                <td><c:out value="${payment.id}" /></td>
                                <td><c:out value="${payment.card_number}" /></td>
                                <td><fmt:formatNumber value="${payment.total_amount}" type="number" />원</td>
                                <td><c:out value="${payment.orderId}" /></td>
                                <td><span class="badge ${payment.payment_status eq 'CANCELED' or payment.payment_status eq 'REFUNDED' ? 'gray' : 'green'}"><c:out value="${payment.payment_status}" /></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${payment.payment_status eq 'CANCELED' or payment.payment_status eq 'REFUNDED'}"><span class="badge gray">취소 완료</span></c:when>
                                        <c:otherwise>
                                            <button type="button" 
                                                    class="mini-btn warning adminrefundSubmitButton"
                                                    data-payment-key="${payment.paymentKey}"
                                                    data-payment-id="${payment.payment_id}"
                                                    data-id="${payment.id}"
                                                    data-settlement-id="${payment.settlement_id}"
                                                    data-total-amount="${payment.total_amount}">
                                                환불하기
                                            </button>
                                          </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div id="paymentDetailFilterEmpty" class="admin-empty-filter" hidden>검색 조건에 해당하는 결제·환불 내역이 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<jsp:include page="/WEB-INF/views/payment/popup.jsp" />
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/payment.js"></script>
