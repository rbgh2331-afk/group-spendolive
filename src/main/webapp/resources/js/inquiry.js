/* ============================================================
   inquiry.js  (사용자 문의사항 AJAX)
   - 목록 필터/페이지네이션: list.do를 fetch해서 #inqBoardArea 조각만 교체
     (전체 새로고침 없음. 관리자 admin.js의 swapBoardArea와 같은 방식)
   - 삭제/작성/수정: /spendolive/inquiry/ajax/* 로 JSON 주고받음
   - 상세 보기는 기존 faq.js의 openInqDetailModal(숨김 템플릿 복사)을 그대로 사용
   - 경로는 프로젝트 컨벤션대로 컨텍스트 없이 루트 기준(/spendolive/inquiry/...) 사용
   - soAlert/soConfirm은 notice.js(전역, 모든 페이지 공통 로드)에 정의된 걸 그대로 사용.
     이 파일엔 더 이상 로컬 모달/CSS 없음.
   ============================================================ */

   (function () {
    "use strict";

    /* ── 공통: 401(미로그인) 응답이면 로그인 페이지로 ── */
    function handleAuth(res) {
        if (res.status === 401) {
            soAlert("로그인이 필요합니다. 로그인 후 다시 시도해 주세요.", { type: "error" });
            window.location.href = "/member/loginForm.do";
            return true;
        }
        return false;
    }

    /* ════════════════════════════════════════════════════════
       [목록 화면]  #inqBoardArea 가 있을 때만 동작
       ════════════════════════════════════════════════════════ */

    // 현재 보고 있는 필터/페이지 (삭제 후 같은 화면 다시 그릴 때 사용)
    let curStatus = "all";
    let curPage = 1;


    const boardArea = document.getElementById("inqBoardArea");
    if (boardArea) {
        initInquiryListPage();
    }



    function initInquiryListPage() {
        syncStateFromDom();

        // 이벤트 위임: #inqBoardArea 안의 필터·페이지 버튼 클릭을 한 곳에서 처리
        // (조각이 교체돼도 #inqBoardArea 자체는 유지되므로 리스너 재등록 불필요)
        boardArea.addEventListener("click", function (e) {
            const filterBtn = e.target.closest(".filter-btn[data-status]");
            if (filterBtn) {
                loadBoard(filterBtn.dataset.status, 1, true);
                return;
            }
            const pgBtn = e.target.closest(".pg-btn[data-page]");
            if (pgBtn) {
                loadBoard(pgBtn.dataset.status || curStatus, Number(pgBtn.dataset.page), true);
                return;
            }
        });

        // 브라우저 뒤로/앞으로 가기 시에도 전체 새로고침 없이 조각만 다시 로드
        window.addEventListener("popstate", function (e) {
            const st = (e.state && e.state.inqStatus) || readParam("status", "all");
            const pg = (e.state && e.state.inqPage) || Number(readParam("page", "1"));
            loadBoard(st, pg, false);
        });
    }

    // 현재 화면(서버가 그려준 조각)에서 활성 필터/페이지를 읽어 상태 변수 초기화
    function syncStateFromDom() {
        const activeFilter = boardArea.querySelector(".filter-btn.active[data-status]");
        if (activeFilter) curStatus = activeFilter.dataset.status;
        const activePg = boardArea.querySelector(".pg-btn.active[data-page]");
        curPage = activePg ? Number(activePg.dataset.page) : 1;
    }

    function readParam(name, def) {
        return new URLSearchParams(location.search).get(name) || def;
    }

    /** list.do를 fetch해서 #inqBoardArea 내부만 교체.
     *  push=true면 주소창도 갱신(사용자 직접 클릭), false면 갱신 안 함(뒤로가기 복원). */
    function loadBoard(status, page, push) {
        const url = `/spendolive/inquiry/list.do?status=${encodeURIComponent(status)}&page=${page}`;

        fetch(url, { credentials: "same-origin" })
            .then(res => {
                if (!res.ok) throw new Error("list fetch failed: " + res.status);
                return res.text();
            })
            .then(html => {
                const doc = new DOMParser().parseFromString(html, "text/html");
                const newArea = doc.getElementById("inqBoardArea");
                if (!newArea) {
                    // 응답 구조가 예상과 다르면(에러 페이지 등) 일반 이동으로 폴백
                    window.location.href = url;
                    return;
                }
                boardArea.innerHTML = newArea.innerHTML;
                syncStateFromDom();

                if (push) {
                    history.pushState({ inqStatus: status, inqPage: page }, "", url);
                }
                window.scrollTo({ top: 0, behavior: "smooth" });
            })
            .catch(() => { window.location.href = url; });
    }

    /** 현재 필터/페이지 그대로 목록만 다시 로드 (삭제 직후 갱신용) */
    function reloadBoard() {
        loadBoard(curStatus, curPage, false);
    }

    /* ── 삭제: faq.js의 deleteInquiry를 AJAX 버전으로 덮어씀 ──
       (inquiry.js가 faq.js보다 뒤에 로드되므로 이 정의가 우선함) */
    window.deleteInquiry = function (inquiryId) {
        soConfirm("이 문의를 삭제하시겠어요? 삭제하면 되돌릴 수 없어요.").then(function (ok) {
            if (ok) doDeleteInquiry(inquiryId);
        });
    };

    function doDeleteInquiry(inquiryId) {
        const body = new URLSearchParams({ inquiryNo: inquiryId });
        fetch("/spendolive/inquiry/ajax/delete.do", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body
        })
            .then(res => {
                if (handleAuth(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.result !== "OK") {
                    soAlert(data.message || "삭제에 실패했습니다.", { type: "error" });
                    return;
                }
                // 상세 모달이 열려 있으면 닫고, 목록을 새로고침 없이 갱신
                if (typeof closeInqDetailModal === "function") closeInqDetailModal();
                reloadBoard();
            })
            .catch(() => soAlert("삭제 중 네트워크 오류가 발생했습니다.", { type: "error" }));
    }

    /* ════════════════════════════════════════════════════════
       [작성 화면]  #inquiryWriteForm 이 있을 때
       ════════════════════════════════════════════════════════ */
    const writeForm = document.getElementById("inquiryWriteForm");
    if (writeForm) {
        const btn = document.getElementById("inquirySubmitBtn");
        if (btn) btn.addEventListener("click", submitWrite);
    }

    function submitWrite() {
        // 개인정보 동의 + 필수값 검증 (작성 폼에만 privacyCheck 존재)
        const privacy = document.getElementById("privacyCheck");
        if (privacy && !privacy.checked) {
            soAlert("개인정보 수집 및 이용에 동의해 주세요.", { type: "error" });
            return;
        }
        const title = writeForm.querySelector('[name="title"]').value.trim();
        const content = writeForm.querySelector('[name="content"]').value.trim();
        const category = writeForm.querySelector('[name="category"]').value;
        const inquiryType = writeForm.querySelector('[name="inquiry_type"]').value;
        if (!category || !inquiryType) { soAlert("카테고리와 문의 유형을 선택해 주세요.", { type: "error" }); return; }
        if (!title || !content) { soAlert("제목과 상세 내용을 입력해 주세요.", { type: "error" }); return; }

        const submitBtn = document.getElementById("inquirySubmitBtn");
        submitBtn.disabled = true;
        submitBtn.textContent = "등록 중...";

        // FormData면 파일 첨부(multipart)가 자동 처리됨. Content-Type은 브라우저가 설정하게 둠
        const formData = new FormData(writeForm);

        fetch("/spendolive/inquiry/ajax/write.do", {
            method: "POST",
            credentials: "same-origin",
            body: formData
        })
            .then(res => {
                if (handleAuth(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.result !== "OK") {
                    soAlert(data.message || "문의 접수에 실패했습니다.", { type: "error" });
                    submitBtn.disabled = false;
                    submitBtn.textContent = "문의 제출하기";
                    return;
                }
                soAlert(data.message).then(function () {
                    window.location.href = "/spendolive/inquiry/list.do";
                });
            })
            .catch(() => {
                soAlert("문의 접수 중 네트워크 오류가 발생했습니다.", { type: "error" });
                submitBtn.disabled = false;
                submitBtn.textContent = "문의 제출하기";
            });
    }

    /* ════════════════════════════════════════════════════════
       문의 수정 화면: #inquiryEditForm이 있을 때
       ════════════════════════════════════════════════════════ */
    const editForm = document.getElementById("inquiryEditForm");
    if (editForm) {
        const btn = document.getElementById("inquiryEditSubmitBtn");
        if (btn) btn.addEventListener("click", submitEdit);
    }

    function submitEdit() {
        const title = editForm.querySelector('[name="title"]').value.trim();
        const content = editForm.querySelector('[name="content"]').value.trim();
        const category = editForm.querySelector('[name="category"]').value;
        const inquiryType = editForm.querySelector('[name="inquiry_type"]').value;
        if (!category || !inquiryType) { soAlert("카테고리와 문의 유형을 선택해 주세요.", { type: "error" }); return; }
        if (!title || !content) { soAlert("제목과 상세 내용을 입력해 주세요.", { type: "error" }); return; }

        const submitBtn = document.getElementById("inquiryEditSubmitBtn");
        submitBtn.disabled = true;
        submitBtn.textContent = "수정 중...";

        const params = new URLSearchParams(new FormData(editForm));

        fetch("/spendolive/inquiry/ajax/edit.do", {
            method: "POST",
            credentials: "same-origin",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params
        })
            .then(res => {
                if (handleAuth(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.result !== "OK") {
                    soAlert(data.message || "수정에 실패했습니다.", { type: "error" });
                    submitBtn.disabled = false;
                    submitBtn.textContent = "수정 완료";
                    return;
                }
                soAlert(data.message).then(function () {
                    window.location.href = "/spendolive/inquiry/list.do";
                });
            })
            .catch(() => {
                soAlert("수정 중 네트워크 오류가 발생했습니다.", { type: "error" });
                submitBtn.disabled = false;
                submitBtn.textContent = "수정 완료";
            });
    }
})();
