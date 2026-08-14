<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<c:set var="requestURI" value="${pageContext.request.requestURI}" />
<c:set var="isAdminPage" value="${fn:contains(body_page, '/admin/')}" />

<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SpendOlive | ${isAdminPage ? '관리자' : '지출관리 플랫폼'}</title>
    <script>
        window.contextPath = "${contextPath}";
    </script>
    <link rel="stylesheet" href="${contextPath}/resources/css/styles.css">
    <c:if test="${isAdminPage}">
        <link rel="stylesheet" href="${contextPath}/resources/css/admin.css">
    </c:if>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Jua&family=Noto+Sans+KR:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">
</head>
<body class="${isAdminPage ? 'admin-body' : ''}">
<c:choose>
    <c:when test="${isAdminPage}">
        <%-- 관리자 페이지는 사용자 헤더·푸터·챗봇을 사용하지 않는다. --%>
        <div id="adminAppShell" class="admin-app-shell">
            <jsp:include page="/WEB-INF/views/admin/common/adminSidebar.jsp" />

            <div class="admin-workspace">
                <div class="admin-mobile-bar">
                    <button type="button" class="admin-mobile-menu-button" data-admin-mobile-menu aria-label="관리자 메뉴 열기">☰</button>
                    <strong>SpendOlive Admin</strong>
                </div>

                <div id="adminSidebarBackdrop" class="admin-sidebar-backdrop" aria-hidden="true"></div>

                <main id="modern-content" class="admin-content-host">
                    <jsp:include page="${body_page}" />
                </main>
            </div>
        </div>

        <div class="toast" aria-live="polite"></div>
        <script>window.spendoliveContextPath = "${contextPath}";</script>
        <script src="${contextPath}/resources/js/admin.js"></script>
    </c:when>

    <c:otherwise>
        <%-- [내 담당 사용자 영역 AJAX]
             메인·지출·OTT 사용자·마이페이지에서만 공통 로딩과 부분 갱신을 사용한다.
             관리자 분기에는 연결하지 않아 담당 밖 화면의 동작을 변경하지 않는다. --%>
        <jsp:include page="/WEB-INF/views/common/ajaxLoading.jsp" />
        <script>window.spendoliveContextPath = "${contextPath}";</script>
        <script src="${contextPath}/resources/js/ajaxloading.js"></script>

        <%-- 일반 사용자 페이지는 기존 구조를 그대로 유지한다. --%>
        <div id="modern-wrapper">
            <header id="modern-header">
                <jsp:include page="/WEB-INF/views/common/header.jsp" />
            </header>

            <div id="modern-container">
                <main id="modern-content">
                    <jsp:include page="${body_page}" />
                </main>
            </div>

            <jsp:include page="/WEB-INF/views/common/chatWidget.jsp" />

            <footer id="modern-footer">
                <jsp:include page="/WEB-INF/views/common/footer.jsp" />
            </footer>
        </div>

        <%-- 페이지 전용 팝업/스크립트는 각 JSP에서만 로드한다. 공통 레이아웃에는 실제 공통 자원만 둔다. --%>
        <jsp:include page="/WEB-INF/views/common/font.jsp" />
        <jsp:include page="/WEB-INF/views/common/chatbotWidget.jsp" />
        <script src="${contextPath}/resources/js/app.js"></script>
        <script src="${contextPath}/resources/js/chatbot.js"></script>
        <%-- data-ajax-form/data-ajax-navigation이 있는 내 담당 화면에서만 요청을 가로챈다. --%>
        <script src="${contextPath}/resources/js/pageAjax.js"></script>
        <script src="${contextPath}/resources/js/bellIcon.js"></script>
        <script src="${contextPath}/resources/js/weather.js"></script>
    </c:otherwise>
</c:choose>
</body>
</html>
