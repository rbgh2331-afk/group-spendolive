<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- 관리자 정산 관리 --%>
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
        <a class="active" href="${contextPath}/admin/settlement/list.do">방장 정산</a>
        <a href="${contextPath}/admin/settlement/paymentlist.do">참여자 결제</a>
        <a href="${contextPath}/admin/settlement/paymentdetaillist.do">결제·환불 내역</a>
    </nav>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <div class="section-kicker">Host Settlement</div>
                <h2>방장 정산 목록 (<span id="settlementVisibleCount">${empty settlementList ? 0 : settlementList.size()}</span>건)</h2>
                <p>현재 상태 목록 안에서 방 ID, 방 이름, 방장 ID와 상태를 검색할 수 있습니다.</p>
            </div>
            <div class="filter-pills">
                <a class="${param.status eq 'DONE' ? '' : 'active'}" href="${contextPath}/admin/settlement/list.do?status=READY">대기</a>
                <a class="${param.status eq 'DONE' ? 'active' : ''}" href="${contextPath}/admin/settlement/list.do?status=DONE">완료</a>
            </div>
        </div>

        <div class="admin-filter-toolbar">
            <div class="filter-pills" aria-label="정산 상태 안내">
                <span class="badge yellow">READY</span>
                <span class="badge green">DONE</span>
            </div>
            <input id="settlementSearchInput" class="admin-search-input" type="search"
                   placeholder="방 ID, 방 이름, 방장 ID, 상태 검색"
                   aria-label="방장 정산 검색"
                   data-search-table="#adminSettlementTable">
        </div>

        <c:choose>
            <c:when test="${empty settlementList}"><div class="admin-empty-filter">해당 조건의 정산 건이 없습니다.</div></c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table id="adminSettlementTable" class="admin-table"
                           data-admin-filter-table="true"
                           data-search-input="settlementSearchInput"
                           data-empty-target="#settlementFilterEmpty"
                           data-count-target="#settlementVisibleCount">
                        <thead><tr><th>번호</th><th>방 ID</th><th>방 이름</th><th>방장 ID</th><th>정산 금액</th><th>결제일</th><th>상태</th><th>처리</th></tr></thead>
                        <tbody>
                        <c:forEach var="room" items="${settlementList}" varStatus="status">
                            <tr data-row-status="${room.settlement_status}">
                                <td>${status.count}</td>
                                <td>${room.room_id}</td>
                                <td><c:out value="${room.room_name}" /></td>
                                <td><c:out value="${room.host_login_id}" /></td>
                                <td><fmt:formatNumber value="${room.total_price}" type="number" />원</td>
                                <td>${room.billing_day}일</td>
                                <td><span class="badge ${room.settlement_status eq 'DONE' ? 'green' : 'yellow'}"><c:out value="${room.settlement_status}" /></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${room.settlement_status eq 'DONE'}"><span class="badge green">정산 완료</span></c:when>
                                        <c:otherwise>
                                            <button type="button"
                                                class="btn btn-primary adminsettlementSubmitButton"
                                                data-room_id="${room.room_id}" data-host_id="${room.host_login_id}">정산금 보내기</button>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div id="settlementFilterEmpty" class="admin-empty-filter" hidden>검색 조건에 해당하는 방장 정산 건이 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<jsp:include page="/WEB-INF/views/payment/popup.jsp" />
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/payment.js"></script>
