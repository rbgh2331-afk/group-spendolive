<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- 관리자 대시보드 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="dashboard" data-admin-title="대시보드">
    <section class="hero">
        <div>
            <div class="hero-kicker">Admin Dashboard</div>
            <h1>관리자 대시보드</h1>
            <p>회원, 공개 모집 파티, OTT 서비스, 신고와 문의 처리 대기 현황을 한눈에 확인합니다.</p>
        </div>
    </section>

    <c:if test="${not empty adminDashboardError}">
        <div class="flash-err"><c:out value="${adminDashboardError}" /></div>
    </c:if>

    <section class="stat-grid">
        <article class="stat-card">
            <div class="stat-icon">👤</div>
            <small>Total Members</small>
            <strong><fmt:formatNumber value="${adminDashboard.totalMemberCount}" pattern="#,##0" /></strong>
            <p>회원 상태와 권한을 포함한 전체 회원 수입니다.</p>
        </article>
        <article class="stat-card">
            <div class="stat-icon">👥</div>
            <small>Recruiting Parties</small>
            <strong><fmt:formatNumber value="${adminDashboard.recruitingPartyCount}" pattern="#,##0" /></strong>
            <p>현재 공개 모집 중인 외부 공유방 수입니다.</p>
        </article>
        <article class="stat-card">
            <div class="stat-icon">🎬</div>
            <small>OTT Services</small>
            <strong><fmt:formatNumber value="${adminDashboard.ottServiceCount}" pattern="#,##0" /></strong>
            <p>관리자 화면에 등록된 OTT 종류 수입니다.</p>
        </article>
        <article class="stat-card">
            <div class="stat-icon">⚠</div>
            <small>Pending Reports</small>
            <strong><fmt:formatNumber value="${adminDashboard.pendingReportCount}" pattern="#,##0" /></strong>
            <p>아직 처리하지 않은 신고 접수 건수입니다.</p>
        </article>
        <article class="stat-card">
            <div class="stat-icon">☏</div>
            <small>Pending Inquiries</small>
            <strong><fmt:formatNumber value="${adminDashboard.pendingInquiryCount}" pattern="#,##0" /></strong>
            <p>관리자 답변을 기다리는 문의 건수입니다.</p>
        </article>
    </section>

    <section class="content-grid">
        <section class="panel">
            <div class="panel-title">
                <div class="section-kicker">Quick Management</div>
                <h2>빠른 관리 메뉴</h2>
                <p>자주 사용하는 관리자 기능으로 바로 이동합니다.</p>
            </div>
            <div class="activity-list">
                <a class="activity-item" href="${contextPath}/admin/member/list.do">
                    <div><strong>회원관리</strong><span>회원 정보와 상태를 확인합니다.</span></div><span class="badge blue">이동</span>
                </a>
                <a class="activity-item" href="${contextPath}/admin/ott/list.do#manage">
                    <div><strong>OTT 목록·수정·숨김</strong><span>서비스 요금과 공유 정책을 관리합니다.</span></div><span class="badge green">이동</span>
                </a>
                <a class="activity-item" href="${contextPath}/admin/settlement/list.do">
                    <div><strong>정산관리</strong><span>방장 정산과 참여자 결제를 처리합니다.</span></div><span class="badge yellow">이동</span>
                </a>
            </div>
        </section>

        <aside class="panel">
            <div class="panel-title">
                <div class="section-kicker">Customer Center</div>
                <h2>고객센터 관리</h2>
                <p>문의 답변과 사용자 안내 내용을 관리합니다.</p>
            </div>
            <div class="activity-list">
                <a class="activity-item" href="${contextPath}/admin/inquiry/list.do"><div><strong>문의관리</strong><span>문의 확인 및 답변</span></div><span>›</span></a>
                <a class="activity-item" href="${contextPath}/spendolive/admin/faq/list.do"><div><strong>FAQ 관리</strong><span>질문·답변 등록 및 순서 변경</span></div><span>›</span></a>
                <a class="activity-item" href="${contextPath}/admin/notice/list.do"><div><strong>공지사항 관리</strong><span>공지 등록·수정·삭제</span></div><span>›</span></a>
            </div>
        </aside>
    </section>
</div>
