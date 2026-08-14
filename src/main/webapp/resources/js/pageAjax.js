'use strict';

/* [AJAX 변경 주석]
 * 사용자 화면의 data-ajax-form 및 data-ajax-navigation을 이벤트 위임으로 처리하고 본문을 부분 갱신
 * 기존 Controller/Service URL과 파라미터는 특별한 문제가 없는 한 그대로 유지
 */

(function (window, document) {
    if (!window.SpendOliveAjax) return;

    // 부분 교체된 HTML이 요구하는 스크립트만 다시 불러와 이벤트 초기화가 누락되지 않게 한다
    function reloadPageScripts(container) {
        const scripts = Array.from(container.querySelectorAll('script[data-ajax-reload][src]'));
        return scripts.reduce(function (promise, oldScript) {
            return promise.then(function () {
                return new Promise(function (resolve, reject) {
                    const script = document.createElement('script');
                    script.src = oldScript.src.split('?')[0] + '?ajax=' + Date.now();
                    script.onload = resolve;
                    script.onerror = reject;
                    document.body.appendChild(script);
                    script.addEventListener('load', function () { script.remove(); }, { once: true });
                });
            });
        }, Promise.resolve());
    }

    // 서버 HTML에서 #modern-content만 추출해 현재 화면에 교체한다
    // 기능별 문구와 클릭 버튼을 전달해 팝업·버튼 상태를 공통 함수 한 곳에서 관리한다
    async function refreshContent(url, loadingMessage, button) {
        const next = await window.SpendOliveAjax.replaceFromUrl(url, '#modern-content', {
            loadingMessage: loadingMessage || '화면을 갱신하고 있습니다.',
            button: button || null
        });
        await reloadPageScripts(next);
    }

    async function handleAjaxForm(form, submitter) {
        if (form.dataset.ajaxPending === 'true') return;
        const confirmMessage = form.dataset.ajaxConfirm;
        if (confirmMessage && !window.confirm(confirmMessage)) return;

        const originalAction = form.action;
        form.dataset.ajaxPending = 'true';
        if (form.dataset.ajaxAction) form.action = window.SpendOliveAjax.normalizeUrl(form.dataset.ajaxAction);
        try {
            const result = await window.SpendOliveAjax.submitForm(form, {
                submitter: submitter,
                loadingMessage: form.dataset.loadingMessage || '처리 중입니다.'
            });
            if (!result.success) throw new Error(result.message || '처리에 실패했습니다.');

            const data = result.data || {};
            if (data.redirectUrl) {
                window.location.href = window.SpendOliveAjax.normalizeUrl(data.redirectUrl);
                return;
            }
            // 등록·수정·삭제 결과는 redirectUrl 또는 refreshUrl로 즉시 화면에 반영되므로
            // 브라우저 기본 alert를 띄우지 않는다. 실패 안내는 catch의 공통 오류 처리로 유지
            if (data.refreshUrl) {
                await refreshContent(
                    data.refreshUrl,
                    form.dataset.refreshLoadingMessage || '변경된 내용을 불러오고 있습니다.'
                );
                history.replaceState(null, '', window.SpendOliveAjax.normalizeUrl(data.refreshUrl));
            } else {
                document.dispatchEvent(new CustomEvent('spendolive:ajax-success', {
                    detail: { form: form, result: result }
                }));
            }
        } catch (error) {
            window.SpendOliveAjax.handleError(error);
        } finally {
            form.action = originalAction;
            delete form.dataset.ajaxPending;
        }
    }

    // 사용자가 직접 월 선택·검색·필터를 실행한 경우에만 로딩 팝업을 표시
    async function handleAjaxNavigation(form, submitter) {
        if (form.dataset.ajaxPending === 'true') return;
        form.dataset.ajaxPending = 'true';
        window.SpendOliveAjax.disableButton(submitter);

        const formData = new FormData(form);
        if (submitter && submitter.name) {
            formData.set(submitter.name, submitter.value);
        }

        const params = new URLSearchParams(formData);
        const url = form.action + (form.action.includes('?') ? '&' : '?') + params.toString();
        try {
            await refreshContent(
                url,
                form.dataset.loadingMessage || '목록을 불러오고 있습니다.',
                submitter
            );
            history.replaceState(null, '', url);
        } catch (error) {
            window.SpendOliveAjax.handleError(error);
        } finally {
            window.SpendOliveAjax.restoreButton(submitter);
            delete form.dataset.ajaxPending;
        }
    }

    // 이벤트 위임으로 AJAX 교체 후 새로 생긴 폼에도 별도 재등록 없이 동작한다
    document.addEventListener('submit', function (event) {
        if (event.defaultPrevented) return;
        const navigationForm = event.target.closest('form[data-ajax-navigation]');
        if (navigationForm) {
            event.preventDefault();
            handleAjaxNavigation(navigationForm, event.submitter || navigationForm.querySelector('button[type="submit"],input[type="submit"]'));
            return;
        }
        const form = event.target.closest('form[data-ajax-form]');
        if (!form) return;
        event.preventDefault();
        handleAjaxForm(form, event.submitter || null);
    });

    document.addEventListener('change', function (event) {
        const form = event.target.closest('form[data-ajax-navigation]');
        if (!form) return;
        event.preventDefault();
        handleAjaxNavigation(form, null);
    });
})(window, document);
