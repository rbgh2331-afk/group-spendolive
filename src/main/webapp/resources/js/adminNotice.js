/* =========================================================
   adminNotice.js
   관리자 공지사항 관리 화면(목록/작성/수정)을 전부 AJAX(fetch)로 처리.
   - 목록/등록/수정/삭제 어느 것도 페이지 전체 새로고침(form submit,
     a 태그 이동)을 쓰지 않고, 전부 JSON을 주고받아 화면 일부만 갱신한다.
   - 서버 쪽은 AdminNoticeController의 /admin/notice/ajax/* 엔드포인트를 사용.
   ========================================================= */

   (function () {

    /* ─────────────────────────────────────────────────────────────
       이 화면 전용 알림/확인 모달 (지역 함수 — 공용 파일/soModal.js 없이 자체 포함)
       회원가입의 payment-status 모달과 같은 느낌(둥근 카드 + 아이콘 + 확인 버튼).
       CSS·DOM은 처음 호출될 때 만들고, id로 가드해서 중복 생성 방지.
         soAlert("메시지")                     // 성공(초록 ✓)
         soAlert("메시지", { type: "error" })  // 실패(빨강 !)
         soAlert(...).then(() => { ... })      // 확인 누른 뒤 실행
         soConfirm("정말?").then(ok => { if (ok) ... })
       ───────────────────────────────────────────────────────────── */
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
    // 이 프로젝트의 다른 js(notice.js 등)와 동일하게, contextPath 변수를 따로
    // 두지 않고 루트 기준 절대경로("/admin/notice/...")를 그대로 사용

    // 현재 페이지 번호를 기억해두고, 삭제 후 같은 페이지를 다시 불러올 때 사용
    let currentPage = 1;

    /* ── 공통: 서버 응답이 로그인/권한 문제(401)면 로그인 페이지로 보냄 ── */
    function handleAuthError(res) {
        if (res.status === 401) {
            soAlert('관리자만 접근할 수 있습니다. 로그인 후 다시 시도해 주세요.', { type: 'error' });
            window.location.href = '/member/loginForm.do';
            return true;
        }
        return false;
    }

    /* =========================================================
       [목록 화면] adminNoticeList.jsp 에서만 동작
       ========================================================= */
    const tableBody = document.getElementById('adminNoticeTableBody');
    if (tableBody) {
        initNoticeListPage();
    }

    function initNoticeListPage() {
        loadNoticeList(1);

        // 이벤트 위임: 표/페이지네이션 안에서 일어나는 클릭을 한 곳에서 처리
        // (행이 새로 그려질 때마다 매번 리스너를 다시 붙일 필요가 없어짐)
        document.getElementById('adminNoticeTableBody').addEventListener('click', onTableClick);
        document.getElementById('adminNoticePagination').addEventListener('click', onPaginationClick);

        // ── 모달 관련 버튼 바인딩 (모달 폼은 정적이라 로드 시 1번만 붙이면 됨) ──
        const createBtn = document.getElementById('noticeCreateBtn');
        if (createBtn) createBtn.addEventListener('click', () => openNoticeModal(0));

        const submitBtn = document.getElementById('noticeModalSubmitBtn');
        if (submitBtn) submitBtn.addEventListener('click', submitNoticeModal);

        // 모달 닫기: X/취소 버튼(data-action=closeNoticeModal) + 어두운 배경 클릭
        const modal = document.getElementById('adminNoticeModal');
        if (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal) { closeNoticeModal(); return; }         // 배경 클릭
                if (e.target.closest('[data-action="closeNoticeModal"]')) closeNoticeModal();
            });
        }
    }

    function onTableClick(e) {
        const delBtn = e.target.closest('[data-action="delete"]');
        if (delBtn) {
            const noticeId = delBtn.dataset.noticeId;
            soConfirm('정말 삭제하시겠습니까?').then(function (ok) {
                if (ok) deleteNotice(noticeId);
            });
            return;
        }
        // 제목/수정 클릭 → 모달을 열어 수정 (페이지 이동 대신)
        const editBtn = e.target.closest('[data-action="editNotice"]');
        if (editBtn) {
            openNoticeModal(Number(editBtn.dataset.noticeId));
        }
    }

    function onPaginationClick(e) {
        const btn = e.target.closest('[data-page]');
        if (!btn) return;
        loadNoticeList(Number(btn.dataset.page));
    }

    /** 목록 + 페이지네이션 데이터를 서버에서 받아와 화면을 다시 그림 */
    function loadNoticeList(page) {
        currentPage = page;
        fetch(`/admin/notice/ajax/list.do?page=${page}`, { credentials: 'same-origin' })
            .then(res => {
                if (handleAuthError(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.result !== 'OK') {
                    soAlert(data.message || '목록을 불러오지 못했습니다.', { type: 'error' });
                    return;
                }
                renderNoticeTable(data.noticeList);
                renderPagination(data.currentPage, data.totalPages);
                document.getElementById('noticeTotalCount').textContent = data.totalCount;
            })
            .catch(() => soAlert('공지 목록을 불러오는 중 네트워크 오류가 발생했습니다.', { type: 'error' }));
    }

    /** notice 배열을 받아 tbody 안에 <tr>들을 직접 그려 넣음 */
    function renderNoticeTable(noticeList) {
        const tbody = document.getElementById('adminNoticeTableBody');

        if (!noticeList || noticeList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6"><div class="admin-empty-filter">등록된 공지사항이 없습니다.</div></td></tr>';
            return;
        }

        tbody.innerHTML = noticeList.map(notice => `
            <tr>
                <td>${notice.notice_id}</td>
                <td>
                    ${notice.pinned_yn === 'Y'
                        ? '<span class="badge green">중요</span>'
                        : '<span class="badge gray">일반</span>'}
                </td>
                <td>
                    <a href="javascript:void(0)" data-action="editNotice" data-notice-id="${notice.notice_id}">${escapeHtml(notice.title)}</a>
                </td>
                <td>${escapeHtml(notice.admin_id || '')}</td>
                <td>${notice.created_at}</td>
                <td>
                    <div class="table-actions">
                        <button type="button" class="mini-btn" data-action="editNotice" data-notice-id="${notice.notice_id}">수정</button>
                        <button type="button" class="mini-btn danger" data-action="delete" data-notice-id="${notice.notice_id}">삭제</button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    /** currentPage/totalPages 를 기반으로 페이지네이션 버튼을 직접 그려 넣음
     *  (기존 JSP의 c:forEach 윈도우 방식과 동일한 로직을 JS로 옮긴 것) */
    function renderPagination(current, totalPages) {
        const wrap = document.getElementById('adminNoticePagination');
        wrap.innerHTML = '';
        if (totalPages <= 1) return;

        const pgStart = Math.max(current - 2, 1);
        const pgEnd = Math.min(current + 2, totalPages);
        let html = '';

        if (pgStart > 1) {
            html += `<button type="button" class="admin-pg-btn" data-page="1">1</button>`;
            if (pgStart > 2) html += `<span class="admin-pg-btn">…</span>`;
        }
        for (let p = pgStart; p <= pgEnd; p++) {
            html += `<button type="button" class="admin-pg-btn ${p === current ? 'active' : ''}" data-page="${p}">${p}</button>`;
        }
        if (pgEnd < totalPages) {
            if (pgEnd < totalPages - 1) html += `<span class="admin-pg-btn">…</span>`;
            html += `<button type="button" class="admin-pg-btn" data-page="${totalPages}">${totalPages}</button>`;
        }
        wrap.innerHTML = html;
    }

    /** 삭제 요청 → 성공하면 같은 페이지를 다시 불러와 화면 갱신 */
    function deleteNotice(noticeId) {
        const body = new URLSearchParams({ notice_id: noticeId });
        fetch(`/admin/notice/ajax/delete.do`, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body
        })
            .then(res => {
                if (handleAuthError(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) return;
                if (data.result !== 'OK') {
                    soAlert(data.message || '삭제에 실패했습니다.', { type: 'error' });
                    return;
                }
                loadNoticeList(currentPage);
            })
            .catch(() => soAlert('삭제 중 네트워크 오류가 발생했습니다.', { type: 'error' }));
    }

    function escapeHtml(str) {
        return String(str)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;');
    }

    /* =========================================================
       공지 작성/수정 모달 (목록 위 팝업 — 문의/FAQ와 동일한 방식)
       - 작성: 빈 폼으로 열기 (openNoticeModal(0))
       - 수정: ajax/detail.do로 기존 값을 불러와 폼에 채운 뒤 열기
       - 저장: notice_id 유무로 insert/update 갈라 호출 → 성공 시 모달 닫고 목록 갱신
       ========================================================= */

    /** 모달 열기. noticeId>0 이면 수정 모드(상세 불러와 채움), 0이면 작성 모드 */
    function openNoticeModal(noticeId) {
        // 폼 초기화
        document.getElementById('modalNoticeId').value = noticeId || 0;
        document.getElementById('modalNoticeTitleInput').value = '';
        document.getElementById('modalNoticeContent').value = '';
        document.getElementById('modalNoticePinned').checked = false;

        const isEdit = noticeId > 0;
        document.getElementById('noticeModalHeading').textContent = isEdit ? '공지사항 수정' : '새 공지사항 등록';
        document.getElementById('noticeModalSubmitBtn').textContent = isEdit ? '수정' : '등록';

        if (isEdit) {
            // 수정 모드: 기존 값을 불러와 채움
            fetch(`/admin/notice/ajax/detail.do?notice_id=${noticeId}`, { credentials: 'same-origin' })
                .then(res => {
                    if (handleAuthError(res)) return null;
                    return res.json();
                })
                .then(data => {
                    if (!data) return;
                    if (data.result !== 'OK') {
                        soAlert(data.message || '공지사항을 불러오지 못했습니다.', { type: 'error' });
                        closeNoticeModal();
                        return;
                    }
                    const n = data.notice;
                    document.getElementById('modalNoticeTitleInput').value = n.title;
                    document.getElementById('modalNoticeContent').value = n.content;
                    document.getElementById('modalNoticePinned').checked = (n.pinned_yn === 'Y');
                })
                .catch(() => soAlert('공지사항을 불러오는 중 네트워크 오류가 발생했습니다.', { type: 'error' }));
        }

        document.getElementById('adminNoticeModal').classList.add('show');
        document.getElementById('modalNoticeTitleInput').focus();
    }

    function closeNoticeModal() {
        document.getElementById('adminNoticeModal').classList.remove('show');
    }

    /** 등록/수정 버튼 클릭 시: notice_id 유무로 insert/update 갈라 호출 */
    function submitNoticeModal() {
        const noticeId = Number(document.getElementById('modalNoticeId').value || 0);
        const isEdit = noticeId > 0;
        const title = document.getElementById('modalNoticeTitleInput').value.trim();
        const content = document.getElementById('modalNoticeContent').value.trim();
        const pinnedYn = document.getElementById('modalNoticePinned').checked ? 'Y' : 'N';

        if (!title || !content) {
            soAlert('제목과 내용을 모두 입력해 주세요.', { type: 'error' });
            return;
        }

        const submitBtn = document.getElementById('noticeModalSubmitBtn');
        submitBtn.disabled = true;

        const url = isEdit ? '/admin/notice/ajax/update.do' : '/admin/notice/ajax/insert.do';
        const params = new URLSearchParams({ title, content, pinned_yn: pinnedYn });
        if (isEdit) params.set('notice_id', noticeId);

        fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params
        })
            .then(res => {
                if (handleAuthError(res)) return null;
                return res.json();
            })
            .then(data => {
                if (!data) { submitBtn.disabled = false; return; }
                if (data.result !== 'OK') {
                    soAlert(data.message || (isEdit ? '수정에 실패했습니다.' : '등록에 실패했습니다.'), { type: 'error' });
                    submitBtn.disabled = false;
                    return;
                }
                closeNoticeModal();
                submitBtn.disabled = false;
                // 등록이면 1페이지(최신이 위), 수정이면 보던 페이지 유지하며 목록만 갱신
                soAlert(data.message).then(function () { loadNoticeList(isEdit ? currentPage : 1); });
            })
            .catch(() => {
                soAlert('저장 중 네트워크 오류가 발생했습니다.', { type: 'error' });
                submitBtn.disabled = false;
            });
    }
})();
