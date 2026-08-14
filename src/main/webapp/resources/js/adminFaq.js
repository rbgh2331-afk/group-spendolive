/* ============================================================
   adminFaq.js  (관리자 FAQ - 전부 AJAX + 모달)
   - 작성/수정: 페이지 이동 없이 목록 위 모달에서 처리 (문의 상세 모달과 같은 방식)
     · 숨김 템플릿(#adminFaqCreateTpl / #adminFaqEditTpl{id})을 모달에 복사해서 띄움
     · 제출 시 /spendolive/admin/faq/ajax/insert|update.do 로 전송
   - 순서변경(▲▼)/삭제: /spendolive/admin/faq/ajax/moveUp|moveDown|delete.do
   - 성공 시 목록 조각(#adminBoardArea)만 다시 불러와 갱신
   - 공용 파일(admin.js)은 건드리지 않고, 알림 모달은 기존 공통 CSS 클래스 재사용
   - soAlert/soConfirm은 adminInquiry.js가 전역으로 정의(IIFE 밖)한 걸 그대로 사용.
     이 파일은 항상 adminInquiry.js 다음에 로드되므로(adminInquiryList.jsp,
     adminFaqList.jsp) 여기선 재정의하지 않음. 이 파일엔 더 이상 로컬 모달 없음.
   ============================================================ */
   (function () {
    "use strict";

    function handleAuth(res) {
        if (res.status === 401) { soAlert("관리자만 접근할 수 있습니다.", { type: "error" }); return true; }
        return false;
    }

    var FAQ_LIST_URL = "/spendolive/admin/faq/list.do";

    /* ── 목록 화면에서만 동작 (#adminBoardArea 존재) ── */
    var boardArea = document.getElementById("adminBoardArea");
    if (!boardArea) return;

    /* 목록 안 클릭을 위임 처리: 편집/작성/삭제/순서/모달닫기 */
    boardArea.addEventListener("click", function (e) {
        var btn = e.target.closest("[data-action]");
        if (btn && !btn.disabled) {
            var action = btn.dataset.action;
            var faqId = btn.dataset.faqId;
            if (action === "edit")      { openFaqModal("adminFaqEditTpl" + faqId); return; }
            if (action === "create")    { openFaqModal("adminFaqCreateTpl"); return; }
            if (action === "closeModal"){ closeFaqModal(); return; }
            if (action === "moveUp")    { sendMove("moveUp", faqId); return; }
            if (action === "moveDown")  { sendMove("moveDown", faqId); return; }
            if (action === "delete")    { confirmDelete(faqId); return; }
        }
        // 모달 바깥(어두운 배경) 클릭 시 닫기
        var modal = document.getElementById("adminFaqModal");
        if (modal && e.target === modal) closeFaqModal();
    });

    /* 작성/수정 폼 제출 위임 (모달 안 폼은 동적으로 복사됨) */
    boardArea.addEventListener("submit", function (e) {
        var form = e.target.closest("form.faq-modal-form");
        if (!form) return;
        e.preventDefault();
        submitFaqForm(form);
    });

    /* ── 모달 열기/닫기 ── */
    function openFaqModal(tplId) {
        var tpl = document.getElementById(tplId);
        var body = document.getElementById("adminFaqModalBody");
        var modal = document.getElementById("adminFaqModal");
        if (!tpl || !body || !modal) return;
        body.innerHTML = tpl.innerHTML;      // 템플릿(폼)을 모달 안으로 복사
        modal.classList.add("show");
        var first = body.querySelector("select,input,textarea");
        if (first) first.focus();
    }
    function closeFaqModal() {
        var modal = document.getElementById("adminFaqModal");
        if (modal) modal.classList.remove("show");
    }

    /* ── 작성/수정 제출 ── */
    function submitFaqForm(form) {
        var faqId = Number(form.dataset.faqId || 0);
        var isEdit = faqId > 0;

        var category = form.querySelector('[name="category"]').value;
        var question = form.querySelector('[name="question"]').value.trim();
        var answer   = form.querySelector('[name="answer"]').value.trim();
        // 체크박스에 name="useYn"이 있으므로 checked 여부로 Y/N 결정
        var useEl = form.querySelector('[name="useYn"]');
        var useYn = (useEl && useEl.checked) ? "Y" : "N";

        if (!category) { soAlert("카테고리를 선택해 주세요.", { type: "error" }); return; }
        if (!question || !answer) { soAlert("질문과 답변을 모두 입력해 주세요.", { type: "error" }); return; }

        var submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) submitBtn.disabled = true;

        var payload = { category: category, question: question, answer: answer, useYn: useYn };
        if (isEdit) payload.faq_id = faqId;
        var url = isEdit ? "/spendolive/admin/faq/ajax/update.do" : "/spendolive/admin/faq/ajax/insert.do";

        postForm(url, payload)
            .then(function (data) {
                if (!data) { if (submitBtn) submitBtn.disabled = false; return; }
                if (data.result !== "OK") {
                    soAlert(data.message || (isEdit ? "수정에 실패했습니다." : "등록에 실패했습니다."), { type: "error" });
                    if (submitBtn) submitBtn.disabled = false;
                    return;
                }
                closeFaqModal();
                soAlert(data.message).then(reloadBoard);
            })
            .catch(function () {
                soAlert("저장 중 네트워크 오류가 발생했습니다.", { type: "error" });
                if (submitBtn) submitBtn.disabled = false;
            });
    }

    /* ── 순서변경 / 삭제 ── */
    function sendMove(dir, faqId) {
        postForm("/spendolive/admin/faq/ajax/" + dir + ".do", { faq_id: faqId })
            .then(function (data) {
                if (!data) return;
                if (data.result !== "OK") { soAlert(data.message || "순서 변경에 실패했습니다.", { type: "error" }); return; }
                reloadBoard();  // 순서는 조용히 반영
            })
            .catch(function () { soAlert("순서 변경 중 네트워크 오류가 발생했습니다.", { type: "error" }); });
    }
    function confirmDelete(faqId) {
        soConfirm("정말 삭제하시겠습니까?").then(function (ok) {
            if (!ok) return;
            postForm("/spendolive/admin/faq/ajax/delete.do", { faq_id: faqId })
                .then(function (data) {
                    if (!data) return;
                    if (data.result !== "OK") { soAlert(data.message || "삭제에 실패했습니다.", { type: "error" }); return; }
                    soAlert(data.message).then(reloadBoard);
                })
                .catch(function () { soAlert("삭제 중 네트워크 오류가 발생했습니다.", { type: "error" }); });
        });
    }

    /* 현재 FAQ 목록을 다시 fetch해서 #adminBoardArea 내부만 교체 */
    function reloadBoard() {
        fetch(FAQ_LIST_URL, { credentials: "same-origin" })
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

    /* 공통: x-www-form-urlencoded POST → JSON. 401이면 null */
    function postForm(url, obj) {
        return fetch(url, {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams(obj)
        }).then(function (res) {
            if (handleAuth(res)) return null;
            return res.json();
        });
    }
})();
