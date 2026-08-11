<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<section class="page-hero">
    <div class="container">
                <p class="eyebrow">
                    NOTICE CENTER
                </p>
                <h1>
                    공지사항 · 알림센터
                </h1>
                <p class="hero-text">
                    SpendOlive의 공지사항과 개인 알림을 한눈에 확인하세요.
                </p>
            </div>
</section>


<section class="section compact notice-list-section">
    <div class="container">
        <div class="notice-board-wrap">

            <div class="notice-board-tabs">
                <button type="button"
                    id="noticeTabBtn"
                    class="notice-board-tab"
                    onclick="setBoardTab('notice')">
                공지사항
            </button>

            <button type="button"
                    id="alertTabBtn"
                    class="notice-board-tab"
                    onclick="setBoardTab('alert')">
                알림
            </button>
            </div>

            <%-- DB 오류 등 서버 오류 메시지 - 화면에 계속 남는 빨간 박스 대신 alert 팝업으로 한 번만 띄움 --%>
            <c:if test="${not empty errorMsg}">
                <script>
                    alert("${errorMsg}");
                </script>
            </c:if>

            <div class="card table-card notice-board-card">
                <div class="notice-row-title">
                    <div>
                        <p class="eyebrow" id="listEyebrow">NOTICE LIST</p>
                        <h2 id="listTitle">공지사항</h2>

                     <div id="boardFilter" class="notification-filter"></div>
                    </div>
                </div>

                <div class="table-wrap">
                    <table>
                        <colgroup>
                                <col style="width:55px">     <!-- 번호 -->
                                <col style="width:45px">     <!-- 찜 -->
                                <col style="width:110px">     <!-- 구분 -->
                                <col>                        <!-- 제목(남는 공간 전부) -->
                                <col style="width:90px">    <!-- 작성자 -->
                                <col style="width:120px">    <!-- 등록일 -->
                            </colgroup>


                        <thead>
                            <tr>
                                <th>번호</th>
                                <th class="star-column"></th>
                                <th>구분</th>
                                <th>제목</th>
                                <th id="writerTypeHeader">작성자</th>
                                <th>등록일</th>
                            </tr>
                        </thead>

                        <tbody id="noticeTableBody">
                            
                                  
                        </tbody>
                    </table>
                    </div>

                    <div id="noticePagination" class="notice-pagination"></div>
            </div>

        </div>
    </div>
</section>

<script>
    <%-- loginYn이 model에 없으면 false로 fallback.
         변수명은 isLoggedIn으로 둠 — app.js/notice.js의 loginYn() 함수와
         이름이 겹치면 "Identifier 'loginYn' has already been declared" 문법 에러가
         나서 두 스크립트가 아예 안 돌아가는 사고가 났었음(모델 속성명 loginYn은 그대로 둠). --%>
    const isLoggedIn = ${not empty loginYn ? loginYn : false};
</script>


<script src="${contextPath}/resources/js/notice.js"></script>

<!-- 공지/알림 상세 모달 (문의 상세 모달과 같은 .modal/.modal-box 구조 재사용, 둘이 공유) -->
<div class="modal" id="detailModal" onclick="closeDetailModal(event)">
    <div class="modal-box modal-inquiry" onclick="event.stopPropagation()">
        <button type="button" class="modal-close" onclick="closeDetailModal(event)">✕</button>
        <div id="detailModalBody"></div>
    </div>
</div>

