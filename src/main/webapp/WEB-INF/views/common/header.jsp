    <%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
    <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
    <c:set var="contextPath" value="${pageContext.request.contextPath}" />
    <c:set var="requestURI" value="${pageContext.request.requestURI}" />

<c:choose>
    <c:when test="${memberInfo.role == 'ADMIN'}">
    <link rel="stylesheet" href="${contextPath}/resources/css/admin.css">
<header class="site-header">
        <div class="container header-inner">
            <a class="admin-brand" href="${contextPath}/spendolive/admin/main.do" aria-label="SpendOlive Admin Home">
                <div class="admin-logo">SO</div>
                <div class="brand-title">
                    <strong>SpendOlive</strong>
                    <span>ADMIN</span>
                </div>
            </a>
            <nav class="admin-nav">
                <a href="${contextPath}/spendolive/admin/main.do" data-nav="dashboard">대시보드</a>
                <a href="${contextPath}/admin/member/list.do" data-nav="member">회원관리</a>
                <a href="${contextPath}/admin/ott/list.do" data-nav="ott">OTT 관리</a>
                <a href="${contextPath}/admin/settlement/list.do" data-nav="settlement">정산관리</a>
                <a href="${contextPath}/admin/report/list.do" data-nav="report">신고관리</a>
                <a href="${contextPath}/admin/inquiry/list.do" data-nav="inquiry">문의관리</a>
                <a href="${contextPath}/admin/notice/list.do" data-nav="notice">공지사항 관리</a>
                
            </nav>
            <div class="admin-actions">
                <strong><a class="btn btn-light" href="${contextPath}/member/logout.do">로그아웃</a>
                        ${memberInfo.member_name}님</strong>
            </div>
        </div>
    </header>
    </c:when>
    <c:otherwise>
    <header class="site-header">
        <div class="container header-inner">
            <a href="${contextPath}/spendolive/main.do" class="logo">
                <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" style="width:42px;height:42px;border-radius:50%;object-fit:cover;">
                <span>SpendOlive</span>
            </a>

            <nav class="nav">
                    <div class="header-weather-wrap">
            <button type="button"
                id="weatherToggleBtn"
                class="header-bell"
                onclick="toggleWeatherDropdown(event)">
                <span class="bell-icon">🌞</span>
            </button>

            <div id="weatherDropdown" class="notif-dropdown">
                <div class="notif-dropdown-header">
                    <strong>날씨</strong>
                    <select id="weatherPageRegionSelect" class="weather-select-mini" onchange="loadWeatherPage()">
                    </select>
                </div>
        <div id="weatherPageResult" class="weather-dropdown-body">
            <span class="weather-page-loading">불러오는 중...</span>
        </div>
    </div>
</div>
                <div class="header-bell-wrap">
                    <button type="button"
                        id="bellToggleBtn"
                        class="header-bell ${fn:contains(requestURI, '/notice') ? 'active' : ''}"
                        onclick="toggleNotifDropdown(event)">

                        <span class="bell-icon">🔔</span>

                        <span id="notificationBadge"
                            class="notification-badge"
                            style="display:none;"></span>
                    </button>

                    <%-- 종 아이콘 클릭 시 뜨는 최근 알림 미리보기. 최근 5개는 그냥 보이고,
                         그 이상은 스크롤로 내려서 봄. 실제 목록/읽음처리는
                         notice.js의 loadNotificationList 로직을 그대로 재사용함 --%>
                    <div id="notifDropdown" class="notif-dropdown">
                        <div class="notif-dropdown-header">
                            <strong>알림</strong>
                            <a href="${contextPath}/spendolive/notice/center.do?tab=alert" onclick="return loginYn('notice', ${isLogOn})">전체보기</a>
                        </div>
                        <div id="notifDropdownList" class="notif-dropdown-list">
                            <div class="notif-dropdown-empty">불러오는 중...</div>
                        </div>
                    </div>
                </div>
            </nav>

            <div class="header-actions">
                <c:choose>
                    <c:when test="${isLogOn == true && not empty memberInfo}">
                    
                    <c:choose>
                        <c:when test="${empty memberInfo.account_status || memberInfo.account_status ne 'YES'}">
                            <a class="btn btn-light" href="${contextPath}/member/openBankingAuth.do">🏦 안전한 오픈뱅킹 계좌 연동하기</a>
                        </c:when>
                    </c:choose>
                        <strong><a class="btn btn-light" href="${contextPath}/member/logout.do">로그아웃</a>
                        ${memberInfo.member_name}님</strong>
                    </c:when>
                    <c:otherwise>
                        <a class="btn btn-light" href="${contextPath}/member/loginForm.do">로그인</a>
                        <a class="btn btn-primary" href="${contextPath}/member/signup.do">회원가입</a>
                    </c:otherwise>
                </c:choose>
                <div class="so-menu-wrapper">
                    <button type="button" class="so-menu-trigger-btn" id="soMenuBtn">
                        메뉴 ▾
                    </button>
                    
                    <ul class="so-menu-popover" id="soMenuPopover">

                        <!-- 알림 & 공지사항 -->
                        <li class="so-menu-item">
                            <a class="so-menu-link ${fn:contains(requestURI, '/notice') ? 'active' : ''}"
                            href="${contextPath}/spendolive/notice/center.do">
                                <strong>알림＆공지사항</strong>
                            </a>
                        </li>

                        <!-- 지출관리 -->
                        <li class="so-menu-item has-submenu">
                            <a class="so-menu-link ${fn:contains(requestURI, '/expense') or fn:contains(requestURI, '/calendar') ? 'active' : ''}"
                            href="${contextPath}/spendolive/expense/list.do" onclick="return loginYn('expense', ${isLogOn})">
                                <strong>지출관리</strong>
                                <span class="so-menu-arrow">◂</span>
                            </a>

                            <ul class="so-submenu">
                                <li>
                                    <a class="so-menu-link ${fn:contains(requestURI, '/calendar') ? 'active' : ''}"
                                    href="${contextPath}/spendolive/calendar.do">
                                        캘린더
                                    </a>
                                </li>
                            </ul>
                        </li>

                        <!-- OTT 관리 -->
                        <li class="so-menu-item has-submenu">
                            <a class="so-menu-link ${fn:contains(requestURI, '/ott') ? 'active' : ''}"
                            href="${contextPath}/spendolive/ott.do" onclick="return loginYn('ott', ${isLogOn})">
                                <strong>OTT관리</strong>
                                <span class="so-menu-arrow">◂</span>
                            </a>

                            <ul class="so-submenu">
                                <li>
                                    <a class="so-menu-link"
                                    href="${contextPath}/spendolive/ott/friends.do" onclick="return loginYn('ott', ${isLogOn})">
                                        가족 공유방
                                    </a>
                                </li>
                                <li>
                                    <a class="so-menu-link"
                                    href="${contextPath}/spendolive/ott/recruit.do" onclick="return loginYn('ott', ${isLogOn})">
                                        외부 공유방
                                    </a>
                                </li>
                                <li>
                                    <a class="so-menu-link"
                                    href="${contextPath}/spendolive/ott/recruit.do?tab=manage" onclick="return loginYn('ott', ${isLogOn})">
                                        참여방 관리
                                    </a>
                                </li>
                            </ul>
                        </li>

                        <!-- 고객센터 -->
                        <li class="so-menu-item has-submenu">
                            <a class="so-menu-link ${fn:contains(requestURI, '/faq') or fn:contains(requestURI, '/inquiry') or fn:contains(requestURI, '/report') ? 'active' : ''}"
                            href="${contextPath}/spendolive/faq/list.do">
                                <strong>고객센터</strong>
                                <span class="so-menu-arrow">◂</span>
                            </a>

                            <ul class="so-submenu">
                                <li>
                                    <a class="so-menu-link ${fn:contains(requestURI, '/inquiry') ? 'active' : ''}"
                                    href="${contextPath}/spendolive/inquiry/list.do" onclick="return loginYn('mypage', ${isLogOn})">
                                        문의하기
                                    </a>
                                </li>
                                <li>
                                    <a class="so-menu-link ${fn:contains(requestURI, '/faq') ? 'active' : ''}"
                                    href="${contextPath}/spendolive/faq/list.do">
                                        자주 묻는 질문
                                    </a>
                                </li>
                            
                            </ul>
                        </li>

                        <!-- 마이페이지 -->
                        <li class="so-menu-item has-submenu">
                            <a class="so-menu-link ${fn:contains(requestURI, '/mypage') ? 'active' : ''}"
                            href="${contextPath}/spendolive/mypage.do" onclick="return loginYn('mypage', ${isLogOn})">
                                <strong>마이페이지</strong>
                                <span class="so-menu-arrow">◂</span>
                            </a>

                            <ul class="so-submenu">
                                <li>
                                    <a class="so-menu-link"
                                    href="${contextPath}/member/openBankingIntegratedAuth.do">
                                        통합 계좌 연동
                                    </a>
                                    <a class="so-menu-link"
                                    href="${contextPath}/member/openBankingAuth.do">    
                                        계좌 연동
                                    </a>
                                </li>
                                <li>
                                    <a class="so-menu-link"
                                    href="javascript:void(0);"
                                    onclick="requestBillingAuth()">
                                        카드 등록
                                    </a>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </div>
                </strong>
            </div>
        </div>
    </header>
    </c:otherwise>
    </c:choose>
    <script src="https://js.tosspayments.com/v2/standard"></script>
 
    <script>
    (function() {
        var btn = document.getElementById('soMenuBtn');
        var popover = document.getElementById('soMenuPopover');
        
        if (btn && popover) {
            // 1. 버튼 클릭 시 열고 닫기
            btn.addEventListener('click', function(e) {
                e.stopPropagation(); // 바깥 클릭 이벤트와 겹치지 않게 방지
                popover.classList.toggle('is-open');
            });
            
            // 2. 메뉴창 바깥 아무 데나 누르면 닫히게 처리
            document.addEventListener('click', function(e) {
                if (!popover.contains(e.target) && !btn.contains(e.target)) {
                    popover.classList.remove('is-open');
                }
            }); 
        }
    })();
        const clientKey = "test_ck_yZqmkKeP8gBgMeYDwNpprbQRxB9l";
        const customerKey = "${memberInfo.id}";
        const tossPayments = TossPayments(clientKey);
       
        //@docs https://docs.tosspayments.com/sdk/v2/js#tosspaymentspayment
        //const payment = tossPayments.payment({ customerKey });
        // 비회원 결제
        const payment = tossPayments.payment({ customerKey })
        // ------ '카드 등록하기' 버튼 누르면 결제창 띄우기 ------  
        //@docs https://docs.tosspayments.com/sdk/v2/js#paymentrequestpayment
        async function requestBillingAuth() {
            const contextPath = "${contextPath}";
             if(customerKey === ""){
            alert("로그인해 주세요");
            }
            await payment.requestBillingAuth({
            method: "CARD", // 자동결제(빌링)는 카드만 지원합니다
            successUrl: window.location.origin + contextPath + "/payment/callback.do", 
            failUrl: window.location.origin + contextPath + "/payment/fail.do",
            customerEmail: '${memberInfo.email}',
            customerName: '${memberInfo.member_name}',
            });
        }
        var msg = "${msg}";
if(msg && msg !== "") {
    alert(msg);
}
    </script>
