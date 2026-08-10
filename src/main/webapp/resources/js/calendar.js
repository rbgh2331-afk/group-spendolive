let calendar;
let monthlyExpenses = [];
let sidePanelPage = 1;
const SIDE_PAGE_SIZE = 3;

// "오늘 할 일"도 "이번달 주요 지출"과 동일하게 3개씩 페이지네이션
let todayTodoItems = [];
let todayTodoPage = 1;

// "자세히보기" 목록 - FullCalendar listMonth뷰 대신 직접 페이지네이션
let isDetailListOpen = false;
let detailListPage = 1;
const DETAIL_LIST_PAGE_SIZE = 9; // 달력(5주 기준) 높이랑 대략 맞춘 값

document.addEventListener('DOMContentLoaded', function() {
    const calendarEl = document.getElementById('calendar')

    calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: 'dayGridMonth',
      locale: 'ko',
      height: 'auto',
      
      headerToolbar: false,
      fixedWeekCount: 1,
      dayMaxEvents: 2, // 날짜 칸당 최대 2개까지만 표시, 넘으면 "+N개" 링크로 숨김 (칸 높이 고정)
      datesSet: function(info) {
        const year = info.view.currentStart.getFullYear();
        const month = info.view.currentStart.getMonth() + 1;
        document.getElementById('calendarTitle').textContent = `${year}년 ${month}월`;

        // 월/뷰가 바뀔 때마다(이전달, 다음달, 초기로드 전부 포함) 그 달 지출 다시 불러옴
        sidePanelPage = 1;
        detailListPage = 1;
        loadMonthlyExpenses(year, month);
      },
      dateClick: function(info) {

        // 클릭한 지점이 날짜 숫자(예: "7일")가 아니면 그냥 무시
        const isDayNumberClick = info.jsEvent.target.closest('.fc-daygrid-day-number');
        if (!isDayNumberClick) {
            return;
        }
        location.href = `/spendolive/expense/list.do?date=${info.dateStr}#expense-list`;
        },
      
      eventContent: function(arg) {
        const amount = arg.event.extendedProps.amount;
        const category_name = arg.event.extendedProps.category_name;
        const expense_type = arg.event.extendedProps.expense_type; // FIXED / VARIABLE / OTT

        const wrapper = document.createElement('div');
        wrapper.className = `calendar-expense-chip chip-type-${(expense_type || 'variable').toLowerCase()}`;
        wrapper.innerHTML = `
          <span class="chip-amount">${Number(amount).toLocaleString()}원</span>
          <span class="chip-category">${category_name}</span>
        `;
        return { domNodes: [wrapper] };
      }
    });
    calendar.render()


    loadTodayTodo();  
  

  function changeMonth(direction) {
    if (direction === -1) calendar.prev();
    if (direction === 1) calendar.next();
  }
  window.changeMonth = changeMonth; // onclick="changeMonth(-1)" 에서 접근 가능하게

  const detailBtn = document.getElementById('detailBtn');
  if (detailBtn) {
    detailBtn.addEventListener('click', function() {
        const calendarGridEl = document.getElementById('calendar');
        const detailListEl = document.getElementById('calendarDetailList');
        if (!isDetailListOpen) {
        calendarGridEl.style.display = 'none';
        detailListEl.style.display = '';
        isDetailListOpen = true;
        detailListPage = 1;
        renderDetailList();
        this.textContent = '달력으로 보기';
        } else {
        detailListEl.style.display = 'none';
        calendarGridEl.style.display = '';
        isDetailListOpen = false;
        this.textContent = '자세히보기';
        // display:none으로 숨겨져 있는 동안 FullCalendar가 크기 계산을 못 해서
        // 내부 치수가 0으로 굳어버림 - 다시 보여준 뒤 강제로 재계산시켜야 함
        // (안 하면 달력이 콩알만하게 찌그러져 보임)
        calendar.updateSize();
        }
    });
} else {
    console.warn('detailBtn 요소를 못 찾았어요');
}
});

/* =========================================================
   지출 데이터 로드 & 렌더링
   ========================================================= */

   /* [AJAX] GET /calendar/expenses.do?year=&month=
   - FullCalendar의 datesSet 콜백(달력에서 월이 바뀔 때마다 자동 발생)에서 호출됨
   - year/month를 쿼리스트링으로 넘겨 "그 달에 해당하는 지출 내역만" 서버에서 걸러 받음
   - 응답 데이터 하나로 달력 이벤트(renderCalendarEvents)와 사이드 패널 목록
     (renderSidePanel)을 같이 그리므로, 여기서만 fetch하고 나머지는 순수 렌더 함수로 분리 */

function loadMonthlyExpenses(year, month) {
    fetch(`/spendolive/calendar/expenses.do?year=${year}&month=${month}`, {
        credentials: 'same-origin'
    })
        .then(res => {
            if (!res.ok) {
                throw new Error('지출 목록을 불러오지 못했습니다.');
            }
            return res.json();
        })
        .then(data => {
            monthlyExpenses = data;
            renderCalendarEvents();
            renderSidePanel();
            if (isDetailListOpen) renderDetailList();
        })
        .catch(err => {
            console.error(err);
        });
}

function renderCalendarEvents() {
    const events = monthlyExpenses.map(exp => ({
        id: String(exp.expense_id),
        title: exp.expense_title,
        date: exp.expense_date,
        extendedProps: {
            amount: exp.amount,
            category_name: exp.category_name,
            expense_type: exp.expense_type
        }
    }));

    calendar.removeAllEvents();
    calendar.addEventSource(events);
}

function renderSidePanel() {
    // 날짜 최신순 정렬
    const sorted = [...monthlyExpenses].sort((a, b) =>
        a.expense_date < b.expense_date ? 1 : -1
    );

    const totalPages = Math.max(1, Math.ceil(sorted.length / SIDE_PAGE_SIZE));
    if (sidePanelPage > totalPages) {
        sidePanelPage = totalPages;
    }

    const startIdx = (sidePanelPage - 1) * SIDE_PAGE_SIZE;
    const pageItems = sorted.slice(startIdx, startIdx + SIDE_PAGE_SIZE);

    const listEl = document.getElementById('sideEventList');
    if (!listEl) {
        console.warn('sideEventList 요소를 못 찾았어요 (calendar.jsp 확인 필요)');
        return;
    }

    listEl.innerHTML = '';

    if (pageItems.length === 0) {
        listEl.innerHTML = '<p class="empty-text">이번 달 지출 내역이 없습니다.</p>';
    } else {
        pageItems.forEach(exp => {
            const dateLabel = exp.expense_date.slice(5).replace('-', '.'); // "2026-07-05" -> "07.05"
            const typeClass = `type-${(exp.expense_type || 'variable').toLowerCase()}`;
            const item = document.createElement('div');
            item.className = `side-event ${typeClass}`;
            item.innerHTML = `
                <strong>${dateLabel} ${exp.expense_title}</strong>
                <span>${Number(exp.amount).toLocaleString()}원 · ${exp.category_name}</span>
            `;
            listEl.appendChild(item);
        });
    }

    renderSidePanelPager(totalPages);
}

function renderSidePanelPager(totalPages) {
    let pagerEl = document.getElementById('sidePanelPager');

    if (!pagerEl) {
        pagerEl = document.createElement('div');
        pagerEl.id = 'sidePanelPager';
        pagerEl.className = 'side-pager';
        document.getElementById('sideEventList').insertAdjacentElement('afterend', pagerEl);
    }

    if (totalPages <= 1) {
        pagerEl.innerHTML = '';
        return;
    }

    // 1, 2, 3, 4, 5 ... 숫자 버튼 생성
    let buttonsHtml = '';
    for (let page = 1; page <= totalPages; page++) {
        const isActive = page === sidePanelPage ? 'active' : '';
        buttonsHtml += `<button type="button" class="pager-num-btn ${isActive}" data-page="${page}">${page}</button>`;
    }
    pagerEl.innerHTML = buttonsHtml;

    pagerEl.querySelectorAll('.pager-num-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            sidePanelPage = Number(btn.dataset.page);
            renderSidePanel();
        });
    });
}

/* =========================================================
   "자세히보기" 목록 - #calendarDetailList
   FullCalendar listMonth뷰는 페이지네이션이 없어서, monthlyExpenses를
   그대로 재활용해 사이드 패널(renderSidePanel)과 동일한 방식으로 직접 그림.
   한 페이지에 DETAIL_LIST_PAGE_SIZE(9)개씩 - 사이드 패널(3개)보다 넉넉하게.
   ========================================================= */
function renderDetailList() {
    const listEl = document.getElementById('calendarDetailList');
    if (!listEl) {
        console.warn('calendarDetailList 요소를 못 찾았어요 (calendar.jsp 확인 필요)');
        return;
    }

    // 날짜 최신순 정렬 (사이드 패널과 동일)
    const sorted = [...monthlyExpenses].sort((a, b) =>
        a.expense_date < b.expense_date ? 1 : -1
    );

    const totalPages = Math.max(1, Math.ceil(sorted.length / DETAIL_LIST_PAGE_SIZE));
    if (detailListPage > totalPages) {
        detailListPage = totalPages;
    }

    const startIdx = (detailListPage - 1) * DETAIL_LIST_PAGE_SIZE;
    const pageItems = sorted.slice(startIdx, startIdx + DETAIL_LIST_PAGE_SIZE);

    let itemsHtml;
    if (pageItems.length === 0) {
        itemsHtml = '<p class="empty-text">이번 달 지출 내역이 없습니다.</p>';
    } else {
        itemsHtml = pageItems.map(exp => {
            const dateLabel = exp.expense_date.slice(5).replace('-', '.'); // "2026-07-05" -> "07.05"
            const typeClass = `type-${(exp.expense_type || 'variable').toLowerCase()}`;
            return `
                <div class="side-event ${typeClass}">
                    <strong>${dateLabel} ${exp.expense_title}</strong>
                    <span>${Number(exp.amount).toLocaleString()}원 · ${exp.category_name}</span>
                </div>
            `;
        }).join('');
    }

    listEl.innerHTML = `<div id="detailListItems">${itemsHtml}</div>`;

    renderDetailListPager(totalPages);
}

function renderDetailListPager(totalPages) {
    let pagerEl = document.getElementById('detailListPager');

    if (!pagerEl) {
        pagerEl = document.createElement('div');
        pagerEl.id = 'detailListPager';
        pagerEl.className = 'side-pager';
        document.getElementById('calendarDetailList').appendChild(pagerEl);
    }

    if (totalPages <= 1) {
        pagerEl.innerHTML = '';
        return;
    }

    let buttonsHtml = '';
    for (let page = 1; page <= totalPages; page++) {
        const isActive = page === detailListPage ? 'active' : '';
        buttonsHtml += `<button type="button" class="pager-num-btn ${isActive}" data-page="${page}">${page}</button>`;
    }
    pagerEl.innerHTML = buttonsHtml;

    pagerEl.querySelectorAll('.pager-num-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            detailListPage = Number(btn.dataset.page);
            renderDetailList();
        });
    });
}
/* =========================================================
   오늘 할 일 - "오늘 날짜에 잡혀있는 지출"만 따로 보여줌
   (달력을 이전달/다음달로 넘겨도 이건 안 바뀜)
   ========================================================= */

   function loadTodayTodo() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + 1;
    const todayStr = `${year}-${String(month).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    /* [AJAX] GET /calendar/expenses.do?year=&month=
       - loadMonthlyExpenses와 같은 엔드포인트를 오늘이 속한 연/월로 다시 호출함
         (별도의 "오늘만 조회" API가 없어서, 이번 달 전체를 받은 뒤 클라이언트에서
          expense_date === 오늘 날짜인 것만 필터링함)
       - 달력 렌더링과 무관하게 페이지 로드 시 한 번만 실행되는 독립적인 요청 */
    fetch(`/spendolive/calendar/expenses.do?year=${year}&month=${month}`, {
        credentials: 'same-origin'
    })
        .then(res => {
            if (!res.ok) {
                throw new Error('오늘 할 일을 불러오지 못했습니다.');
            }
            return res.json();
        })
        .then(data => {
            todayTodoItems = data.filter(exp => exp.expense_date === todayStr);
            todayTodoPage = 1;
            renderTodayTodo();
        })
        .catch(err => {
            console.error(err);
        });
}

function renderTodayTodo() {
    const listEl = document.getElementById('todayTodoList');
    if (!listEl) {
        console.warn('todayTodoList 요소를 못 찾았어요 (calendar.jsp 확인 필요)');
        return;
    }

    // 이번달 주요 지출과 동일하게 3개씩 잘라서 현재 페이지만 표시
    const totalPages = Math.max(1, Math.ceil(todayTodoItems.length / SIDE_PAGE_SIZE));
    if (todayTodoPage > totalPages) {
        todayTodoPage = totalPages;
    }

    const startIdx = (todayTodoPage - 1) * SIDE_PAGE_SIZE;
    const pageItems = todayTodoItems.slice(startIdx, startIdx + SIDE_PAGE_SIZE);

    listEl.innerHTML = '';

    if (pageItems.length === 0) {
        listEl.innerHTML = isLoggedIn
            ? '<p class="empty-text">오늘 예정된 지출이 없습니다.</p>'
            : '<p class="empty-text">로그인 시 <br>오늘 예정된 지출을 확인할 수 있어요.</p>';
        renderTodayTodoPager(totalPages);
        return;
    }

    pageItems.forEach(exp => {
        const typeClass = `type-${(exp.expense_type || 'variable').toLowerCase()}`;

        const item = document.createElement('div');
        item.className = `side-event ${typeClass}`;
        item.innerHTML = `
            <strong>${exp.expense_title}</strong>
            <span>${Number(exp.amount).toLocaleString()}원 · ${exp.category_name}</span>
        `;
        listEl.appendChild(item);
    });

    renderTodayTodoPager(totalPages);
}

function renderTodayTodoPager(totalPages) {
    let pagerEl = document.getElementById('todayTodoPager');

    if (!pagerEl) {
        pagerEl = document.createElement('div');
        pagerEl.id = 'todayTodoPager';
        pagerEl.className = 'side-pager';   // 주요 지출 pager와 같은 스타일 재사용
        document.getElementById('todayTodoList').insertAdjacentElement('afterend', pagerEl);
    }

    // 페이지가 1개뿐이면 버튼 숨김
    if (totalPages <= 1) {
        pagerEl.innerHTML = '';
        return;
    }

    let buttonsHtml = '';
    for (let page = 1; page <= totalPages; page++) {
        const isActive = page === todayTodoPage ? 'active' : '';
        buttonsHtml += `<button type="button" class="pager-num-btn ${isActive}" data-page="${page}">${page}</button>`;
    }
    pagerEl.innerHTML = buttonsHtml;

    pagerEl.querySelectorAll('.pager-num-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            todayTodoPage = Number(btn.dataset.page);
            renderTodayTodo();
        });
    });
}