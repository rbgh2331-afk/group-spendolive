// AJAX 팝업 모달 모듈
function showMemberModal(prefix, type, titleText, messageText) {
    const overlay = document.getElementById(prefix + 'StatusOverlay');
    const title = document.getElementById(prefix + 'StatusTitle');
    const message = document.getElementById(prefix + 'StatusMessage');
    const spinner = document.getElementById(prefix + 'StatusSpinner');
    const icon = document.getElementById(prefix + 'StatusIcon');
    const closeBtn = document.getElementById(prefix + 'StatusCloseButton');
    const actions = document.getElementById(prefix + 'StatusActions');

    if (!overlay) return;

    overlay.hidden = false;
    if (title) title.textContent = titleText;
    if (message) message.textContent = messageText;

    // 아이콘 / 스피너 제어
    if (spinner) spinner.hidden = (type !== 'processing');
    if (icon) {
        icon.hidden = (type === 'processing');
        icon.textContent = (type === 'success') ? '✓' : '!';
    }

    // 버튼 영역 제어
    if (actions) actions.hidden = (type === 'processing');
    overlay.dataset.state = type;
    overlay.style.display = 'flex';

    if (closeBtn) {
        closeBtn.onclick = function () {
            overlay.style.display = 'none';
        };
    }
}
(function () {
    const userIdInput = document.getElementById('userId');
    const checkIdButton = document.getElementById('checkIdButton');

    if (!checkIdButton || !userIdInput) return;



    // 2. 아이디 중복확인 버튼 클릭 이벤트
    checkIdButton.addEventListener('click', async function () {
        const userId = userIdInput.value.trim();

        if (!userId) {
            showMemberModal('member','error', '입력 오류', '아이디를 입력해 주세요.');
            userIdInput.focus();
            return;
        }

        try {
            const response = await fetch('/member/checkId.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: new URLSearchParams({ id: userId })
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'CHECK_COMPLETED') {
                isIdVerified = true; 
                showMemberModal('member','success', '중복확인 성공', result.message || '사용 가능한 아이디입니다.');
            } else {
                isIdVerified = false;
                showMemberModal('member','error', '중복확인 실패', result.message || '이미 사용 중인 아이디입니다.');
            }

        } catch (error) {
            showMemberModal('member','error', '시스템 오류', '중복확인 중 오류가 발생했습니다.');
        }
    });
})();
// Email 인증번호 전송
(function () {
    const emailInput = document.getElementById('email');
    const emailButton = document.getElementById('emailButton');
    if (!emailButton || !emailInput) return;


    // 2. 아이디 중복확인 버튼 클릭 이벤트
    emailButton.addEventListener('click', async function (e) {
        e.preventDefault(); // 🛑 브라우저의 기본 동작(새로고침 등)을 막아줍니다!
        const email = emailInput.value.trim();
        if (!email) {
            showMemberModal('member','error', '입력 오류', 'email을 입력해 주세요.');
            emailInput.focus();
            return;
        }

        try {
            showMemberModal('member','processing', '인증번호 발송 중입니다.', '잠시만 기다려주세요.');
            const response = await fetch('/member/sendEmail.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: new URLSearchParams({ email: email })
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'SEND_COMPLETED') {
                showMemberModal('member','success', '인증번호 전송!', result.message || '인증번호 전송 완료되었습니다.');
                document.getElementById('emailAuthArea').classList.remove('is-hidden');
            } else {
                showMemberModal('member','error', '인증번호 전송 실패', result.message || '인증번호 발생 중 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.');
            }

        } catch (error) {

    showMemberModal('member','error', '시스템 오류', '중복확인 중 오류가 발생했습니다.');
        }
    });
})();

    // SMS 인증번호 전송
(function () {
    const phoneInput = document.getElementById('phone');
    const phoneButton = document.getElementById('phoneButton');
    if (!phoneButton || !phoneInput) return;


    phoneButton.addEventListener('click', async function (e) {

        const phone = phoneInput.value.trim();
        if (!phone) {
            showMemberModal('member','error', '입력 오류', 'phone을 입력해 주세요.');
            phoneInput.focus();
            return;
        }

        try {
            showMemberModal('member','processing', '인증번호 발송 중입니다.', '잠시만 기다려주세요.');
            const response = await fetch('/member/sendSms.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: new URLSearchParams({ phone: phone })
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'SEND_COMPLETED') {
                showMemberModal('member','success', '인증번호 전송!', result.message || '인증번호 전송 완료되었습니다.');
                document.getElementById('phoneAuthArea').classList.remove('is-hidden');
            } else {
                showMemberModal('member','error', '인증번호 전송 실패', result.message || '인증번호 발생 중 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.');
            }

        } catch (error) {
            console.error("🚨 에러 원인:", error);
    showMemberModal('member','error', '시스템 오류', '중복확인 중 오류가 발생했습니다.');
        }
    });
})();
    //  아이디 중복확인 버튼 클릭 이벤트
(function () {
    const signupForm = document.querySelector('form');
    const signupButton = document.getElementById('signupButton');
    if (!signupButton||!signupForm) return;


    signupButton.addEventListener('click', async function (e) {
        const formData = new FormData(signupForm);
        const payload = new URLSearchParams(formData);
        e.preventDefault();
        const loginType = formData.get('login_type');
        const isValid = loginType === 'KAKAO' ? joinCheckKakao() : joinCheck();
        if (!isValid) {
            return;
        }
        try {
            showMemberModal('signup','processing', '회원가입 중 입니다.', '잠시만 기다려주세요.');
            const response = await fetch('/member/addmember.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: payload
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'SIGNUP_COMPLETED') {
                showMemberModal('signup','success', '회원가입 완료 !', result.message || '회원가입이 완료되었습니다.로그인 페이지로 이동하겠습니다.');
                signupmoveAfterSuccess(result);
            } else {
                showMemberModal('signup','error', '회원가입 실패', result.message || '회원가입 중 오류가 발생하였습니다. 잠시 후 다시 시도해주세요.');
            }

        } catch (error) {
            console.error("회원가입 에러 상세 내용:", error);
        showMemberModal('signup','error', '시스템 오류', '회원가입 중 오류가 발생했습니다.');
        }
    });
    function signupmoveAfterSuccess(result) {

        window.setTimeout(function () {
            window.location.href = result.redirectUrl;
        }, 1200);
    }
})();

    // 2. 로그인
(function () {
    const loginForm = document.querySelector('form');
    const loginButton = document.getElementById('loginButton');
    if (!loginButton||!loginForm) return;

    loginButton.addEventListener('click', async function (e) {
        const formData = new FormData(loginForm);
        const payload = new URLSearchParams(formData);
        e.preventDefault();
        try {
            showMemberModal('login','processing', '로그인 중 입니다.', '잠시만 기다려주세요.');
            const response = await fetch('/member/login.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: payload
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'LOGIN_COMPLETED') {
                showMemberModal('login','success', '로그인 완료 !', result.message || '로그인이 완료되었습니다.메인 페이지로 이동하겠습니다.');
                loginmoveAfterSuccess(result);
            } else if(result.code === 'ADMINLOGIN_COMPLETED') {
                showMemberModal('login','success', '관리자 로그인 완료 !', result.message || '관리자 로그인이 완료되었습니다.관리자  페이지로 이동하겠습니다.');
                loginmoveAfterSuccess(result);
            }else if(result.code === 'LOGIN_FAILED'){
                showMemberModal('login','error', '로그인 실패',  result.message);
            }

        } catch (error) {
            console.error("로그인 에러 상세 내용:", error);
        showMemberModal('login','error', '시스템 오류', '로그인 중 오류가 발생했습니다.');
        }
    });
    function loginmoveAfterSuccess(result) {

        window.setTimeout(function () {
            window.location.href = result.redirectUrl;
        }, 500);
    }
})();

    // 주카드 변경
(function () {
    // class로 모든 카드 변경 버튼을 가져옵니다
    const changeCardButtons = document.querySelectorAll('.btn-change-card');
    if (changeCardButtons.length === 0) return;

    changeCardButtons.forEach(function (button) {
        button.addEventListener('click', async function (e) {
            e.preventDefault();

            // 클릭된 버튼의 data-card-idx 값 추출
            const card_idx = this.dataset.cardIdx;

            if (!card_idx) {
                alert("카드 정보를 찾을 수 없습니다.");
                return;
            }

            try {
                showMemberModal('card', 'processing', '변경 중입니다.', '잠시만 기다려주세요.');

                const response = await fetch('/payment/updatePrimaryCard.do', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                        'Accept': 'application/json'
                    },
                    body: new URLSearchParams({ card_idx: card_idx })
                });

                const result = await response.json();

                if (result.code === 'UPDATE_COMPLETED' || result.success) {
                    showMemberModal('card', 'success', '카드 변경 성공!', result.message || '카드 변경이 완료되었습니다.');

                    // 1초 후 페이지 새로고침하여 적용 상태 반영
                    setTimeout(function () {
                        location.reload();
                    }, 1000);
                } else {
                    showMemberModal('card', 'error', '카드 변경 실패', result.message || '카드 변경 중 오류가 발생하였습니다.');
                }

            } catch (error) {
                console.error("🚨 에러 발생:", error);
                showMemberModal('card', 'error', '시스템 오류', '카드 변경 처리 중 통신 오류가 발생했습니다.');
            }
        });
    });
})();

    // 관리자 회원 강제 탈퇴
(function () {

    document.addEventListener('click', async function (e) {

        const whitdrawButton = e.target.closest('.adminmemberSubmitButton');
        if (!whitdrawButton) return;
        const id = whitdrawButton.dataset.id;
        const body = new URLSearchParams({ id });
        if(!confirm("정말 탈퇴 시키겠습니까?")){
            return;
        }
        try {
            showMemberModal('adminmember','processing', '탈퇴 진행 중 입니다.', '잠시만 기다려주세요.');
            const response = await fetch((window.contextPath || '') + '/admin/member/withdraw.do', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                    'Accept': 'application/json'
                },
                body: body
            });

            const result = await response.json();

            // 백엔드가 보내주는 code 값으로 확인 (CHECK_COMPLETED = 사용 가능)
            if (result.code === 'WITHDRAW_COMPLETED') {
                showMemberModal('adminmember','success', '탈퇴 완료 !', result.message || '탈퇴 완료되었습니다.');
                signupmoveAfterSuccess(result);
            } else {
                showMemberModal('adminmember','error', '탈퇴 실패', result.message || '탈퇴 중 오류가 발생하였습니다.');
            }

        } catch (error) {
            console.error("탈퇴 에러 상세 내용:", error);
        showMemberModal('adminmember','error', '시스템 오류', '탈퇴 중 오류가 발생했습니다.');
        }
    });
    function signupmoveAfterSuccess(result) {

        window.setTimeout(function () {
            window.location.href = result.redirectUrl;
        }, 1200);
    }
})();
