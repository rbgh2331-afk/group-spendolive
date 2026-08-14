/* [AJAX 변경 주석]
 * 기존 지출 화면 동작은 유지하고 AJAX 재렌더링 후 필요한 이벤트와 차트를 다시 초기화한다.
 * 기존 Controller/Service URL과 파라미터는 특별한 문제가 없는 한 그대로 유지
 */
function initExpensePage() {
    const expenseTypeSelect = document.getElementById('expense_type');
    const categorySelect = document.getElementById('category_id');
    const repeatCycleArea = document.getElementById('repeatCycleArea');
    const repeatCycleSelect = document.getElementById('repeat_cycle');
    const repeatYnInput = document.getElementById('repeat_yn');
    const fixedYnInput = document.getElementById('fixed_yn');
    // 등록 화면의 종료월 영역과 시작일을 함께 제어한다
    const repeatEndMonthArea = document.getElementById('repeatEndMonthArea');
    const repeatEndMonthInput = document.getElementById('repeat_end_month');
    const expenseDateInput = document.querySelector('#expense-form input[name="expense_date"]');

    const categoryMasterList = categorySelect
        ? Array.from(categorySelect.querySelectorAll('option[data-type]')).map(option => ({
            value: option.value,
            type: (option.dataset.type || '').trim(),
            text: option.textContent.trim()
        }))
        : [];

    function isRepeatTargetType(type) {
        return type === 'FIXED' || type === 'OTT';
    }

    function makeCategoryOption(category) {
        const option = document.createElement('option');
        option.value = category.value;
        option.dataset.type = category.type;
        option.textContent = category.text;
        return option;
    }

    function renderCategoryOptions(select, selectedType, selectedValue, placeholderText) {
        if (!select) {
            return;
        }

        select.innerHTML = '';

        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = placeholderText || '카테고리 선택';
        select.appendChild(placeholder);

        if (!selectedType) {
            select.value = '';
            return;
        }

        const filteredList = categoryMasterList.filter(category => category.type === selectedType);

        if (filteredList.length === 0) {
            placeholder.textContent = '해당 분류의 카테고리가 없습니다';
            select.value = '';
            return;
        }

        filteredList.forEach(category => {
            select.appendChild(makeCategoryOption(category));
        });

        const hasSelectedValue = filteredList.some(category => category.value === String(selectedValue || ''));

        if (hasSelectedValue) {
            select.value = selectedValue;
        } else {
            select.value = '';
        }
    }

    // 고정 분류에서만 종료월 입력을 노출하고 시작월 이전 선택을 막는다
    function syncRepeatEndMonthArea() {
        const fixedExpense = expenseTypeSelect && expenseTypeSelect.value === 'FIXED';

        if (repeatEndMonthArea) {
            repeatEndMonthArea.classList.toggle('expense-hidden', !fixedExpense);
        }

        if (!fixedExpense && repeatEndMonthInput) {
            repeatEndMonthInput.value = '';
        }

        syncRepeatEndMonthMin();
    }

    function syncRepeatEndMonthMin() {
        if (!repeatEndMonthInput) {
            return;
        }

        repeatEndMonthInput.min = expenseDateInput && expenseDateInput.value
            ? expenseDateInput.value.substring(0, 7)
            : '';
    }

    function filterCategories() {
        if (!expenseTypeSelect || !categorySelect) {
            return;
        }

        const selectedType = expenseTypeSelect.value;

        if (!selectedType) {
            renderCategoryOptions(categorySelect, '', '', '먼저 분류를 선택하세요');
        } else {
            renderCategoryOptions(categorySelect, selectedType, categorySelect.value, '카테고리 선택');
        }

        if (isRepeatTargetType(selectedType)) {
            if (repeatCycleArea) {
                repeatCycleArea.classList.remove('expense-hidden');
            }

            if (fixedYnInput) {
                fixedYnInput.value = 'Y';
            }
        } else {
            if (repeatCycleArea) {
                repeatCycleArea.classList.add('expense-hidden');
            }

            if (repeatCycleSelect) {
                repeatCycleSelect.value = '';
            }

            if (repeatYnInput) {
                repeatYnInput.value = 'N';
            }

            if (fixedYnInput) {
                fixedYnInput.value = 'N';
            }
        }

        syncRepeatEndMonthArea();
        changeRepeatYn();
    }

    function changeRepeatYn() {
        if (!expenseTypeSelect) {
            return;
        }

        if (isRepeatTargetType(expenseTypeSelect.value) && repeatCycleSelect && repeatCycleSelect.value !== '') {
            if (repeatYnInput) {
                repeatYnInput.value = 'Y';
            }

            if (fixedYnInput) {
                fixedYnInput.value = 'Y';
            }
        } else if (isRepeatTargetType(expenseTypeSelect.value)) {
            if (repeatYnInput) {
                repeatYnInput.value = 'N';
            }

            if (fixedYnInput) {
                fixedYnInput.value = 'Y';
            }
        } else {
            if (repeatYnInput) {
                repeatYnInput.value = 'N';
            }

            if (fixedYnInput) {
                fixedYnInput.value = 'N';
            }
        }
    }

    function changeEditMode(button) {
        const row = button.closest('tr');

        if (!row) {
            console.error('수정할 행을 찾을 수 없습니다.');
            return;
        }

        row.querySelectorAll('.view-mode').forEach(element => {
            element.classList.add('expense-hidden');
        });

        row.querySelectorAll('.edit-mode').forEach(element => {
            element.classList.remove('expense-hidden');
        });

        const editButton = row.querySelector('.edit-btn');
        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        const deleteButton = row.querySelector('.delete-btn');

        if (editButton) {
            editButton.classList.add('expense-hidden');
        }

        if (saveButton) {
            saveButton.classList.remove('expense-hidden');
        }

        if (cancelButton) {
            cancelButton.classList.remove('expense-hidden');
        }

        if (deleteButton) {
            deleteButton.classList.add('expense-hidden');
        }

        filterEditCategoriesByRow(row);
        changeEditRepeatYnByRow(row);
    }

    function cancelEditMode(button) {
        const row = button.closest('tr');

        if (!row) {
            return;
        }

        row.querySelectorAll('.view-mode').forEach(element => {
            element.classList.remove('expense-hidden');
        });

        row.querySelectorAll('.edit-mode').forEach(element => {
            element.classList.add('expense-hidden');
        });

        const editButton = row.querySelector('.edit-btn');
        const saveButton = row.querySelector('.save-btn');
        const cancelButton = row.querySelector('.cancel-btn');
        const deleteButton = row.querySelector('.delete-btn');

        if (editButton) {
            editButton.classList.remove('expense-hidden');
        }

        if (saveButton) {
            saveButton.classList.add('expense-hidden');
        }

        if (cancelButton) {
            cancelButton.classList.add('expense-hidden');
        }

        if (deleteButton) {
            deleteButton.classList.remove('expense-hidden');
        }
    }

    function filterEditCategoriesFromSelect(select) {
        const row = select.closest('tr');

        if (!row) {
            return;
        }

        filterEditCategoriesByRow(row);
        changeEditRepeatYnByRow(row);
    }

    function changeEditRepeatYnFromSelect(select) {
        const row = select.closest('tr');

        if (!row) {
            return;
        }

        changeEditRepeatYnByRow(row);
    }

    function filterEditCategoriesByRow(row) {
        const typeSelect = row.querySelector('.edit-expense-type');
        const editCategorySelect = row.querySelector('.edit-category');

        if (!typeSelect || !editCategorySelect) {
            return;
        }

        renderCategoryOptions(
            editCategorySelect,
            typeSelect.value,
            editCategorySelect.value,
            '카테고리 선택'
        );
    }

    function changeEditRepeatYnByRow(row) {
        // 자동 반복 행의 화면 키와 실제 DB 지출 ID를 분리한다
        const expense_id = row.dataset.sourceExpenseId;
        const typeSelect = row.querySelector('.edit-expense-type');
        const editRepeatCycleSelect = row.querySelector('.edit-repeat-cycle');
        const editRepeatYnInput = document.getElementById(`editRepeatYn${expense_id}`);
        const editFixedYnInput = document.getElementById(`editFixedYn${expense_id}`);
        // 수정 행의 종료월 입력과 시작일을 함께 제어한다
        const editRepeatEndArea = row.querySelector('.edit-repeat-end-area');
        const editRepeatEndInput = row.querySelector('.edit-repeat-end-month');
        const editExpenseDateInput = row.querySelector('.edit-expense-date');

        if (!typeSelect || !editRepeatCycleSelect || !editRepeatYnInput || !editFixedYnInput) {
            return;
        }

        if (isRepeatTargetType(typeSelect.value)) {
            editFixedYnInput.value = 'Y';

            if (editRepeatCycleSelect.value !== '') {
                editRepeatYnInput.value = 'Y';
            } else {
                editRepeatYnInput.value = 'N';
            }
        } else {
            editFixedYnInput.value = 'N';
            editRepeatYnInput.value = 'N';
            editRepeatCycleSelect.value = '';
        }

        const fixedExpense = typeSelect.value === 'FIXED';
        if (editRepeatEndArea) {
            editRepeatEndArea.classList.toggle('expense-hidden', !fixedExpense);
        }
        if (!fixedExpense && editRepeatEndInput) {
            editRepeatEndInput.value = '';
        }
        if (editRepeatEndInput) {
            editRepeatEndInput.min = editExpenseDateInput && editExpenseDateInput.value
                ? editExpenseDateInput.value.substring(0, 7)
                : '';
        }
    }

    // 수정 중 시작일이 바뀌면 종료월의 최소값도 즉시 갱신
    function syncEditRepeatEndMonthMin(input) {
        const row = input.closest('tr');
        if (row) {
            changeEditRepeatYnByRow(row);
        }
    }

    window.changeEditMode = changeEditMode;
    window.cancelEditMode = cancelEditMode;
    window.filterEditCategoriesFromSelect = filterEditCategoriesFromSelect;
    window.changeEditRepeatYnFromSelect = changeEditRepeatYnFromSelect;
    window.syncEditRepeatEndMonthMin = syncEditRepeatEndMonthMin;

    if (expenseTypeSelect) {
        expenseTypeSelect.addEventListener('change', filterCategories);
    }

    if (repeatCycleSelect) {
        repeatCycleSelect.addEventListener('change', changeRepeatYn);
    }

    if (expenseDateInput) {
        expenseDateInput.addEventListener('change', syncRepeatEndMonthMin);
    }

    filterCategories();

const typeFilter = document.getElementById('expenseTypeFilter');
const categoryFilter = document.getElementById('expenseCategoryFilter');
const amountSort = document.getElementById('expenseAmountSort');
const expenseRows = Array.from(document.querySelectorAll('.expense-row'));
const expenseTbody = document.getElementById('expenseRows');
// 월별 전체 데이터는 유지하고 화면에서만 10개씩 나누어 표시
const EXPENSE_PAGE_SIZE = 10;
const expensePagination = document.getElementById('expensePagination');
const expensePageNumbers = document.getElementById('expensePageNumbers');
const expensePrevButton = expensePagination
    ? expensePagination.querySelector('[data-expense-page-direction="prev"]')
    : null;
const expenseNextButton = expensePagination
    ? expensePagination.querySelector('[data-expense-page-direction="next"]')
    : null;
let expenseCurrentPage = 1;
const categoryFilterMasterList = categoryFilter
    ? Array.from(categoryFilter.querySelectorAll('option[data-type]')).map(option => ({
        value: option.value,
        type: (option.dataset.type || '').trim(),
        text: option.textContent.trim()
    }))
    : [];

// 목록의 분류를 선택하면 해당 분류에 속한 카테고리만 필터 선택창에 표시
function syncExpenseCategoryFilter() {
    if (!typeFilter || !categoryFilter) {
        return;
    }

    const selectedType = typeFilter.value;
    const previousCategory = categoryFilter.value;
    const visibleCategories = selectedType
        ? categoryFilterMasterList.filter(category => category.type === selectedType)
        : categoryFilterMasterList;

    categoryFilter.innerHTML = '';

    const allOption = document.createElement('option');
    allOption.value = '';
    allOption.textContent = selectedType ? '해당 분류 전체 카테고리' : '전체 카테고리';
    categoryFilter.appendChild(allOption);

    visibleCategories.forEach(function (category) {
        const option = document.createElement('option');
        option.value = category.value;
        option.dataset.type = category.type;
        option.textContent = category.text;
        categoryFilter.appendChild(option);
    });

    const canKeepPreviousCategory = visibleCategories.some(
        category => category.value === previousCategory
    );
    categoryFilter.value = canKeepPreviousCategory ? previousCategory : '';
}

// 페이지가 많아져도 버튼이 지나치게 길어지지 않도록 표시할 번호를 계산
function makeExpensePageItems(currentPage, totalPages) {
    if (totalPages <= 7) {
        return Array.from({length: totalPages}, (_, index) => index + 1);
    }

    const pageItems = [1];
    const pageStart = Math.max(2, currentPage - 1);
    const pageEnd = Math.min(totalPages - 1, currentPage + 1);

    if (pageStart > 2) {
        pageItems.push('ellipsis-start');
    }

    for (let page = pageStart; page <= pageEnd; page += 1) {
        pageItems.push(page);
    }

    if (pageEnd < totalPages - 1) {
        pageItems.push('ellipsis-end');
    }

    pageItems.push(totalPages);
    return pageItems;
}

// 현재 필터 결과에 맞춰 이전·숫자·다음 버튼을 다시 그린다
function renderExpensePagination(totalItemCount) {
    if (!expensePagination || !expensePageNumbers || !expensePrevButton || !expenseNextButton) {
        return;
    }

    const totalPages = Math.ceil(totalItemCount / EXPENSE_PAGE_SIZE);
    const showPagination = totalPages > 1;
    expensePagination.classList.toggle('expense-hidden', !showPagination);
    expensePageNumbers.replaceChildren();

    if (!showPagination) {
        expensePrevButton.disabled = true;
        expenseNextButton.disabled = true;
        return;
    }

    expensePrevButton.disabled = expenseCurrentPage === 1;
    expenseNextButton.disabled = expenseCurrentPage === totalPages;

    makeExpensePageItems(expenseCurrentPage, totalPages).forEach(pageItem => {
        if (typeof pageItem !== 'number') {
            const ellipsis = document.createElement('span');
            ellipsis.className = 'pg-ellipsis';
            ellipsis.textContent = '…';
            expensePageNumbers.appendChild(ellipsis);
            return;
        }

        const pageButton = document.createElement('button');
        pageButton.type = 'button';
        pageButton.className = `pg-btn${pageItem === expenseCurrentPage ? ' active' : ''}`;
        pageButton.dataset.expensePage = String(pageItem);
        pageButton.textContent = String(pageItem);
        pageButton.disabled = pageItem === expenseCurrentPage;
        pageButton.setAttribute('aria-label', `${pageItem}페이지`);
        expensePageNumbers.appendChild(pageButton);
    });
}

// 필터·정렬을 먼저 적용한 뒤 현재 페이지에 해당하는 행만 노출한다
function filterExpenseRows(resetPage) {
    if (!typeFilter || !categoryFilter || !amountSort || !expenseTbody) {
        return;
    }

    const selectedType = typeFilter.value;
    const selectedCategory = categoryFilter.value;
    const selectedSort = amountSort.value;

    const sortedRows = [...expenseRows].sort((a, b) => {
        if (selectedSort === 'DESC') {
            return Number(b.dataset.amount) - Number(a.dataset.amount);
        }

        if (selectedSort === 'ASC') {
            return Number(a.dataset.amount) - Number(b.dataset.amount);
        }

        return Number(a.dataset.order) - Number(b.dataset.order);
    });

    const filteredRows = sortedRows.filter(row => {
        const typeMatch = !selectedType || row.dataset.type === selectedType;
        const categoryMatch = !selectedCategory || row.dataset.category === selectedCategory;
        return typeMatch && categoryMatch;
    });

    if (resetPage) {
        expenseCurrentPage = 1;
    }

    const totalPages = Math.max(1, Math.ceil(filteredRows.length / EXPENSE_PAGE_SIZE));
    expenseCurrentPage = Math.min(Math.max(expenseCurrentPage, 1), totalPages);

    sortedRows.forEach(row => {
        row.classList.add('expense-hidden');
        expenseTbody.appendChild(row);
    });

    const pageStart = (expenseCurrentPage - 1) * EXPENSE_PAGE_SIZE;
    const pageRows = filteredRows.slice(pageStart, pageStart + EXPENSE_PAGE_SIZE);
    pageRows.forEach(row => row.classList.remove('expense-hidden'));

    const emptyMessage = document.getElementById('expenseFilterEmpty');
    if (emptyMessage) {
        // 월별 원본 지출이 없는 경우에는 기존 '등록된 내역 없음' 행만 표시
        const showFilterEmpty = expenseRows.length > 0 && filteredRows.length === 0;
        emptyMessage.classList.toggle('expense-hidden', !showFilterEmpty);
    }

    renderExpensePagination(filteredRows.length);
}

if (typeFilter) {
    typeFilter.addEventListener('change', function () {
        syncExpenseCategoryFilter();
        filterExpenseRows(true);
    });
}

[categoryFilter, amountSort].forEach(filter => {
    if (filter) {
        filter.addEventListener('change', function () {
            filterExpenseRows(true);
        });
    }
});

if (expensePageNumbers) {
    expensePageNumbers.addEventListener('click', function (event) {
        const pageButton = event.target.closest('[data-expense-page]');
        if (!pageButton) {
            return;
        }

        expenseCurrentPage = Number(pageButton.dataset.expensePage);
        filterExpenseRows(false);
    });
}

if (expensePrevButton) {
    expensePrevButton.addEventListener('click', function () {
        if (expenseCurrentPage > 1) {
            expenseCurrentPage -= 1;
            filterExpenseRows(false);
        }
    });
}

if (expenseNextButton) {
    expenseNextButton.addEventListener('click', function () {
        expenseCurrentPage += 1;
        filterExpenseRows(false);
    });
}

// 최초 진입 시에도 분류에 맞는 카테고리와 첫 페이지를 표시
if (typeFilter && categoryFilter && amountSort && expenseTbody) {
    syncExpenseCategoryFilter();
    filterExpenseRows(true);
}

// 지출관리 하단에서 상품 검색과 최근 조사 가격 조회를 AJAX로 처리
const consumerPriceSection = document.getElementById('consumer-price-compare');

if (consumerPriceSection && consumerPriceSection.dataset.initialized !== 'true') {
    consumerPriceSection.dataset.initialized = 'true';

    const contextPath = consumerPriceSection.dataset.contextPath || '';
    const searchForm = document.getElementById('consumerPriceSearchForm');
    const keywordInput = document.getElementById('consumerProductKeyword');
    const searchButton = document.getElementById('consumerProductSearchButton');
    const messageElement = document.getElementById('consumerPriceMessage');
    const productResults = document.getElementById('consumerProductResults');
    const priceResults = document.getElementById('consumerPriceResults');
    const selectedProduct = document.getElementById('consumerSelectedProduct');
    const inspectDay = document.getElementById('consumerInspectDay');
    const lowestPrice = document.getElementById('consumerLowestPrice');
    const averagePrice = document.getElementById('consumerAveragePrice');
    const highestPrice = document.getElementById('consumerHighestPrice');
    const storePriceRows = document.getElementById('consumerStorePriceRows');

    // 인증키 활성화 전 결과 화면 확인을 위한 시연용 상품 목록이다
    const consumerPriceDemoProducts = [
        {goodId: 'DEMO-MILK-001', goodName: '서울우유 나100% 1L', goodTotalCnt: '1000', goodTotalDivCode: 'mL', demo: true},
        {goodId: 'DEMO-MILK-002', goodName: '매일우유 오리지널 900mL', goodTotalCnt: '900', goodTotalDivCode: 'mL', demo: true},
        {goodId: 'DEMO-NOODLE-001', goodName: '신라면 5개입', goodTotalCnt: '5', goodTotalDivCode: '개', demo: true},
        {goodId: 'DEMO-EGG-001', goodName: '신선한 계란 30구', goodTotalCnt: '30', goodTotalDivCode: '개', demo: true}
    ];

    // 상품별 시연용 가격 비교 결과다. DB에는 저장하지 않는다
    const consumerPriceDemoComparisons = {
        'DEMO-MILK-001': {
            goodId: 'DEMO-MILK-001', goodName: '서울우유 나100% 1L', inspectDay: '2026-07-31', lowestPrice: 2580, averagePrice: 2830, highestPrice: 3200,
            stores: [
                {entpId: 'DEMO-STORE-01', storeName: '하나로마트 둔산점', roadAddress: '대전광역시 서구 둔산중로 00', plusOneYn: 'N', discountYn: 'Y', price: 2580},
                {entpId: 'DEMO-STORE-02', storeName: '그린마트 탄방점', roadAddress: '대전광역시 서구 탄방로 00', plusOneYn: 'N', discountYn: 'N', price: 2710},
                {entpId: 'DEMO-STORE-03', storeName: '우리마트 유성점', roadAddress: '대전광역시 유성구 대학로 00', plusOneYn: 'Y', discountYn: 'N', price: 2900},
                {entpId: 'DEMO-STORE-04', storeName: '행복마트 노은점', roadAddress: '대전광역시 유성구 노은로 00', plusOneYn: 'N', discountYn: 'N', price: 3130}
            ]
        },
        'DEMO-MILK-002': {
            goodId: 'DEMO-MILK-002', goodName: '매일우유 오리지널 900mL', inspectDay: '2026-07-31', lowestPrice: 2380, averagePrice: 2615, highestPrice: 2890,
            stores: [
                {entpId: 'DEMO-STORE-01', storeName: '하나로마트 둔산점', roadAddress: '대전광역시 서구 둔산중로 00', plusOneYn: 'N', discountYn: 'Y', price: 2380},
                {entpId: 'DEMO-STORE-02', storeName: '그린마트 탄방점', roadAddress: '대전광역시 서구 탄방로 00', plusOneYn: 'N', discountYn: 'N', price: 2540},
                {entpId: 'DEMO-STORE-03', storeName: '우리마트 유성점', roadAddress: '대전광역시 유성구 대학로 00', plusOneYn: 'N', discountYn: 'N', price: 2650},
                {entpId: 'DEMO-STORE-04', storeName: '행복마트 노은점', roadAddress: '대전광역시 유성구 노은로 00', plusOneYn: 'N', discountYn: 'N', price: 2890}
            ]
        },
        'DEMO-NOODLE-001': {
            goodId: 'DEMO-NOODLE-001', goodName: '신라면 5개입', inspectDay: '2026-07-31', lowestPrice: 3650, averagePrice: 3975, highestPrice: 4290,
            stores: [
                {entpId: 'DEMO-STORE-01', storeName: '하나로마트 둔산점', roadAddress: '대전광역시 서구 둔산중로 00', plusOneYn: 'N', discountYn: 'Y', price: 3650},
                {entpId: 'DEMO-STORE-02', storeName: '그린마트 탄방점', roadAddress: '대전광역시 서구 탄방로 00', plusOneYn: 'N', discountYn: 'N', price: 3890},
                {entpId: 'DEMO-STORE-03', storeName: '우리마트 유성점', roadAddress: '대전광역시 유성구 대학로 00', plusOneYn: 'N', discountYn: 'N', price: 4070},
                {entpId: 'DEMO-STORE-04', storeName: '행복마트 노은점', roadAddress: '대전광역시 유성구 노은로 00', plusOneYn: 'N', discountYn: 'N', price: 4290}
            ]
        },
        'DEMO-EGG-001': {
            goodId: 'DEMO-EGG-001', goodName: '신선한 계란 30구', inspectDay: '2026-07-31', lowestPrice: 6990, averagePrice: 7645, highestPrice: 8290,
            stores: [
                {entpId: 'DEMO-STORE-01', storeName: '하나로마트 둔산점', roadAddress: '대전광역시 서구 둔산중로 00', plusOneYn: 'N', discountYn: 'Y', price: 6990},
                {entpId: 'DEMO-STORE-02', storeName: '그린마트 탄방점', roadAddress: '대전광역시 서구 탄방로 00', plusOneYn: 'N', discountYn: 'N', price: 7490},
                {entpId: 'DEMO-STORE-03', storeName: '우리마트 유성점', roadAddress: '대전광역시 유성구 대학로 00', plusOneYn: 'N', discountYn: 'N', price: 7810},
                {entpId: 'DEMO-STORE-04', storeName: '행복마트 노은점', roadAddress: '대전광역시 유성구 노은로 00', plusOneYn: 'N', discountYn: 'N', price: 8290}
            ]
        }
    };

    function formatWon(value) { return `${Number(value || 0).toLocaleString('ko-KR')}원`; }

    function setConsumerPriceMessage(message, state) {
        messageElement.textContent = message;
        messageElement.classList.toggle('is-error', state === 'error');
        messageElement.classList.toggle('is-loading', state === 'loading');
    }

    function setSearchBusy(busy) {
        searchButton.disabled = busy;
        searchButton.textContent = busy ? '조회 중' : '조회';
    }

    function clearPriceResult() {
        priceResults.classList.add('expense-hidden');
        storePriceRows.replaceChildren();
    }

    // 공통 AJAX 응답 형식과 로그인 만료 응답을 함께 처리
    async function requestConsumerPrice(url) {
        const response = await fetch(url, {method: 'GET', credentials: 'same-origin', headers: {'Accept': 'application/json'}});
        const payload = await response.json().catch(function () { return null; });

        if (response.status === 401 && payload && payload.redirectUrl) {
            window.location.href = contextPath + payload.redirectUrl;
            throw new Error('로그인이 필요합니다.');
        }

        if (!response.ok || !payload || payload.success !== true) {
            throw new Error(payload && payload.message ? payload.message : '공공데이터를 불러오지 못했습니다.');
        }

        return payload;
    }

    function makeProductButton(product) {
        const button = document.createElement('button');
        const name = document.createElement('strong');
        const detail = document.createElement('small');
        const volume = [product.goodTotalCnt, product.goodTotalDivCode].filter(Boolean).join('');

        button.type = 'button';
        button.className = 'consumer-product-button';
        name.textContent = product.goodName || '상품명 없음';
        detail.textContent = volume ? `상품번호 ${product.goodId} · ${volume}` : `상품번호 ${product.goodId}`;
        button.append(name, detail);
        button.addEventListener('click', function () { loadConsumerPrices(product); });
        return button;
    }

    function renderProductResults(products) {
        productResults.replaceChildren();
        clearPriceResult();

        if (!Array.isArray(products) || products.length === 0) {
            productResults.classList.add('expense-hidden');
            setConsumerPriceMessage('검색된 상품이 없습니다. 다른 상품명을 입력해주세요.', 'error');
            return;
        }

        const title = document.createElement('p');
        const list = document.createElement('div');
        title.className = 'consumer-product-result-title';
        title.textContent = `검색 결과 ${products.length}개 · 가격을 확인할 상품을 선택해주세요.`;
        list.className = 'consumer-product-list';
        products.forEach(function (product) { list.appendChild(makeProductButton(product)); });
        productResults.append(title, list);
        productResults.classList.remove('expense-hidden');
        setConsumerPriceMessage('상품을 선택하면 최근 조사일의 판매점별 가격을 조회합니다.', '');
    }

    // 검색어와 일치하는 시연용 상품을 찾는다
    function findConsumerPriceDemoProducts(keyword) {
        const normalizedKeyword = String(keyword || '').replace(/\s/g, '').toLowerCase();
        const matchedProducts = consumerPriceDemoProducts.filter(function (product) {
            return product.goodName.replace(/\s/g, '').toLowerCase().includes(normalizedKeyword);
        });

        return matchedProducts.length > 0 ? matchedProducts : consumerPriceDemoProducts.slice(0, 3);
    }

    // 외부 API 오류 시 DB 없이 브라우저 데이터만으로 결과 화면을 표시
    function renderConsumerPriceDemo(keyword) {
        const demoProducts = findConsumerPriceDemoProducts(keyword);
        const firstProduct = demoProducts[0];
        renderProductResults(demoProducts);

        if (firstProduct && consumerPriceDemoComparisons[firstProduct.goodId]) {
            renderPriceComparison(consumerPriceDemoComparisons[firstProduct.goodId]);
        }

        setConsumerPriceMessage('한국소비자원 API 인증 대기 중 · 현재 시연용 예시 데이터를 표시합니다.', '');
    }

    function renderStoreRows(stores) {
        storePriceRows.replaceChildren();

        stores.forEach(function (store) {
            const row = document.createElement('tr');
            const storeCell = document.createElement('td');
            const addressCell = document.createElement('td');
            const eventCell = document.createElement('td');
            const priceCell = document.createElement('td');
            const eventLabels = [];

            if (store.plusOneYn === 'Y') eventLabels.push('1+1');
            if (store.discountYn === 'Y') eventLabels.push('할인');
            storeCell.textContent = store.storeName || `판매점 ${store.entpId}`;
            addressCell.textContent = store.roadAddress || '-';
            eventCell.textContent = eventLabels.length > 0 ? eventLabels.join(' · ') : '-';
            priceCell.textContent = formatWon(store.price);
            priceCell.className = 'consumer-store-price';
            row.append(storeCell, addressCell, eventCell, priceCell);
            storePriceRows.appendChild(row);
        });
    }

    function renderPriceComparison(comparison) {
        const stores = Array.isArray(comparison.stores) ? comparison.stores : [];
        selectedProduct.textContent = comparison.goodName || '선택 상품';
        inspectDay.textContent = `${comparison.inspectDay} 조사 기준`;
        lowestPrice.textContent = formatWon(comparison.lowestPrice);
        averagePrice.textContent = formatWon(comparison.averagePrice);
        highestPrice.textContent = formatWon(comparison.highestPrice);
        renderStoreRows(stores);
        priceResults.classList.remove('expense-hidden');
        setConsumerPriceMessage(`판매점 ${stores.length}곳의 가격을 비교했습니다.`, '');
        priceResults.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }

    // 선택 상품의 goodId로 최근 금요일 가격을 서버에 요청
    async function loadConsumerPrices(product) {
        clearPriceResult();

        // 시연용 상품은 외부 API를 호출하지 않고 즉시 화면에 표시
        if (product.demo === true && consumerPriceDemoComparisons[product.goodId]) {
            renderPriceComparison(consumerPriceDemoComparisons[product.goodId]);
            setConsumerPriceMessage('한국소비자원 API 인증 대기 중 · 현재 시연용 예시 데이터를 표시합니다.', '');
            return;
        }

        setConsumerPriceMessage(`${product.goodName} 가격을 불러오고 있습니다.`, 'loading');

        try {
            const query = new URLSearchParams({goodId: product.goodId, goodName: product.goodName});
            const payload = await requestConsumerPrice(`${contextPath}/spendolive/publicdata/consumer-price/prices.do?${query.toString()}`);
            renderPriceComparison(payload.data);
        } catch (error) {
            setConsumerPriceMessage(error.message, 'error');
        }
    }

    searchForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        const keyword = keywordInput.value.trim();

        if (!keyword) {
            setConsumerPriceMessage('검색할 상품명을 입력해주세요.', 'error');
            keywordInput.focus();
            return;
        }

        productResults.classList.add('expense-hidden');
        productResults.replaceChildren();
        clearPriceResult();
        setSearchBusy(true);
        setConsumerPriceMessage(`${keyword} 상품을 검색하고 있습니다.`, 'loading');

        try {
            const query = new URLSearchParams({keyword: keyword});
            const payload = await requestConsumerPrice(`${contextPath}/spendolive/publicdata/consumer-price/products.do?${query.toString()}`);
            renderProductResults(payload.data);
        } catch (error) {
            // 인증키 동기화 전에도 결과 화면을 확인할 수 있도록 자동 전환한다
            console.warn('[생필품 가격 비교] 실제 API 호출 실패로 시연용 데이터를 표시합니다.', error);
            renderConsumerPriceDemo(keyword);
        } finally {
            setSearchBusy(false);
        }
    });
}
}

window.initExpensePage = initExpensePage;
initExpensePage();
