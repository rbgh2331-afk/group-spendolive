// 기상청 격자좌표(nx, ny) - 지역별 대표 지점 (근사값)
const WEATHER_REGIONS = {
    seoul:    { nx: 60, ny: 127, label: "서울" },
    incheon:  { nx: 55, ny: 124, label: "인천" },
    suwon:    { nx: 60, ny: 121, label: "경기(수원)" },
    chuncheon:{ nx: 73, ny: 134, label: "강원(춘천)" },
    cheongju: { nx: 69, ny: 106, label: "충북(청주)" },
    hongseong:{ nx: 55, ny: 106, label: "충남(홍성)" },
    daejeon:  { nx: 67, ny: 100, label: "대전" },
    sejong:   { nx: 66, ny: 103, label: "세종" },
    jeonju:   { nx: 63, ny: 89,  label: "전북(전주)" },
    mokpo:    { nx: 50, ny: 67,  label: "전남(목포)" },
    gwangju:  { nx: 58, ny: 74,  label: "광주" },
    andong:   { nx: 91, ny: 106, label: "경북(안동)" },
    daegu:    { nx: 89, ny: 90,  label: "대구" },
    changwon: { nx: 90, ny: 77,  label: "경남(창원)" },
    busan:    { nx: 98, ny: 76,  label: "부산" },
    ulsan:    { nx: 102, ny: 84, label: "울산" },
    jeju:     { nx: 52, ny: 38,  label: "제주" }
};

// 날씨 지역 선택 옵션 구성
// 내 위치 옵션과 지역 목록을 추가하고 서울을 기본 선택값으로 설정
function populateWeatherRegionSelect() {
    const select = document.getElementById("weatherPageRegionSelect");
    if (!select) return;

    let html = '<option value="current">내 위치</option>';
    for (const key in WEATHER_REGIONS) {
        const selected = key === "seoul" ? " selected" : "";
        html += `<option value="${key}"${selected}>${WEATHER_REGIONS[key].label}</option>`;
    }
    select.innerHTML = html;
}
populateWeatherRegionSelect();

// ===== 날씨 페이지 (/spendolive/weather.do) 전용 =====

// 지역 select에서 "내 위치 사용"(value="current")을 고르면 브라우저 GPS로 조회,
// 그 외 지역이면 WEATHER_REGIONS에 미리 정해둔 좌표로 바로 조회
function loadWeatherPage() {
    const select = document.getElementById("weatherPageRegionSelect");
    const region = select ? select.value : "seoul";
    const el = document.getElementById("weatherPageResult");

    if (region !== "current") {
        const coords = WEATHER_REGIONS[region];
        if (!coords) return;
        if (el) el.innerHTML = '<span class="weather-page-loading">날씨 정보를 불러오는 중...</span>';
        fetchWeatherPageByGrid(coords.nx, coords.ny);
        return;
    }

    if (!navigator.geolocation) {
        if (el) el.innerHTML = "<span>이 브라우저는 위치 확인을 지원하지 않습니다.</span>";
        return;
    }

    if (el) el.innerHTML = '<span class="weather-page-loading">날씨 정보를 불러오는 중...</span>';

    navigator.geolocation.getCurrentPosition(
        function (position) {
            fetchWeatherPageByLatLon(position.coords.latitude, position.coords.longitude);
        },
        function (error) {
            console.warn("위치 권한 거부 또는 오류:", error.message);
            if (el) el.innerHTML = "<span>위치 권한을 허용하거나 지역을 선택해 주세요.</span>";
        }
    );
}

// 지역 좌표(nx, ny)로 날씨 조회 - WeatherAjaxController의 nx/ny 파라미터 경로
function fetchWeatherPageByGrid(nx, ny) {
    fetch(`/ajax/weather.do?nx=${nx}&ny=${ny}`)
        .then(res => {
            if (!res.ok) throw new Error("날씨 조회 실패");
            return res.json();
        })
        .then(items => renderWeatherPage(items))
        .catch(err => {
            console.error(err);
            const el = document.getElementById("weatherPageResult");
            if (el) el.innerHTML = "<span>날씨 정보를 불러오지 못했습니다.</span>";
        });
}

// 위도/경도로 날씨 조회 - "내 위치 사용" 선택 시 브라우저 GPS 좌표를 그대로 서버에 넘김
// (nx/ny 변환은 프론트에서 안 하고 WeatherAjaxController가 서버에서 처리)
function fetchWeatherPageByLatLon(lat, lon) {
    fetch(`/ajax/weather.do?lat=${lat}&lon=${lon}`)
        .then(res => {
            if (!res.ok) throw new Error("날씨 조회 실패");
            return res.json();
        })
        .then(items => renderWeatherPage(items))
        .catch(err => {
            console.error(err);
            const el = document.getElementById("weatherPageResult");
            if (el) el.innerHTML = "<span>날씨 정보를 불러오지 못했습니다.</span>";
        });
}

/**
 * 기상청 PTY(강수형태) / SKY(하늘상태) 코드를 이모지로 변환
 *
 * PTY: 0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기
 * SKY: 1 맑음, 3 구름많음, 4 흐림  (PTY가 0일 때만 의미 있음)
 *
 * 참고: 이 API(단기예보 getVilageFcst)는 낙뢰(번개) 데이터를 별도로 제공하지 않는다.
 * 그래서 "번개"는 실제 낙뢰 감지가 아니라, 소나기(PTY=4)일 때의 근사 표현으로만 넣었다.
 */
function getWeatherEmoji(pty, sky) {
    switch (pty) {
        case "1": return "🌧️";  // 비
        case "2": return "🌨️";  // 비/눈
        case "3": return "⛄";   // 눈
        case "4": return "⛈️";  // 소나기 (번개 느낌으로 표현, 실제 낙뢰 감지 아님)
        default:
            switch (sky) {
                case "1": return "☀️";  // 맑음
                case "3": return "⛅";  // 구름많음
                case "4": return "☁️";  // 흐림
                default: return "🌡️";
            }
    }
}

function getWeatherLabel(pty, sky) {
    switch (pty) {
        case "1": return "비";
        case "2": return "비/눈";
        case "3": return "눈";
        case "4": return "소나기";
        default:
            switch (sky) {
                case "1": return "맑음";
                case "3": return "구름많음";
                case "4": return "흐림";
                default: return "-";
            }
    }
}

// 기상청 응답 배열(items)에서 필요한 카테고리(TMP=기온, POP=강수확률, PTY=강수형태,
// SKY=하늘상태)만 찾아 뽑아서 이모지 + 기온 + 강수확률 카드로 그림
function renderWeatherPage(items) {
    const tmp = items.find(i => i.category === "TMP");
    const pop = items.find(i => i.category === "POP");
    const pty = items.find(i => i.category === "PTY");
    const sky = items.find(i => i.category === "SKY");

    const el = document.getElementById("weatherPageResult");
    if (!el) return;

    const ptyValue = pty ? pty.fcstValue : "0";
    const skyValue = sky ? sky.fcstValue : "1";

    const emoji = getWeatherEmoji(ptyValue, skyValue);
    const label = getWeatherLabel(ptyValue, skyValue);

    el.innerHTML = `
        <div class="weather-page-emoji">${emoji}</div>
        <div class="weather-page-label">${label}</div>
        <div class="weather-page-numbers">
            <span>기온 ${tmp ? tmp.fcstValue : "-"}°C</span>
            <span>강수확률 ${pop ? pop.fcstValue : "-"}%</span>
        </div>
    `;
}


// ===== 헤더 날씨 드롭다운 토글 (bellIcon.js의 toggleNotifDropdown과 동일한 패턴) =====

// 헤더 날씨 아이콘 클릭 시 드롭다운 열기/닫기. 처음 열릴 때만 loadWeatherPage() 호출
// (이미 열려있는 걸 또 열면 재조회 안 함 - willShow가 false일 때는 그냥 닫기만 함)
function toggleWeatherDropdown(event) {
    event.stopPropagation();
    const dropdown = document.getElementById("weatherDropdown");
    if (!dropdown) return;

    const willShow = !dropdown.classList.contains("show");
    dropdown.classList.toggle("show", willShow);

    if (willShow) {
        loadWeatherPage();
    }
}

// 드롭다운 열려있을 때 바깥 아무 데나 클릭하면 닫기
document.addEventListener("click", function (e) {
    const dropdown = document.getElementById("weatherDropdown");
    const toggleBtn = document.getElementById("weatherToggleBtn");
    if (!dropdown || !dropdown.classList.contains("show")) return;

    if (!dropdown.contains(e.target) && toggleBtn && !toggleBtn.contains(e.target)) {
        dropdown.classList.remove("show");
    }
});
