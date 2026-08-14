<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="faq">
    <div class="hero">
        <div>
            <p class="hero-kicker">ADMIN</p>
            <h1>FAQ 관리</h1>
            <p>사용자 화면(자주 묻는 질문)에 노출되는 FAQ를 추가·수정·삭제합니다.</p>
        </div>
    </div>

    <c:if test="${not empty msg}"><div class="flash-ok">${msg}</div></c:if>
    <c:if test="${not empty errorMsg}"><div class="flash-err">⚠ ${errorMsg}</div></c:if>

    <div id="adminBoardArea">
        <div class="admin-board-tabs">
            <a href="${contextPath}/admin/inquiry/list.do" class="admin-board-tab">문의사항</a>
            <a href="${contextPath}/spendolive/admin/faq/list.do" class="admin-board-tab active">자주 묻는 질문</a>
        </div>

        <div class="panel">
            <div class="panel-header">
                <div class="panel-title">
                    <p class="section-kicker">FAQ LIST</p>
                    <h2>FAQ 목록 (총 ${faqList.size()}건)</h2>
                </div>
                <button type="button" class="btn primary" data-action="create">+ 새 FAQ 작성</button>
            </div>

            <c:if test="${empty faqGroups}">
                <div class="admin-empty-filter">등록된 FAQ가 없습니다.</div>
            </c:if>

            <c:forEach var="entry" items="${faqGroups}">
                <h3 class="category-group-heading">${entry.value[0].categoryLabel}<span class="category-count">${entry.value.size()}건</span></h3>

                <div class="table-wrap">
                    <table class="admin-table">
                        <thead>
                            <tr><th>번호</th><th>질문</th><th>노출</th><th>관리</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="faq" items="${entry.value}" varStatus="s">
                                <tr>
                                    <td>${s.count}</td>
                                    <td><a href="javascript:void(0)" data-action="edit" data-faq-id="${faq.faq_id}">${faq.question}</a></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${faq.use_yn == 'Y'}"><span class="badge green">노출</span></c:when>
                                            <c:otherwise><span class="badge gray">숨김</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div class="table-actions">
                                            <button type="button" class="mini-btn" data-action="moveUp" data-faq-id="${faq.faq_id}" ${s.index == 0 ? 'disabled' : ''}>▲</button>
                                            <button type="button" class="mini-btn" data-action="moveDown" data-faq-id="${faq.faq_id}" ${s.index == entry.value.size()-1 ? 'disabled' : ''}>▼</button>
                                            <button type="button" class="mini-btn" data-action="edit" data-faq-id="${faq.faq_id}">수정</button>
                                            <button type="button" class="mini-btn danger" data-action="delete" data-faq-id="${faq.faq_id}">삭제</button>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:forEach>
        </div>

        <template id="adminFaqCreateTpl">
            <form class="faq-modal-form" data-faq-id="0" onsubmit="return false;">
                <div class="panel-title"><p class="section-kicker">FAQ</p><h2>새 FAQ 등록</h2></div>

                <div class="form-field">
                    <label for="faqCreateCategory">카테고리 (필수)</label>
                    <select id="faqCreateCategory" name="category" class="form-input" required>
                        <option value="">선택하세요</option>
                        <option value="account">계정·로그인</option>
                        <option value="expense">지출관리</option>
                        <option value="ott">OTT관리</option>
                        <option value="notice">공지·알림</option>
                        <option value="etc">기타</option>
                    </select>
                    <p class="admin-form-note">새로 등록하면 선택한 카테고리의 마지막 순서에 추가됩니다.</p>
                </div>

                <div class="form-field">
                    <label for="faqCreateQuestion">질문 (필수)</label>
                    <input type="text" id="faqCreateQuestion" name="question" class="form-input" placeholder="질문을 입력하세요" required>
                </div>
                <br>
                <div class="form-field">
                    <label for="faqCreateAnswer">답변 (필수)</label>
                    <textarea id="faqCreateAnswer" name="answer" class="form-textarea" placeholder="답변 내용을 입력하세요" required></textarea>
                </div>

                <label class="expose-check"><input type="checkbox" name="useYn" value="Y" checked><span>사용자 화면에 노출</span></label>

                <div class="toolbar"><span></span><div class="toolbar-left">
                    <button type="button" class="btn ghost" data-action="closeModal">취소</button>
                    <button type="submit" class="btn primary">등록</button>
                </div></div>
            </form>
        </template>

        <%-- FAQ마다 수정용 숨김 템플릿을 하나씩 미리 만들어둠 (adminFaqEditTpl{id}).
             수정 버튼 누르면 adminFaq.js가 이 템플릿을 모달 안으로 복사해서 보여줌
             (서버에 다시 안 물어보고 이미 렌더링된 값을 그대로 재사용) --%>
        <c:forEach var="entry" items="${faqGroups}">
            <c:forEach var="faq" items="${entry.value}">
                <template id="adminFaqEditTpl${faq.faq_id}">
                    <form class="faq-modal-form" data-faq-id="${faq.faq_id}" onsubmit="return false;">
                        <div class="panel-title"><p class="section-kicker">FAQ</p><h2>FAQ 수정</h2></div>

                        <div class="form-field">
                            <label for="faqCategory${faq.faq_id}">카테고리 (필수)</label>
                            <select id="faqCategory${faq.faq_id}" name="category" class="form-input" required>
                                <option value="">선택하세요</option>
                                <option value="account" ${faq.category == 'account' ? 'selected' : ''}>계정·로그인</option>
                                <option value="expense" ${faq.category == 'expense' ? 'selected' : ''}>지출관리</option>
                                <option value="ott" ${faq.category == 'ott' ? 'selected' : ''}>OTT관리</option>
                                <option value="notice" ${faq.category == 'notice' ? 'selected' : ''}>공지·알림</option>
                                <option value="etc" ${faq.category == 'etc' ? 'selected' : ''}>기타</option>
                            </select>
                        </div>

                        <div class="form-field">
                            <label for="faqQuestion${faq.faq_id}">질문 (필수)</label>
                            <input type="text" id="faqQuestion${faq.faq_id}" name="question" class="form-input" value="${faq.question}" required>
                        </div>

                        <div class="form-field">
                            <label for="faqAnswer${faq.faq_id}">답변 (필수)</label>
                            <textarea id="faqAnswer${faq.faq_id}" name="answer" class="form-textarea" required>${faq.answer}</textarea>
                        </div>

                        <label class="expose-check"><input type="checkbox" name="useYn" value="Y" ${faq.use_yn == 'Y' ? 'checked' : ''}><span>사용자 화면에 노출</span></label>

                        <div class="toolbar"><span></span><div class="toolbar-left">
                            <button type="button" class="btn ghost" data-action="closeModal">취소</button>
                            <button type="submit" class="btn primary">수정</button>
                        </div></div>
                    </form>
                </template>
            </c:forEach>
        </c:forEach>

        <div class="modal" id="adminFaqModal">
            <div class="modal-box modal-admin-inquiry">
                <button type="button" class="modal-close" data-action="closeModal">✕</button>
                <div id="adminFaqModalBody"></div>
            </div>
        </div>
    </div>
</div>

<%-- 순서 중요: adminFaq.js가 adminInquiry.js의 전역 soAlert/soConfirm을 갖다 쓰므로
     adminInquiry.js가 먼저 로드돼야 함 --%>
<script src="${contextPath}/resources/js/adminInquiry.js"></script>
<script src="${contextPath}/resources/js/adminFaq.js"></script>
