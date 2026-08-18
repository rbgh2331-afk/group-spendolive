<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 관리자 문의 상세 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="inquiry" data-admin-title="문의 상세">
    <section class="hero"><div><p class="hero-kicker">Inquiry Detail</p><h1>문의 상세</h1><p>문의 내용을 확인하고 현재 화면에서 답변과 처리 상태를 저장합니다.</p></div></section>

    <c:if test="${not empty msg}"><div class="flash-ok"><c:out value="${msg}" /></div></c:if>
    <c:if test="${not empty errorMsg}"><div class="flash-err"><c:out value="${errorMsg}" /></div></c:if>

    <section class="panel">
        <div class="panel-header">
            <div class="panel-title">
                <p class="section-kicker">문의 #${inquiry.inquiry_id}</p><h2><c:out value="${inquiry.title}" /></h2>
                <p><c:out value="${inquiry.category}" /> · <c:out value="${inquiry.inquiry_type}" /> · 작성자 <c:out value="${not empty inquiry.writer_nickname ? inquiry.writer_nickname : inquiry.id}" /> · <c:out value="${inquiry.reg_date}" />
                <c:choose><c:when test="${inquiry.statusCode eq 'done'}"><span class="badge green">답변완료</span></c:when><c:when test="${inquiry.statusCode eq 'review'}"><span class="badge blue">검토중</span></c:when><c:otherwise><span class="badge yellow">답변대기</span></c:otherwise></c:choose></p>
            </div>
            <a href="${contextPath}/admin/inquiry/list.do" class="btn ghost">목록으로</a>
        </div>

        <div class="form-field"><label for="inquiryContent">문의 내용</label><textarea id="inquiryContent" class="form-textarea" readonly><c:out value="${inquiry.content}" /></textarea></div>

        <c:if test="${not empty inquiry.files}">
            <div class="form-field"><label>첨부파일</label><div class="toolbar-left">
                <c:forEach var="file" items="${inquiry.files}">
                    <c:choose>
                        <c:when test="${file.image}"><a href="${contextPath}/spendolive/inquiry/file/${file.file_id}" target="_blank" rel="noopener"><img src="${contextPath}/spendolive/inquiry/file/${file.file_id}" alt="<c:out value='${file.origin_name}' />" class="admin-inquiry-image"></a></c:when>
                        <c:otherwise><a href="${contextPath}/spendolive/inquiry/file/${file.file_id}" target="_blank" rel="noopener" class="mini-btn">📎 <c:out value="${file.origin_name}" /></a></c:otherwise>
                    </c:choose>
                </c:forEach>
            </div></div>
        </c:if>

        <form class="admin-reply-form" onsubmit="return false;">
            <input type="hidden" name="inquiry_id" value="${inquiry.inquiry_id}">
            <div class="form-field"><label for="reply_content">답변 내용</label><textarea id="reply_content" name="reply_content" class="form-textarea" placeholder="답변 내용을 입력하세요" required><c:out value="${inquiry.reply_content}" /></textarea></div>
            <div class="form-field"><label for="status">처리 상태</label><select id="status" name="status" class="form-input"><option value="DONE" ${inquiry.status eq 'REVIEW' ? '' : 'selected'}>답변 완료</option><option value="REVIEW" ${inquiry.status eq 'REVIEW' ? 'selected' : ''}>검토 중</option></select></div>
            <div class="toolbar"><span></span><div class="toolbar-left"><button type="submit" class="btn primary">${empty inquiry.reply_content ? '답변 등록' : '답변 수정'}</button></div></div>
        </form>
    </section>
</div>

<script src="${contextPath}/resources/js/adminInquiry.js"></script>
