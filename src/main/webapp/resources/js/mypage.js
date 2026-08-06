/* [AJAX 변경 주석]
 * 회원정보·계좌·카드 AJAX 폼과 기존 거래내역 요청을 공통 로딩 시스템에 연결한다.
 * 기존 Controller/Service URL과 파라미터는 특별한 문제가 없는 한 그대로 유지한다.
 */
/* JSP에서 전달한 컨텍스트 경로를 페이지 data 속성에서 읽는다. */
function getMyPageContextPath() {
    const page = document.querySelector('.mypage-page');
    return page ? (page.dataset.contextPath || '') : '';
}

// 거래내역은 전체 데이터를 유지한 채 화면 출력만 10건씩 나눈다.
var ACCOUNT_TRANSACTION_PAGE_SIZE = 10;
var accountTransactionList = [];
var accountTransactionCurrentPage = 1;

/* =========================================================
   [마이페이지 계좌·카드 연결 JavaScript 추가 시작]
   화면 전환, 계좌 제목 수정, 계좌·카드 4개 단위 페이지 처리를 담당한다.
   ========================================================= */
// 상단 버튼을 누른 경우에만 회원정보·신고내역·계좌카드 영역을 표시한다.
function showMyPagePanel(panelId) {
    document.querySelectorAll('.mypage-toggle-panel').forEach(function (panel) {
        panel.classList.add('is-hidden');
    });

    const target = document.getElementById(panelId);
    if (!target) {
        return;
    }

    target.classList.remove('is-hidden');
    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    window.history.replaceState(null, '', '#' + panelId);
}

// 계좌 제목은 처음에는 읽기 전용으로 표시하고, 수정 버튼을 누르면 입력과 저장이 가능해진다.
function toggleAccountNameEdit(button) {
    const form = button.closest('.mypage-asset-title-form');
    const input = form ? form.querySelector('input[name="accountName"]') : null;
    if (!form || !input) {
        return;
    }

    if (input.readOnly) {
        input.readOnly = false;
        input.focus();
        input.select();
        button.textContent = '저장';
        return;
    }

    if (!input.value.trim()) {
        alert('계좌 제목을 입력해주세요.');
        input.focus();
        return;
    }

    form.requestSubmit();
}

// 카드 이름도 계좌 제목과 같은 방식으로 읽기 전용 상태에서 수정 모드로 전환한다.
function toggleCardNameEdit(button) {
    const form = button.closest('.mypage-asset-title-form');
    const input = form ? form.querySelector('input[name="cardName"]') : null;
    if (!form || !input) {
        return;
    }

    if (input.readOnly) {
        input.readOnly = false;
        input.focus();
        input.select();
        button.textContent = '저장';
        return;
    }

    const cardName = input.value.trim();
    if (!cardName) {
        alert('카드 이름을 입력해주세요.');
        input.focus();
        return;
    }
    if (cardName.length > 30) {
        alert('카드 이름은 30자 이하로 입력해주세요.');
        input.focus();
        return;
    }

    // submit 이벤트를 발생시켜 pageAjax.js의 공통 AJAX 처리와 버튼 잠금을 그대로 사용한다.
    form.requestSubmit();
}

// 계좌와 카드 목록은 4개 단위로 보이며, 부족한 칸은 빈 정보 카드로 채운다.
function initializeAssetPager(listId) {
    const list = document.getElementById(listId);
    const pager = document.querySelector('[data-pager-for="' + listId + '"]');
    if (!list || !pager) {
        return;
    }

    let items = Array.from(list.querySelectorAll('[data-asset-item]'));
    const emptyText = list.dataset.emptyText || '정보가 없습니다.';
    const requiredSlots = Math.max(4, Math.ceil(items.length / 4) * 4);

    while (items.length < requiredSlots) {
        const emptyItem = document.createElement('div');
        emptyItem.className = 'mypage-asset-item mypage-asset-empty';
        emptyItem.setAttribute('data-asset-item', '');
        emptyItem.textContent = emptyText;
        list.appendChild(emptyItem);
        items.push(emptyItem);
    }

    const totalPages = Math.max(1, Math.ceil(items.length / 4));
    let currentPage = 1;
    const currentPageText = pager.querySelector('[data-current-page]');
    const totalPageText = pager.querySelector('[data-total-page]');
    const prevButton = pager.querySelector('[data-page-direction="prev"]');
    const nextButton = pager.querySelector('[data-page-direction="next"]');

    function renderPage() {
        items.forEach(function (item, index) {
            const itemPage = Math.floor(index / 4) + 1;
            item.classList.toggle('is-hidden', itemPage !== currentPage);
        });

        currentPageText.textContent = currentPage;
        totalPageText.textContent = totalPages;
        prevButton.disabled = currentPage === 1;
        nextButton.disabled = currentPage === totalPages;
    }

    prevButton.addEventListener('click', function () {
        if (currentPage > 1) {
            currentPage -= 1;
            renderPage();
        }
    });

    nextButton.addEventListener('click', function () {
        if (currentPage < totalPages) {
            currentPage += 1;
            renderPage();
        }
    });

    renderPage();
}

function initializeMyPageAssets() {
    initializeAssetPager('accountAssetList');
    initializeAssetPager('cardAssetList');

    // 거래내역 버튼마다 선택한 계좌 번호를 Ajax 조회 함수로 전달한다.
    document.querySelectorAll('.transaction-history-btn').forEach(function (button) {
        button.addEventListener('click', function () {
            loadAccountTransactions(button);
        });
    });

    initializeAccountTransactionPager();

    const targetId = window.location.hash.replace('#', '');
    if (['profile-edit', 'report-manage', 'asset-manage'].includes(targetId)) {
        showMyPagePanel(targetId);
    }
}

initializeMyPageAssets();

/* 선택한 계좌의 거래내역을 Ajax로 조회한다. */
// [공통 AJAX 로딩 적용] 거래내역 조회 버튼을 전달해 요청 중 중복 클릭을 막고 완료 후 복구한다.
function loadAccountTransactions(button) {
    const accountIdx = button.dataset.accountIdx;
    const panel = document.getElementById('accountTransactionPanel');
    const tbody = document.getElementById('accountTransactionBody');
    const currentBalance = Number(button.dataset.currentBalance || 0);

    document.getElementById('accountTransactionTitle').textContent =
        (button.dataset.accountName || '계좌') + ' 거래내역';
    document.getElementById('accountTransactionAccountInfo').textContent =
        (button.dataset.bankName || '') + ' 계좌번호 - ' + (button.dataset.accountNumber || '-');
    document.getElementById('accountTransactionCurrentBalance').textContent =
        formatWon(currentBalance);
    document.getElementById('accountTransactionInitialBalance').textContent = '-';

    tbody.innerHTML = '<tr><td colspan="4">거래내역을 불러오는 중입니다.</td></tr>';
    accountTransactionList = [];
    accountTransactionCurrentPage = 1;
    setAccountTransactionPagerVisible(false);
    // mypage-form-section의 display:grid가 hidden 표시를 덮어쓰지 않도록 숨김 클래스도 함께 제거한다.
    panel.classList.remove('is-hidden');
    panel.hidden = false;

    window.fetchWithLoading(
        getMyPageContextPath() + '/spendolive/mypage/account/transactions.do?accountIdx='
        + encodeURIComponent(accountIdx),
        {
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' },
            button: button,
            loadingMessage: '거래내역을 불러오고 있습니다.'
        }
    )
        .then(function (response) {
            if (response.status === 401) {
                throw new Error('로그인이 필요합니다.');
            }
            if (!response.ok) {
                throw new Error('거래내역 조회에 실패했습니다.');
            }
            return response.json();
        })
        .then(function (transactionList) {
            renderAccountTransactions(transactionList, currentBalance);
            panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        })
        .catch(function (error) {
            accountTransactionList = [];
            accountTransactionCurrentPage = 1;
            setAccountTransactionPagerVisible(false);
            tbody.innerHTML = '';
            const row = document.createElement('tr');
            const cell = document.createElement('td');
            cell.colSpan = 4;
            cell.textContent = error.message;
            row.appendChild(cell);
            tbody.appendChild(row);
        });
}

/* 최신 거래가 위로 오도록 전달된 거래내역과 거래 직후 잔액을 표에 출력한다. */
function renderAccountTransactions(transactionList, currentBalance) {
    const initialBalanceTarget = document.getElementById('accountTransactionInitialBalance');
    accountTransactionList = Array.isArray(transactionList) ? transactionList : [];
    accountTransactionCurrentPage = 1;

    if (accountTransactionList.length === 0) {
        renderAccountTransactionPage();
        initialBalanceTarget.textContent = formatWon(currentBalance);
        return;
    }

    // 최초 잔액은 현재 페이지 10건이 아니라 전체 거래내역의 가장 오래된 거래를 기준으로 계산한다.
    const oldestTransaction = accountTransactionList[accountTransactionList.length - 1];
    if (oldestTransaction.balance_after === null || oldestTransaction.balance_after === undefined) {
        initialBalanceTarget.textContent = '기록 없음';
    } else {
        const initialBalance =
            Number(oldestTransaction.balance_after) - getSignedTransactionAmount(oldestTransaction);
        initialBalanceTarget.textContent = formatWon(initialBalance);
    }

    renderAccountTransactionPage();
}

/* 현재 거래내역 페이지의 최대 10건만 표에 출력한다. */
function renderAccountTransactionPage() {
    const tbody = document.getElementById('accountTransactionBody');
    const pager = document.getElementById('accountTransactionPager');
    const currentPageText = document.getElementById('accountTransactionCurrentPage');
    const totalPageText = document.getElementById('accountTransactionTotalPage');

    if (!tbody || !pager || !currentPageText || !totalPageText) {
        return;
    }

    tbody.innerHTML = '';

    if (accountTransactionList.length === 0) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.colSpan = 4;
        cell.textContent = '등록된 거래내역이 없습니다.';
        row.appendChild(cell);
        tbody.appendChild(row);
        setAccountTransactionPagerVisible(false);
        return;
    }

    const totalPages = Math.ceil(accountTransactionList.length / ACCOUNT_TRANSACTION_PAGE_SIZE);
    accountTransactionCurrentPage = Math.min(Math.max(accountTransactionCurrentPage, 1), totalPages);

    const startIndex = (accountTransactionCurrentPage - 1) * ACCOUNT_TRANSACTION_PAGE_SIZE;
    const pageTransactions = accountTransactionList.slice(
        startIndex,
        startIndex + ACCOUNT_TRANSACTION_PAGE_SIZE
    );

    pageTransactions.forEach(function (transaction) {
        const row = document.createElement('tr');
        const signedAmount = getSignedTransactionAmount(transaction);

        appendTransactionCell(row, formatTransactionDate(transaction.tran_date));
        appendTransactionCell(row, transaction.inout_type || '-');
        appendTransactionCell(row, formatSignedWon(signedAmount));

        if (transaction.balance_after === null || transaction.balance_after === undefined) {
            appendTransactionCell(row, '기록 없음');
        } else {
            appendTransactionCell(row, formatWon(Number(transaction.balance_after)));
        }

        tbody.appendChild(row);
    });

    currentPageText.textContent = accountTransactionCurrentPage;
    totalPageText.textContent = totalPages;
    setAccountTransactionPagerVisible(totalPages > 1);

    const prevButton = pager.querySelector('[data-transaction-page="prev"]');
    const nextButton = pager.querySelector('[data-transaction-page="next"]');
    if (prevButton) {
        prevButton.disabled = accountTransactionCurrentPage === 1;
    }
    if (nextButton) {
        nextButton.disabled = accountTransactionCurrentPage === totalPages;
    }
}

/* 기존 마이페이지 페이지 버튼을 거래내역에도 그대로 연결한다. */
function initializeAccountTransactionPager() {
    const pager = document.getElementById('accountTransactionPager');
    if (!pager) {
        return;
    }

    pager.querySelectorAll('[data-transaction-page]').forEach(function (button) {
        button.addEventListener('click', function () {
            const direction = button.dataset.transactionPage;
            const totalPages = Math.max(1, Math.ceil(accountTransactionList.length / ACCOUNT_TRANSACTION_PAGE_SIZE));

            if (direction === 'prev' && accountTransactionCurrentPage > 1) {
                accountTransactionCurrentPage -= 1;
            } else if (direction === 'next' && accountTransactionCurrentPage < totalPages) {
                accountTransactionCurrentPage += 1;
            } else {
                return;
            }

            renderAccountTransactionPage();
        });
    });
}

function setAccountTransactionPagerVisible(visible) {
    const pager = document.getElementById('accountTransactionPager');
    if (!pager) {
        return;
    }

    pager.hidden = !visible;
    pager.classList.toggle('is-hidden', !visible);
}

/* 사용자 입력값을 HTML 문자열로 합치지 않고 textContent로 안전하게 출력한다. */
function appendTransactionCell(row, value) {
    const cell = document.createElement('td');
    cell.textContent = value;
    row.appendChild(cell);
}

/* 출금은 음수, 입금은 양수로 맞춰 화면 표시와 최초 잔액 계산에 사용한다. */
function getSignedTransactionAmount(transaction) {
    const amount = Math.abs(Number(transaction.tran_amt || 0));
    return transaction.inout_type === '출금' ? amount * -1 : amount;
}

function formatWon(value) {
    return Number(value || 0).toLocaleString('ko-KR') + '원';
}

function formatSignedWon(value) {
    const amount = Number(value || 0);
    const sign = amount > 0 ? '+' : amount < 0 ? '-' : '';
    return sign + Math.abs(amount).toLocaleString('ko-KR') + '원';
}

/* DB의 yyyyMMddHHmmss 거래일시를 화면용 형식으로 변환한다. */
function formatTransactionDate(value) {
    const date = String(value || '');
    if (date.length !== 14) {
        return date || '-';
    }

    return date.substring(0, 4) + '-'
        + date.substring(4, 6) + '-'
        + date.substring(6, 8) + ' '
        + date.substring(8, 10) + ':'
        + date.substring(10, 12) + ':'
        + date.substring(12, 14);
}

function closeAccountTransactions() {
    const panel = document.getElementById('accountTransactionPanel');
    if (!panel) {
        return;
    }

    // mypage-form-section의 display:grid보다 우선하는 공통 숨김 클래스로 거래내역 영역을 닫는다.
    panel.classList.add('is-hidden');
    panel.hidden = true;
    accountTransactionList = [];
    accountTransactionCurrentPage = 1;
    setAccountTransactionPagerVisible(false);
}
/* [마이페이지 계좌·카드 연결 JavaScript 추가 끝] */

(function () {
    const emailInput = document.getElementById('mypageEmail');
    const phoneInput = document.getElementById('mypagePhone');
    const passwordFields = ['currentPassword', 'newPassword', 'passwordConfirm'];

    if (emailInput) {
        emailInput.addEventListener('input', function () {
            document.getElementById('emailVerified').value = 'N';
            setMessage('emailVerifyMessage', '이메일을 변경했다면 인증을 다시 진행해주세요.', 'warn');
        });
    }

    if (phoneInput) {
        phoneInput.addEventListener('input', function () {
            document.getElementById('phoneVerified').value = 'N';
            setMessage('phoneVerifyMessage', '전화번호를 변경했다면 인증을 다시 진행해주세요.', 'warn');
        });
    }

    passwordFields.forEach(function (fieldId) {
        const field = document.getElementById(fieldId);
        if (field) {
            field.addEventListener('input', function () {
                document.getElementById('passwordChecked').value = 'N';
                setMessage('passwordCheckMessage', '비밀번호 변경 전 확인 버튼을 눌러주세요.', 'warn');
            });
        }
    });

    const form = document.getElementById('mypageProfileForm');
    if (form) {
        form.addEventListener('submit', function (event) {
            const emailChanged = document.getElementById('mypageEmail').value.trim() !== document.getElementById('originalEmail').value.trim();
            const phoneChanged = document.getElementById('mypagePhone').value.trim() !== document.getElementById('originalPhone').value.trim();
            const newPassword = document.getElementById('newPassword').value.trim();

            if (emailChanged && document.getElementById('emailVerified').value !== 'Y') {
                alert('이메일을 변경하려면 이메일 인증을 완료해주세요.');
                event.preventDefault();
                return;
            }

            if (phoneChanged && document.getElementById('phoneVerified').value !== 'Y') {
                alert('전화번호를 변경하려면 전화번호 인증을 완료해주세요.');
                event.preventDefault();
                return;
            }

            if (newPassword && document.getElementById('passwordChecked').value !== 'Y') {
                alert('비밀번호 변경 전 비밀번호 확인 버튼을 눌러주세요.');
                event.preventDefault();
            }
        });
    }
})();

// [공통 AJAX 로딩 적용] 마이페이지 인증 POST의 버튼·문구 전달 방식을 한 곳으로 규격화한다.
function postForm(url, data, options) {
    const settings = options || {};
    return window.fetchWithLoading(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
        },
        body: new URLSearchParams(data),
        button: settings.button || null,
        loadingMessage: settings.loadingMessage
    });
}

function setMessage(id, message, type) {
    const target = document.getElementById(id);
    if (!target) {
        return;
    }
    target.textContent = message;
    target.classList.remove('success', 'warn');
    if (type) {
        target.classList.add(type);
    }
}

// [이메일 인증 AJAX] JSP에서 전달한 버튼을 요청 중에만 잠그고 공통 팝업 문구를 표시한다.
function sendMyPageEmailCode(button) {
    const email = document.getElementById('mypageEmail').value.trim();
    if (!email) {
        alert('이메일을 입력해주세요.');
        return;
    }

    postForm(getMyPageContextPath() + '/spendolive/mypage/email/send.do', { email: email }, { button: button, loadingMessage: '이메일 인증번호를 발송하고 있습니다.' })
        .then(function (res) { return res.text(); })
        .then(function (result) {
            if (result === 'SUCCESS') {
                document.getElementById('emailVerified').value = 'N';
                setMessage('emailVerifyMessage', '인증번호를 발송했습니다. 이메일을 확인해주세요.', 'success');
            } else {
                setMessage('emailVerifyMessage', '인증번호 발송에 실패했습니다.', 'warn');
            }
        })
        .catch(function () {
            setMessage('emailVerifyMessage', '인증번호 발송 중 오류가 발생했습니다.', 'warn');
        });
}

function verifyMyPageEmailCode(button) {
    const email = document.getElementById('mypageEmail').value.trim();
    const inputCode = document.getElementById('mypageEmailCode').value.trim();
    if (!inputCode) {
        alert('이메일 인증번호를 입력해주세요.');
        return;
    }

    postForm(getMyPageContextPath() + '/spendolive/mypage/email/verify.do', { email: email, inputCode: inputCode }, { button: button, loadingMessage: '이메일 인증번호를 확인하고 있습니다.' })
        .then(function (res) { return res.text(); })
        .then(function (result) {
            if (result === 'true') {
                document.getElementById('emailVerified').value = 'Y';
                setMessage('emailVerifyMessage', '이메일 인증이 완료되었습니다.', 'success');
            } else {
                document.getElementById('emailVerified').value = 'N';
                setMessage('emailVerifyMessage', '인증번호가 일치하지 않습니다.', 'warn');
            }
        })
        .catch(function () {
            setMessage('emailVerifyMessage', '이메일 인증 확인 중 오류가 발생했습니다.', 'warn');
        });
}

// [휴대전화 인증 AJAX] 이메일 인증과 같은 공통 팝업·버튼 잠금 규격을 사용한다.
function sendMyPagePhoneCode(button) {
    const phone = document.getElementById('mypagePhone').value.trim();
    if (!phone) {
        alert('전화번호를 입력해주세요.');
        return;
    }

    postForm(getMyPageContextPath() + '/spendolive/mypage/phone/send.do', { phone: phone }, { button: button, loadingMessage: '휴대전화 인증번호를 발송하고 있습니다.' })
        .then(function (res) { return res.text(); })
        .then(function (result) {
            if (result === 'SUCCESS') {
                document.getElementById('phoneVerified').value = 'N';
                setMessage('phoneVerifyMessage', '인증번호를 발송했습니다. 문자를 확인해주세요.', 'success');
            } else {
                setMessage('phoneVerifyMessage', '인증번호 발송에 실패했습니다.', 'warn');
            }
        })
        .catch(function () {
            setMessage('phoneVerifyMessage', '인증번호 발송 중 오류가 발생했습니다.', 'warn');
        });
}

function verifyMyPagePhoneCode(button) {
    const phone = document.getElementById('mypagePhone').value.trim();
    const inputCode = document.getElementById('mypagePhoneCode').value.trim();
    if (!inputCode) {
        alert('문자 인증번호를 입력해주세요.');
        return;
    }

    postForm(getMyPageContextPath() + '/spendolive/mypage/phone/verify.do', { phone: phone, inputCode: inputCode }, { button: button, loadingMessage: '휴대전화 인증번호를 확인하고 있습니다.' })
        .then(function (res) { return res.text(); })
        .then(function (result) {
            if (result === 'true') {
                document.getElementById('phoneVerified').value = 'Y';
                setMessage('phoneVerifyMessage', '전화번호 인증이 완료되었습니다.', 'success');
            } else {
                document.getElementById('phoneVerified').value = 'N';
                setMessage('phoneVerifyMessage', '인증번호가 일치하지 않습니다.', 'warn');
            }
        })
        .catch(function () {
            setMessage('phoneVerifyMessage', '전화번호 인증 확인 중 오류가 발생했습니다.', 'warn');
        });
}






// [회원탈퇴 불가 팝업] 탈퇴 제한 안내 팝업을 닫는다.
function closeWithdrawBlockedModal() {
    const modal = document.getElementById('withdrawBlockedModal');
    if (!modal) return;

    modal.classList.remove('show');
    modal.setAttribute('aria-hidden', 'true');
}

function submitWithdrawForm() {
    const confirmInput = document.getElementById('withdrawConfirm');
    const form = document.getElementById('withdrawForm');

    if (!confirmInput || !form) {
        return;
    }

    if (confirmInput.value.trim() !== '탈퇴합니다') {
        alert('확인 문구를 정확히 입력해주세요.');
        confirmInput.focus();
        return;
    }

    if (confirm('정말 회원탈퇴를 진행할까요? 탈퇴 후에는 현재 계정으로 다시 로그인할 수 없습니다.')) {
        form.submit();
    }
}

if (!window.__mypageWithdrawClickBound) {
    // [회원탈퇴 불가 팝업] 기존 탈퇴 팝업과 탈퇴 불가 팝업 모두 배경 클릭으로 닫는다.
    document.addEventListener('click', function (event) {
        const withdrawModal = document.getElementById('withdrawModal');
        const blockedModal = document.getElementById('withdrawBlockedModal');

        if (withdrawModal && event.target === withdrawModal) closeWithdrawModal();
        if (blockedModal && event.target === blockedModal) closeWithdrawBlockedModal();
    });

    // [회원탈퇴 불가 팝업] ESC 키로 열려 있는 회원탈퇴 관련 팝업을 닫는다.
    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') return;

        const withdrawModal = document.getElementById('withdrawModal');
        const blockedModal = document.getElementById('withdrawBlockedModal');

        if (withdrawModal && withdrawModal.classList.contains('show')) closeWithdrawModal();
        if (blockedModal && blockedModal.classList.contains('show')) closeWithdrawBlockedModal();
    });

    window.__mypageWithdrawClickBound = true;
}

function checkMyPagePassword() {
    const currentPassword = document.getElementById('currentPassword').value.trim();
    const newPassword = document.getElementById('newPassword').value.trim();
    const passwordConfirm = document.getElementById('passwordConfirm').value.trim();

    if (!currentPassword || !newPassword || !passwordConfirm) {
        document.getElementById('passwordChecked').value = 'N';
        setMessage('passwordCheckMessage', '현재 비밀번호, 새 비밀번호, 새 비밀번호 확인을 모두 입력해주세요.', 'warn');
        return;
    }

    if (newPassword !== passwordConfirm) {
        document.getElementById('passwordChecked').value = 'N';
        setMessage('passwordCheckMessage', '새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.', 'warn');
        return;
    }

    document.getElementById('passwordChecked').value = 'Y';
    setMessage('passwordCheckMessage', '새 비밀번호 확인이 완료되었습니다. 저장 시 현재 비밀번호도 다시 확인됩니다.', 'success');
}
