<%-- [AJAX 변경 주석] 가족방 생성·나가기·종료·정산 폼을 부분 갱신 가능한 AJAX 폼으로 표시했다. --%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<%--
    가족·지인 공유방 화면
    초대, 정산, 탈퇴 기능 제공
--%>

<%-- 페이지 상단 영역 --%>
<section id="ottFriendsPage" class="page-hero"
         data-context-path="${contextPath}"
         data-kakao-key="${fn:escapeXml(kakaoJavascriptKey)}">
    <div class="container ">
        <p class="eyebrow">FRIENDS SHARE ROOM</p>
        <h1>가족 · 지인 공유방</h1>
        <p class="hero-text">
            가족 또는 지인과 함께 쓸 OTT 공유방을 만들고, 초대 URL·QR·카카오톡 링크로 결제 화면까지 바로 연결합니다.
        </p>
        <div class="ott-page-actions">
            <a href="${contextPath}/spendolive/ott.do" class="btn btn-primary">OTT관리로 돌아가기</a>
            <a href="#createRoom" class="btn btn-primary">공유방 만들기</a>
        </div>
    </div>
</section>

<section class="section compact ott-page-section">
    <div class="container ott-wide-container">
        <%-- 가족방 이용 규칙 --%>
        <div class="ott-system-guide card">
            <strong>정산 규칙</strong>
            <ol>
                <li>${settlementGuide}</li>
                <li>가족방은 승인 없이 초대 URL 또는 QR을 통해 결제 화면으로 이동합니다.</li>
                <li>방 삭제 요청 시 이번 이용 기간까지만 유지되며, 다음 이용분 결제 완료 건은 자동 환불 처리됩니다.</li>
            </ol>
        </div>

        <%-- 참여 중인 가족방 목록 --%>
        <article class="card ott-tab-panel family-room-section">
            <div class="panel-header">
                <div>
                    <p class="eyebrow">FAMILY & FRIENDS ROOMS</p>
                    <h2>가족 지인과의 공유방</h2>
                </div>
                <span>${fn:length(myRoomList)}개</span>
            </div>

            <c:choose>
                <c:when test="${not empty myRoomList}">
                    <div class="ott-room-card-list family-room-list">
                        <c:forEach var="room" items="${myRoomList}" varStatus="roomStatus">
                            <div class="ott-room-card family-room-card">
                                <div class="room-index-badge">${roomStatus.count}</div>

                                <div class="family-room-info">
                                    <strong><c:out value="${room.room_name}" /></strong>
                                    <p>
                                        ${room.service_name} · ${room.plan_name} · ${room.current_member_count}/${room.member_limit}명 ·
                                        결제일 매월 ${room.billing_day}일
                                    </p>
                                    <small>
                                        1인 결제금액 <fmt:formatNumber value="${room.per_person_amount}" pattern="#,##0" />원
                                        <c:if test="${not empty room.invite_code}"> · 초대코드 ${room.invite_code}</c:if>
                                    </small>
                                    <c:if test="${room.status eq 'CLOSE_REQUESTED'}">
                                        <small class="danger-text">방 삭제 예약됨 · ${room.close_effective_date} 종료 예정</small>
                                    </c:if>
                                    <c:if test="${room.status eq 'REPLACE_RECRUITING'}">
                                        <small class="warn-text">미결제자 발생으로 대체 모집 중</small>
                                    </c:if>

                                    <%-- 방장 초대 공유 --%>
                                    <c:if test="${room.host_login_id eq loginId and not empty room.invite_code and room.status ne 'CLOSE_REQUESTED' and room.status ne 'CLOSED'}">
                                        <div class="invite-share-box" data-room-name="${fn:escapeXml(room.room_name)}">
                                            <strong>초대 링크 공유</strong>
                                            <small>URL 복사, QR 코드, 카카오톡 공유 중 하나로 초대할 수 있습니다. 링크를 타고 들어오면 결제 화면으로 이동합니다.</small>

                                            <div class="invite-url-row">
                                                <input type="text"
                                                       class="invite-url-input"
                                                       readonly
                                                       value="${pageContext.request.scheme}://${pageContext.request.serverName}:${pageContext.request.serverPort}${contextPath}/spendolive/ott/friends/invite.do?code=${room.invite_code}">
                                                <button type="button" class="btn btn-primary btn-mini invite-copy-btn">URL 복사</button>
                                            </div>

                                            <div class="invite-share-actions">
                                                <button type="button" class="btn btn-primary btn-mini invite-qr-btn">QR 코드 보기</button>
                                                <button type="button" class="btn btn-primary btn-mini invite-kakao-btn">카카오톡 공유</button>
                                            </div>

                                            <div class="invite-qr-box">
                                                <img alt="가족방 초대 QR 코드">
                                                <small>QR을 스캔하면 결제 화면으로 이동합니다.</small>
                                            </div>
                                        </div>
                                    </c:if>
                                </div>

                                <div class="family-room-actions">
                                    <span class="status-pill ${room.status}">${room.status}</span>
                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary btn-mini">대화방</a>

                                    <%-- 참여자별 방 관리 기능 --%>
                                    <c:if test="${room.host_login_id ne loginId and room.status ne 'CLOSE_REQUESTED' and room.status ne 'CLOSED'}">
                                        <c:choose>
                                            <c:when test="${room.leave_reserved_yn eq 'Y'}">
                                                <small class="warn-text">나가기 예약됨 · ${room.leave_scheduled_date} 자동 퇴장</small>
                                                <%-- OTT 상태 변경 폼은 실제 저장 성공 후 현재 OTT 본문만 다시 불러온다. --%>
                                                <form action="${contextPath}/spendolive/ott/room/leave-cancel.do" method="post" class="compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/leave-cancel.do" data-loading-message="나가기 예약을 취소하고 있습니다.">
                                                    <input type="hidden" name="room_id" value="${room.room_id}">
                                                    <input type="hidden" name="returnPage" value="friends">
                                                    <button type="submit" class="btn btn-outline btn-mini">예약 취소</button>
                                                </form>
                                            </c:when>
                                            <c:otherwise>
                                                <form action="${contextPath}/spendolive/ott/room/leave-reserve.do" method="post" class="compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/leave-reserve.do" data-ajax-confirm="다음 이용 회차부터 나가도록 예약할까요?" data-loading-message="나가기 예약을 처리하고 있습니다.">
                                                    <input type="hidden" name="room_id" value="${room.room_id}">
                                                    <input type="hidden" name="returnPage" value="friends">
                                                    <button type="submit" class="btn btn-primary btn-mini">나가기 예약</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:if>

                                    <c:if test="${room.host_login_id eq loginId and room.status ne 'CLOSE_REQUESTED' and room.status ne 'CLOSED'}">
                                        <form action="${contextPath}/spendolive/ott/room/close-request.do" method="post" class="room-close-form compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/close-request.do" data-ajax-confirm="방 종료를 예약할까요?" data-loading-message="방 종료 예약을 처리하고 있습니다.">
                                            <input type="hidden" name="room_id" value="${room.room_id}">
                                            <input type="hidden" name="returnPage" value="friends">
                                            <input type="hidden" name="close_reason" value="파티장 요청">
                                            <input type="text" name="close_notice" placeholder="종료 공지 입력">
                                            <button type="submit" class="btn btn-danger-outline btn-mini" onclick="return confirm('방 삭제 요청을 진행할까요? 이번 이용 기간 종료일까지 유지되고, 다음 이용분 결제 완료 건은 자동 환불됩니다.');">방 삭제 요청</button>
                                        </form>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-box">아직 가족 지인과 만든 공유방이 없습니다.</div>
                </c:otherwise>
            </c:choose>
        </article>

        <%-- 가족방 생성 폼 --%>
        <article id="createRoom" class="card ott-tab-panel">
            <div class="panel-header">
                <div>
                    <p class="eyebrow">CREATE ROOM</p>
                    <h2>공유방 만들기</h2>
                </div>
                <span>가족 · 지인 전용</span>
            </div>

            <form action="${contextPath}/spendolive/ott/friends/create.do" method="post" class="recruit-search-form ott-fixed-plan-form" data-room-mode="FRIEND" data-ajax-form data-ajax-action="/spendolive/ott/ajax/friends/create.do" data-loading-message="가족·지인 공유방을 개설하고 있습니다.">
                <label>
                    OTT 종류
                    <select name="ott_service_id" class="ott-service-select" required>
                        <option value="">선택</option>
                        <c:forEach var="service" items="${serviceList}">
                            <option value="${service.ott_service_id}"
                                    data-service-name="${service.service_name}"
                                    data-plan="${service.fixed_plan_name}"
                                    data-base-price="${service.base_price}"
                                    data-extra-fee="${service.extra_member_fee}"
                                    data-extra-count="${service.extra_member_count}"
                                    data-total-price="${service.default_price}"
                                    data-member-limit="${service.max_member_limit}"
                                    data-share-amount="${service.share_amount}"
                                    data-fee-amount="${service.fee_amount}"
                                    data-person-amount="${service.per_person_amount}">
                                ${service.service_name}
                            </option>
                        </c:forEach>
                    </select>
                </label>

                <label>
                    공유방 이름
                    <input type="text" name="room_name" placeholder="예: 우리 가족 Netflix 공유방">
                </label>

                <label>
                    결제일
                    <input type="number" name="billing_day" min="1" max="31" value="1" required>
                </label>

                <input type="hidden" name="plan_name" class="ott-plan-input">
                <input type="hidden" name="total_price" class="ott-total-price-input">
                <input type="hidden" name="member_limit" class="ott-member-limit-input">

                <div class="ott-fixed-plan-preview">
                    <strong>OTT를 선택하면 최고 멤버십 기준 금액이 자동 적용됩니다.</strong>
                    <p>구독종류, 전체 구독료, 최대 인원은 직접 입력하지 않고 서비스 규칙으로 고정됩니다.</p>
                </div>

                <button type="submit" class="btn btn-primary full ott-form-submit">공유방 생성</button>
            </form>
        </article>

        <%-- 정산 관리 영역 --%>
        <article class="card ott-tab-panel settlement-panel-wide family-settlement-panel">
            <div class="panel-header">
                <div>
                    <p class="eyebrow">SETTLEMENT</p>
                    <h2>정산 관리</h2>
                </div>
                <span>가족 지인 공유방만 표시</span>
            </div>

            <div class="settlement-stack">
                <%-- 개인 및 팀원 결제 상태 --%>
                <section class="settlement-wide-block">
                    <div class="settlement-sub-header">
                        <div>
                            <h3>정산 상태</h3>
                            <p>가족 지인 공유방의 결제 가능 기간, 마감일, 내 결제 상태를 확인합니다.</p>
                        </div>
                    </div>

                    <c:choose>
                        <c:when test="${not empty settlementList}">
                            <div class="status-list settlement-status-wide-list">
                                <c:forEach var="settlement" items="${settlementList}">
                                    <div class="status-row wide settlement-status-row">
                                        <span>
                                            <strong><c:out value="${settlement.room_name}" /></strong><br>
                                            <small>
                                                ${settlement.settlement_month} 이용분 · 결제기간 ${settlement.payment_start_date} ~ ${settlement.payment_close_date}<br>
                                                이용기간 ${settlement.service_start_date} ~ ${settlement.service_end_date}
                                            </small>
                                        </span>
                                        <em class="${settlement.status eq 'DONE' or settlement.status eq 'CONFIRMED' ? 'done' : 'wait'}">${settlement.status}</em>

                                        <c:if test="${settlement.my_role eq 'MEMBER'}">
                                            <div class="settlement-pay-box">
                                                <b><fmt:formatNumber value="${settlement.my_total_amount}" pattern="#,##0" />원</b>
                                                <small>${settlement.my_payment_status}</small>
                                                <c:if test="${settlement.my_payment_status eq 'UNPAID'}">
                                                    <form action="${contextPath}/spendolive/ott/settlement/pay.do" method="post" data-ajax-form data-ajax-action="/spendolive/ott/ajax/settlement/pay.do" data-ajax-confirm="정산 결제를 완료 처리할까요?" data-loading-message="정산을 처리하고 있습니다.">
                                                        <input type="hidden" name="payment_id" value="${settlement.payment_id}">
                                                        <input type="hidden" name="returnPage" value="friends">
                                                        <button type="submit" class="btn btn-primary btn-mini">결제 완료</button>
                                                    </form>
                                                </c:if>
                                            </div>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-box">아직 가족 지인 공유방 정산 내역이 없습니다.</div>
                        </c:otherwise>
                    </c:choose>

                    <c:if test="${not empty hostedSettlementPaymentList}">
                        <div class="team-status-box">
                            <h3>팀원별 정산 상태</h3>
                            <div class="team-payment-list">
                                <c:forEach var="payment" items="${hostedSettlementPaymentList}">
                                    <div class="team-payment-row">
                                        <span>
                                            <strong><c:out value="${payment.room_name}" /></strong>
                                            <small>${payment.settlement_month} 이용분 · <c:out value="${payment.member_name}" />(<c:out value="${payment.member_login_id}" />)</small>
                                        </span>
                                        <b><fmt:formatNumber value="${payment.total_amount}" pattern="#,##0" />원</b>
                                        <em class="${payment.payment_status eq 'PAID' or payment.payment_status eq 'CONFIRMED' ? 'done' : 'wait'}">${payment.payment_status}</em>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </c:if>
                </section>
            </div>
        </article>
    </div>
</section>

<script src="https://developers.kakao.com/sdk/js/kakao.js"></script>
<script src="${contextPath}/resources/js/ott.js" data-ajax-reload></script>
