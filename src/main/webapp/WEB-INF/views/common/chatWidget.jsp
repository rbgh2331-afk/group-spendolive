<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 공통 채팅 위젯 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="chat-floating-widget">
    <input type="checkbox" id="chatWidgetToggle" class="chat-widget-check">

    <label for="chatWidgetToggle" class="chat-widget-button" aria-label="공유방 대화 목록 열기">
        <span>💬</span>
        <c:if test="${chatTotalUnreadCount gt 0}">
            <em>${chatTotalUnreadCount}</em>
        </c:if>
    </label>

    <div class="chat-widget-panel">
        <div class="chat-widget-head">
            <div>
                <p class="eyebrow">SHARE CHAT</p>
                <h3>공유방 대화</h3>
            </div>
            <label for="chatWidgetToggle" class="chat-widget-close" aria-label="공유방 대화 목록 닫기">×</label>
        </div>

        <c:choose>
            <c:when test="${isLogOn == true && not empty memberInfo}">
                <c:choose>
                    <c:when test="${not empty chatRoomSummaryList}">
                        <div class="chat-room-list">
                            <c:forEach var="room" items="${chatRoomSummaryList}">
                                <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="chat-room-item ${room.unread_count gt 0 ? 'unread' : ''}">
                                    <div class="chat-room-avatar">🎬</div>
                                    <div>
                                        <strong><c:out value="${room.room_name}" /></strong>
                                        <p><c:out value="${room.last_message}" /></p>
                                    </div>
                                    <c:if test="${room.unread_count gt 0}">
                                        <span class="chat-unread">${room.unread_count}</span>
                                    </c:if>
                                </a>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="chat-login-box">
                            <p>아직 참여 중인 OTT 공유방이 없습니다.</p>
                            <a href="${contextPath}/spendolive/ott.do" class="btn btn-primary full">OTT 공유 시작하기</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <div class="chat-login-box">
                    <p>로그인하면 내가 속한 공유방 대화와 읽지 않은 알림을 확인할 수 있습니다.</p>
                    <a href="${contextPath}/member/loginForm.do" class="btn btn-primary full">로그인하기</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>
