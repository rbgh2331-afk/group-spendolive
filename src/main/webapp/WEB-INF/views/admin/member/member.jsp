<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 관리자 회원 관리 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="member" data-admin-title="회원관리">
    <section class="hero">
        <div>
            <div class="hero-kicker">Member Management</div>
            <h1>회원관리</h1>
            <p>회원의 기본 정보, 가입 상태, 경고 횟수와 서비스 연동 상태를 한 화면에서 확인합니다.</p>
        </div>
    </section>

    <c:if test="${not empty msg}"><div class="flash-ok"><c:out value="${msg}" /></div></c:if>

    <div class="admin-local-tabs" aria-label="회원 상태 필터">
        <button type="button" class="admin-local-tab active" data-admin-row-filter="all" data-filter-target="#adminMemberTable">전체 회원</button>
        <button type="button" class="admin-local-tab" data-admin-row-filter="active" data-filter-target="#adminMemberTable">활동 회원</button>
        <button type="button" class="admin-local-tab" data-admin-row-filter="warning" data-filter-target="#adminMemberTable">경고·제한 회원</button>
        <button type="button" class="admin-local-tab" data-admin-row-filter="leave" data-filter-target="#adminMemberTable">탈퇴 회원</button>
    </div>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <div class="section-kicker">Member List</div>
                <h2>회원 목록 (<span id="memberVisibleCount">${empty memberList ? 0 : memberList.size()}</span>명)</h2>
                <p>검색과 상태 필터는 페이지 새로고침 없이 현재 목록 안에서 적용됩니다.</p>
            </div>
        </div>

        <div class="admin-filter-toolbar">
            <div class="filter-pills">
                <span class="badge green">ACTIVE</span>
                <span class="badge yellow">경고 보유</span>
                <span class="badge gray">LEAVE</span>
            </div>
            <input id="memberSearchInput" class="admin-search-input" type="search"
                   placeholder="아이디, 이름, 이메일, 전화번호 검색"
                   data-search-table="#adminMemberTable">
        </div>

        <c:choose>
            <c:when test="${empty memberList}">
                <div class="admin-empty-filter">등록된 회원이 없습니다.</div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table id="adminMemberTable" class="admin-table"
                           data-admin-filter-table="true"
                           data-search-input="memberSearchInput"
                           data-empty-target="#memberFilterEmpty"
                           data-count-target="#memberVisibleCount">
                        <thead>
                        <tr>
                            <th>번호</th><th>아이디</th><th>이름</th><th>닉네임</th><th>이메일</th>
                            <th>연락처</th><th>가입일</th><th>경고</th><th>상태</th><th>권한</th><th></th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="member" items="${memberList}" varStatus="status">
                            <tr data-row-status="${member.status}" data-warning-count="${member.warning_count}">
                                <td>${status.count}</td>
                                <td><c:out value="${member.id}" /></td>
                                <td><c:out value="${member.member_name}" /></td>
                                <td><c:out value="${member.nickname}" /></td>
                                <td><c:out value="${member.email}" /></td>
                                <td><c:out value="${member.phone}" /></td>
                                <td><c:out value="${member.created_at}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${member.warning_count > 0}"><span class="badge yellow">${member.warning_count}회</span></c:when>
                                        <c:otherwise><span class="badge gray">0회</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${member.status eq 'ACTIVE'}"><span class="badge green">ACTIVE</span><td><button type="button"
                                                    class="mini-btn warning adminmemberSubmitButton"
                                                    data-id="${member.id}">
                                                탈퇴
                                            </button></td></c:when>
                                        <c:when test="${member.status eq 'LEAVE'}"><span class="badge gray">LEAVE</span>
                                        </td></c:when>
                                        <c:otherwise><span class="badge yellow"><c:out value="${member.status}" /></span></td></c:otherwise>
                                    </c:choose>
                                <td></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div id="memberFilterEmpty" class="admin-empty-filter" hidden>선택한 조건에 해당하는 회원이 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<jsp:include page="/WEB-INF/views/member/popup.jsp" />
<script src="${contextPath}/resources/js/signup.js"></script>
