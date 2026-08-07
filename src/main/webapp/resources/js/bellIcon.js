function loadNotificationBadge() {
    const badge = document.getElementById("notificationBadge");
    if (!badge) return;


     /* [AJAX] GET /notification/ajax/unread_count.do
       - 헤더 종 아이콘 위 빨간 뱃지에 표시할 "안 읽은 알림 개수"만 가볍게 조회
       - 목록 전체를 안 받아오고 개수만 받아오는 이유: 이 함수는 페이지 로드 시마다,
         그리고 탭 포커스가 돌아올 때마다(visibilitychange) 계속 호출되기 때문에
         가벼운 응답이 필요함
        - 사용자가 직접 누른 게 아니라 백그라운드에서 조용히 자동으로 도는 요청이라
         fetchWithLoading()을 안 쓰고 순정 fetch를 씀 (매번 화면에 스피너가 번쩍이면
         오히려 거슬림)
       - 비로그인이거나 서버 오류면 catch에서 뱃지를 그냥 숨김(에러를 사용자에게 노출 안 함) */

    fetch("/spendolive/notification/ajax/unread_count.do",  { credentials: 'same-origin' })
        .then(response => {
            if (!response.ok) throw new Error("HTTP " + response.status);
            return response.json();
        })
        .then(data => {
            const count = (data && data.unread_count) ? data.unread_count : 0;
            if (count <= 0) {
                badge.style.display = "none";
                badge.textContent   = "";
            } else {
                badge.textContent   = count > 99 ? "99+" : String(count);
                badge.style.display = "inline-flex";
            }
        })
        .catch(() => {
            // 비로그인이거나 서버 오류 → 배지 숨김
            badge.style.display = "none";
        });
}

// ── 즉시 실행 (DOMContentLoaded 타이밍과 무관하게)
(function tryBadge() {
    const badge = document.getElementById("notificationBadge");
    if (badge) {
        loadNotificationBadge();
    } else {
        // badge 요소가 아직 파싱 안 됐으면 DOM 준비 후 재시도
        document.addEventListener("DOMContentLoaded", loadNotificationBadge);
    }
})();

// ── 탭 포커스 복귀 시 자동 갱신
document.addEventListener("visibilitychange", function () {
    if (document.visibilityState === "visible") {
        loadNotificationBadge();
    }
});

/* =========================================================
   종 아이콘 드롭다운 - 최근 알림 미리보기
   - 최근 5개 정도는 스크롤 없이 바로 보이고, 그 이상은 목록 안에서 스크롤(.notif-dropdown-list
     의 max-height + overflow-y로 처리, CSS 쪽에서 설정).
   - notice.js가 이 페이지에 없을 수도 있어서(header.jsp는 전역이지만 notice.js는
     공지/캘린더 페이지에만 로드됨), 읽음처리+이동 로직은 notice.js에 기대지 않고
     여기서 독립적으로 처리함.
   ========================================================= */
function toggleNotifDropdown(event) {
    event.stopPropagation();
    const dropdown = document.getElementById("notifDropdown");
    if (!dropdown) return;

    const willShow = !dropdown.classList.contains("show");
    dropdown.classList.toggle("show", willShow);

    if (willShow) {
        loadNotifDropdownList();
    }
}

// 드롭다운 열려있을 때 바깥 아무 데나 클릭하면 닫기
document.addEventListener("click", function (e) {
    const dropdown = document.getElementById("notifDropdown");
    const toggleBtn = document.getElementById("bellToggleBtn");
    if (!dropdown || !dropdown.classList.contains("show")) return;
    if (dropdown.contains(e.target) || (toggleBtn && toggleBtn.contains(e.target))) return;
    dropdown.classList.remove("show");
});

function loadNotifDropdownList() {
    const list = document.getElementById("notifDropdownList");
    if (!list) return;

    list.innerHTML = '<div class="notif-dropdown-empty">불러오는 중...</div>';

    fetch("/spendolive/notification/ajax/unread_list.do", { credentials: 'same-origin' })
        .then(response => {
            if (response.status === 401) {
                // 비로그인 - 서버가 401로 구분해서 알려줌
                list.innerHTML = '<div class="notif-dropdown-empty">로그인 시 알림을 확인할 수 있습니다.</div>';
                return null;
            }
            if (!response.ok) throw new Error("HTTP " + response.status);
            return response.json();
        })
        .then(data => {
            if (data === null) return; // 위에서 401 처리하고 끝낸 경우
            if (!data || data.length === 0) {
                list.innerHTML = '<div class="notif-dropdown-empty">새 알림이 없습니다.</div>';
                return;
            }
            list.innerHTML = data.map(n => `
                <a href="javascript:void(0)" class="notif-dropdown-item unread"
                   onclick="readNotificationFromBell(${n.notification_id}, '${(n.link_url || '').replace(/'/g, "\\'")}')">
                    <strong>${n.title}</strong>
                    <span>${n.message}</span>
                    <small>${n.created_at}</small>
                </a>
            `).join("");
        })
        .catch(() => {
            list.innerHTML = '<div class="notif-dropdown-empty">알림을 불러오지 못했습니다.</div>';
        });
}

// 드롭다운 안 알림 클릭 시 읽음 처리 후 이동 (notice.js의 readNotification과 동일한 서버
// 엔드포인트를 쓰지만, 이 파일은 어느 페이지에서든 로드되므로 notice.js에 의존하지 않고 독립 구현)
function readNotificationFromBell(notification_id, link_url) {
    /* [AJAX] POST /notification/ajax/read.do
       - notice.js의 readNotification()과 같은 엔드포인트를 호출하지만,
         bellIcon.js는 알림센터 페이지(noticeCenter.jsp)가 아닌 곳에서도
         로드되는 전역 스크립트라 notice.js의 함수를 그대로 재사용하지 않고
         별도로 구현함 (파일 간 의존성을 만들지 않기 위해) */
    fetch("/spendolive/notification/ajax/read.do", {
        method: "POST",
        credentials: 'same-origin',
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "notification_id=" + notification_id
    })
    .then(response => response.json())
    .then(data => {
        if (data.result === "OK") {
            // 읽음 처리 성공 → 헤더 뱃지 카운트를 즉시 다시 불러와서 -1 반영
            loadNotificationBadge();
            if (link_url && link_url !== "null" && link_url !== "") {
                location.href = link_url;
            } else {
                location.href = "/spendolive/notification/detail.do?notification_id=" + notification_id;
            }
        }
    })
    .catch(() => {
        alert("네트워크 오류가 발생했습니다.");
    });
}