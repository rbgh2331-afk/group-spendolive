<%-- [AJAX 변경 주석] 회원정보·계좌명·주계좌·주카드 폼은 기존 action을 호환용으로 유지하고 AJAX 전용 주소를 추가했다. --%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />

<section class="page-hero">
    <div class="container">
        <p class="eyebrow">MY PAGE</p>
        <h1>마이페이지</h1>
        <p class="hero-text">프로필, 이번 달 지출 총액, 계좌연동, 신고/차단 관리, 나의 OTT 공유방을 한 화면에서 확인합니다.</p>
    </div>
</section>

<section class="section compact mypage-page" data-context-path="${contextPath}">
    <div class="container">
        <c:if test="${param.profileUpdated == 'Y'}">
            <div class="alert done">회원정보가 수정되었습니다.</div>
        </c:if>
        <c:if test="${param.profileError == 'passwordMismatch'}">
            <div class="alert warn">새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.</div>
        </c:if>
        <c:if test="${param.profileError == 'currentPasswordMismatch'}">
            <div class="alert warn">현재 비밀번호가 일치하지 않습니다.</div>
        </c:if>
        <c:if test="${param.profileError == 'passwordCheckRequired'}">
            <div class="alert warn">비밀번호 변경 전 확인 버튼을 눌러주세요.</div>
        </c:if>
        <c:if test="${param.profileError == 'emailNotVerified'}">
            <div class="alert warn">이메일을 변경하려면 이메일 인증을 완료해야 합니다.</div>
        </c:if>
        <c:if test="${param.profileError == 'phoneNotVerified'}">
            <div class="alert warn">전화번호를 변경하려면 전화번호 인증을 완료해야 합니다.</div>
        </c:if>
        <c:if test="${param.profileError == 'updateFailed'}">
            <div class="alert warn">회원정보 수정 중 오류가 발생했습니다. 이메일/전화번호 중복 여부를 확인해 주세요.</div>
        </c:if>
        <%-- =====================================================
             [마이페이지 계좌·카드 연결 추가]
             계좌 제목 수정 결과 메시지
             ===================================================== --%>
        <c:if test="${param.accountNameUpdated == 'Y'}">
            <div class="alert done">계좌 제목이 수정되었습니다.</div>
        </c:if>
        <c:if test="${param.assetError == 'invalidAccountName'}">
            <div class="alert warn">계좌 제목은 1자 이상 20자 이하로 입력해주세요.</div>
        </c:if>
        <c:if test="${param.assetError == 'accountNameUpdateFailed'}">
            <div class="alert warn">계좌 제목 수정 중 오류가 발생했습니다.</div>
        </c:if>
        <%-- 주계좌 변경 결과 메시지 --%>
        <c:if test="${param.primaryAccountUpdated == 'Y'}">
            <div class="alert done">주계좌가 변경되었습니다.</div>
        </c:if>
        <c:if test="${param.assetError == 'primaryAccountUpdateFailed'}">
            <div class="alert warn">주계좌 변경 중 오류가 발생했습니다.</div>
        </c:if>

        <c:if test="${param.withdrawError == 'confirmRequired'}">
            <div class="alert warn">회원탈퇴를 진행하려면 확인 문구를 정확히 입력해주세요.</div>
        </c:if>
        <c:if test="${param.withdrawError == 'failed'}">
            <div class="alert warn">회원탈퇴 처리 중 오류가 발생했습니다. 다시 시도해 주세요.</div>
        </c:if>

        <div class="mypage-top-grid">
            <article class="card mypage-profile-card">
                <p class="eyebrow">PROFILE</p>
                <div class="avatar">${profileInitial}</div>
                <div>
                    <h3>${memberInfo.member_name}</h3>
                    <p class="mypage-muted">닉네임 : ${memberInfo.nickname}</p>
                    <p class="mypage-muted">아이디 : ${memberInfo.id}</p>
                    <p class="mypage-muted">가입일 : ${memberInfo.created_at}</p>
                </div>
                <%-- [마이페이지 화면 전환 추가] 최초에는 숨기고 버튼을 눌렀을 때 회원정보 수정 영역 표시 --%>
                <button type="button" class="btn btn-primary full" onclick="showMyPagePanel('profile-edit')">회원정보 수정</button>
            </article>

            <article class="card mypage-stat-card">
                <p class="eyebrow">MONTHLY EXPENSE</p>
                <h3>이번 달 지출 총액</h3>
                <strong class="mypage-monthly-expense-amount">
                    <fmt:formatNumber value="${thisMonthExpenseTotal}" pattern="#,##0" />원
                </strong>

                <%-- 이번 달 예산과 지출 대비 사용률을 표시한다. --%>
                <div class="mypage-budget-summary">
                    <div>
                        <span>이번 달 예산</span>
                        <c:choose>
                            <c:when test="${thisMonthBudget > 0}">
                                <strong>
                                    <fmt:formatNumber value="${thisMonthBudget}" pattern="#,##0" />원
                                </strong>
                            </c:when>
                            <c:otherwise>
                                <strong>미설정</strong>
                            </c:otherwise>
                        </c:choose>
                    </div>

                    <c:if test="${thisMonthBudget > 0}">
                        <div>
                            <span>예산 사용률</span>
                            <strong>${thisMonthBudgetPercent}%</strong>
                        </div>
                    </c:if>
                </div>

                <p class="mypage-muted">이번 달 등록된 지출 내역과 설정 예산입니다.</p>
                <a href="${contextPath}/spendolive/expense/list.do" class="btn btn-primary full">지출관리로 이동</a>
            </article>

            <%-- =====================================================
                 [마이페이지 주계좌 표시 추가]
                 STATUS가 YES인 계좌만 상단 계좌관리 카드에 표시한다.
                 연결 계좌는 있지만 주계좌가 없으면 목록에서 선택하도록 안내한다.
                 ===================================================== --%>
            <article class="card mypage-bank-card">
                <p class="eyebrow">OPEN BANKING</p>
                <h3>계좌관리</h3>
                <c:choose>
                    <c:when test="${not empty currentAccount}">
                        <c:set var="currentBankName" value="${bankNameMap[currentAccount.bank_code]}" />
                        <p class="mypage-muted">현재 주계좌</p>
                        <div class="mypage-account-box">
                            <span><c:out value="${empty currentAccount.account_name ? '주계좌' : currentAccount.account_name}" /></span>
                            <strong>
                                <c:out value="${empty currentBankName ? currentAccount.bank_code : currentBankName}" />
                                · <c:out value="${currentAccount.account_number}" />
                            </strong>
                            <span>잔여금</span>
                            <strong><fmt:formatNumber value="${currentAccount.balance}" pattern="#,##0" />원</strong>
                        </div>
                    </c:when>
                    <c:when test="${not empty accountList}">
                        <p class="mypage-muted">연결된 계좌 중 주계좌를 선택해주세요.</p>
                        <div class="mypage-account-box">
                            <span>현재 주계좌</span>
                            <strong>설정된 주계좌가 없습니다.</strong>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="mypage-muted">현재 연결된 계좌가 없습니다.</p>
                        <div class="mypage-account-box">
                            <span>현재 주계좌</span>
                            <strong>계좌 정보가 없습니다.</strong>
                        </div>
                    </c:otherwise>
                </c:choose>
                <button type="button" class="btn btn-primary full" onclick="showMyPagePanel('asset-manage')">나의 계좌 · 카드 목록보기</button>
            </article>

            <article class="card mypage-report-card">
                <p class="eyebrow">REPORT</p>
                <h3>신고 · 차단관리</h3>
                <div class="mypage-report-summary mypage-report-summary-vertical">
                    <div class="mypage-report-line">
                        <span>내 패널티</span>
                        <strong>${warning_count}번째</strong>
                    </div>
                    <div class="mypage-report-line">
                        <span>내가 신고한 건수</span>
                        <strong>${myReportCount}건</strong>
                    </div>
                </div>
                <%-- [마이페이지 화면 전환 추가] 버튼을 눌렀을 때 신고·차단 영역 표시 --%>
                <button type="button" class="btn btn-primary full" onclick="showMyPagePanel('report-manage')">신고/차단 내역 보기</button>
            </article>
        </div>

        <%-- [마이페이지 화면 전환 추가] 최초 진입 시 숨겨지는 회원정보 수정 영역 --%>
        <article id="profile-edit" class="card mypage-panel mypage-profile-edit mypage-toggle-panel is-hidden">
            <div class="mypage-panel-head">
                <div>
                    <p class="eyebrow">EDIT PROFILE</p>
                    <h2>회원정보 수정</h2>
                </div>
                <span>비밀번호 변경 시 확인 입력이 필요합니다.</span>
            </div>

            <%-- 기존 일반 POST action은 호환용이며, data-ajax-action이 실제 비동기 처리 주소다. --%>
            <form action="${contextPath}/spendolive/mypage/update.do" method="post" class="mypage-edit-form" id="mypageProfileForm" data-ajax-form data-ajax-action="/spendolive/mypage/ajax/update.do" data-loading-message="회원정보를 저장하고 있습니다.">
                <input type="hidden" id="originalEmail" value="${memberInfo.email}">
                <input type="hidden" id="originalPhone" value="${memberInfo.phone}">
                <input type="hidden" id="emailVerified" value="N">
                <input type="hidden" id="phoneVerified" value="N">
                <input type="hidden" id="passwordChecked" name="passwordChecked" value="N">

                <div class="mypage-form-section">
                    <div class="mypage-form-section-head">
                        <h3>기본 정보</h3>
                        <p>이름과 닉네임을 수정합니다.</p>
                    </div>
                    <div class="mypage-form-stack">
                        <label class="mypage-field">
                            이름
                            <input type="text" name="member_name" value="${memberInfo.member_name}" required>
                        </label>
                        <label class="mypage-field">
                            닉네임
                            <input type="text" name="nickname" value="${memberInfo.nickname}">
                        </label>
                    </div>
                </div>

                <div class="mypage-form-section">
                    <div class="mypage-form-section-head">
                        <h3>연락처 인증</h3>
                        <p>이메일 또는 전화번호를 바꿀 때만 인증을 진행하면 됩니다.</p>
                    </div>

                    <div class="mypage-verify-group">
                        <div class="mypage-field-with-button">
                            <label class="mypage-field">
                                이메일
                                <input type="email" name="email" id="mypageEmail" value="${memberInfo.email}" required>
                            </label>
                            <%-- [공통 AJAX 로딩 적용] this를 전달해 요청 중 클릭한 버튼만 잠그고 완료 후 복구한다. --%>
                            <button type="button" class="btn btn-primary full" onclick="sendMyPageEmailCode(this)">이메일 인증</button>
                        </div>
                        <div class="mypage-code-row">
                            <input type="text" id="mypageEmailCode" placeholder="이메일 인증번호 입력">
                            <button type="button" class="btn btn-primary full" onclick="verifyMyPageEmailCode(this)">확인</button>
                        </div>
                        <p class="mypage-help" id="emailVerifyMessage">이메일을 변경할 때만 인증이 필요합니다.</p>
                    </div>

                    <div class="mypage-verify-group">
                        <div class="mypage-field-with-button">
                            <label class="mypage-field">
                                전화번호
                                <input type="text" name="phone" id="mypagePhone" value="${memberInfo.phone}">
                            </label>
                            <%-- [공통 AJAX 로딩 적용] 휴대전화 인증도 동일한 팝업·버튼 잠금 규격을 사용한다. --%>
                            <button type="button" class="btn btn-primary full" onclick="sendMyPagePhoneCode(this)">전화번호 인증</button>
                        </div>
                        <div class="mypage-code-row">
                            <input type="text" id="mypagePhoneCode" placeholder="문자 인증번호 입력">
                            <button type="button" class="btn btn-primary full" onclick="verifyMyPagePhoneCode(this)">확인</button>
                        </div>
                        <p class="mypage-help" id="phoneVerifyMessage">전화번호를 변경할 때만 인증이 필요합니다.</p>
                    </div>
                </div>

                <div class="mypage-form-section">
                    <div class="mypage-form-section-head">
                        <h3>비밀번호 변경</h3>
                        <p>비밀번호를 바꾸지 않을 경우 아래 입력칸은 비워두면 됩니다.</p>
                    </div>
                    <div class="mypage-form-stack">
                        <label class="mypage-field">
                            현재 비밀번호
                            <input type="password" name="currentPassword" id="currentPassword" placeholder="비밀번호 변경 시 입력">
                        </label>
                        <label class="mypage-field">
                            새 비밀번호
                            <input type="password" name="password" id="newPassword" placeholder="변경할 때만 입력">
                        </label>
                        <div class="mypage-verify-group">
                            <div class="mypage-field-with-button">
                                <label class="mypage-field">
                                    새 비밀번호 확인
                                    <input type="password" name="passwordConfirm" id="passwordConfirm" placeholder="새 비밀번호 재입력">
                                </label>
                                <button type="button" class="btn btn-primary full" onclick="checkMyPagePassword()">비밀번호 확인</button>
                            </div>
                            <p class="mypage-help" id="passwordCheckMessage">비밀번호를 변경할 때는 현재 비밀번호와 새 비밀번호 확인이 필요합니다.</p>
                        </div>
                    </div>
                </div>

                <div class="mypage-form-actions">
                    <button type="submit" class="btn btn-primary">수정 완료</button>
                    <a href="${contextPath}/spendolive/mypage.do" class="btn btn-danger-outline">취소</a>
                </div>
            </form>
        </article>

        <%-- [마이페이지 화면 전환 추가] 최초 진입 시 숨겨지는 신고·차단 내역 영역 --%>
        <article id="report-manage" class="card mypage-panel mypage-toggle-panel is-hidden">
            <div class="mypage-panel-head">
                <div>
                    <p class="eyebrow">REPORT HISTORY</p>
                    <h2>신고 내역</h2>
                </div>
                <span>내가 신고한 상대와 처리 상태를 확인합니다.</span>
            </div>

            <c:choose>
                <c:when test="${empty myReportList}">
                    <div class="mypage-empty">내가 신고한 내역이 없습니다.</div>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table class="mypage-table">
                            <thead>
                                <tr>
                                    <th>신고 상대</th>
                                    <th>상세 이유</th>
                                    <th>접수 상태</th>
                                    <th>차단 유무</th>
                                    <th>신고일</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="report" items="${myReportList}">
                                    <tr>
                                        <td>

                                            <strong>${report.reported_member_nickname}</strong>
                                            <small>${report.reported_member_id}</small>

                                        </td>
                                        <td class="mypage-reason">${report.report_reason}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${report.report_status == 'WAIT'}"><span class="chip wait">접수</span></c:when>
                                                <c:when test="${report.report_status == 'PROCESSING'}"><span class="chip request">처리중</span></c:when>
                                                <c:when test="${report.report_status == 'COMPLETE'}"><span class="chip done">처리완료</span></c:when>
                                                <c:when test="${report.report_status == 'REJECT'}"><span class="chip muted-chip">반려</span></c:when>
                                                <c:otherwise><span class="chip muted-chip">${report.report_status}</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${report.blocked_yn == 'Y'}"><span class="chip done">차단됨</span></c:when>
                                                <c:otherwise><span class="chip muted-chip">미차단</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>${report.created_at}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
        </article>

        <%-- =====================================================
             [마이페이지 계좌·카드 연결 추가 시작]
             담당자가 만든 조회 결과를 계좌와 카드로 나누어 출력한다.
             한 페이지에 각각 4칸을 보여주며 부족한 칸은 JavaScript가 빈 칸으로 채운다.
             계좌의 수정 버튼은 ACCOUNT_NAME을 변경하고 거래내역 버튼은 Ajax로 상세 내역을 조회한다.
             ===================================================== --%>
        <article id="asset-manage" class="card mypage-panel mypage-toggle-panel is-hidden">
            <div class="mypage-panel-head">
                <div>
                    <p class="eyebrow">MY ASSETS</p>
                    <h2>나의 계좌 · 카드 목록</h2>
                </div>
                <span>계좌와 카드를 각각 한 화면에 4개씩 확인합니다.</span>
            </div>

            <section class="mypage-asset-section">
                <div class="mypage-asset-section-head">
                    <h3>계좌</h3>
                    <div class="mypage-asset-pager" data-pager-for="accountAssetList">
                        <button type="button" class="btn btn-outline btn-mini" data-page-direction="prev">이전</button>
                        <span><b data-current-page>1</b> / <b data-total-page>1</b></span>
                        <button type="button" class="btn btn-outline btn-mini" data-page-direction="next">다음</button>
                    </div>
                </div>

                <div class="mypage-asset-list" id="accountAssetList" data-empty-text="계좌 정보가 없습니다.">
                    <c:forEach var="account" items="${accountList}">
                        <c:set var="accountBankName" value="${bankNameMap[account.bank_code]}" />
                        <div class="mypage-asset-item" data-asset-item>
                            <div class="mypage-asset-main">
                                <form action="${contextPath}/spendolive/mypage/account/name/update.do" method="post" class="mypage-asset-title-form" data-ajax-form data-ajax-action="/spendolive/mypage/ajax/account/name/update.do" data-loading-message="계좌 제목을 수정하고 있습니다.">
                                    <input type="hidden" name="accountIdx" value="${account.account_idx}">
                                    <input type="text" name="accountName" maxlength="20" readonly
                                           value="${fn:escapeXml(empty account.account_name ? '계좌' : account.account_name)}"
                                           aria-label="계좌 제목">
                                    <button type="button" class="btn btn-outline btn-mini" onclick="toggleAccountNameEdit(this)">수정</button>
                                                <span>
                                    <button type="button"
                                                       class="btn btn-primary accountdeleteSubmitButton"
                                                       data-account_idx="${account.account_idx}" >계좌 삭제</button></span>
                                </form>
                                <p>
                                    <c:out value="${empty accountBankName ? account.bank_code : accountBankName}" />
                                    계좌번호 - <c:out value="${account.account_number}" />
                                </p>
                            </div>
                            <div class="mypage-account-balance">
                                <span>남은 금액</span>
                                <strong><fmt:formatNumber value="${account.balance}" pattern="#,##0" />원</strong>
                            </div>
                            <%-- 기존 버튼 스타일을 활용해 주계좌 선택과 거래내역 버튼을 함께 표시한다. --%>
                            <div class="mypage-room-actions">
                                <c:choose>
                                    <c:when test="${account.status eq 'YES'}">
                                        <span class="chip done">주계좌</span>
                                    </c:when>
                                    <c:otherwise>
                                        <form action="${contextPath}/spendolive/mypage/account/primary/update.do" method="post">
                                            <input type="hidden" name="accountIdx" value="${account.account_idx}">
                                            <button type="submit" class="btn btn-primary btn-mini" onclick="return confirm('이 계좌를 주계좌로 설정할까요?');">주계좌로 설정</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                                <button type="button"
                                        class="btn btn-outline btn-mini transaction-history-btn"
                                        data-account-idx="${account.account_idx}"
                                        data-account-name="${fn:escapeXml(empty account.account_name ? '계좌' : account.account_name)}"
                                        data-bank-name="${fn:escapeXml(empty accountBankName ? account.bank_code : accountBankName)}"
                                        data-account-number="${fn:escapeXml(account.account_number)}"
                                        data-current-balance="${account.balance}">거래내역
                                </button>
                            </div>
                        </div>
                    </c:forEach>
                </div>

                <%-- 선택한 계좌의 거래내역과 거래 직후 잔액을 표시한다. --%>
                <div id="accountTransactionPanel" class="mypage-form-section is-hidden" hidden>
                    <div class="mypage-form-section-head">
                        <div>
                            <h3 id="accountTransactionTitle">계좌 거래내역</h3>
                            <p id="accountTransactionAccountInfo">계좌를 선택해주세요.</p>
                        </div>
                        <button type="button" class="btn btn-outline btn-mini" onclick="closeAccountTransactions()">닫기</button>
                    </div>

                    <div class="mypage-account-box">
                        <span>현재 잔액</span>
                        <strong id="accountTransactionCurrentBalance">0원</strong>
                    </div>

                    <div class="table-wrap">
                        <table class="mypage-table">
                            <thead>
                                <tr>
                                    <th>거래일시</th>
                                    <th>구분</th>
                                    <th>거래금액</th>
                                    <th>거래 후 잔액</th>
                                </tr>
                            </thead>
                            <tbody id="accountTransactionBody">
                                <tr>
                                    <td colspan="4">거래내역 버튼을 눌러주세요.</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>

                    <%-- 거래내역이 10건을 초과하면 기존 계좌·카드 페이지 버튼 스타일로 페이지를 이동한다. --%>
                    <div id="accountTransactionPager" class="mypage-form-actions mypage-asset-pager is-hidden" hidden>
                        <button type="button" class="btn btn-outline btn-mini" data-transaction-page="prev">이전</button>
                        <span><b id="accountTransactionCurrentPage">1</b> / <b id="accountTransactionTotalPage">1</b></span>
                        <button type="button" class="btn btn-outline btn-mini" data-transaction-page="next">다음</button>
                    </div>

                    <div class="mypage-account-box">
                        <span>계좌 등록 당시 잔액</span>
                        <strong id="accountTransactionInitialBalance">0원</strong>
                    </div>
                </div>
            </section>

            <section class="mypage-asset-section">
                <div class="mypage-asset-section-head">
                    <h3>카드</h3>
                    <div class="mypage-asset-pager" data-pager-for="cardAssetList">
                        <button type="button" class="btn btn-outline btn-mini" data-page-direction="prev">이전</button>
                        <span><b data-current-page>1</b> / <b data-total-page>1</b></span>
                        <button type="button" class="btn btn-outline btn-mini" data-page-direction="next">다음</button>
                    </div>
                </div>

                <div class="mypage-asset-list" id="cardAssetList" data-empty-text="카드 정보가 없습니다.">
                    <c:forEach var="card" items="${cardList}">
                        <%-- CARD_COMPANY에는 issuerCode 원본을 유지하고 화면에서만 카드사명으로 바꾼다. --%>
                        <c:set var="cardCompanyDisplayName"
                               value="${not empty cardCompanyNameMap[card.card_company] ? cardCompanyNameMap[card.card_company] : (empty card.card_company ? '카드' : card.card_company)}" />

                        <%-- 15번 SQL 실행으로 CARD_NAME에 코드가 복사된 기존 데이터는
                             카드 별칭을 직접 수정하기 전까지 카드사명으로 자연스럽게 표시한다. --%>
                        <c:set var="cardDisplayName"
                               value="${empty card.card_name or card.card_name eq card.card_company ? cardCompanyDisplayName : card.card_name}" />

                        <div class="mypage-asset-item mypage-card-item" data-asset-item>
                            <div class="mypage-asset-main">
                                <%-- [마이페이지 카드 이름 수정]
                                     카드사명과 별개로 사용자가 알아보기 쉬운 표시 이름을 저장한다. --%>
                                <form action="${contextPath}/spendolive/mypage/card/name/update.do" method="post"
                                      class="mypage-asset-title-form" data-ajax-form
                                      data-ajax-action="/spendolive/mypage/ajax/card/name/update.do"
                                      data-loading-message="카드 이름을 수정하고 있습니다.">
                                    <input type="hidden" name="cardIdx" value="${card.card_idx}">
                                    <input type="text" name="cardName" maxlength="30" readonly
                                           value="${fn:escapeXml(cardDisplayName)}"
                                           aria-label="카드 이름">
                                    <button type="button" class="btn btn-outline btn-mini" onclick="toggleCardNameEdit(this)">수정</button>
                                    <span>
                                    <button type="button"
                                                       class="btn btn-primary carddeleteSubmitButton"
                                                       data-card_idx="${card.card_idx}">카드 삭제</button></span>
                                </form>
                                <p>
                                    <c:out value="${cardCompanyDisplayName}" />
                                    카드번호 - <c:out value="${card.card_number}" />
                                </p>
                            </div>

                            <%-- 계좌의 주계좌 UI와 같은 버튼·상태 디자인을 재사용한다. --%>
                            <div>
                                <c:choose>
                                    <c:when test="${card.status eq 'YES'}">
                                        <span class="chip done">주카드</span>
                                    </c:when>
                                    <c:otherwise>
                                        <form action="${contextPath}/spendolive/mypage/card/primary/update.do" method="post" data-ajax-form data-ajax-action="/spendolive/mypage/ajax/card/primary/update.do" data-ajax-confirm="이 카드를 주카드로 설정할까요?" data-loading-message="주카드를 변경하고 있습니다.">
                                            <input type="hidden" name="cardIdx" value="${card.card_idx}">
                                            <button type="submit" class="btn btn-primary btn-mini"
                                                    >주카드로 설정</button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </section>
        </article>
        <%-- [마이페이지 계좌·카드 연결 추가 끝] --%>

        <article class="card mypage-panel">
            <div class="mypage-panel-head">
                <div>
                    <p class="eyebrow">FRIENDS ROOM</p>
                    <h2>가족 · 지인들과의 공유방</h2>
                </div>
                <a href="${contextPath}/spendolive/ott/friends.do" class="btn btn-primary">가족 · 지인 공유방 관리</a>
            </div>

            <c:choose>
                <c:when test="${empty friendRoomList}">
                    <div class="mypage-empty">가족 · 지인들과 만든 공유방이 없습니다.</div>
                </c:when>
                <c:otherwise>
                    <div class="mypage-room-list">
                        <c:forEach var="room" items="${friendRoomList}">
                            <div class="mypage-room-card">
                                <div>
                                    <strong>${room.room_name}</strong>
                                    <p>${room.service_name} · ${room.plan_name} · ${room.current_member_count}/${room.member_limit}명</p>
                                    <small>결제일 매월 ${room.billing_day}일 · 상태 ${room.status}</small>
                                </div>
                                <div class="mypage-room-actions">
                                    <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary full">대화방</a>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </article>

        <article class="card mypage-panel">
            <div class="mypage-panel-head">
                <div>
                    <p class="eyebrow">RECRUIT ROOM</p>
                    <h2>외부인들과의 공유방</h2>
                </div>
                <a href="${contextPath}/spendolive/ott/recruit.do" class="btn btn-primary">모든 모집글 관리</a>
            </div>

            <div class="mypage-room-two-col">
                <section>
                    <h3>내가 만든 방</h3>
                    <c:choose>
                        <c:when test="${empty hostedRecruitRoomList}">
                            <div class="mypage-empty small">내가 만든 외부 모집방이 없습니다.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="mypage-room-list compact">
                                <c:forEach var="room" items="${hostedRecruitRoomList}">
                                    <div class="mypage-room-card">
                                        <div>
                                            <strong>${room.room_name}</strong>
                                            <p>${room.service_name} · ${room.current_member_count}/${room.member_limit}명</p>
                                            <small>내가 만든 방 · ${room.status}</small>
                                        </div>
                                        <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary">대화방</a>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section>
                    <h3>내가 신청/참여한 방</h3>
                    <c:choose>
                        <c:when test="${empty joinedRecruitRoomList}">
                            <div class="mypage-empty small">내가 신청하거나 참여한 외부 모집방이 없습니다.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="mypage-room-list compact">
                                <c:forEach var="room" items="${joinedRecruitRoomList}">
                                    <div class="mypage-room-card">
                                        <div>
                                            <strong>${room.room_name}</strong>
                                            <p>${room.service_name} · ${room.current_member_count}/${room.member_limit}명</p>
                                            <small>방장 ${room.host_nickname}</small>
                                            <c:choose>
                                                <c:when test="${room.my_application_status eq 'APPLIED'}">
                                                    <span class="status-pill APPLIED">승인 대기중</span>
                                                </c:when>
                                                <c:when test="${room.my_application_status eq 'REJECTED'}">
                                                    <span class="status-pill REJECTED">거절됨</span>
                                                </c:when>
                                                <c:when test="${room.my_application_status eq 'ACTIVE'}">
                                                    <span class="status-pill ACTIVE">참여중</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="status-pill ACTIVE">참여중</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <c:if test="${room.my_application_status eq 'ACTIVE' or empty room.my_application_status}">
                                            <a href="${contextPath}/spendolive/ott/chat/room.do?room_id=${room.room_id}" class="btn btn-primary">대화방</a>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>
        </article>

        <div id="withdraw-section" class="withdraw-row">
            <%-- [회원탈퇴 불가 팝업] 기존 화면 안 경고문은 제거하고 회원탈퇴 버튼만 유지한다. --%>
            <button type="button" class="btn btn-danger-outline" onclick="openWithdrawModal()">회원탈퇴</button>
        </div>

    </div>
</section>

<div class="withdraw-modal" id="withdrawModal" aria-hidden="true">
    <div class="withdraw-modal-box">
        <button type="button" class="withdraw-close" onclick="closeWithdrawModal()" aria-label="회원탈퇴 창 닫기">×</button>
        <p class="eyebrow">ACCOUNT DELETE</p>
        <h2>회원탈퇴</h2>
        <p class="mypage-muted">회원탈퇴를 하면 현재 계정으로 다시 로그인할 수 없습니다.</p>
        <%-- [회원탈퇴 정책 변경] 과거 OTT 참여 이력·문의는 삭제하고, 신고 등 필요한 처리 기록만 익명화 보존한다. --%>
        <ul class="withdraw-list">
            <li>개인 지출 내역과 월별 예산, 등록된 계좌·카드 정보는 모두 삭제됩니다.</li>
            <li>과거 OTT 참여 이력과 문의 내역·첨부파일은 모두 삭제됩니다.</li>
            <li>결제·정산·환불·채팅·신고 기록은 탈퇴한 회원 정보로 익명화하여 보존합니다.</li>
            <li>탈퇴한 아이디와 이메일은 익명값으로 변경되며, 같은 정보로 새로 가입할 수 있습니다.</li>
            <li>탈퇴가 완료되면 로그인 세션이 즉시 종료됩니다.</li>
        </ul>
        <form action="${contextPath}/spendolive/mypage/withdraw.do" method="post" id="withdrawForm">
            <label class="mypage-field">
                확인 문구 입력
                <input type="text" name="withdrawConfirm" id="withdrawConfirm" placeholder="탈퇴합니다">
            </label>
            <p class="mypage-help warn">위 입력칸에 <strong>탈퇴합니다</strong>를 정확히 입력해야 탈퇴할 수 있습니다.</p>
            <div class="withdraw-actions">
                <button type="button" class="btn btn-primary btn-mini" onclick="closeWithdrawModal()">취소</button>
                <button type="button" class="btn btn-danger-outline" onclick="submitWithdrawForm()">회원탈퇴 진행</button>
            </div>
        </form>
    </div>
</div>

<%-- [회원탈퇴 불가 팝업] 탈퇴 제한 조건이 있으면 마이페이지 재진입 시 자동으로 안내 팝업을 표시한다. --%>
<c:if test="${withdrawBlocked}">
    <div class="withdraw-modal show" id="withdrawBlockedModal" role="dialog" aria-modal="true" aria-hidden="false" aria-labelledby="withdrawBlockedTitle">
        <div class="withdraw-modal-box withdraw-blocked-modal-box">
            <button type="button" class="withdraw-close" onclick="closeWithdrawBlockedModal()" aria-label="회원탈퇴 불가 안내 창 닫기">×</button>
            <p class="eyebrow">ACCOUNT DELETE</p>
            <h2 id="withdrawBlockedTitle">회원탈퇴를 진행할 수 없습니다.</h2>
            <p class="mypage-muted">현재 계정에 먼저 처리해야 할 항목이 남아 있습니다.</p>

            <%-- [회원탈퇴 불가 팝업] 개수가 0보다 큰 항목만 팝업에 표시한다. --%>
            <ul class="withdraw-list withdraw-blocked-list">
                <c:if test="${ownedRoomCount > 0}"><li>운영 중인 방이 ${ownedRoomCount}개 있습니다.</li></c:if>
                <c:if test="${joinedRoomCount > 0}"><li>참여 중인 방이 ${joinedRoomCount}개 있습니다.</li></c:if>
                <c:if test="${pendingRefundCount > 0}"><li>처리 중인 환불 요청이 ${pendingRefundCount}건 있습니다.</li></c:if>
            </ul>

            <p class="withdraw-blocked-guide">각 항목을 모두 처리한 후 다시 시도해주세요.</p>
            <div class="withdraw-actions">
                <button type="button" class="btn btn-primary" onclick="closeWithdrawBlockedModal()">확인</button>
            </div>
        </div>
    </div>
</c:if>

<script src="${contextPath}/resources/js/mypage.js" data-ajax-reload></script>

