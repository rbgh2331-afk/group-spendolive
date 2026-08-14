<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- 관리자 공지 목록 --%>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="admin-main" data-admin-page="notice">
    <div id="adminBoardArea">
        <div class="hero"><div><p class="hero-kicker">ADMIN</p><h1>공지사항 관리</h1><p>사용자 화면(알림&공지사항)에 노출되는 공지를 추가·수정·삭제합니다.</p></div></div>

        <div class="panel">
            <div class="panel-header"><div class="panel-title"><p class="section-kicker">NOTICE LIST</p><h2>공지사항 목록 (총 <span id="noticeTotalCount">0</span>건)</h2></div><button type="button" id="noticeCreateBtn" class="btn primary">+ 새 공지 작성</button></div>

            <div class="table-wrap">
                <table class="admin-table admin-table-fixed">
                    <colgroup>
                        <col style="width:56px">
                        <col style="width:70px">
                        <col>
                        <col style="width:110px">
                        <col style="width:100px">
                        <col style="width:140px">
                    </colgroup>
                    <thead><tr><th>번호</th><th>구분</th><th>제목</th><th>작성자</th><th>등록일</th><th>관리</th></tr></thead>
                    <tbody id="adminNoticeTableBody"><tr><td colspan="6"><div class="admin-empty-filter">불러오는 중...</div></td></tr></tbody>
                </table>
            </div>

            <div id="adminNoticePagination" class="admin-pagination"></div>
        </div>

        <div class="modal" id="adminNoticeModal">
            <div class="modal-box modal-admin-inquiry">
                <button type="button" class="modal-close" data-action="closeNoticeModal">✕</button>
                <form id="noticeModalForm" onsubmit="return false;">
                    <input type="hidden" id="modalNoticeId" name="notice_id" value="0">

                    <div class="panel-title"><p class="section-kicker">NOTICE</p><h2 id="noticeModalHeading">새 공지사항 등록</h2><br></div>

                    <div class="form-field"><label for="modalNoticeTitleInput">제목</label><input type="text" id="modalNoticeTitleInput" name="title" class="form-input" placeholder="공지 제목을 입력하세요" required><br></div>

                    <div class="form-field"><label>구분</label><label class="expose-check"><input type="checkbox" id="modalNoticePinned" name="pinned_yn" value="Y"><span>중요 공지로 설정</span></label><br></div>

                    <div class="form-field"><label for="modalNoticeContent">내용</label><textarea id="modalNoticeContent" name="content" class="form-textarea" placeholder="공지 내용을 입력하세요" required></textarea><br></div>

                    <div class="toolbar"><span></span><div class="toolbar-left">
                        <button type="button" class="btn ghost" data-action="closeNoticeModal">취소</button>
                        <button type="button" id="noticeModalSubmitBtn" class="btn primary">등록</button>
                    </div></div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="${contextPath}/resources/js/adminNotice.js"></script>
