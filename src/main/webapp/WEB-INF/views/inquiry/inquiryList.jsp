<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />


<div class="faq-page">
    <div class="page-hero">
        <div class="wrap">
            <p class="eyebrow">MY INQUIRIES</p>
            <h1>내 문의 조회</h1>
            <p class="hero-sub">SpendOlive에 남긴 문의 내역과 답변을 확인하세요.</p>
        </div>
    </div>

    <div class="wrap">
        <div class="board-header">
            <h2>문의 내역</h2>
            <a class="btn btn-primary" href="${contextPath}/spendolive/inquiry/write.do" onclick="return loginYn('inquiry', ${isLogOn})">+ 새 문의 작성</a>
        </div>

        <%-- ═══════════════════════════════════════════════════════════
             #inqBoardArea : 필터/목록/페이지네이션을 감싸는 "교체 대상" 영역.
             필터·페이지 클릭 시 inquiry.js가 list.do를 fetch해서 이 영역만
             통째로 갈아끼운다(전체 새로고침 없음). 모달·삭제 폼은 이 밖에 둠. --%>
        <div id="inqBoardArea">

        <div class="filters">
            <%-- a href → data-status 버튼으로 변경. inquiry.js가 클릭을 가로채 AJAX 처리 --%>
            <button type="button" class="filter-btn ${currentStatus == 'all' ? 'active' : ''}" data-status="all">전체</button>
            <button type="button" class="filter-btn ${currentStatus == 'wait' ? 'active' : ''}" data-status="wait">답변 대기</button>
            <button type="button" class="filter-btn ${currentStatus == 'done' ? 'active' : ''}" data-status="done">답변 완료</button>
            <button type="button" class="filter-btn ${currentStatus == 'review' ? 'active' : ''}" data-status="review">검토 중</button>
        </div>

        <c:if test="${not empty inquiryList}">
            <div class="inq-list" id="inqList">
                <c:forEach var="inq" items="${inquiryList}">
                    <div class="inq-card" onclick="openInqDetailModal('${inq.inquiry_id}')">
                        <div class="inq-top">
                            <span class="inq-category"><c:out value="${inq.category}" /></span>
                            <div class="inq-meta">
                                <span class="badge ${inq.statusCode}"><c:out value="${inq.statusLabel}" /></span>
                                <span class="inq-date"><c:out value="${inq.reg_date}" /></span>
                            </div>
                        </div>
                        <div class="inq-title"><c:out value="${inq.title}" /></div>
                        <div class="inq-preview"><c:out value="${inq.preview}" /></div>
                        <c:if test="${not empty inq.files}">
                            <div class="inq-attachments" onclick="event.stopPropagation()">
                                <c:forEach var="file" items="${inq.files}">
                                    <c:choose>
                                        <c:when test="${file.image}">
                                            <img src="${contextPath}/spendolive/inquiry/file/${file.file_id}"
                                                alt="${fn:escapeXml(file.origin_name)}" class="inq-thumb"
                                                onclick="event.stopPropagation(); openInqLightbox(this.src, this.alt)">
                                        </c:when>
                                        <c:otherwise>
                                            <a href="${contextPath}/spendolive/inquiry/file/${file.file_id}"
                                            target="_blank" class="inq-file-link">📎 <c:out value="${file.origin_name}" /></a>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>
                            </div>
                        </c:if>
                        <div class="inq-bottom">
                            <c:choose>
                                <c:when test="${inq.hasReply}">
                                    <div class="inq-reply"><span class="reply-dot"></span> 관리자 답변이 달렸습니다</div>
                                </c:when>
                                <c:otherwise>
                                    <div class="inq-reply"><span class="reply-dot none"></span> 아직 답변이 없습니다</div>
                                </c:otherwise>
                            </c:choose>
                            <span class="inq-no">문의 #${inq.inquiry_id}</span>
                        </div>
                    </div>

                    <%-- 이 카드 클릭 시 위 정보를 모달에 그대로 복사해서 보여줄 숨김 템플릿 --%>
                    <div class="inq-detail-tpl" id="inqDetailTpl${inq.inquiry_id}">
                        <div class="inq-detail-header">
                            <div class="inq-top">
                                <span class="inq-category"><c:out value="${inq.categoryLabel}" /> · <c:out value="${inq.inquiryTypeLabel}" /></span>
                                <div class="inq-meta">
                                    <span class="badge ${inq.statusCode}"><c:out value="${inq.statusLabel}" /></span>
                                    <span class="inq-date"><c:out value="${inq.reg_date}" /></span>
                                </div>
                            </div>
                            <div class="inq-title inq-title-detail"><c:out value="${inq.title}" /></div>
                        </div>

                        <div class="inq-detail-section">
                            <span class="inq-detail-label">문의 내용</span>
                            <div class="inq-detail-body"><c:out value="${inq.content}" /></div>
                        </div>

                        <c:if test="${not empty inq.files}">
                            <div class="inq-detail-section">
                                <span class="inq-detail-label">첨부파일</span>
                                <div class="inq-attachments">
                                    <c:forEach var="file" items="${inq.files}">
                                        <c:choose>
                                            <c:when test="${file.image}">
                                                <img src="${contextPath}/spendolive/inquiry/file/${file.file_id}"
                                                    alt="${fn:escapeXml(file.origin_name)}" class="inq-thumb"
                                                    onclick="openInqLightbox(this.src, this.alt)">
                                            </c:when>
                                            <c:otherwise>
                                                <a href="${contextPath}/spendolive/inquiry/file/${file.file_id}"
                                                target="_blank" class="inq-file-link">📎 <c:out value="${file.origin_name}" /></a>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:forEach>
                                </div>
                            </div>
                        </c:if>

                        <div class="inq-detail-section">
                            <c:choose>
                                <c:when test="${inq.hasReply}">
                                    <div class="inq-reply-box">
                                        <div class="inq-reply-head">
                                            <strong>관리자 답변</strong>
                                            <span class="inq-date">${inq.reply_date}</span>
                                        </div>
                                        <div class="inq-reply-text"><c:out value="${inq.reply_content}" /></div>
                                    </div>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-box empty-box-no-margin">
                                        <p>아직 답변이 등록되지 않았습니다.</p>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <%-- 답변 대기 상태(관리자 답변 전)인 문의만 수정/삭제 가능.
                             이미 답변이 달린 문의는 내용을 바꾸면 답변과 안 맞아질 수 있어서 막음. --%>
                        <c:if test="${inq.status == 'WAIT'}">
                            <div class="inq-detail-actions inq-detail-actions-inline">
                                <a class="btn btn-outline inq-action-flex"
                                   href="${contextPath}/spendolive/inquiry/edit.do?inquiryNo=${inq.inquiry_id}">수정</a>
                                <button type="button" class="btn btn-danger-outline inq-action-flex"
                                        onclick="event.stopPropagation(); deleteInquiry(${inq.inquiry_id})">삭제</button>
                            </div>
                        </c:if>
                    </div>
                </c:forEach>
            </div>

            <c:if test="${totalPages > 1}">
                <%-- 현재 페이지 기준 앞뒤 2개씩만 보여주는 윈도우 방식 --%>
                <c:set var="pgStart" value="${currentPage - 2 < 1 ? 1 : currentPage - 2}" />
                <c:set var="pgEnd" value="${currentPage + 2 > totalPages ? totalPages : currentPage + 2}" />

                <%-- 페이지 번호도 a href → data-page 버튼. inquiry.js가 클릭을 가로채 AJAX 처리.
                     현재 상태 필터(currentStatus)는 data-status로 같이 실어보내 유지 --%>
                <div class="pagination">
                    <c:if test="${pgStart > 1}">
                        <button type="button" class="pg-btn" data-page="1" data-status="${currentStatus}">1</button>
                        <c:if test="${pgStart > 2}">
                            <span class="pg-ellipsis">…</span>
                        </c:if>
                    </c:if>

                    <c:forEach begin="${pgStart}" end="${pgEnd}" var="p">
                        <button type="button" class="pg-btn ${p == currentPage ? 'active' : ''}"
                                data-page="${p}" data-status="${currentStatus}">${p}</button>
                    </c:forEach>

                    <c:if test="${pgEnd < totalPages}">
                        <c:if test="${pgEnd < totalPages - 1}">
                            <span class="pg-ellipsis">…</span>
                        </c:if>
                        <button type="button" class="pg-btn" data-page="${totalPages}" data-status="${currentStatus}">${totalPages}</button>
                    </c:if>
                </div>
            </c:if>
        </c:if>

        <c:if test="${empty inquiryList}">
            <div class="empty-box">
                <div class="icon-big">📭</div>
                <c:choose>
                    <c:when test="${currentStatus == 'all'}">
                        <p>아직 등록한 문의가 없습니다.</p>
                        <a class="btn btn-primary" href="${contextPath}/spendolive/inquiry/write.do">+ 새 문의 작성</a>
                    </c:when>
                    <c:otherwise>
                        <p>해당 상태의 문의가 없습니다.</p>
                        <%-- '전체 문의 보기'도 AJAX 필터 버튼으로 (data-status=all) --%>
                        <button type="button" class="btn btn-outline filter-btn" data-status="all">전체 문의 보기</button>
                    </c:otherwise>
                </c:choose>
            </div>
        </c:if>

        </div> <%-- /#inqBoardArea --%>
    </div>

    <div class="modal" id="inqDetailModal" onclick="closeInqDetailModal(event)">
        <div class="modal-box modal-inquiry" onclick="event.stopPropagation()">
            <button type="button" class="modal-close" onclick="closeInqDetailModal(event)">✕</button>
            <div id="inqDetailBody"></div>
        </div>
    </div>

    <%-- 삭제는 이제 inquiry.js가 fetch(ajax/delete.do)로 처리하므로 숨김 폼 불필요 --%>

    <%-- 첨부 사진 확대보기 (01-foundation.css의 공통 .modal 시스템 재사용) --%>
    <div class="modal" id="inqLightbox" onclick="closeInqLightbox(event)">
        <div class="modal-box modal-photo" onclick="event.stopPropagation()">
            <button type="button" class="modal-close" onclick="closeInqLightbox(event)">✕</button>
            <img src="" alt="" id="inqLightboxImg">
        </div>
    </div>
</div>

<script src="${contextPath}/resources/js/faq.js"></script>
<script src="${contextPath}/resources/js/inquiry.js"></script>

