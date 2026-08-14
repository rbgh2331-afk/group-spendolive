<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- FAQ 목록 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="faq-page">
    <section class="page-hero ">
    <div class="container ">
            <p class="eyebrow">FAQ</p>
            <h1>자주 묻는 질문</h1>
            <p class="hero-sub">SpendOlive 이용 중 궁금한 점을 빠르게 해결하세요.</p>
        </div>
    </section>

    <div class="wrap">
        <c:if test="${not empty errorMsg}">
            <div class="empty-box">
                <div class="icon-big">⚠️</div>
                <p>${errorMsg}</p>
            </div>
        </c:if>
        <div class="search-barb">
            <div class="search-bar">
                <input type="text" id="faqSearchInput" placeholder="질문을 검색해보세요 (예: 비밀번호 변경)"
                       onkeydown="if(event.key==='Enter'){ searchFaq(); }">
                <button type="button" onclick="searchFaq()">검색</button>
            </div>
        </div>
        <div class="cats">
            <button type="button" class="cat-btn active" onclick="filterFaqCat(this,'all')">전체</button>
            <button type="button" class="cat-btn" onclick="filterFaqCat(this,'account')">계정·로그인</button>
            <button type="button" class="cat-btn" onclick="filterFaqCat(this,'expense')">지출관리</button>
            <button type="button" class="cat-btn" onclick="filterFaqCat(this,'ott')">OTT관리</button>
            <button type="button" class="cat-btn" onclick="filterFaqCat(this,'notice')">공지·알림</button>
            <button type="button" class="cat-btn" onclick="filterFaqCat(this,'etc')">기타</button>
        </div>


        <c:forEach var="entry" items="${faqGroups}">
            <p class="section-label">${entry.value[0].categoryLabel}</p>
            <div class="faq-list" data-cat="${entry.key}">
                <c:forEach var="faq" items="${entry.value}">
                    <div class="faq-item" onclick="toggleFaq(this)">
                        <div class="faq-q">
                            <div class="faq-q-left"><span class="q-badge">Q</span>${faq.question}</div>
                            <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="6 9 12 15 18 9"/></svg>
                        </div>
                        <div class="faq-a"><div class="faq-a-inner">${faq.answer}</div></div>
                    </div>
                </c:forEach>
            </div>
        </c:forEach>

        <c:if test="${empty faqGroups}">
            <div class="empty-box">
                <div class="icon-big">📭</div>
                <p>아직 등록된 FAQ가 없습니다.</p>
            </div>
        </c:if>

        <div class="empty-box is-hidden" id="faqSearchEmpty">
            <div class="icon-big" id="faqSearchEmptyIcon">🔍</div>
            <p id="faqSearchEmptyText">검색 결과가 없습니다.</p>
        </div>

        <div class="contact-banner">
            <div>
                <h3>원하는 답을 찾지 못하셨나요?</h3>
                <p>고객센터로 직접 문의하시면 빠르게 도와드립니다.</p>
            </div>
            <a class="contact-btn" href="${contextPath}/spendolive/inquiry/write.do" onclick="return loginYn('inquiry', ${isLogOn})">문의하기 →</a>
        </div>
    </div>
</div>

<script src="${contextPath}/resources/js/faq.js"></script>
