<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- 관리자 문의 목록 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="inquiry">
    <div id="adminBoardArea">
        <div class="hero"><div><p class="hero-kicker">ADMIN</p><h1>문의 관리</h1><p>회원이 남긴 문의를 확인하고 답변을 등록합니다.</p></div></div>

        <c:if test="${not empty msg}"><div class="flash-ok">${msg}</div></c:if>
        <c:if test="${not empty errorMsg}"><div class="flash-err">⚠ ${errorMsg}</div></c:if>

        <div class="admin-board-tabs">
            <a href="${contextPath}/admin/inquiry/list.do" class="admin-board-tab active">문의사항</a>
            <a href="${contextPath}/spendolive/admin/faq/list.do" class="admin-board-tab">자주 묻는 질문</a>
        </div>

        <div class="panel">
            <div class="panel-header"><div class="panel-title"><p class="section-kicker">INQUIRY LIST</p><h2>문의 목록 (총 ${inquiryList.size()}건)</h2></div></div>

            <div class="toolbar"><div class="toolbar-left filter-pills">
                <a class="${currentStatus == 'all' ? 'active' : ''}" href="${contextPath}/admin/inquiry/list.do?status=all">전체</a>
                <a class="${currentStatus == 'wait' ? 'active' : ''}" href="${contextPath}/admin/inquiry/list.do?status=wait">답변 대기</a>
                <a class="${currentStatus == 'done' ? 'active' : ''}" href="${contextPath}/admin/inquiry/list.do?status=done">답변 완료</a>
                <a class="${currentStatus == 'review' ? 'active' : ''}" href="${contextPath}/admin/inquiry/list.do?status=review">검토 중</a>
            </div></div>

            <div class="table-wrap">
                <table class="admin-table admin-table-fixed">
                    <colgroup>
                        <col style="width:56px">
                        <col style="width:90px">
                        <col style="width:90px">
                        <col>
                        <col style="width:60px">
                        <col style="width:110px">
                        <col style="width:80px">
                        <col style="width:100px">
                    </colgroup>
                    <thead><tr><th>번호</th><th>카테고리</th><th>유형</th><th>제목</th><th>첨부</th><th>작성자</th><th>상태</th><th>등록일</th></tr></thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty inquiryList}"><tr><td colspan="8"><div class="admin-empty-filter">해당 조건의 문의가 없습니다.</div></td></tr></c:when>
                            <c:otherwise>
                                <c:forEach var="inq" items="${inquiryList}">
                                    <tr>
                                        <td>${inq.inquiry_id}</td><td>${inq.category}</td><td>${inq.inquiry_type}</td>
                                        <td><a href="javascript:void(0)" onclick="openAdminInquiryModal(${inq.inquiry_id})">${inq.title}</a></td>
                                        <td class="admin-attach-cell"><c:if test="${not empty inq.files}"><span class="admin-attach-check" title="첨부파일 있음">📎</span></c:if></td>
                                        <td><c:choose><c:when test="${not empty inq.writer_nickname}">${inq.writer_nickname}</c:when><c:otherwise>${inq.id}</c:otherwise></c:choose></td>
                                        <td><c:choose><c:when test="${inq.statusCode == 'done'}"><span class="badge green">완료</span></c:when><c:when test="${inq.statusCode == 'review'}"><span class="badge blue">검토중</span></c:when><c:otherwise><span class="badge yellow">대기</span></c:otherwise></c:choose></td>
                                        <td>${inq.reg_date}</td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <c:if test="${totalPages > 1}">
                <c:set var="pgStart" value="${currentPage - 2 < 1 ? 1 : currentPage - 2}" />
                <c:set var="pgEnd" value="${currentPage + 2 > totalPages ? totalPages : currentPage + 2}" />
                <div class="admin-pagination">
                    <c:if test="${pgStart > 1}"><a class="admin-pg-btn" href="${contextPath}/admin/inquiry/list.do?status=${currentStatus}&page=1">1</a><c:if test="${pgStart > 2}"><span class="admin-pg-btn">…</span></c:if></c:if>
                    <c:forEach begin="${pgStart}" end="${pgEnd}" var="p"><a class="admin-pg-btn ${p == currentPage ? 'active' : ''}" href="${contextPath}/admin/inquiry/list.do?status=${currentStatus}&page=${p}">${p}</a></c:forEach>
                    <c:if test="${pgEnd < totalPages}"><c:if test="${pgEnd < totalPages - 1}"><span class="admin-pg-btn">…</span></c:if><a class="admin-pg-btn" href="${contextPath}/admin/inquiry/list.do?status=${currentStatus}&page=${totalPages}">${totalPages}</a></c:if>
                </div>
            </c:if>
        </div>

        <c:forEach var="inq" items="${inquiryList}">
            <template id="adminInqDetailTpl${inq.inquiry_id}">
                <div class="panel-title">
                    <p class="section-kicker">문의 #${inq.inquiry_id}</p><h2><c:out value="${inq.title}" /></h2>
                    <p><c:out value="${inq.category}" /> · <c:out value="${inq.inquiry_type}" /> · 작성자 <c:choose><c:when test="${not empty inq.writer_nickname}"><c:out value="${inq.writer_nickname}" /> (<c:out value="${inq.id}" />)</c:when><c:otherwise><c:out value="${inq.id}" /></c:otherwise></c:choose> · ${inq.reg_date}
                    <c:choose><c:when test="${inq.statusCode == 'done'}"><span class="badge green">답변완료</span></c:when><c:when test="${inq.statusCode == 'review'}"><span class="badge blue">검토중</span></c:when><c:otherwise><span class="badge yellow">답변대기</span></c:otherwise></c:choose></p>
                </div>

                <div class="form-field"><label for="inquiryContent${inq.inquiry_id}">문의 내용</label><textarea id="inquiryContent${inq.inquiry_id}" class="form-textarea" readonly><c:out value="${inq.content}" /></textarea></div>

                <c:if test="${not empty inq.files}">
                    <div class="form-field"><label>첨부파일</label><div class="toolbar-left">
                        <c:forEach var="file" items="${inq.files}">
                            <c:choose>
                                <c:when test="${file.image}"><img src="${contextPath}/spendolive/inquiry/file/${file.file_id}" alt="${file.origin_name}" class="admin-inquiry-image" onclick="openAdminInquiryLightbox(this.src, '${file.origin_name}')"></c:when>
                                <c:otherwise><a href="${contextPath}/spendolive/inquiry/file/${file.file_id}" target="_blank" rel="noopener" class="mini-btn">📎 ${file.origin_name}</a></c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </div></div>
                </c:if>

                <form class="admin-reply-form" onsubmit="return false;">
                    <input type="hidden" name="inquiry_id" value="${inq.inquiry_id}">
                    <div class="form-field"><label for="reply_content_${inq.inquiry_id}">답변 내용</label><textarea id="reply_content_${inq.inquiry_id}" name="reply_content" class="form-textarea" placeholder="답변 내용을 입력하세요" required><c:out value="${inq.reply_content}" /></textarea></div>
                    <div class="form-field"><label for="status_${inq.inquiry_id}">처리 상태</label><select id="status_${inq.inquiry_id}" name="status" class="form-input"><option value="DONE" ${inq.status == 'REVIEW' ? '' : 'selected'}>답변 완료</option><option value="REVIEW" ${inq.status == 'REVIEW' ? 'selected' : ''}>검토 중</option></select></div>
                    <div class="toolbar"><span></span><div class="toolbar-left"><button type="submit" class="btn primary">${empty inq.reply_content ? '답변 등록' : '답변 수정'}</button></div></div>
                </form>
            </template>
        </c:forEach>

        <div class="modal" id="adminInqDetailModal" onclick="closeAdminInquiryModal(event)">
            <div class="modal-box modal-admin-inquiry" onclick="event.stopPropagation()"><button type="button" class="modal-close" onclick="closeAdminInquiryModal(event)">✕</button><div id="adminInqDetailBody"></div></div>
        </div>

        <%-- 첨부 사진 확대보기(라이트박스). 상세 모달 이미지 + 목록 테이블 미리보기 썸네일이 공통으로 사용 --%>
        <div class="modal" id="adminInqLightbox" onclick="closeAdminInquiryLightbox(event)">
            <div class="modal-box modal-photo" onclick="event.stopPropagation()">
                <button type="button" class="modal-close" onclick="closeAdminInquiryLightbox(event)">✕</button>
                <img src="" alt="" id="adminInqLightboxImg">
            </div>
        </div>
    </div>
</div>

<script src="${contextPath}/resources/js/adminInquiry.js"></script>
<script src="${contextPath}/resources/js/adminFaq.js"></script>
