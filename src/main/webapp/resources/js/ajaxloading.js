'use strict';

/* [공통 AJAX 로딩]
 * 사용자 클릭으로 실행되는 AJAX 요청의 로딩 팝업, 버튼 잠금, 응답 형식 검사,
 * 세션 만료 및 공통 오류 처리를 한 곳에서 담당한다.
 * 자동 알림 조회·채팅 폴링·챗봇·결제 전용 상태창처럼 별도 표시가 필요한 요청은
 * options.loading = false 또는 기존 순정 fetch를 사용해 전역 팝업에서 제외
 */

(function (window, document) {
    const DEFAULT_MESSAGE = '처리 중입니다.';
    const DEFAULT_DESCRIPTION = '잠시만 기다려주세요.';
    const SHOW_DELAY_MS = 180;
    const MIN_VISIBLE_MS = 280;

    let requestSequence = 0;
    let showTimer = null;
    let hideTimer = null;
    let visibleSince = 0;
    const activeLoadings = new Map();
    const legacyTokenStack = [];

    function contextPath() {
        return window.spendoliveContextPath || window.contextPath || '';
    }

    function elements() {
        return {
            overlay: document.getElementById('globalLoadingOverlay'),
            message: document.getElementById('globalLoadingMessage'),
            description: document.getElementById('globalLoadingDescription')
        };
    }

    function latestLoading() {
        const values = Array.from(activeLoadings.values());
        return values.length ? values[values.length - 1] : null;
    }

    function updateText() {
        const refs = elements();
        const latest = latestLoading();
        if (refs.message) refs.message.textContent = latest ? latest.message : DEFAULT_MESSAGE;
        if (refs.description) refs.description.textContent = latest ? latest.description : DEFAULT_DESCRIPTION;
    }

    function reveal() {
        const refs = elements();
        if (!refs.overlay || activeLoadings.size === 0) return;
        refs.overlay.hidden = false;
        refs.overlay.setAttribute('aria-busy', 'true');
        refs.overlay.classList.add('show');
        visibleSince = Date.now();
    }

    function conceal(force) {
        const refs = elements();
        if (!refs.overlay || (!force && activeLoadings.size > 0)) return;
        refs.overlay.classList.remove('show');
        refs.overlay.setAttribute('aria-busy', 'false');
        window.setTimeout(function () {
            if (activeLoadings.size === 0) refs.overlay.hidden = true;
        }, 170);
    }

    /* 요청별 토큰을 반환해, 겹친 요청이 완료 순서와 다르게 끝나도
       다른 요청의 로딩 메시지나 표시 상태를 잘못 제거하지 않게 한다. */
    function beginLoading(message, description, legacy) {
        const token = 'ajax-loading-' + (++requestSequence);
        activeLoadings.set(token, {
            message: message || DEFAULT_MESSAGE,
            description: description || DEFAULT_DESCRIPTION
        });
        if (legacy) legacyTokenStack.push(token);

        updateText();
        window.clearTimeout(hideTimer);
        if (activeLoadings.size === 1) {
            window.clearTimeout(showTimer);
            showTimer = window.setTimeout(reveal, SHOW_DELAY_MS);
        }
        return token;
    }

    function finishLoading(token) {
        let targetToken = token;
        if (!targetToken) targetToken = legacyTokenStack.pop();
        if (!targetToken || !activeLoadings.has(targetToken)) return;

        activeLoadings.delete(targetToken);
        const legacyIndex = legacyTokenStack.lastIndexOf(targetToken);
        if (legacyIndex >= 0) legacyTokenStack.splice(legacyIndex, 1);

        if (activeLoadings.size > 0) {
            updateText();
            return;
        }

        window.clearTimeout(showTimer);
        const elapsed = visibleSince ? Date.now() - visibleSince : MIN_VISIBLE_MS;
        window.clearTimeout(hideTimer);
        hideTimer = window.setTimeout(function () {
            conceal(false);
            visibleSince = 0;
            updateText();
        }, Math.max(0, MIN_VISIBLE_MS - elapsed));
    }

    /* 직접 호출 호환용 API. 신규 요청 코드는 SpendOliveAjax.request 또는 rawRequest 사용을 권장한다. */
    function showLoading(message, description) {
        return beginLoading(message, description, true);
    }

    function hideLoading(token) {
        finishLoading(token);
    }

    function resetLoading() {
        activeLoadings.clear();
        legacyTokenStack.length = 0;
        visibleSince = 0;
        window.clearTimeout(showTimer);
        window.clearTimeout(hideTimer);
        conceal(true);
        updateText();
        document.querySelectorAll('[data-ajax-disabled="true"]').forEach(function (button) {
            restoreButton(button);
        });
    }

    function disableButton(button) {
        if (!button || button.dataset.ajaxDisabled === 'true') return;
        button.dataset.ajaxDisabled = 'true';
        button.dataset.ajaxWasDisabled = String(Boolean(button.disabled));
        button.disabled = true;
        button.classList.add('is-ajax-pending');
    }

    function restoreButton(button) {
        if (!button || button.dataset.ajaxDisabled !== 'true') return;
        button.disabled = button.dataset.ajaxWasDisabled === 'true';
        button.classList.remove('is-ajax-pending');
        delete button.dataset.ajaxDisabled;
        delete button.dataset.ajaxWasDisabled;
    }

    function resolveButton(config) {
        if (config.button) return config.button;
        if (config.trigger) return config.trigger;
        const active = document.activeElement;
        return active && active.matches('button,input[type="submit"],input[type="button"]') ? active : null;
    }

    function normalizeUrl(url) {
        const base = contextPath();
        if (!url) return base;
        if (/^(https?:)?\/\//i.test(url)) return url;
        if (base && (url === base || url.startsWith(base + '/'))) return url;
        return base + (url.startsWith('/') ? url : '/' + url);
    }

    /* 명시적인 loadingMessage가 없는 기존 AJAX도 같은 규격의 안내 문구를 사용하도록
       URL과 HTTP 방식으로 안전한 기본 문구를 정한다. */
    function inferLoadingMessage(input, method) {
        const url = String(input || '');
        const rules = [
            [/\/member\/login\.do/, '로그인 중입니다.'],
            [/\/member\/addmember\.do/, '회원가입을 처리하고 있습니다.'],
            [/\/member\/checkId\.do/, '아이디 중복 여부를 확인하고 있습니다.'],
            [/\/member\/(sendEmail|sendSms)\.do/, '인증번호를 발송하고 있습니다.'],
            [/\/member\/(verifyEmail|verifySms)/, '인증번호를 확인하고 있습니다.'],
            [/\/expense\/ajax\/budget\/save\.do/, '예산을 저장하고 있습니다.'],
            [/\/expense\/ajax\/add\.do/, '지출을 등록하고 있습니다.'],
            [/\/expense\/ajax\/modify\.do/, '지출을 수정하고 있습니다.'],
            [/\/expense\/ajax\/delete\.do/, '지출을 삭제하고 있습니다.'],
            [/\/calendar\/expenses\.do/, '지출 내역을 불러오고 있습니다.'],
            [/\/mypage\/account\/transactions\.do/, '거래내역을 불러오고 있습니다.'],
            [/\/mypage\/email\/send\.do/, '이메일 인증번호를 발송하고 있습니다.'],
            [/\/mypage\/email\/verify\.do/, '이메일 인증번호를 확인하고 있습니다.'],
            [/\/mypage\/phone\/send\.do/, '휴대전화 인증번호를 발송하고 있습니다.'],
            [/\/mypage\/phone\/verify\.do/, '휴대전화 인증번호를 확인하고 있습니다.'],
            [/\/ott\/ajax\/friends\/create\.do/, '가족·지인 공유방을 개설하고 있습니다.'],
            [/\/ott\/ajax\/recruit\/create\.do/, '모집글을 등록하고 있습니다.'],
            [/\/ott\/ajax\/recruit\/quick-join\.do/, '참가 가능한 방을 확인하고 있습니다.'],
            [/\/ott\/ajax\/settlement\/pay\.do/, '정산을 처리하고 있습니다.'],
            [/\/ott\/ajax\/room\/leave-cancel\.do/, '나가기 예약을 취소하고 있습니다.'],
            [/\/ott\/ajax\/room\/leave-reserve\.do/, '나가기 예약을 처리하고 있습니다.'],
            [/\/ott\/ajax\/room\/close-request\.do/, '방 종료 예약을 처리하고 있습니다.'],
            [/\/inquiry\/ajax\/write\.do/, '문의를 등록하고 있습니다.'],
            [/\/inquiry\/ajax\/edit\.do/, '문의를 수정하고 있습니다.'],
            [/\/inquiry\/ajax\/delete\.do/, '문의를 삭제하고 있습니다.'],
            [/\/inquiry\/list\.do/, '문의 목록을 불러오고 있습니다.'],
            [/\/admin\/inquiry\/ajax\/reply\.do/, '문의 답변을 등록하고 있습니다.'],
            [/\/admin\/faq\/ajax\/(insert|update)\.do/, 'FAQ를 저장하고 있습니다.'],
            [/\/admin\/faq\/ajax\/(moveUp|moveDown)\.do/, 'FAQ 순서를 변경하고 있습니다.'],
            [/\/admin\/faq\/ajax\/delete\.do/, 'FAQ를 삭제하고 있습니다.'],
            [/\/admin\/notice\/ajax\/list\.do/, '공지사항 목록을 불러오고 있습니다.'],
            [/\/admin\/notice\/ajax\/detail\.do/, '공지사항 정보를 불러오고 있습니다.'],
            [/\/admin\/notice\/ajax\/(insert|update)\.do/, '공지사항을 저장하고 있습니다.'],
            [/\/admin\/notice\/ajax\/delete\.do/, '공지사항을 삭제하고 있습니다.'],
            [/\/notification\/ajax\/read\.do/, '알림을 확인하고 있습니다.'],
            [/\/notification\/ajax\/list\.do/, '알림을 불러오고 있습니다.'],
            [/\/notice\/ajax\//, '공지사항을 불러오고 있습니다.']
        ];
        const matched = rules.find(function (rule) { return rule[0].test(url); });
        if (matched) return matched[1];
        return String(method || 'GET').toUpperCase() === 'GET'
            ? '정보를 불러오고 있습니다.'
            : '요청을 처리하고 있습니다.';
    }

    async function parseResponse(response) {
        const contentType = response.headers.get('content-type') || '';
        if (!contentType.toLowerCase().includes('json')) {
            const text = await response.text();
            const redirectedToLogin = response.redirected && /\/member\/loginForm\.do(?:[?#]|$)/.test(response.url || '');
            const error = new Error(redirectedToLogin
                ? '로그인 시간이 만료되었습니다. 다시 로그인해주세요.'
                : /<html[\s>]/i.test(text)
                    ? '서버에서 화면 응답이 반환되었습니다. 잠시 후 다시 시도해주세요.'
                    : '서버 응답 형식을 확인할 수 없습니다.');
            error.status = response.status;
            error.responseText = text;
            if (redirectedToLogin) {
                error.sessionExpired = true;
                error.redirectUrl = normalizeUrl('/member/loginForm.do');
            }
            throw error;
        }

        let result;
        try {
            result = await response.json();
        } catch (cause) {
            const error = new Error('서버의 JSON 응답을 해석할 수 없습니다.');
            error.status = response.status;
            error.cause = cause;
            throw error;
        }

        if (response.status === 401 || (result && result.code === 'SESSION_EXPIRED')) {
            const error = new Error(result.message || '로그인 시간이 만료되었습니다.');
            error.sessionExpired = true;
            error.redirectUrl = normalizeUrl(result.redirectUrl || '/member/loginForm.do');
            throw error;
        }
        if (response.status === 403 || (result && result.code === 'FORBIDDEN')) {
            const error = new Error(result.message || '권한이 없습니다.');
            error.forbidden = true;
            throw error;
        }
        if (!response.ok) {
            const error = new Error((result && result.message) || '처리 중 오류가 발생했습니다.');
            error.status = response.status;
            error.result = result;
            throw error;
        }
        return result;
    }

    function handleCommonError(error) {
        if (error && error.commonHandled) return;
        console.error(error);
        if (error && error.sessionExpired) {
            alert(error.message);
            window.location.href = error.redirectUrl || normalizeUrl('/member/loginForm.do');
            return;
        }
        alert(error && error.message ? error.message : '네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }

    function prepareRequest(input, options) {
        const config = Object.assign({ credentials: 'same-origin' }, options || {});
        const button = resolveButton(config);
        const loadingEnabled = config.loading !== false && config.silent !== true;
        const method = String(config.method || 'GET').toUpperCase();
        const loadingMessage = config.loadingMessage || inferLoadingMessage(input, method);
        const loadingDescription = config.loadingDescription || DEFAULT_DESCRIPTION;

        delete config.button;
        delete config.trigger;
        delete config.loading;
        delete config.silent;
        delete config.loadingMessage;
        delete config.loadingDescription;
        config.headers = Object.assign({ 'X-Requested-With': 'XMLHttpRequest' }, config.headers || {});

        return {
            config: config,
            button: button,
            loadingEnabled: loadingEnabled,
            loadingMessage: loadingMessage,
            loadingDescription: loadingDescription
        };
    }

    /* HTML·텍스트 응답을 그대로 사용하는 기존 AJAX용 함수. */
    async function rawRequest(input, options) {
        const prepared = prepareRequest(input, options);
        const loadingToken = prepared.loadingEnabled
            ? beginLoading(prepared.loadingMessage, prepared.loadingDescription, false)
            : null;
        disableButton(prepared.button);

        try {
            const response = await fetch(normalizeUrl(input), prepared.config);
            const redirectedToLogin = response.redirected && /\/member\/loginForm\.do(?:[?#]|$)/.test(response.url || '');
            if (response.status === 401 || redirectedToLogin) {
                alert('로그인 시간이 만료되었습니다. 다시 로그인해주세요.');
                window.location.href = normalizeUrl('/member/loginForm.do');
                return new Promise(function () {});
            }
            return response;
        } finally {
            if (loadingToken) finishLoading(loadingToken);
            restoreButton(prepared.button);
        }
    }

    /* 공통 JSON 응답을 사용하는 신규 AJAX용 함수. */
    async function request(input, options) {
        const prepared = prepareRequest(input, Object.assign({
            method: 'GET',
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        }, options || {}));
        prepared.config.headers = Object.assign({
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }, prepared.config.headers || {});

        const loadingToken = prepared.loadingEnabled
            ? beginLoading(prepared.loadingMessage, prepared.loadingDescription, false)
            : null;
        disableButton(prepared.button);

        try {
            const response = await fetch(normalizeUrl(input), prepared.config);
            return await parseResponse(response);
        } finally {
            if (loadingToken) finishLoading(loadingToken);
            restoreButton(prepared.button);
        }
    }

    function submitForm(form, options) {
        const settings = options || {};
        const submitter = settings.submitter
            ? settings.submitter
            : form.querySelector('button[type="submit"],input[type="submit"]');
        const method = (form.method || 'POST').toUpperCase();
        let body;
        try {
            body = submitter ? new FormData(form, submitter) : new FormData(form);
        } catch (ignore) {
            body = new FormData(form);
            if (submitter && submitter.name) body.append(submitter.name, submitter.value || '');
        }
        return request(form.action, {
            method: method,
            body: body,
            button: submitter,
            loading: settings.loading,
            loadingMessage: settings.loadingMessage,
            loadingDescription: settings.loadingDescription
        });
    }

    /* 서버 렌더링 화면의 일부만 교체하는 공통 함수. 요청 자체의 로딩도 여기서 관리한다. */
    async function replaceFromUrl(url, selector, options) {
        const prepared = prepareRequest(url, Object.assign({ method: 'GET' }, options || {}));
        const loadingToken = prepared.loadingEnabled
            ? beginLoading(prepared.loadingMessage, prepared.loadingDescription, false)
            : null;
        disableButton(prepared.button);

        try {
            const response = await fetch(normalizeUrl(url), prepared.config);
            const redirectedToLogin = response.redirected && /\/member\/loginForm\.do(?:[?#]|$)/.test(response.url || '');
            if (response.status === 401 || redirectedToLogin) {
                const error = new Error('로그인 시간이 만료되었습니다. 다시 로그인해주세요.');
                error.sessionExpired = true;
                error.redirectUrl = normalizeUrl('/member/loginForm.do');
                throw error;
            }
            if (response.status === 403) {
                const error = new Error('관리자 권한이 없습니다.');
                error.forbidden = true;
                throw error;
            }
            if (!response.ok) throw new Error('화면 갱신에 실패했습니다.');

            const html = await response.text();
            const nextDocument = new DOMParser().parseFromString(html, 'text/html');
            const next = nextDocument.querySelector(selector);
            const current = document.querySelector(selector);
            if (!next || !current) throw new Error('갱신할 화면 영역을 찾지 못했습니다.');
            current.replaceWith(next);
            document.dispatchEvent(new CustomEvent('spendolive:content-updated', { detail: { selector: selector } }));
            return next;
        } finally {
            if (loadingToken) finishLoading(loadingToken);
            restoreButton(prepared.button);
        }
    }

    window.showLoading = showLoading;
    window.hideLoading = hideLoading;
    window.fetchWithLoading = rawRequest;
    window.fetchJsonWithLoading = request;
    window.SpendOliveAjax = {
        request: request,
        rawRequest: rawRequest,
        submitForm: submitForm,
        replaceFromUrl: replaceFromUrl,
        normalizeUrl: normalizeUrl,
        handleError: handleCommonError,
        reset: resetLoading,
        disableButton: disableButton,
        restoreButton: restoreButton,
        inferLoadingMessage: inferLoadingMessage
    };

    window.addEventListener('pageshow', resetLoading);
    window.addEventListener('pagehide', resetLoading);
    window.addEventListener('unhandledrejection', function () {
        if (activeLoadings.size > 0) resetLoading();
    });
    window.addEventListener('error', function () {
        if (activeLoadings.size > 0) resetLoading();
    });
})(window, document);
