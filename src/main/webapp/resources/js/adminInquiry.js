/* ============================================================
   adminInquiry.js  (관리자 문의 - 답변 AJAX)
   - 관리자 문의 목록의 필터/페이지/탭 전환과 상세 모달은 이미
     admin.js가 처리하고 있음. 이 파일은 "답변 등록/수정"만 AJAX로 바꾼다.
   - 답변 폼은 상세 모달 안에 있고(숨김 템플릿을 복사해 씀), 목록이 AJAX로
     교체될 때마다 새로 만들어지므로 document에 위임(delegation)해서 가로챈다.
   - 성공하면 모달을 닫고 목록 조각(#adminBoardArea)만 다시 불러와 배지를 갱신.
   - 공용 파일(admin.js 등)은 건드리지 않고, 알림 모달은 기존 공통 CSS 클래스 재사용.
   - soAlert/soConfirm은 이 파일이 전역으로 정의(IIFE 밖). adminFaq.js는 항상
     이 파일 다음에 로드되므로(adminInquiryList.jsp, adminFaqList.jsp) 거기서
     재정의하지 않고 이 전역 함수를 그대로 가져다 씀. adminNotice.js는 단독
     페이지에서만 로드되므로 그대로 자체 사본을 유지함.
   ============================================================ */

/* 공통 CSS(.modal/.modal-box/.panel-title/.toolbar/.btn)만 사용하는 알림·확인 모달 */
function soEnsureModal() {
    var el = document.getElementById("soLocalModalOverlay");
    if (el == null) {
        el = document.createElement("div");
        el.id = "soLocalModalOverlay";
        el.className = "modal";
        el.setAttribute("role", "dialog");
        el.setAttribute("aria-modal", "true");
        el.innerHTML =
              '<div class="modal-box">'
            +   '<div class="panel-title">'
            +     '<p class="section-kicker" id="soLocalModalKicker">NOTICE</p>'
            +     '<h3 id="soLocalModalTitle"></h3>'
            +     '<p id="soLocalModalMessage" hidden></p>'
            +   '</div>'
            +   '<div class="toolbar">'
            +     '<span></span>'
            +     '<div class="toolbar-left">'
            +       '<button type="button" class="btn ghost" id="soLocalModalCancel" hidden>취소</button>'
            +       '<button type="button" class="btn primary" id="soLocalModalOk">확인</button>'
            +     '</div>'
            +   '</div>'
            + '</div>';
        document.body.appendChild(el);
    }
    return el;
}

function soOpenModal(message, opts, isConfirm) {
    opts = opts || {};
    var el = soEnsureModal();
    var kickerEl = document.getElementById("soLocalModalKicker");
    var titleEl = document.getElementById("soLocalModalTitle");
    var msgEl = document.getElementById("soLocalModalMessage");
    var okBtn = document.getElementById("soLocalModalOk");
    var cancelBtn = document.getElementById("soLocalModalCancel");
    var type = opts.type || (isConfirm ? "info" : "success");

    kickerEl.textContent = type === "error" ? "ERROR" : (isConfirm ? "CONFIRM" : "NOTICE");

    if (opts.title) {
        titleEl.textContent = opts.title;
        msgEl.textContent = message || "";
        msgEl.hidden = !message;
    } else {
        titleEl.textContent = message || "";
        msgEl.textContent = "";
        msgEl.hidden = true;
    }

    okBtn.textContent = opts.confirmText || "확인";
    cancelBtn.textContent = opts.cancelText || "취소";
    cancelBtn.hidden = !isConfirm;

    return new Promise(function (resolve) {
        function done(result) {
            el.classList.remove("show");
            okBtn.onclick = null;
            cancelBtn.onclick = null;
            el.onclick = null;
            document.removeEventListener("keydown", onKey);
            resolve(result);
        }

        function onKey(e) {
            if (e.key === "Escape") done(false);
            else if (e.key === "Enter") done(true);
        }

        okBtn.onclick = function () { done(true); };
        cancelBtn.onclick = function () { done(false); };
        el.onclick = function (e) { if (e.target === el) done(false); };
        document.addEventListener("keydown", onKey);
        el.classList.add("show");
        okBtn.focus();
    });
}

function soAlert(message, opts) { return soOpenModal(message, opts, false); }
function soConfirm(message, opts) { return soOpenModal(message, opts, true); }

   (function () {
    "use strict";

    /* ── 답변 폼 제출 가로채기 (모달 안 폼이 동적 생성되므로 document 위임) ── */
    document.addEventListener("submit", function (e) {
        var form = e.target.closest("form.admin-reply-form");
        if (!form) return;
        e.preventDefault();
        submitReply(form);
    });

    function submitReply(form) {
        var contentEl = form.querySelector('[name="reply_content"]');
        var content = contentEl ? contentEl.value.trim() : "";
        if (!content) {
            soAlert("답변 내용을 입력해 주세요.", { type: "error" });
            return;
        }

        var submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) { submitBtn.disabled = true; }

        var params = new URLSearchParams(new FormData(form));

        fetch("/admin/inquiry/ajax/reply.do", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params
        })
            .then(function (res) {
                if (res.status === 401) {
                    soAlert("관리자만 접근할 수 있습니다.", { type: "error" });
                    return null;
                }
                return res.json();
            })
            .then(function (data) {
                if (!data) return;
                if (data.result !== "OK") {
                    soAlert(data.message || "답변 등록에 실패했습니다.", { type: "error" });
                    if (submitBtn) submitBtn.disabled = false;
                    return;
                }
                // 상세 모달 닫기 (admin.js의 전역 함수 재사용)
                if (typeof closeAdminInquiryModal === "function") closeAdminInquiryModal();
                // 확인을 누르면 목록 조각만 다시 불러와 상태 배지 갱신
                soAlert(data.message).then(function () { reloadBoard(); });
            })
            .catch(function () {
                soAlert("답변 등록 중 네트워크 오류가 발생했습니다.", { type: "error" });
                if (submitBtn) submitBtn.disabled = false;
            });
    }

    /* 현재 목록 URL 그대로 다시 fetch → #adminBoardArea 내부만 교체.
       (admin.js의 loadBoard는 IIFE 내부라 못 부르므로 여기서 동일 동작 수행) */
    function reloadBoard() {
        var url = location.pathname + location.search; // /admin/inquiry/list.do?status=..&page=..
        fetch(url, { credentials: "same-origin" })
            .then(function (res) { return res.text(); })
            .then(function (html) {
                var doc = new DOMParser().parseFromString(html, "text/html");
                var na = doc.getElementById("adminBoardArea");
                var ca = document.getElementById("adminBoardArea");
                if (na && ca) ca.innerHTML = na.innerHTML;
                else window.location.reload();
            })
            .catch(function () { window.location.reload(); });
    }
})();