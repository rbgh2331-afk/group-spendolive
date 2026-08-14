<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- 관리자 결제 관리 --%>
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
        <a class="active" href="${contextPath}/admin/settlement/paymentlist.do">참여자 결제</a>
        <a href="${contextPath}/admin/settlement/paymentdetaillist.do">결제·환불 내역</a>
    </nav>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <div class="section-kicker">Member Payment</div>
                <h2>참여자 결제 목록 (<span id="paymentVisibleCount">${empty paymentList ? 0 : paymentList.size()}</span>건)</h2>
                <p>현재 상태 목록 안에서 방 ID, 참여자 ID와 상태를 검색할 수 있습니다.</p>
            </div>
            <div class="filter-pills">
                <a class="${param.status eq 'DONE' ? '' : 'active'}" href="${contextPath}/admin/settlement/paymentlist.do?status=READY">대기</a>
                <a class="${param.status eq 'DONE' ? 'active' : ''}" href="${contextPath}/admin/settlement/paymentlist.do?status=DONE">완료</a>
            </div>
        </div>

        <div class="admin-filter-toolbar">
            <div class="filter-pills" aria-label="결제 상태 안내">
                <span class="badge yellow">READY</span>
                <span class="badge green">DONE</span>
            </div>
            <input id="paymentSearchInput" class="admin-search-input" type="search"
                   placeholder="방 ID, 참여자 ID, 상태 검색"
                   aria-label="참여자 결제 검색"
                   data-search-table="#adminPaymentTable">
        </div>

        <c:choose>
            <c:when test="${empty paymentList}"><div class="admin-empty-filter">해당 조건의 참여자 결제 건이 없습니다.</div></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table id="adminPaymentTable" class="admin-table"
                           data-admin-filter-table="true"
                           data-search-input="paymentSearchInput"
                           data-empty-target="#paymentFilterEmpty"
                           data-count-target="#paymentVisibleCount">
                        <thead><tr><th>번호</th><th>방 ID</th><th>참여자 ID</th><th>결제 금액</th><th>결제 예정일</th><th>연체일</th><th>상태</th><th>처리</th></tr></thead>
                        <tbody>
                        <c:forEach var="member" items="${paymentList}" varStatus="s">
                            <tr>
                                <td>${s.count}</td>
                                <td>${member.room_id}</td>
                                <td><c:out value="${member.member_login_id}" /></td>
                                <td><fmt:formatNumber value="${member.pay_amount}" type="number" />원</td>
                                <td>${member.pay_day}일</td>
                                <td>${member.pay_late_day}일</td>
                                <td><span class="badge ${member.settlement_status eq 'DONE' ? 'green' : 'yellow'}"><c:out value="${member.settlement_status}" /></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${member.settlement_status eq 'DONE'}"><span class="badge green">결제 완료</span></c:when>
                                        <c:otherwise>
                                            <div class="table-actions">
                                                <button type="button"
                                                     class="btn btn-primary adminpaymentSubmitButton"
                                                      data-room_id="${member.room_id}"
                                                      data-member_login_id="${member.member_login_id}">정산금 받기</button>

                                                 <button type="button"
                                                       class="btn btn-primary adminlateSubmitButton"
                                                       data-room_id="${member.room_id}"
                                                       data-pay_late_day="${member.pay_late_day}"
                                                       data-member_login_id="${member.member_login_id}">하루 연기</button>
                                            </div>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div id="paymentFilterEmpty" class="admin-empty-filter" hidden>검색 조건에 해당하는 참여자 결제 건이 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>


<jsp:include page="/WEB-INF/views/payment/popup.jsp" />
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/payment.js"></script>
