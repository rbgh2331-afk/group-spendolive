<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 관리자 신고 관리 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="report" data-admin-title="신고관리">
    <section class="hero">
        <div>
            <div class="hero-kicker">Report Management</div>
            <h1>신고관리</h1>
            <p>신고 내용과 신고자·대상자를 확인하고 경고, 퇴출 또는 반려 결과를 기록합니다.</p>
        </div>
    </section>

    <c:if test="${not empty msg}"><div class="flash-ok"><c:out value="${msg}" /></div></c:if>

    <div class="admin-local-tabs" aria-label="신고 상태 필터">
        <button type="button" class="admin-local-tab active" data-admin-row-filter="all" data-filter-target="#adminReportTable">전체 신고</button>
        <button type="button" class="admin-local-tab" data-admin-row-filter="wait" data-filter-target="#adminReportTable">처리 대기</button>
        <button type="button" class="admin-local-tab" data-admin-row-filter="complete" data-filter-target="#adminReportTable">처리 완료</button>
    </div>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <div class="section-kicker">Report List</div>
                <h2>신고 목록 (<span id="reportVisibleCount">${empty reportList ? 0 : reportList.size()}</span>건)</h2>
                <p>필터를 눌러도 페이지를 다시 불러오지 않고 현재 표 안에서 결과가 바뀝니다.</p>
            </div>
            <input id="reportSearchInput" class="admin-search-input" type="search"
                   placeholder="신고자, 대상자, 내용 검색" data-search-table="#adminReportTable">
        </div>

        <c:choose>
            <c:when test="${empty reportList}">
                <div class="admin-empty-filter">등록된 신고가 없습니다.</div>
            </c:when>
            <c:otherwise>
                <div class="table-wrap">
                    <table id="adminReportTable" class="admin-table"
                           data-admin-filter-table="true"
                           data-search-input="reportSearchInput"
                           data-empty-target="#reportFilterEmpty"
                           data-count-target="#reportVisibleCount">
                        <thead>
                        <tr><th>번호</th><th>신고자</th><th>신고 대상</th><th>신고 내용</th><th>신고일</th><th>상태</th><th>처리</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach var="report" items="${reportList}" varStatus="status">
                            <tr data-row-status="${report.report_status}">
                                <td>${status.count}</td>
                                <td><c:out value="${report.reporter_id}" /></td>
                                <td><c:out value="${report.reported_member_id}" /></td>
                                <td><div class="admin-report-reason"><c:out value="${report.report_reason}" /></div></td>
                                <td><c:out value="${report.created_at}" /></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${report.report_status eq 'WAIT'}"><span class="badge yellow">WAIT</span></c:when>
                                        <c:otherwise><span class="badge green"><c:out value="${report.report_status}" /></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${report.report_status eq 'WAIT'}">
                                            <button type="button" class="mini-btn warning" data-report-process
                                                    data-report-id="${report.report_id}"
                                                    data-reported-member-id="${report.reported_member_id}"
                                                    data-report-reason="<c:out value='${report.report_reason}' />">처리</button>
                                        </c:when>
                                        <c:otherwise><span class="badge green">처리 완료</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div id="reportFilterEmpty" class="admin-empty-filter" hidden>선택한 조건에 해당하는 신고가 없습니다.</div>
            </c:otherwise>
        </c:choose>
    </section>


        <section id="commentArea" class="panel" hidden>
            <div class="panel-header">
                <div class="panel-title">
                    <div class="section-kicker">Process Result</div>
                    <h2>신고 처리 결과 작성</h2>
                    <p id="selectedReportReason" class="admin-form-note"></p>
                </div>
                <button type="button" class="btn ghost" data-report-process-cancel>닫기</button>
            </div>

            <input type="hidden" id="formReportMemberId" name="reported_member_id" value="">
            <input type="hidden" id="formReportId" name="report_id" value="">

            <div class="form-grid admin-report-form-grid">
                <div class="form-field">
                    <label for="reportResult">처리 상태</label>
                    <select class="form-input" name="result" id="reportResult">
                        <option value="1" >경고</option>
                        <option value="2">퇴출</option>
                        <option value="3">반려</option>
                    </select>
                </div>
                <div class="form-field">
                    <label for="adminComment">관리자 코멘트</label>
                    <textarea id="adminComment" name="admin_comment" class="form-textarea"
                              placeholder="처리 결과를 입력하세요." required></textarea>
                </div>
            </div>
            <div class="toolbar admin-toolbar-bottom">
                 <button type="button" class="mini-btn warning waringSubmitButton"
                        >처리</button>
            </div>
        </section>
</div>

<jsp:include page="/WEB-INF/views/ott/popup.jsp" />
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/report.js"></script>
