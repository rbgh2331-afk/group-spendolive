/* =========================================================
    공지 상세 페이지 찜하기(별) 버튼

   ========================================================= */
   (function () {
    var btn = document.getElementById("detailStarBtn");
    if (!btn) return;

    var noticeId = btn.dataset.noticeId;
    var isLogin  = btn.dataset.login === "true";
    var lsKey    = "notice_star_" + noticeId;

    function setStar(active) {
        btn.textContent = active ? "★" : "☆";
        active ? btn.classList.add("active") : btn.classList.remove("active");
    }

    // 로그인 상태면 서버가 렌더링 시점에 계산해준 실제 찜 상태(data-star)를 신뢰함
    // (localStorage는 서버 상태를 모르는 비로그인 사용자를 위한 값이라
    //  로그인 사용자에게 쓰면 목록에서 찜한 게 상세에서는 안 보이는 등 서로 어긋날 수 있음)
    var initialActive = isLogin ? (btn.dataset.star === "Y") : (localStorage.getItem(lsKey) === "Y");
    setStar(initialActive);

    btn.addEventListener("click", function () {
        if (isLogin) {
            postNoticeStarToggle(noticeId)
            .then(function (data) {
                if (data.result === "OK") {
                    var nowActive = btn.textContent.trim() === "★";
                    setStar(!nowActive);
                    nowActive ? localStorage.removeItem(lsKey) : localStorage.setItem(lsKey, "Y");
                } else if (data.result === "LOGIN_REQUIRED") {
                    alert("로그인이 필요합니다.");
                } else {
                    // 서버가 정상 응답했지만 result가 OK/LOGIN_REQUIRED가 아닌 경우
                    // (INVALID_PARAM 또는 ERROR). 콘솔에 실제 응답을 남겨서
                    // 어떤 값이 왔는지 바로 확인할 수 있게 함
                    console.error("[noticeDetail] star.do 응답:", data);
                    alert("처리 중 오류가 발생했습니다.");
                }
            })
            .catch(function (e) {
                console.error("[noticeDetail] star.do 요청 실패:", e);
                alert("네트워크 오류가 발생했습니다.");
            });
        } else {
            var nowActive = btn.textContent.trim() === "★";
            setStar(!nowActive);
            nowActive ? localStorage.removeItem(lsKey) : localStorage.setItem(lsKey, "Y");
        }
    });
})();
