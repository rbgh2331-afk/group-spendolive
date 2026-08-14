<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<section class="page-hero payment-detail-page">
    <div class="container">
        <div class="table-card card payment-summary-card">
            <div class="payment-summary-head">
                <span class="status-pill PAYMENT_OPEN">결제 정보 확인</span>
                <h2><c:out value="${paymentAmount.roomName}" /></h2>
                <p>
                    결제 버튼을 누르면 등록된 주 결제 카드로 자동결제가 진행됩니다.
                </p>
            </div>
    <c:choose>
                <c:when test="${empty cardList}">
                <p>등록 카드가 없습니다.</p>
                </c:when>
<c:otherwise>
<table class="payment-amount-table">
                    <thead>
                        <tr>
                         <th>카드 구분</th>
                             <th>카드 이름</th>
                                    <th>카드사</th>
                                    <th>카드 번호</th>
                        </tr>
                    </thead>
                    <tbody>
<c:forEach var="card" items="${cardList}" varStatus="s">
    <tr>
<td><strong><c:choose>
    <c:when test="${card.status == 'YES'}">
        주 카드
        </c:when>
            <c:otherwise>
                        <button type="button" 
                                class="btn btn-primary btn-mini btn-change-card" 
                                data-card-idx="${card.card_idx}">
                                결제 카드 변경</button>

            </c:otherwise>
        </c:choose>
    </strong></td>
    <c:set var="cardCompanyDisplayName"
                               value="${not empty cardCompanyNameMap[card.card_company] ? cardCompanyNameMap[card.card_company] : (empty card.card_company ? '카드' : card.card_company)}" />
                                   <c:set var="cardDisplayName"
                               value="${fn:escapeXml(empty card.card_name or card.card_name eq card.card_company ? cardCompanyDisplayName : card.card_name)}" />
                             <td>
                                <strong>
                                  
                               <c:out value="${cardDisplayName}" />
                                </strong>
                            </td>
                            <td>                                  
                               <c:out value="${cardCompanyDisplayName}" />
                                
                            </td>
                            <td><strong>${card.card_number}</strong></td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
                </c:otherwise>
                </c:choose>
            <div class="table-wrap">
                <table class="payment-amount-table">
                    <thead>
                        <tr>
                            <th>구분</th>
                            <th>금액</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>OTT 사용료</strong></td>
                            <td>
                                <strong>
                                    <fmt:formatNumber value="${paymentAmount.baseAmount}" type="number" />원
                                </strong>
                            </td>
                        </tr>
                        <tr>
                            <td><strong>플랫폼 수수료 (${paymentAmount.feeRate}%)</strong></td>
                            <td>
                                <strong>
                                    + <fmt:formatNumber value="${paymentAmount.feeAmount}" type="number" />원
                                </strong>
                            </td>
                        </tr>
                        <tr class="payment-total-row">
                            <td><strong>최종 결제 금액</strong></td>
                            <td>
                                <strong>
                                    <fmt:formatNumber value="${paymentAmount.totalAmount}" type="number" />원
                                </strong>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="payment-auto-guide">
                결제 완료 후 매월 <strong>${paymentAmount.automaticPaymentDay}일</strong>에 자동결제됩니다.
            </div>

            <c:set var="paymentCancelUrl" value="/spendolive/ott/friends.do" />
            <c:if test="${param.room_mode eq 'RECRUIT'}">
                <c:set var="paymentCancelUrl" value="/spendolive/ott/recruit.do?tab=all" />
            </c:if>

            <div class="payment-action-row">
                <a href="${contextPath}${paymentCancelUrl}"
                   class="btn btn-danger-outline">
                    취소하기
                </a>

                <button type="button"
                        id="paymentSubmitButton"
                        class="btn btn-primary"
                        data-room_id="${paymentAmount.roomId}">
                    결제하기
                </button>
            </div>
        </div>
    </div>
</section>
 <div id="cardStatusOverlay"
                    class="status-overlay"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="cardStatusTitle"
                    aria-describedby="cardStatusMessage"
                    hidden>
                    <div class="status-box">
                    
                    <div id="cardStatusSpinner"
                            class="status-spinner"
                            aria-hidden="true"></div>

                        <div id="cardStatusIcon"
                            class="status-icon"
                            aria-hidden="true"
                            hidden></div>
                            <h3 id="cardStatusTitle">카드 변경 중 입니다.</h3>
                        <p id="cardStatusMessage">
                            창을 닫거나 새로고침하지 말아주세요.
                        </p>
                    <div id="cardStatusActions"
                            class="status-actions"
                            hidden>
                            <button type="button"
                                    id="cardStatusCloseButton"
                                    class="btn btn-outline">
                                확인
                            </button>
                            <button type="button"
                                    id="cardStatusActionButton"
                                    class="btn btn-primary"
                                    hidden>
                                이동하기
                            </button>
                        </div>
                </div>
            </div>

<jsp:include page="/WEB-INF/views/payment/popup.jsp" />
<script src="${contextPath}/resources/js/payment.js"></script>
<script src="${contextPath}/resources/js/signup.js"></script>