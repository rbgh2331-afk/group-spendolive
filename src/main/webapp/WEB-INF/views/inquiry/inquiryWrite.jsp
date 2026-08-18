<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<div class="faq-page">
    <div class="page-hero">
        <div class="wrap">
            <p class="eyebrow">INQUIRY</p>
            <h1>문의사항 작성</h1>
            <p class="hero-sub">궁금한 점이나 불편사항을 남겨주세요. 빠르게 도와드리겠습니다.</p>
        </div>
    </div>

    <div class="wrap">
        <div class="tips">
            <h4>문의 전 확인해 보세요</h4>
            <ul>
                <li>자주 묻는 질문(FAQ)에서 빠르게 해결할 수 있는 경우가 많습니다.</li>
                <li>영업일 기준 1~2일 내로 이메일로 답변 드립니다.</li>
                <li>개인정보(비밀번호 등)는 절대 입력하지 마세요.</li>
            </ul>
        </div>

        <%-- AJAX 전환: form submit(페이지 이동) 대신 inquiry.js가 FormData로 fetch 전송.
             파일 첨부가 있어 enctype은 multipart/form-data 유지(FormData가 자동 처리). --%>
        <form class="form-card" id="inquiryWriteForm" enctype="multipart/form-data"
              onsubmit="return false;">

            <h2>문의 내용 입력</h2>

            <div class="field-row">
                <div class="field">
                    <label>카테고리 <span>필수</span></label>
                    <select name="category" required>
                        <option value="" disabled selected>카테고리 선택</option>
                        <option value="ACCOUNT">계정·로그인</option>
                        <option value="EXPENSE">지출관리</option>
                        <option value="OTT">OTT관리</option>
                        <option value="CALENDAR">캘린더</option>
                        <option value="NOTICE">공지·알림</option>
                        <option value="PAYMENT">결제·정산</option>
                        <option value="ETC">기타</option>
                    </select>
                </div>
                <div class="field">
                    <label>문의 유형 <span>필수</span></label>
                    <select name="inquiry_type" required>
                        <option value="" disabled selected>유형 선택</option>
                        <option value="BUG">오류/버그 신고</option>
                        <option value="SUGGEST">기능 개선 제안</option>
                        <option value="HOWTO">사용 방법 문의</option>
                        <option value="ETC">기타 문의</option>
                    </select>
                </div>
            </div>

            <div class="field">
                <label>제목 <span>필수</span></label>
                <input type="text" name="title" id="titleInput" maxlength="50"
                       placeholder="문의 내용을 간략하게 입력해주세요" oninput="countChars('titleInput','titleCount',50)" required>
                <div class="char-count"><span id="titleCount">0</span>/50자</div>
            </div>

            <div class="field">
                <label>상세 내용 <span>필수</span></label>
                <textarea name="content" id="bodyInput" maxlength="1000"
                          placeholder="문제가 발생한 상황, 사용하신 기능, 기대했던 동작 등을 자세히 설명해주세요."
                          oninput="countChars('bodyInput','bodyCount',1000)" required></textarea>
                <div class="char-count"><span id="bodyCount">0</span>/1000자</div>
                <span class="hint">스크린샷이나 오류 메시지가 있다면 아래에 첨부해 주세요.</span>
            </div>

            <div class="field">
                <label>파일 첨부 <span class="field-note">선택 · 최대 5개 / 5MB</span></label>
                <label class="upload-area" for="attachmentInput">
                    <div class="upload-icon">📎</div>
                    <p><strong>파일을 드래그하거나 클릭해서 업로드</strong><br>PNG, JPG, GIF, PDF · 파일당 최대 5MB</p>
                </label>
                <input type="file" id="attachmentInput" name="attachments" multiple
                       accept=".png,.jpg,.jpeg,.gif,.pdf" class="is-hidden"
                       onchange="handleFileSelect(this)">
                <div class="upload-filenames" id="uploadFileNames"></div>
            </div>

            <div class="privacy-box">
                <h4>개인정보 수집 및 이용 동의</h4>
                <p>
                    SpendOlive는 문의 접수 및 답변을 위해 아래와 같이 개인정보를 수집·이용합니다.
                    수집 항목은 아이디, 이메일 주소이며(문의 내용에 개인정보를 직접 작성하신 경우 해당 내용 포함),
                    이용 목적은 문의 내용 확인 및 답변 안내입니다.
                    보유 및 이용 기간은 문의 처리 완료 후 3년이며, 이후 즉시 파기됩니다.
                </p>
                <label class="check-row">
                    <input type="checkbox" id="privacyCheck" name="privacyAgree" required>
                    <span>개인정보 수집 및 이용에 동의합니다. <a href="javascript:void(0)" onclick="togglePrivacyDetail(this)">[자세히 보기]</a></span>
                </label>
                <div class="privacy-detail" id="privacyDetail">
                    <table class="privacy-table">
                        <tr><th>수집 항목</th><td>아이디, 이메일 주소 (문의 내용에 개인정보를 직접 작성한 경우 해당 내용 포함)</td></tr>
                        <tr><th>이용 목적</th><td>문의 내용 확인 및 답변 안내</td></tr>
                        <tr><th>보유 및 이용 기간</th><td>문의 처리 완료 후 3년간 보관 후 파기<br>※ 전자상거래 등에서의 소비자보호에 관한 법률에 따른 소비자 불만·분쟁처리 기록 보존 의무 준용</td></tr>
                    </table>
                    <p class="privacy-refuse-note">
                        귀하는 개인정보 수집·이용에 대한 동의를 거부할 권리가 있습니다.
                        다만, 동의하지 않으실 경우 문의 접수 및 답변이 제한될 수 있습니다.
                    </p>
                </div>
            </div>

            <div class="form-actions">
                <a class="btn btn-danger-outline form-action-cancel" href="${contextPath}/spendolive/inquiry/list.do">취소</a>
                <button type="button" id="inquirySubmitBtn" class="btn btn-primary form-action-submit">문의 제출하기</button>
            </div>
        </form>
    </div>
</div>

<script src="${contextPath}/resources/js/faq.js"></script>
<script src="${contextPath}/resources/js/notice.js"></script>
<script src="${contextPath}/resources/js/inquiry.js"></script>
