<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- OTT 사용자 메인 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<section class="page-hero">
    <div class="container">
                <p class="eyebrow">
                    SPENDING CALENDAR
                </p>
                <h1>
                    OTT 공유방
                </h1>
                <p class="hero-text">
                    월별로 넘겨보는 큰 달력입니다. 날짜에는 지출 금액과 카테고리만 간단히 보여줍니다.
                </p>
            </div>
</section>

<section class="section compact ott-main-section">
    <div class="container ott-wide-container">
        <div class="ott-stat-grid">
            <div class="ott-stat-card">
                <span>내 공유방</span>
                <strong>${myRoomCount}</strong>
                <p>참여 중이거나 내가 만든 공유방</p>
            </div>
            <div class="ott-stat-card">
                <span>모든 모집글</span>
                <strong>${recruitRoomCount}</strong>
                <p>현재 모집 중인 공개 OTT 파티</p>
            </div>
            <div class="ott-stat-card ott-service-logo-card">
                <span>공유 가능 OTT</span>
                <div class="ott-logo-strip" aria-label="공유 가능한 OTT 목록">
                    <img src="${contextPath}/resources/images/ott/net.png" alt="Netflix">
                    <img src="${contextPath}/resources/images/ott/tving.png" alt="TVING">
                    <img src="${contextPath}/resources/images/ott/disney.png" alt="Disney+">
                    <img src="${contextPath}/resources/images/ott/wave.png" alt="Wavve">
                    <img src="${contextPath}/resources/images/ott/watcha.png" alt="Watcha">
                    <img src="${contextPath}/resources/images/ott/laftel.png" alt="Laftel">
                </div>
                <p>인기 OTT 6종 지원</p>
            </div>
        </div>

        <div class="ott-two-grid ott-choice-grid">
            <article class="card ott-panel ott-choice-card">
                <div class="panel-header">
                    <div>
                        <p class="eyebrow">WITH FAMILY & FRIENDS</p>
                        <h2>가족 또는 지인과</h2>
                    </div>
                    <span>초대 기반 공유</span>
                </div>

                <p class="card-desc">
                    내가 아는 사람들과 공유방을 만들고, 초대 URL·QR·카카오톡 링크로 결제 화면까지 바로 연결합니다.
                </p>

                <div class="ott-guide-grid">
                    <div>
                        <strong>OTT 공유방</strong>
                        <span>Netflix · Disney+ · TVING 등</span>
                    </div>
                    <div>
                        <strong>초대 링크</strong>
                        <span>URL · QR · 카카오톡 공유</span>
                    </div>
                    <div>
                        <strong>공유방 대화</strong>
                        <span>오른쪽 아래 말풍선에서 확인</span>
                    </div>
                </div>

                <a href="${contextPath}/spendolive/ott/friends.do" class="btn btn-primary full">공유방 만들기</a>
            </article>

            <article class="card ott-panel ott-choice-card">
                <div class="panel-header">
                    <div>
                        <p class="eyebrow">WITH OTHERS</p>
                        <h2>외부 다른 사람들과</h2>
                    </div>
                    <span>모집 게시판 기반 공유</span>
                </div>

                <p class="card-desc">
                    모든 모집글을 확인하고, 신청 버튼을 통해 결제 화면으로 이동하거나 직접 모집글을 작성할 수 있습니다.
                </p>

                <div class="ott-guide-grid">
                    <div>
                        <strong>모든 모집글</strong>
                        <span>모집인원 · 3% 수수료 포함 금액 확인</span>
                    </div>
                    <div>
                        <strong>모집글 작성</strong>
                        <span>OTT 선택 시 최고 멤버십 자동 계산</span>
                    </div>
                    <div>
                        <strong>결제 후 입장</strong>
                        <span>승인 없이 결제 완료 후 참여</span>
                    </div>
                </div>

                <a href="${contextPath}/spendolive/ott/recruit.do" class="btn btn-primary full">모든 모집글</a>
            </article>
        </div>
    </div>
</section>

<script src="${contextPath}/resources/js/ott.js"></script>
