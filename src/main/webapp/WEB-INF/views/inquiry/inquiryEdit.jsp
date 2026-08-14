<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="faq-page">
    <div class="page-hero">
        <div class="wrap">
            <p class="eyebrow">INQUIRY</p>
            <h1>문의사항 수정</h1>
            <p class="hero-sub">아직 답변이 등록되지 않은 문의만 수정할 수 있어요.</p>
        </div>
    </div>

    <div class="wrap">
        <%-- 첨부파일은 이번 수정에서는 그대로 유지됩니다 (추가/삭제 불가). 첨부파일을 바꾸고 싶다면
             이 문의를 삭제하고 새로 작성해 주세요. --%>
        <%-- AJAX 전환: inquiry.js가 fetch(ajax/edit.do)로 전송. 첨부파일은 수정 대상 아님 --%>
        <form class="form-card" id="inquiryEditForm" onsubmit="return false;">

            <input type="hidden" name="inquiryNo" id="editInquiryNo" value="${inquiry.inquiry_id}">

            <h2>문의 내용 수정</h2>

            <div class="field-row">
                <div class="field">
                    <label>카테고리 <span>필수</span></label>
                    <select name="category" required>
                        <option value="" disabled>카테고리 선택</option>
                        <option value="ACCOUNT"  ${inquiry.category == 'ACCOUNT'  ? 'selected' : ''}>계정·로그인</option>
                        <option value="EXPENSE"  ${inquiry.category == 'EXPENSE'  ? 'selected' : ''}>지출관리</option>
                        <option value="OTT"      ${inquiry.category == 'OTT'      ? 'selected' : ''}>OTT관리</option>
                        <option value="CALENDAR" ${inquiry.category == 'CALENDAR' ? 'selected' : ''}>캘린더</option>
                        <option value="NOTICE"   ${inquiry.category == 'NOTICE'   ? 'selected' : ''}>공지·알림</option>
                        <option value="PAYMENT"  ${inquiry.category == 'PAYMENT'  ? 'selected' : ''}>결제·정산</option>
                        <option value="ETC"      ${inquiry.category == 'ETC'      ? 'selected' : ''}>기타</option>
                    </select>
                </div>
                <div class="field">
                    <label>문의 유형 <span>필수</span></label>
                    <select name="inquiry_type" required>
                        <option value="" disabled>유형 선택</option>
                        <option value="BUG"     ${inquiry.inquiry_type == 'BUG'     ? 'selected' : ''}>오류/버그 신고</option>
                        <option value="SUGGEST" ${inquiry.inquiry_type == 'SUGGEST' ? 'selected' : ''}>기능 개선 제안</option>
                        <option value="HOWTO"   ${inquiry.inquiry_type == 'HOWTO'   ? 'selected' : ''}>사용 방법 문의</option>
                        <option value="ETC"     ${inquiry.inquiry_type == 'ETC'     ? 'selected' : ''}>기타 문의</option>
                    </select>
                </div>
            </div>

            <div class="field">
                <label>제목 <span>필수</span></label>
                <input type="text" name="title" id="titleInput" maxlength="50"
                       value="${fn:escapeXml(inquiry.title)}"
                       oninput="countChars('titleInput','titleCount',50)" required>
                <div class="char-count"><span id="titleCount">${fn:length(inquiry.title)}</span>/50자</div>
            </div>

            <div class="field">
                <label>상세 내용 <span>필수</span></label>
                <textarea name="content" id="bodyInput" maxlength="1000"
                          oninput="countChars('bodyInput','bodyCount',1000)" required><c:out value="${inquiry.content}" /></textarea>
                <div class="char-count"><span id="bodyCount">${fn:length(inquiry.content)}</span>/1000자</div>
            </div>

            <c:if test="${not empty inquiry.files}">
                <div class="field">
                    <label>첨부파일 <span class="field-note">수정 시 변경 불가 · 삭제 후 재작성 필요</span></label>
                    <div class="inq-attachments">
                        <c:forEach var="file" items="${inquiry.files}">
                            <c:choose>
                                <c:when test="${file.image}">
                                    <img src="${contextPath}/spendolive/inquiry/file/${file.file_id}"
                                        alt="${fn:escapeXml(file.origin_name)}" class="inq-thumb">
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

            <div class="form-actions">
                <a class="btn btn-danger-outline form-action-cancel" href="${contextPath}/spendolive/inquiry/list.do">취소</a>
                <button type="button" id="inquiryEditSubmitBtn" class="btn btn-primary form-action-submit">수정 완료</button>
            </div>
        </form>
    </div>
</div>

<script src="${contextPath}/resources/js/faq.js"></script>
<script src="${contextPath}/resources/js/inquiry.js"></script>
