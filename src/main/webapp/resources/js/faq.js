/* ============================================================
   FAQ / 문의하기 공통 스크립트
   경로: resources/js/faq.js
   대상 페이지: faqList.jsp, inquiryWrite.jsp, inquiryList.jsp
   ============================================================ */

/* ---- faqList.jsp : 아코디언 토글 ---- */
function toggleFaq(el) {
    const isOpen = el.classList.contains('open');
    document.querySelectorAll('.faq-item.open').forEach(i => i.classList.remove('open'));
    if (!isOpen) el.classList.add('open');
}

/* ---- faqList.jsp : 카테고리 필터 ---- */
function filterFaqCat(btn, cat) {
    // 카테고리를 누르면 검색어/검색 결과 숨김 상태를 초기화하고 카테고리 기준으로만 다시 보여준다
    if (document.querySelectorAll('.faq-list').length === 0) return;
    const searchInput = document.getElementById('faqSearchInput');
    if (searchInput) searchInput.value = '';
    document.querySelectorAll('.faq-item').forEach(item => { item.style.display = ''; });

    document.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    let totalVisible = 0;
    document.querySelectorAll('.faq-list').forEach(list => {
        const matches = (cat === 'all') || (list.dataset.cat === cat);
        list.style.display = matches ? '' : 'none';

        const label = list.previousElementSibling;
        if (label && label.classList.contains('section-label')) {
            label.style.display = matches ? '' : 'none';
        }
        if (matches) {
            totalVisible += list.querySelectorAll('.faq-item').length;
        }
    });

    // 해당 카테고리에 등록된 FAQ가 하나도 없으면 빈 공간 대신 안내 문구로 채운다
    // (내용이 사라지면서 화면 높이가 확 줄어드는 것 방지)
    const emptyBox = document.getElementById('faqSearchEmpty');
    const emptyIcon = document.getElementById('faqSearchEmptyIcon');
    const emptyText = document.getElementById('faqSearchEmptyText');
    if (emptyBox) {
        emptyBox.style.display = totalVisible === 0 ? '' : 'none';
        if (totalVisible === 0) {
            if (emptyIcon) emptyIcon.textContent = '📭';
            if (emptyText) emptyText.textContent = '아직 등록된 FAQ가 없습니다.';
        }
    }
}

/* ---- faqList.jsp : 질문/답변 텍스트 검색 (전체 카테고리 대상) ---- */
function searchFaq() {
    const input = document.getElementById('faqSearchInput');
    const query = input ? input.value.trim().toLowerCase() : '';
    const emptyBox = document.getElementById('faqSearchEmpty');
    const emptyIcon = document.getElementById('faqSearchEmptyIcon');
    const emptyText = document.getElementById('faqSearchEmptyText');

    // 검색은 카테고리 구분 없이 전체에서 찾는 게 자연스러우므로 카테고리 버튼을 '전체'로 리셋
    const catButtons = document.querySelectorAll('.cat-btn');
    catButtons.forEach(b => b.classList.remove('active'));
    if (catButtons.length > 0) catButtons[0].classList.add('active');

    let totalVisible = 0;

    document.querySelectorAll('.faq-list').forEach(list => {
        let visibleInGroup = 0;
        list.querySelectorAll('.faq-item').forEach(item => {
            const qText = item.querySelector('.faq-q-left') ? item.querySelector('.faq-q-left').textContent : '';
            const aText = item.querySelector('.faq-a-inner') ? item.querySelector('.faq-a-inner').textContent : '';
            const matches = query === '' || (qText + ' ' + aText).toLowerCase().includes(query);
            item.style.display = matches ? '' : 'none';
            if (matches) visibleInGroup++;
        });

        list.style.display = visibleInGroup > 0 ? '' : 'none';
        const label = list.previousElementSibling;
        if (label && label.classList.contains('section-label')) {
            label.style.display = visibleInGroup > 0 ? '' : 'none';
        }

        totalVisible += visibleInGroup;
    });

    if (emptyBox) {
        emptyBox.style.display = totalVisible === 0 ? '' : 'none';
        if (totalVisible === 0) {
            if (emptyIcon) emptyIcon.textContent = '🔍';
            if (emptyText) emptyText.textContent = '검색 결과가 없습니다.';
        }
    }
}

/* ---- inquiryList.jsp : 상태 필터 ---- */
function filterInquiryStatus(btn, status) {
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    document.querySelectorAll('.inq-card').forEach(card => {
        card.style.display = (status === 'all' || card.dataset.status === status) ? '' : 'none';
    });
}

/* ---- inquiryList.jsp : 카드 클릭 시 상세 팝업 ---- */
function openInqDetailModal(inquiryId) {
    const tpl = document.getElementById('inqDetailTpl' + inquiryId);
    const body = document.getElementById('inqDetailBody');
    if (!tpl || !body) return;
    body.innerHTML = tpl.innerHTML;
    document.getElementById('inqDetailModal').classList.add('show');
}
function closeInqDetailModal(e) {
    document.getElementById('inqDetailModal').classList.remove('show');
}

/* ---- inquiryList.jsp : 삭제 버튼 (답변 대기 상태만 노출됨) ---- */
function deleteInquiry(inquiryId) {
    if (!confirm('이 문의를 삭제하시겠어요? 삭제하면 되돌릴 수 없어요.')) return;
    document.getElementById('inqDeleteInquiryNo').value = inquiryId;
    document.getElementById('inqDeleteForm').submit();
}

/* ---- inquiryWrite.jsp : 개인정보 수집·이용 상세 [자세히 보기] 토글 ---- */
function togglePrivacyDetail(linkEl) {
    const detail = document.getElementById('privacyDetail');
    if (!detail) return;
    const isOpen = detail.classList.toggle('open');
    linkEl.textContent = isOpen ? '[접기]' : '[자세히 보기]';
}

/* ---- inquiryWrite.jsp : 글자수 카운트 ---- */
function countChars(inputId, countId, max) {
    const val = document.getElementById(inputId).value.length;
    const el = document.getElementById(countId);
    el.textContent = val;
    el.style.color = val > max * 0.9 ? '#c0564a' : 'var(--so-olive-dark)';
}

/* ---- inquiryWrite.jsp : 첨부파일 선택 시 파일명 표시 ---- */
/* ---- inquiryWrite.jsp : 첨부파일 선택 (여러 번 선택해도 누적, 최대 5개 / 5MB) ----
   <input type="file" multiple>은 파일 선택창을 다시 열 때마다 input.files를
   통째로 새 걸로 덮어써버리는 브라우저 기본 동작 때문에, 이전에 고른 파일이
   사라지는 문제가 있었음. 이를 우회하기 위해 선택한 파일을 별도 배열
   (inqSelectedFiles)로 직접 관리하고, 매번 DataTransfer로 input.files를
   다시 조립해서 "누적 선택"처럼 보이게 함. */
const INQ_MAX_FILES = 5;
const INQ_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
let inqSelectedFiles = [];

function handleFileSelect(input) {
    const newFiles = Array.from(input.files || []);

    newFiles.forEach(file => {
        if (inqSelectedFiles.length >= INQ_MAX_FILES) {
            alert('파일은 최대 ' + INQ_MAX_FILES + '개까지 첨부할 수 있습니다.');
            return;
        }
        if (file.size > INQ_MAX_FILE_SIZE) {
            alert('"' + file.name + '" 파일은 5MB를 초과해 첨부할 수 없습니다.');
            return;
        }
        // 같은 이름 + 같은 크기의 파일은 중복으로 보고 다시 담지 않음
        const isDuplicate = inqSelectedFiles.some(f => f.name === file.name && f.size === file.size);
        if (isDuplicate) return;

        inqSelectedFiles.push(file);
    });

    syncInquiryFileInput(input);
    renderInquiryFileList();
}

/* 누적된 inqSelectedFiles 배열 내용을 실제 <input type="file">의
   input.files에 다시 채워넣는다 (그래야 폼 제출 시 서버로 전송됨) */
function syncInquiryFileInput(input) {
    const dt = new DataTransfer();
    inqSelectedFiles.forEach(file => dt.items.add(file));
    input.files = dt.files;
}

/* 선택된 파일마다 만들어둔 미리보기 URL(URL.createObjectURL 결과).
   렌더링할 때마다 재사용하고, 파일이 목록에서 빠지면 메모리 해제를 위해 revoke함. */
let inqPreviewUrls = [];

/* 이미지 확장자 여부 (서버 InquiryFileVO.isImage()와 기준 통일) */
function isImageFile(file) {
    return /\.(png|jpe?g|gif)$/i.test(file.name);
}

/* 선택된 파일 목록을 화면에 그림. 이미지면 작은 썸네일, 아니면 파일명만.
   파일마다 × 버튼으로 개별 삭제 가능 */
function renderInquiryFileList() {
    const list = document.getElementById('uploadFileNames');
    if (!list) return;

    // 이전에 만들어둔 미리보기 URL은 다시 그리기 전에 정리(메모리 누수 방지)
    inqPreviewUrls.forEach(url => { if (url) URL.revokeObjectURL(url); });
    inqPreviewUrls = [];

    if (inqSelectedFiles.length === 0) {
        list.innerHTML = '';
        return;
    }

    const chips = inqSelectedFiles.map((file, idx) => {
        let thumbHtml = '';
        if (isImageFile(file)) {
            const url = URL.createObjectURL(file);
            inqPreviewUrls[idx] = url;
            thumbHtml = '<img src="' + url + '" alt="' + file.name + '" class="upload-file-thumb">';
        }
        return (
            '<span class="upload-file-chip">' +
                thumbHtml +
                '<span class="upload-file-name">' + file.name + '</span>' +
                ' <a href="javascript:void(0)" class="upload-file-remove" onclick="removeInquiryFile(' + idx + ')" aria-label="파일 삭제">×</a>' +
            '</span>'
        );
    }).join('');

    list.innerHTML = '<div class="upload-file-count">선택된 파일 (' + inqSelectedFiles.length + '/' + INQ_MAX_FILES + ')</div>' +
        '<div class="upload-file-list">' + chips + '</div>';
}

/* × 버튼 클릭 시 해당 파일만 목록/실제 input에서 제거 */
function removeInquiryFile(idx) {
    inqSelectedFiles.splice(idx, 1);
    const input = document.getElementById('attachmentInput');
    if (input) syncInquiryFileInput(input);
    renderInquiryFileList();
}

/* ---- inquiryWrite.jsp : 제출 전 유효성 검사 (실제 제출은 form action으로 처리) ---- */
function validateInquiryForm(formEl) {
    const privacyCheck = document.getElementById('privacyCheck');
    if (!privacyCheck.checked) {
        alert('개인정보 수집 및 이용에 동의해 주세요.');
        return false;
    }
    const title = document.getElementById('titleInput').value.trim();
    const body = document.getElementById('bodyInput').value.trim();
    if (!title || !body) {
        alert('제목과 상세 내용을 입력해 주세요.');
        return false;
    }



    const submitBtn = formEl.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = '등록 중...';

    return true;
}
/* ---- inquiryList.jsp / inquiryDetail.jsp : 첨부 사진 확대보기(라이트박스) ---- */
function openInqLightbox(src, name) {
    const img = document.getElementById('inqLightboxImg');
    img.src = src;
    img.alt = name || '';
    document.getElementById('inqLightbox').classList.add('show');
}
function closeInqLightbox(e) {
    document.getElementById('inqLightbox').classList.remove('show');
}
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeInqLightbox();
});
