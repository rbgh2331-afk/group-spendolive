<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<section class="section compact">
    <div class="container">

        <%-- 오류 메시지 (notice=null 또는 서버 오류) --%>
        <c:if test="${not empty errorMsg}">
            <div class="notice-error-box">
                ⚠ <c:out value="${errorMsg}" />
                <div class="notice-error-actions">
                    <a href="${contextPath}/spendolive/notice/center.do?filter=${filter}" class="btn btn-primary">목록으로</a>
                </div>
            </div>
        </c:if>

        <%-- 정상 공지 상세 --%>
        <c:if test="${not empty notice}">
            <div class="card notice-detail-card">

                <div class="notice-detail-top">
                    <c:choose>
                        <c:when test="${notice.pinned_yn == 'Y'}">
                            <span class="chip notice-important">중요 공지</span>
                        </c:when>
                        <c:otherwise>
                            <span class="chip notice-normal">공지</span>
                        </c:otherwise>
                    </c:choose>

                    <button type="button" class="notice-star-btn ${notice.star_yn == 'Y' ? 'active' : ''}" id="detailStarBtn"
                            data-notice-id="${notice.notice_id}"
                            data-login="${not empty loginYn ? loginYn : false}"
                            data-star="${notice.star_yn == 'Y' ? 'Y' : 'N'}">
                        ${notice.star_yn == 'Y' ? '★' : '☆'}
                    </button>
                </div>

                <h1 class="notice-detail-title"><c:out value="${notice.title}" /></h1>

                <div class="notice-detail-info">
                    <span>작성자 <c:out value="${not empty notice.admin_id ? notice.admin_id : '관리자'}" /></span>

                    <span>등록일 ${notice.created_at}</span>
                </div>

                <div class="notice-detail-content"><c:out value="${notice.content}" /></div>

                <div class="notice-detail-actions">
                    <a href="${contextPath}/spendolive/notice/center.do?filter=${filter}" class="btn btn-primary">목록으로</a>
                </div>
            </div>
        </c:if>

    </div>
</section>

<script src="${contextPath}/resources/js/notice.js"></script>
<script src="${contextPath}/resources/js/noticeDetail.js"></script>
