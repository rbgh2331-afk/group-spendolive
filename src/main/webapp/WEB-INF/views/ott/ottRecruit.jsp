<%-- [AJAX 변경 주석] 검색·빠른 참가·방 생성·나가기·종료·정산을 AJAX 또는 부분 이동 대상으로 표시했다. --%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<%--
    외부 OTT 모집방 화면
    모집, 참여방, 정산 탭 제공
--%>

<%-- 페이지 상단 영역 --%>
<section class="page-hero">
    <div class="container ">
        <p class="eyebrow">OTT RECRUIT</p>
        <h1>모든 모집글</h1>
        <p class="hero-text">
            외부 다른 사람들과 함께 이용할 OTT 파티를 찾고, 신청 버튼을 통해 결제 화면으로 이동합니다.
        </p>
        <div class="ott-page-actions">
            <a href="${contextPath}/spendolive/ott.do" class="btn btn-primary">OTT관리로 돌아가기</a>
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=write" class="btn btn-primary">모집글 작성</a>
        </div>
    </div>
</section>

<section class="section compact ott-page-section">
    <div class="container ott-wide-container">
        <%-- 외부 모집방 이용 규칙 --%>
        <div class="ott-system-guide card">
            <strong>정산 규칙</strong>
            <ol>
                <li>${settlementGuide}</li>
                <li>신청 버튼은 결제 화면으로 연결되며, 결제 완료 후 참여방 입장이 처리됩니다.</li>
                <li>방 삭제 요청 상태의 파티는 신규 결제 입장과 다음 이용분 결제가 막히고, 결제 완료 건은 환불됩니다.</li>
            </ol>
        </div>

        <%-- 탭 화면 분기 --%>
        <div class="ott-tab-menu">
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=all" class="${tab eq 'all' ? 'active' : ''}">모든 모집글</a>
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=write" class="${tab eq 'write' ? 'active' : ''}">모집글 작성</a>
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=manage" class="${tab eq 'manage' or tab eq 'apply' ? 'active' : ''}">참여방 관리</a>
            <a href="${contextPath}/spendolive/ott/recruit.do?tab=settlement" class="${tab eq 'settlement' ? 'active' : ''}">정산 관리</a>
        </div>

        <div>
            <c:choose>
                <%-- 전체 모집글 탭 --%>
                <c:when test="${tab eq 'all'}">
                    <%--
                        빠른 참가 영역
                        위치: 상단 탭 메뉴 아래, 모든 모집글 목록 카드 위
                        역할: 사용자가 OTT 종류만 선택하면 서버에서 가장 오래된 빈 모집방을 자동으로 찾아 입장 처리한다.
                        주의: 이 JSP는 화면과 요청 전송만 담당한다.
                             실제 방 찾기/구성원 저장 로직은 Controller → Service → Repository에서 처리한다.
                    --%>
                    <article class="card ott-tab-panel">
                        <div class="panel-header">
                            <div>
                                <p class="eyebrow">QUICK JOIN</p>
                                <h2>빠른 참가</h2>
                                <p>
                                    원하는 OTT를 선택하면 오래된 모집방 중 자리가 비어있는 방부터 자동으로 참여합니다.
                                </p>
                            </div>
                        </div>

                        <%--
                            빠른 참가 form

                            일반 신청하기와 차이점:
                            - 일반 신청하기는 특정 방 카드 안에서 누르기 때문에 roomId를 바로 보낼 수 있다.
                            - 빠른 참가는 사용자가 방을 직접 고르는 게 아니라 OTT만 고른다.
                            - 그래서 여기서는 roomId를 보내지 않고 ottServiceId만 보낸다.

                            이후 서버 흐름:
                            1. ottServiceId를 Controller로 보냄
                            2. Service/Repository에서 해당 OTT의 모집방 중 가장 오래된 빈 방을 찾음
                            3. 찾은 방의 roomId를 결제쪽으로 넘김
                            4. 결제쪽 개발자는 기존 신청하기처럼 roomId만 받아서 처리하면 됨
                        --%>
                        <%-- 빠른 참가는 서버에서 참가 가능한 방을 먼저 확인한 뒤 기존 결제 화면 주소를 받는다. --%>
                        <form action="${contextPath}/spendolive/ott/recruit/quick-join.do"
                            method="post"
                            class="quick-join-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/recruit/quick-join.do" data-loading-message="참가 가능한 방을 확인하고 있습니다.">

                                <%--
                                    빠른 참가에서는 roomId가 없다.
                                    사용자가 방을 고른 게 아니라 OTT만 고른 상태이기 때문이다.

                                    여기서 선택한 ottServiceId를 서버로 보내면,
                                    서버가 이 OTT에 해당하는 빈 모집방을 자동으로 찾는다.
                                --%>
                            <div class="quick-join-grid">
                                <label>
                                    OTT 종류
                                    <select name="ott_service_id" class="form-control ott-service-select" required>
                                        <option value="">OTT 선택</option>

                                <%--
                                    serviceList는 Controller에서 model에 담아준 OTT 목록이다.
                                    value="${service.ott_service_id}" 값이 서버로 넘어간다.
                                --%>
                                    <c:forEach var="service" items="${serviceList}">
                                        <option value="${service.ott_service_id}">
                                            ${service.service_name}
                                        </option>
                                    </c:forEach>
                                </select>
                            </label>

                            <%--
                                빠른 참가 버튼 클릭 시:
                                /spendolive/ott/recruit/quick-join.do 로 POST 요청이 간다.
                                이때 넘어가는 값은 ott_service_id 하나다.
                            --%>
                                <div class="recruit-search-actions quick-join-actions">
                                    <button type="submit" class="btn btn-primary">
                                        빠른 참가
                                    </button>
                                </div>
                            </div>
                        </form>
                    </article>

                    <br>

                    <%-- 모집글 검색 및 목록 --%>
                    <article class="card ott-tab-panel">
                        <div class="panel-header">
                            <div>
                                <p class="eyebrow">ALL POSTS</p>
                                <h2>모든 모집글</h2>
                            </div>
                            <span>${totalRecruitRoomCount}개</span>
                        </div>

                        <%-- [공통 AJAX 로딩 적용] 사용자가 검색을 실행할 때만 팝업을 띄우고 모집글 영역을 부분 갱신한다. --%>
                        <form action="${contextPath}/spendolive/ott/recruit.do" method="get" class="recruit-search-form" data-ajax-navigation data-loading-message="모집글을 검색하고 있습니다.">
                            <input type="hidden" name="tab" value="all">
                        <div>
                            <label>
                                <span>OTT 종류</span>
                                <select name="ott_service_id">
                                    <option value="">전체 OTT</option>
                                    <c:forEach var="service" items="${serviceList}">
                                        <option value="${service.ott_service_id}" ${selectedOttServiceId eq service.ott_service_id ? 'selected' : ''}>
                                            ${service.service_name}
                                        </option>
                                    </c:forEach>
                                </select>
                            </label>

                            </div>
                            <div>
                            <label>
                                <span>방 제목</span>
                                <input type="text" name="roomNameKeyword" value="${fn:escapeXml(roomNameKeyword)}" placeholder="예) 모집합니다">
                            </label>
                         </div>
                          <div>
                                <button type="submit" class="btn btn-primary">검색</button>
                                <a href="${contextPath}/spendolive/ott/recruit.do?tab=all" class="btn btn-danger-outline">초기화</a>
                            </div>
                        </form>

                        <c:if test="${not empty selectedOttServiceId or not empty roomNameKeyword}">
                            <div class="recruit-search-result-text">
                                검색 조건에 맞는 모집글 <strong>${totalRecruitRoomCount}</strong>개가 조회되었습니다.
                            </div>
                        </c:if>

                        <c:choose>
                            <c:when test="${not empty recruitRoomList}">
                                <div class="recruit-card-grid">
                                    <c:forEach var="room" items="${recruitRoomList}">

                                        <div class="recruit-card ${room.host_login_id eq loginId ? 'my-recruit-card' : ''}">

                                            <div class="recruit-card-head">
                                                <div>
                                                    <h3><c:out value="${room.room_name}" /></h3>
                                                    <p>${room.service_name} · ${room.plan_name}</p>
                                                </div>
                                                <div class="recruit-card-badges">

                                                    <c:if test="${room.host_login_id eq loginId}">

                                                        <span class="owner-badge">내가 만든 방</span>
                                                    </c:if>
                                                    <span class="status-pill ${room.status}">${room.status}</span>
                                                </div>
                                            </div>

                                            <div class="recruit-info-grid">
                                                <div>
                                                    <span>모집 인원</span>
                                                    <strong>${room.current_member_count}/${room.member_limit}명</strong>
                                                </div>
                                                <div>
                                                    <span>월 총액</span>
                                                    <strong><fmt:formatNumber value="${room.total_price}" pattern="#,##0" />원</strong>
                                                </div>
                                                <div>
                                                    <span>1인 결제금액</span>
                                                    <strong><fmt:formatNumber value="${room.per_person_amount}" pattern="#,##0" />원</strong>
                                                    <small>분담금 <fmt:formatNumber value="${room.share_amount}" pattern="#,##0" />원 + 수수료 3%</small>
                                                </div>
                                                <div>
                                                    <span>결제일</span>
                                                    <strong>매월 ${room.billing_day}일</strong>
                                                </div>
                                            </div>

                                            <%-- 사용자 상태별 버튼 분기 --%>
                                            <c:choose>

                                                <c:when test="${room.host_login_id eq loginId}">
                                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary full">내 모집글 대화방</a>

                                                </c:when>
                                                <c:when test="${(room.status eq 'RECRUITING' or room.status eq 'REPLACE_RECRUITING') and room.my_application_status eq 'NONE'}">
                                                    <form action="${contextPath}/payment/detail.do" method="post">
                                                        <input type="hidden" name="room_id" value="${room.room_id}">
                                                        <input type="hidden" name="room_mode" value="RECRUIT">
                                                        <button type="submit" class="btn btn-primary full recruit-apply-button">신청하기</button>
                                                    </form>
                                                </c:when>
                                                <c:when test="${room.my_application_status eq 'ACTIVE'}">
                                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}"  class="btn btn-primary full">대화방 입장</a>
                                                </c:when>
                                                <c:otherwise>
                                                    <button type="button" class="btn btn-primary full" disabled>${room.my_application_status}</button>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </c:forEach>
                                </div>

                                <%-- 기존 문의 목록의 pagination / pg-btn / pg-ellipsis 구조를 재사용한다. --%>
                                <c:if test="${totalPages gt 1}">
                                    <c:set var="pgStart" value="${currentPage - 2 < 1 ? 1 : currentPage - 2}" />
                                    <c:set var="pgEnd" value="${currentPage + 2 > totalPages ? totalPages : currentPage + 2}" />

                                    <form action="${contextPath}/spendolive/ott/recruit.do"
                                          method="get"
                                          class="pagination"
                                          data-ajax-navigation
                                          data-loading-message="모집글을 불러오고 있습니다.">
                                        <input type="hidden" name="tab" value="all">
                                        <c:if test="${not empty selectedOttServiceId}">
                                            <input type="hidden" name="ott_service_id" value="${selectedOttServiceId}">
                                        </c:if>
                                        <c:if test="${not empty roomNameKeyword}">
                                            <input type="hidden" name="roomNameKeyword" value="${fn:escapeXml(roomNameKeyword)}">
                                        </c:if>

                                        <c:if test="${pgStart gt 1}">
                                            <button type="submit" class="pg-btn" name="page" value="1">1</button>
                                            <c:if test="${pgStart gt 2}">
                                                <span class="pg-ellipsis">…</span>
                                            </c:if>
                                        </c:if>

                                        <c:forEach begin="${pgStart}" end="${pgEnd}" var="p">
                                            <button type="submit"
                                                    class="pg-btn ${p eq currentPage ? 'active' : ''}"
                                                    name="page"
                                                    value="${p}"
                                                    ${p eq currentPage ? 'disabled' : ''}>${p}</button>
                                        </c:forEach>

                                        <c:if test="${pgEnd lt totalPages}">
                                            <c:if test="${pgEnd lt totalPages - 1}">
                                                <span class="pg-ellipsis">…</span>
                                            </c:if>
                                            <button type="submit" class="pg-btn" name="page" value="${totalPages}">${totalPages}</button>
                                        </c:if>
                                    </form>
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                <div class="empty-box">
                                    <c:choose>
                                        <c:when test="${not empty selectedOttServiceId or not empty roomNameKeyword}">검색 조건에 맞는 모집글이 없습니다.</c:when>
                                        <c:otherwise>현재 모집글이 없습니다.</c:otherwise>
                                    </c:choose>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </article>
                </c:when>

                <%-- 참여방 관리 탭 --%>
                <c:when test="${tab eq 'manage' or tab eq 'apply'}">
                    <article class="card ott-tab-panel manage-panel">
                        <div class="panel-header">
                            <div>
                                <p class="eyebrow">ROOM MANAGEMENT</p>
                                <h2>참여방 관리</h2>
                            </div>
                            <span>내 모집글과 참여방</span>
                        </div>

                        <%-- 방장 관리 영역 --%>
                        <section class="manage-section">
                            <div class="settlement-sub-header">
                                <div>
                                    <h3>내가 만든방</h3>
                                    <p>내가 만든 모집글 목록과 결제 완료 후 참여 중인 사람을 확인합니다.</p>
                                </div>
                                <span>${fn:length(hostedRoomList)}개</span>
                            </div>

                            <c:choose>
                                <c:when test="${not empty hostedRoomList}">
                                    <div class="ott-room-card-list manage-room-list">
                                        <c:forEach var="room" items="${hostedRoomList}" varStatus="roomStatus">
                                            <div class="ott-room-card manage-room-card">
                                                <div class="room-index-badge">${roomStatus.count}</div>
                                                <div class="family-room-info">
                                                    <strong><c:out value="${room.room_name}" /></strong>
                                                    <p>${room.service_name} · ${room.plan_name} · ${room.current_member_count}/${room.member_limit}명 · 결제일 매월 ${room.billing_day}일</p>
                                                    <small>1인 결제금액 <fmt:formatNumber value="${room.per_person_amount}" pattern="#,##0" />원</small>
                                                </div>
                                                <div class="family-room-actions">
                                                    <span class="status-pill ${room.status}">${room.status}</span>
                                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary btn-mini">대화방</a>
                                                    <c:if test="${room.status ne 'CLOSE_REQUESTED' and room.status ne 'CLOSED'}">
                                                        <form action="${contextPath}/spendolive/ott/room/close-request.do" method="post" class="room-close-form compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/close-request.do" data-ajax-confirm="방 종료를 예약할까요?" data-loading-message="방 종료 예약을 처리하고 있습니다.">
                                                            <input type="hidden" name="room_id" value="${room.room_id}">
                                                            <input type="hidden" name="returnPage" value="recruit">
                                                            <input type="hidden" name="close_reason" value="파티장 요청">
                                                            <input type="text" name="close_notice" placeholder="종료 공지 입력">
                                                            <button type="submit" class="btn btn-danger-outline btn-mini" onclick="return confirm('방 삭제 요청을 진행할까요? 이번 이용 기간 종료일까지 유지되고, 다음 이용분 결제 완료 건은 자동 환불됩니다.');">방 삭제 요청</button>
                                                        </form>
                                                    </c:if>
                                                </div>

                                                <div class="application-mini-list">
                                                    <h4>참여자 현황</h4>
                                                    <c:set var="hasParticipant" value="false" />
                                                    <c:forEach var="member" items="${hostedRoomMemberList}">
                                                        <c:if test="${member.room_id eq room.room_id}">
                                                            <c:set var="hasParticipant" value="true" />
                                                            <div class="apply-manage-row application-row ACTIVE">
                                                                <div>
                                                                    <strong><c:out value="${member.member_name}" /></strong>

                                                                    <p>아이디: <c:out value="${member.member_login_id}" /> · 참여일 ${member.joined_at}</p>
                                                                    <c:if test="${member.leave_reserved_yn eq 'Y'}">
                                                                        <p class="warn-text">나가기 예약됨 · ${member.leave_scheduled_date} 자동 퇴장 예정</p>

                                                                    </c:if>
                                                                </div>
                                                                <div class="apply-price-box">
                                                                    <span>참여중</span>
                                                                    <b><fmt:formatNumber value="${member.pay_amount}" pattern="#,##0" />원</b>
                                                                </div>
                                                            </div>
                                                        </c:if>
                                                    </c:forEach>
                                                    <c:if test="${not hasParticipant}">
                                                        <div class="empty-box">아직 결제 완료 후 참여 중인 사람이 없습니다.</div>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-box">내가 만든 모집글이 없습니다.</div>
                                </c:otherwise>
                            </c:choose>
                        </section>

                        <%-- 참여자 관리 영역 --%>
                        <section class="manage-section">
                            <div class="settlement-sub-header">
                                <div>
                                    <h3>내가 참여한 방</h3>
                                    <p>결제 완료 후 참여 중인 외부 모집글 공유방입니다.</p>
                                </div>
                                <span>${fn:length(joinedRoomList)}개</span>
                            </div>

                            <c:choose>
                                <c:when test="${not empty joinedRoomList}">
                                    <div class="ott-room-card-list manage-room-list">
                                        <c:forEach var="room" items="${joinedRoomList}" varStatus="roomStatus">
                                            <div class="ott-room-card manage-room-card">
                                                <div class="room-index-badge">${roomStatus.count}</div>
                                                <div class="family-room-info">
                                                    <strong><c:out value="${room.room_name}" /></strong>
                                                    <p>${room.service_name} · ${room.plan_name} · ${room.current_member_count}/${room.member_limit}명 · 결제일 매월 ${room.billing_day}일</p>
                                                    <small>1인 결제금액 <fmt:formatNumber value="${room.per_person_amount}" pattern="#,##0" />원 · 방장 ${room.host_nickname}</small>
                                                </div>
                                                <div class="family-room-actions">
                                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary btn-mini">대화방</a>
                                                    <c:choose>
                                                        <c:when test="${room.leave_reserved_yn eq 'Y'}">
                                                            <small class="warn-text">나가기 예약됨 · ${room.leave_scheduled_date} 자동 퇴장</small>
                                                            <form action="${contextPath}/spendolive/ott/room/leave-cancel.do" method="post" class="compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/leave-cancel.do" data-loading-message="나가기 예약을 취소하고 있습니다.">
                                                                <input type="hidden" name="room_id" value="${room.room_id}">
                                                                <input type="hidden" name="returnPage" value="recruit">
                                                                <button type="submit" class="btn btn-danger-outline btn-mini">예약 취소</button>
                                                            </form>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <form action="${contextPath}/spendolive/ott/room/leave-reserve.do" method="post" class="compact-close-form" data-ajax-form data-ajax-action="/spendolive/ott/ajax/room/leave-reserve.do" data-ajax-confirm="다음 이용 회차부터 나가도록 예약할까요?" data-loading-message="나가기 예약을 처리하고 있습니다.">
                                                                <input type="hidden" name="room_id" value="${room.room_id}">
                                                                <input type="hidden" name="returnPage" value="recruit">
                                                                <button type="submit" class="btn btn-danger-outline btn-mini">나가기 예약</button>
                                                            </form>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </div>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-box">아직 참여 중인 외부 모집글 공유방이 없습니다.</div>
                                </c:otherwise>
                            </c:choose>
                        </section>
                    </article>
                </c:when>

                <%-- 정산 및 알림 탭 --%>
                <c:when test="${tab eq 'settlement'}">
                    <article class="card ott-tab-panel settlement-panel-wide">
                        <div class="panel-header">
                            <div>
                                <p class="eyebrow">SETTLEMENT & ALERT</p>
                                <h2>정산 상태</h2>
                            </div>
                            <span>참여자별 정산 상태 확인</span>
                        </div>

                        <div class="settlement-stack">
                            <%-- 개인 및 팀원 결제 상태 --%>
                            <section class="settlement-wide-block">
                                <div class="settlement-sub-header">
                                    <div>
                                        <h3>내 정산 상태</h3>
                                        <p>내가 만든 모집글 또는 참여 중인 모집글의 정산 진행 상태입니다.</p>
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
                                                            ${settlement.settlement_month} 이용분 · ${settlement.my_role}<br>
                                                            결제기간 ${settlement.payment_start_date} ~ ${settlement.payment_close_date}<br>
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
                                                                    <input type="hidden" name="returnPage" value="recruit">
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
                                        <div class="empty-box">아직 정산 내역이 없습니다.</div>
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
                </c:when>

                <%-- 모집글 작성 탭 --%>
                <c:otherwise>
                    <article class="card ott-tab-panel">
                        <div class="panel-header">
                            <div>
                                <p class="eyebrow">WRITE POST</p>
                                <h2>모집글 작성</h2>
                            </div>
                            <span>신청 버튼은 결제 화면으로 연결됩니다</span>
                        </div>

                        <form action="${contextPath}/spendolive/ott/recruit/create.do" method="post" class="recruit-search-form ott-fixed-plan-form" data-room-mode="RECRUIT" data-ajax-form data-ajax-action="/spendolive/ott/ajax/recruit/create.do" data-loading-message="모집글을 등록하고 있습니다.">
                            <%-- OTT 종류 --%>
                            <div class="auth-form-group">
                                <label for="recruitOttService">OTT 종류</label>

                                <select id="recruitOttService" name="ott_service_id" class="ott-service-select" required>
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
                            </div>

                            <%-- 모집글 제목 --%>
                            <div class="auth-form-group">
                                <label for="recruitRoomName">모집글 제목</label>

                                <input type="text"
                                    id="recruitRoomName"
                                    name="room_name"
                                    placeholder="비워두면 OTT - 최고 멤버십 - 모집으로 저장됩니다.">
                            </div>

                            <%-- 결제일 --%>
                            <div class="auth-form-group">
                                <label for="recruitBillingDay">결제일</label>

                                <input type="number"
                                    id="recruitBillingDay"
                                    name="billing_day"
                                    min="1"
                                    max="31"
                                    value="1"
                                    required>
                            </div>

                            <input type="hidden"
                                name="plan_name"
                                class="ott-plan-input">

                            <input type="hidden"
                                name="total_price"
                                class="ott-total-price-input">

                            <input type="hidden"
                                name="member_limit"
                                class="ott-member-limit-input">

                            <%-- 요금 안내 --%>
                            <div class="ott-fixed-plan-preview">
                                <strong>
                                    OTT를 선택하면 최고 멤버십 기준 금액이 자동 적용됩니다.
                                </strong>

                                <p>
                                    추가 계정 비용이 있는 OTT는 기본 구독료에 추가 비용을 더한 뒤
                                    N분의 1로 계산합니다. 서비스 수수료는 3%입니다.
                                </p>
                            </div>

                            <%-- 등록 버튼은 아래 전체 너비 유지 --%>
                            <button type="submit"
                                    class="btn btn-primary full ott-form-submit">
                                모집글 등록
                            </button>
                        </form>
                    </article>
                </c:otherwise>
            </c:choose>
            </div>
        </div>
</section>

<script src="${contextPath}/resources/js/ott.js" data-ajax-reload></script>
