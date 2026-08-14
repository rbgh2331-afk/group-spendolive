<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>
        SpendOlive | 로그인
    </title>
    <link rel="stylesheet" href="${contextPath}/resources/css/styles.css">

<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Jua&display=swap" rel="stylesheet">
</head>
<body class="auth-body">
    <div class="auth-wrap">
        <aside class="auth-brand-panel">
        <a href="${contextPath}/spendolive/main.do" class="auth-logo">
            <img src="${contextPath}/resources/images/logo.png" alt="SpendOlive" class="brand-logo-img">
            <span>
                SpendOlive
            </span>
        </a>
        <div class="auth-brand-copy">
            <p class="auth-label">
                LOGIN
            </p>
            <h1>
                오늘의 지출을
                <br>
                더 쉽게 관리하세요
            </h1>
            <p class="auth-desc">
                지출관리, 캘린더, OTT관리, 정산 상태를 로그인 후 한 번에 확인할 수 있습니다.
            </p>
        </div>
        <div class="auth-brand-stats">
            <div>
                <strong>
                3개
            </strong>
            <span>
                지출 구분
            </span>
        </div>
        <div>
            <strong>
            월별
        </strong>
        <span>
            캘린더 관리
        </span>
    </div>
    <div>
        <strong>
        OTT
    </strong>
    <span>
        공유 정산
    </span>
</div>
</div>
</aside>
<main class="auth-form-panel">
    <section class="auth-card">
        <div class="auth-card-header" id="authCardHeader">
            <p class="eyebrow" id="authEyebrow">WELCOME BACK</p>
            <h2 id="authTitle">로그인</h2>
            <p id="authDescription">SpendOlive 계정으로 로그인하고 나의 지출 현황을 확인하세요.</p>
        </div>

        <!-- 로그인 폼 -->
        <div id="loginPanel" class="auth-switch-panel active">
            <div align="center">
                <a href="${kakaoAuthUrl}">
                    <img src="${contextPath}/resources/images/kakao_login_medium_wide.png" alt="카카오 로그인 버튼" />
                </a>
            </div>
            <div class="auth-divider">또는 일반 로그인</div>

            <form action="${contextPath}/member/login.do" method="post"onsubmit="return checkKakaoPassword()">
                <div class="auth-form-group">
                    <label for="loginId">아이디 또는 이메일</label>
                    <input id="loginId" type="text" name="id" placeholder="아이디 또는 이메일을 입력하세요" required>
                </div>
                <div class="auth-form-group">
                    <label for="loginPw">비밀번호</label>
                    <input id="loginPw" type="password" name="password" placeholder="비밀번호를 입력하세요" required>
                </div>
                <div class="auth-options">
                    <label>
                        <input type="checkbox">
                        로그인 상태 유지
                    </label>
                    <div class="auth-find-links">
                        <%-- [추가 기능] 페이지 이동 없이 로그인 카드 내부를 아이디 찾기 폼으로 전환 --%>
                        <a href="#" onclick="showAuthPanel('findId'); return false;">아이디 찾기</a>
                        <span>|</span>
                        <%-- [추가 기능] 페이지 이동 없이 로그인 카드 내부를 비밀번호 찾기 폼으로 전환 --%>
                        <a href="#" onclick="showAuthPanel('findPw'); return false;">비밀번호 찾기</a>
                    </div>
                </div>
                <button id="loginButton" class="auth-btn auth-btn-primary" type="submit">로그인</button>
            </form>
            <div class="auth-link-row">
                <span>아직 회원이 아니신가요?</span>
                <a href="${contextPath}/member/signup.do">회원가입</a>
            </div>
        </div>

        <%-- =========================================================
             [추가 폼] 아이디 찾기 영역
             ---------------------------------------------------------
             흐름:
             1) 가입한 휴대폰 번호 입력
             2) /member/findId/sendSms.do 로 인증번호 발급 요청
             3) 인증번호 입력 후 /member/findId/verify.do 로 검증
             4) 성공 시 가입된 아이디를 findIdResult 영역에 표시
             ========================================================= --%>
        <!-- 아이디 찾기 폼 -->
        <div id="findIdPanel" class="auth-switch-panel">
            <div class="auth-form-group">
                <label for="findIdPhone">가입한 휴대폰 번호</label>
                <div class="auth-input-row">
                    <input id="findIdPhone" type="text" placeholder="010-1234-5678 형식">
                    <button type="button" class="auth-btn auth-btn-light" onclick="sendFindIdSms()">인증번호 받기</button>
                </div>
                <p class="auth-help-text">회원가입 때 입력한 휴대폰 번호를 입력하세요.</p>
            </div>
            <div class="auth-form-group">
                <label for="findIdCode">인증번호</label>
                <div class="auth-input-row">
                    <input id="findIdCode" type="text" placeholder="6자리 인증번호">
                    <button type="button" class="auth-btn auth-btn-outline" onclick="verifyFindIdSms()">아이디 찾기</button>
                </div>
            </div>
            <p id="findIdResult" class="auth-result-text"></p>
            <button type="button" class="auth-btn auth-btn-primary auth-back-login" onclick="showAuthPanel('login')">로그인으로 돌아가기</button>
        </div>

        <%-- =========================================================
             [추가 폼] 비밀번호 찾기 영역
             ---------------------------------------------------------
             흐름:
             1) 아이디 + 가입한 휴대폰 번호 입력
             2) /member/findPw/sendSms.do 에서 아이디 존재 여부와 휴대폰 일치 여부 확인
             3) 인증번호 확인 성공 시 새 비밀번호 입력 영역 표시
             4) /member/findPw/reset.do 로 비밀번호 변경
             ========================================================= --%>
        <!-- 비밀번호 찾기 폼 -->
        <div id="findPwPanel" class="auth-switch-panel">
            <div id="findPwStepVerify">
                <div class="auth-form-group">
                    <label for="findPwId">아이디</label>
                    <input id="findPwId" type="text" placeholder="아이디를 입력하세요">
                </div>
                <div class="auth-form-group">
                    <label for="findPwPhone">가입한 휴대폰 번호</label>
                    <div class="auth-input-row">
                        <input id="findPwPhone" type="text" placeholder="010-1234-5678 형식">
                        <button type="button" class="auth-btn auth-btn-light" onclick="sendFindPwSms()">인증번호 받기</button>
                    </div>
                    <p class="auth-help-text">아이디와 휴대폰 번호가 DB에 있는 회원 정보와 일치해야 인증번호가 발급됩니다.</p>
                </div>
                <div class="auth-form-group">
                    <label for="findPwCode">인증번호</label>
                    <div class="auth-input-row">
                        <input id="findPwCode" type="text" placeholder="6자리 인증번호">
                        <button type="button" class="auth-btn auth-btn-outline" onclick="verifyFindPwSms()">인증 확인</button>
                    </div>
                </div>
            </div>

            <div id="findPwStepReset" class="auth-reset-box is-hidden">
                <div class="auth-form-group">
                    <label for="newPassword">새 비밀번호</label>
                    <input id="newPassword" type="password" placeholder="새 비밀번호를 입력하세요">
                </div>
                <div class="auth-form-group">
                    <label for="newPasswordConfirm">새 비밀번호 확인</label>
                    <input id="newPasswordConfirm" type="password" placeholder="새 비밀번호를 한 번 더 입력하세요">
                </div>
                <button type="button" class="auth-btn auth-btn-primary" onclick="resetPassword()">비밀번호 변경</button>
            </div>

            <p id="findPwResult" class="auth-result-text"></p>
            <button type="button" class="auth-btn auth-btn-primary auth-back-login" onclick="showAuthPanel('login')">로그인으로 돌아가기</button>
        </div>
             <jsp:include page="/WEB-INF/views/common/font.jsp" />
    </section>
     <div id="loginStatusOverlay"
                    class="status-overlay"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="loginStatusTitle"
                    aria-describedby="loginStatusMessage"
                    hidden>
                    <div class="status-box">

                    <div id="loginStatusSpinner"
                            class="status-spinner"
                            aria-hidden="true"></div>

                        <div id="loginStatusIcon"
                            class="status-icon"
                            aria-hidden="true"
                            hidden></div>
                            <h3 id="loginStatusTitle">로그인중 입니다.</h3>
                        <p id="loginStatusMessage">
                            창을 닫거나 새로고침하지 말아주세요.
                        </p>
                    <div id="loginStatusActions"
                            class="status-actions"
                            hidden>
                            <button type="button"
                                    id="loginStatusCloseButton"
                                    class="btn btn-outline">
                                확인
                            </button>
                            <button type="button"
                                    id="loginStatusActionButton"
                                    class="btn btn-primary"
                                    hidden>
                                이동하기
                            </button>
                        </div>
                </div>
            </div>
</main>
</div>
<script src="${contextPath}/resources/js/app.js"></script>
<script src="${contextPath}/resources/js/signup.js"></script>

<script>
    var loginContextPath = "${contextPath}";

    var msg = "${msg}";
    var message = "${message}";
    if (msg && msg !== "") {
        alert(msg);
    }
    if (message && message !== "") {
        alert(message);
    }

    /* =========================================================
       [추가 JS] 로그인/아이디 찾기/비밀번호 찾기 패널 전환
       ---------------------------------------------------------
       기존 로그인 페이지 안에서 URL 이동 없이 카드 내용만 바꿔 보여준다.
       type = 'login'  -> 로그인 폼
       type = 'findId' -> 아이디 찾기 폼
       type = 'findPw' -> 비밀번호 찾기 폼
       ========================================================= */
    function showAuthPanel(type) {
        var panels = document.querySelectorAll('.auth-switch-panel');
        panels.forEach(function(panel) {
            panel.classList.remove('active');
        });

        if (type === 'findId') {
            document.getElementById('findIdPanel').classList.add('active');
            document.getElementById('authEyebrow').innerText = 'FIND ID';
            document.getElementById('authTitle').innerText = '아이디 찾기';
            document.getElementById('authDescription').innerText = '가입한 휴대폰 번호로 인증 후 아이디를 확인하세요.';
        } else if (type === 'findPw') {
            document.getElementById('findPwPanel').classList.add('active');
            document.getElementById('authEyebrow').innerText = 'RESET PASSWORD';
            document.getElementById('authTitle').innerText = '비밀번호 찾기';
            document.getElementById('authDescription').innerText = '아이디 확인과 휴대폰 인증 후 새 비밀번호로 변경하세요.';
        } else {
            document.getElementById('loginPanel').classList.add('active');
            document.getElementById('authEyebrow').innerText = 'WELCOME BACK';
            document.getElementById('authTitle').innerText = '로그인';
            document.getElementById('authDescription').innerText = 'SpendOlive 계정으로 로그인하고 나의 지출 현황을 확인하세요.';
            resetFindMessages();
        }
    }

    function resetFindMessages() {
        setResult('findIdResult', '', '');
        setResult('findPwResult', '', '');
    }

    function setResult(id, text, type) {
        var el = document.getElementById(id);
        el.innerText = text || '';
        el.classList.remove('ok', 'warn');
        if (type) {
            el.classList.add(type);
        }
    }

    /* =========================================================
       [추가 JS] 공통 AJAX POST 함수
       ---------------------------------------------------------
       아이디/비밀번호 찾기 버튼들은 form submit이 아니라 fetch를 사용한다.
       서버에서는 Map<String,Object>를 JSON으로 반환하고,
       화면에서는 res.success / res.message를 이용해 결과 문구를 출력한다.
       ========================================================= */
    function postForm(url, data) {
        var params = new URLSearchParams();
        Object.keys(data).forEach(function(key) {
            params.append(key, data[key]);
        });

            return fetch(loginContextPath + url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: params.toString()
        }).then(function(response) {
            return response.json();
        });
    }

    // [추가 JS] 아이디 찾기 1단계: 휴대폰 번호로 인증번호 발급 요청
    function sendFindIdSms() {
        var phone = document.getElementById('findIdPhone').value.trim();
        if (!phone) {
            setResult('findIdResult', '휴대폰 번호를 입력해주세요.', 'warn');
            return;
        }

        postForm('/member/findId/sendSms.do', { phone: phone })
            .then(function(res) {
                setResult('findIdResult', res.message, res.success ? 'ok' : 'warn');
            })
            .catch(function() {
                setResult('findIdResult', '인증번호 발송 중 오류가 발생했습니다.', 'warn');
            });
    }

    // [추가 JS] 아이디 찾기 2단계: 인증번호 확인 후 아이디 출력
    function verifyFindIdSms() {
        var code = document.getElementById('findIdCode').value.trim();
        if (!code) {
            setResult('findIdResult', '인증번호를 입력해주세요.', 'warn');
            return;
        }

        postForm('/member/findId/verify.do', { inputCode: code })
            .then(function(res) {
                setResult('findIdResult', res.message, res.success ? 'ok' : 'warn');
            })
            .catch(function() {
                setResult('findIdResult', '아이디 찾기 중 오류가 발생했습니다.', 'warn');
            });
    }

    // [추가 JS] 비밀번호 찾기 1단계: 아이디/휴대폰 확인 후 인증번호 발급 요청
    function sendFindPwSms() {
        var id = document.getElementById('findPwId').value.trim();
        var phone = document.getElementById('findPwPhone').value.trim();
        if (!id || !phone) {
            setResult('findPwResult', '아이디와 휴대폰 번호를 모두 입력해주세요.', 'warn');
            return;
        }

        postForm('/member/findPw/sendSms.do', { id: id, phone: phone })
            .then(function(res) {
                setResult('findPwResult', res.message, res.success ? 'ok' : 'warn');
            })
            .catch(function() {
                setResult('findPwResult', '인증번호 발송 중 오류가 발생했습니다.', 'warn');
            });
    }

    // [추가 JS] 비밀번호 찾기 2단계: 인증번호 확인 후 새 비밀번호 입력 영역 표시
    function verifyFindPwSms() {
        var code = document.getElementById('findPwCode').value.trim();
        if (!code) {
            setResult('findPwResult', '인증번호를 입력해주세요.', 'warn');
            return;
        }

        postForm('/member/findPw/verify.do', { inputCode: code })
            .then(function(res) {
                setResult('findPwResult', res.message, res.success ? 'ok' : 'warn');
                if (res.success) {
                    document.getElementById('findPwStepReset').style.display = 'block';
                    document.getElementById('newPassword').focus();
                }
            })
            .catch(function() {
                setResult('findPwResult', '인증 확인 중 오류가 발생했습니다.', 'warn');
            });
    }

    // [추가 JS] 비밀번호 찾기 3단계: 새 비밀번호와 확인값을 서버로 보내 비밀번호 변경
    function resetPassword() {
        var newPassword = document.getElementById('newPassword').value.trim();
        var newPasswordConfirm = document.getElementById('newPasswordConfirm').value.trim();

        if (!newPassword || !newPasswordConfirm) {
            setResult('findPwResult', '새 비밀번호와 비밀번호 확인을 모두 입력해주세요.', 'warn');
            return;
        }
        if (newPassword !== newPasswordConfirm) {
            setResult('findPwResult', '새 비밀번호와 비밀번호 확인이 일치하지 않습니다.', 'warn');
            return;
        }

        postForm('/member/findPw/reset.do', {
            newPassword: newPassword,
            newPasswordConfirm: newPasswordConfirm
        }).then(function(res) {
            setResult('findPwResult', res.message, res.success ? 'ok' : 'warn');
            if (res.success) {
                /*
                 * [비밀번호 찾기 - 변경 완료 안내]
                 * 기존에는 비밀번호 변경 성공 후 약 0.9초 뒤에 조용히 로그인 폼으로 돌아가서
                 * 사용자가 "정상 처리된 건가?" 하고 어색하게 느낄 수 있었다.
                 *
                 * 그래서 변경 성공 시 먼저 안내 팝업을 보여주고,
                 * 사용자가 확인을 누르면 로그인 폼으로 전환되도록 수정했다.
                 */
                alert('비밀번호가 변경되었습니다. 로그인 화면으로 이동합니다!');

                showAuthPanel('login');
                document.getElementById('loginId').value = document.getElementById('findPwId').value.trim();
                document.getElementById('loginPw').focus();
                document.getElementById('findPwStepReset').style.display = 'none';
                document.getElementById('findPwCode').value = '';
                document.getElementById('newPassword').value = '';
                document.getElementById('newPasswordConfirm').value = '';
            }
        }).catch(function() {
            setResult('findPwResult', '비밀번호 변경 중 오류가 발생했습니다.', 'warn');
        });
    }
</script>
</body>
</html>
