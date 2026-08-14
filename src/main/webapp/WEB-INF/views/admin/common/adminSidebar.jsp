<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<aside id="adminSidebar" class="admin-sidebar" aria-label="관리자 메뉴">
    <div class="admin-sidebar-brand">
        <a href="${contextPath}/spendolive/admin/main.do" class="admin-sidebar-brand-link" aria-label="관리자 대시보드">
            <span class="admin-sidebar-logo">SO</span>
            <span class="admin-menu-text admin-sidebar-brand-text">
                <strong>SpendOlive</strong>
                <small>ADMIN</small>
            </span>
        </a>
        <button type="button" class="admin-sidebar-inner-toggle" data-admin-sidebar-toggle aria-label="사이드바 접기" aria-expanded="true">‹</button>
    </div>

    <nav class="admin-sidebar-nav">
        <a class="admin-sidebar-link" href="${contextPath}/spendolive/admin/main.do" data-admin-nav="dashboard">
            <span class="admin-menu-icon">⌂</span><span class="admin-menu-text">대시보드</span>
        </a>

        <section class="admin-sidebar-group" data-admin-group="member">
            <div class="admin-sidebar-main-row">
                <a class="admin-sidebar-link" href="${contextPath}/admin/member/list.do" data-admin-nav="member">
                    <span class="admin-menu-icon">👤</span><span class="admin-menu-text">회원관리</span>
                </a>
                <button type="button" class="admin-submenu-toggle" data-admin-submenu-toggle="member" aria-label="회원관리 하위 메뉴">⌄</button>
            </div>
            <div class="admin-sidebar-submenu" data-admin-submenu="member">
                <a href="${contextPath}/admin/member/list.do#all" data-admin-filter-link="all">전체 회원</a>
                <a href="${contextPath}/admin/member/list.do#active" data-admin-filter-link="active">활동 회원</a>
                <a href="${contextPath}/admin/member/list.do#warning" data-admin-filter-link="warning">경고·제한 회원</a>
                <a href="${contextPath}/admin/member/list.do#leave" data-admin-filter-link="leave">탈퇴 회원</a>
            </div>
        </section>

        <section class="admin-sidebar-group" data-admin-group="ott">
            <div class="admin-sidebar-main-row">
                <a class="admin-sidebar-link" href="${contextPath}/admin/ott/list.do" data-admin-nav="ott">
                    <span class="admin-menu-icon">🎬</span><span class="admin-menu-text">OTT 관리</span>
                </a>
                <button type="button" class="admin-submenu-toggle" data-admin-submenu-toggle="ott" aria-label="OTT 관리 하위 메뉴">⌄</button>
            </div>
            <div class="admin-sidebar-submenu" data-admin-submenu="ott">
                <a href="${contextPath}/admin/ott/list.do#manage" data-admin-section-link="manage">OTT 목록·수정·숨김</a>
                <a href="${contextPath}/admin/ott/list.do#edit" data-admin-section-link="form">OTT 수정</a>
                <a href="${contextPath}/admin/ott/list.do#add" data-admin-section-link="form">OTT 추가</a>
            </div>
        </section>

        <section class="admin-sidebar-group" data-admin-group="settlement">
            <div class="admin-sidebar-main-row">
                <a class="admin-sidebar-link" href="${contextPath}/admin/settlement/list.do" data-admin-nav="settlement">
                    <span class="admin-menu-icon">₩</span><span class="admin-menu-text">정산관리</span>
                </a>
                <button type="button" class="admin-submenu-toggle" data-admin-submenu-toggle="settlement" aria-label="정산관리 하위 메뉴">⌄</button>
            </div>
            <div class="admin-sidebar-submenu" data-admin-submenu="settlement">
                <a href="${contextPath}/admin/settlement/list.do">방장 정산</a>
                <a href="${contextPath}/admin/settlement/paymentlist.do">참여자 결제</a>
                <a href="${contextPath}/admin/settlement/paymentdetaillist.do">결제·환불 내역</a>
            </div>
        </section>

        <section class="admin-sidebar-group" data-admin-group="report">
            <div class="admin-sidebar-main-row">
                <a class="admin-sidebar-link" href="${contextPath}/admin/report/list.do" data-admin-nav="report">
                    <span class="admin-menu-icon">⚠</span><span class="admin-menu-text">신고관리</span>
                </a>
                <button type="button" class="admin-submenu-toggle" data-admin-submenu-toggle="report" aria-label="신고관리 하위 메뉴">⌄</button>
            </div>
            <div class="admin-sidebar-submenu" data-admin-submenu="report">
                <a href="${contextPath}/admin/report/list.do#all" data-admin-filter-link="all">전체 신고</a>
                <a href="${contextPath}/admin/report/list.do#wait" data-admin-filter-link="wait">처리 대기</a>
                <a href="${contextPath}/admin/report/list.do#complete" data-admin-filter-link="complete">처리 완료</a>
            </div>
        </section>

        <section class="admin-sidebar-group" data-admin-group="customer">
            <div class="admin-sidebar-main-row">
                <a class="admin-sidebar-link" href="${contextPath}/admin/inquiry/list.do" data-admin-nav="inquiry">
                    <span class="admin-menu-icon">☏</span><span class="admin-menu-text">고객센터</span>
                </a>
                <button type="button" class="admin-submenu-toggle" data-admin-submenu-toggle="customer" aria-label="고객센터 하위 메뉴">⌄</button>
            </div>
            <div class="admin-sidebar-submenu" data-admin-submenu="customer">
                <a href="${contextPath}/admin/inquiry/list.do" data-admin-nav-child="inquiry">문의관리</a>
                <a href="${contextPath}/spendolive/admin/faq/list.do" data-admin-nav-child="faq">FAQ 관리</a>
                <a href="${contextPath}/admin/notice/list.do" data-admin-nav-child="notice">공지사항 관리</a>
            </div>
        </section>
    </nav>

    <div class="admin-sidebar-foot">
        <div class="admin-sidebar-user">
            <span class="admin-menu-icon">A</span>
            <span class="admin-menu-text">
                <strong><c:out value="${empty memberInfo.member_name ? '관리자' : memberInfo.member_name}" /></strong>
                <small>관리자 계정</small>
            </span>
        </div>
        <a class="admin-sidebar-logout" href="${contextPath}/member/logout.do" title="로그아웃">
            <span aria-hidden="true">↪</span><span class="admin-menu-text">로그아웃</span>
        </a>
    </div>
</aside>
