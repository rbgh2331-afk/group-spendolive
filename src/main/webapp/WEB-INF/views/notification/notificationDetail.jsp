<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />



<section class="section compact">
    <div class="container">

        <%-- 오류 메시지 --%>
        <c:if test="${not empty errorMsg}">
            <div class="notif-error-box">
                ⚠ <c:out value="${errorMsg}" />
                <div class="notif-error-actions">
                    <a href="${contextPath}/spendolive/notice/center.do" class="btn btn-primary">목록으로</a>
                </div>
            </div>
        </c:if>

        <%-- 정상 알림 상세 --%>
        <c:if test="${not empty notification}">
            <div class="notif-detail-card">

                <div class="notif-detail-top">
                    <c:choose>
                        <c:when test="${notification.notification_type == 'HOME'}">
                            <span class="notif-badge badge-home">공지</span>
                        </c:when>
                        <c:when test="${notification.notification_type == 'OTT'}">
                            <span class="notif-badge badge-ott">OTT</span>
                        </c:when>
                        <c:otherwise>
                            <span class="notif-badge badge-personal">개인</span>
                        </c:otherwise>
                    </c:choose>
                </div>

                <h1 class="notif-detail-title"><c:out value="${notification.title}" /></h1>

                <div class="notif-detail-meta">
                    <span>수신일 ${notification.created_at}</span>

                    <span>${notification.read_yn == 'Y' ? '읽음' : '안 읽음'}</span>

                </div>

                <div class="notif-detail-message"><c:out value="${notification.message}" /></div>

                <div class="notif-detail-actions">
                    <a href="${contextPath}/spendolive/notice/center.do?tab=alert" class="btn btn-primary">
                        알림 목록
                    </a>
                    <c:if test="${not empty notification.link_url}">
                        <a href="${contextPath}${notification.link_url}" class="btn btn-light">
                            관련 페이지 이동
                        </a>
                    </c:if>
                </div>
            </div>
        </c:if>

    </div>
</section>
