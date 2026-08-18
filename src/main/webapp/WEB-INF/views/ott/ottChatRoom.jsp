<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<%--
    OTT 채팅방 화면
    AJAX 폴링으로 메시지 조회 및 전송
--%>

<%-- 채팅방 기본정보 --%>
<section id="ottChatPage" class="page-hero ott-sub-hero"
         data-context-path="${contextPath}"
         data-room-id="${chatRoom.room_id}"
         data-message="${fn:escapeXml(msg)}">
    <div class="container ott-wide-container">
        <p class="eyebrow">SHARE CHAT</p>
        <h1><c:out value="${chatRoom.room_name}" /></h1>
        <p class="hero-text">
            ${chatRoom.service_name} 공유방 대화입니다. 결제 완료 후 참여 중인 멤버와 방장만 대화방에 들어올 수 있습니다.
        </p>
        <div class="ott-page-actions">
            <a href="${contextPath}/spendolive/ott/friends.do" class="btn btn-outline">내 공유방</a>
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=manage" class="btn btn-outline">참여방 관리</a>
        </div>
    </div>
</section>

<%-- 메시지 목록 및 전송 --%>
<section class="section compact ott-page-section">
    <div class="container ott-wide-container">
        <article class="card chat-room-page-card">
            <div class="chat-room-page-head">
                <div>
                    <p class="eyebrow">LIVE ROOM</p>
                    <h2>공유방 대화</h2>
                    <span>3초마다 새 메시지를 확인합니다.</span>
                </div>
                <div class="chat-room-page-meta">
                    <b>${chatRoom.current_member_count}/${chatRoom.member_limit}명</b>
                    <small>결제일 매월 ${chatRoom.billing_day}일</small>
                </div>
            </div>

            <%-- 메시지 유형별 화면 분기 --%>
            <div id="chatMessageList" class="chat-message-list-page">
                <c:choose>
                    <c:when test="${not empty chatMessageList}">
                        <c:forEach var="message" items="${chatMessageList}">
                            <c:choose>

                                <%-- 시스템 알림 --%>
                                <c:when test="${message.system_yn eq 'Y'}">
                                    <div class="chat-message-row system">
                                        <div class="chat-system-bubble">
                                            <strong>시스템 알림</strong>
                                            <p><c:out value="${message.message_content}" /></p>
                                            <small>${message.created_at}</small>
                                        </div>
                                    </div>
                                </c:when>

                                <%-- 일반 채팅 --%>
                                <c:otherwise>
                                    <div class="chat-message-row ${message.mine_yn eq 'Y' ? 'mine' : 'other'}">

                                        <div class="chat-message-bubble">
                                            <strong><c:out value="${message.sender_name}" /></strong>
                                            <p><c:out value="${message.message_content}" /></p>
                                            <small>${message.created_at}</small>
                                        </div>

                                        <%-- 상대방 메시지만 신고 가능 --%>
                                        <c:if test="${message.mine_yn ne 'Y'}">
                                            <button type="button"
                                                    class="btn btn-danger-outline mini reportSubmitButton"
                                                    data-reported_member_id="${fn:escapeXml(message.sender_id)}"
                                                    data-room_id="${chatRoom.room_id}"
                                                    data-chat_text="${fn:escapeXml(message.message_content)}">
                                                신고하기
                                            </button>
                                        </c:if>

                                    </div>
                                </c:otherwise>

                            </c:choose>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-box chat-empty-box">아직 대화가 없습니다. 첫 메시지를 보내보세요.</div>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- 메시지 전송 폼 --%>
            <form id="chatSendForm" action="${contextPath}/spendolive/ott/chat/send.do" method="post" class="chat-send-form">
                <input type="hidden" name="room_id" value="${chatRoom.room_id}">
                <input type="text" name="message_content" id="chatMessageInput" placeholder="메시지를 입력하세요" autocomplete="off" required>
                <button type="submit" class="btn btn-primary">전송</button>
            </form>
        </article>
    </div>
</section>
<jsp:include page="/WEB-INF/views/ott/popup.jsp" />
<script src="${contextPath}/resources/js/ott.js"></script>
<script src="${contextPath}/resources/js/report.js"></script>
