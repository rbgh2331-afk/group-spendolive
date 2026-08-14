
const reportCloseBtn = document.getElementById('StatusCloseButton');
if (reportCloseBtn) {
    reportCloseBtn.addEventListener('click', function() {
        hideStatusModal('report');
    });
}

const reportActionBtn = document.getElementById('StatusActionButton');
if (reportActionBtn) {
    reportActionBtn.addEventListener('click', function () {
      if (typeof window.modalActionHandler === 'function') {
          window.modalActionHandler();
        }
    });
}
// 신고 처리
(function () {

    document.addEventListener('click', async function (event) {
        const reportButton = event.target.closest('.reportSubmitButton');
        if (!reportButton) return;

        const room_id = reportButton.dataset.room_id;
        const reported_member_id = reportButton.dataset.reported_member_id;
        const chat_text = reportButton.dataset.chat_text;
        if (!room_id || !reported_member_id || !chat_text) {
            showFailure(reportButton, { message: '신고할 회원 정보를 찾을 수 없습니다.' },'report');
            return;
        }
        const body = new URLSearchParams({ room_id });
        if (reported_member_id) {
            body.append('reported_member_id', reported_member_id);
        }
        if (chat_text) {
            body.append('chat_text', chat_text);
        }
        // 모듈화된 함수 호출
        await executeRequest({
            button: reportButton,
            confirmMessage: '신고 하시겠습니까?',
            requestUrl: '/report/report.do',
            bodyData: body,
            modalTitle : '신고를 처리하고 있습니다.',
            fallbackErrorMessage: '신고 결과를 확인하지 못했습니다. 다시 시도해주세요.'
        },'report');
    });
    // 결제 중 새로고침이나 창 닫기를 시도하면 브라우저 기본 경고를 표시

  })();
  // 경고 처리
  (function () {

    document.addEventListener('click', async function (event) {
        const waringButton = event.target.closest('.waringSubmitButton');
        if (!waringButton) return;

        const reportIdInput = document.getElementById('formReportId');
        const reportedMemberInput = document.getElementById('formReportMemberId');
        const adminCommentInput = document.getElementById('adminComment');
        const reportResultInput = document.getElementById('reportResult');
        const report_id = reportIdInput ? reportIdInput.value.trim() : '';
        const reported_member_id = reportedMemberInput ? reportedMemberInput.value.trim() : '';
        const admin_comment = adminCommentInput ? adminCommentInput.value.trim() : '';
        const result = reportResultInput ? reportResultInput.value.trim() : '';
        if (!admin_comment) {
            alert('처리 결과를 입력해주세요.');
            if (adminCommentInput) adminCommentInput.focus();
            return;
        }
        if (!report_id || !reported_member_id || !admin_comment) {
            showFailure(waringButton, { message: '경고할 회원 정보를 찾을 수 없습니다.' },'report');
            return;
        }
        const body = new URLSearchParams({ report_id });
        if (reported_member_id) {
            body.append('reported_member_id', reported_member_id);
        }
        if (admin_comment) {
            body.append('admin_comment', admin_comment);
        }if (result) {
            body.append('result', result);
        }
        // 모듈화된 함수 호출
        await executeRequest({
            button: waringButton,
            confirmMessage: '처리 하시겠습니까?',
            requestUrl: '/admin/report/comment.do',
            bodyData: body,
            modalTitle : '신고를 처리하고 있습니다.',
            fallbackErrorMessage: '신고 결과를 확인하지 못했습니다. 다시 시도해주세요.'
        },'report');
    });
    // 결제 중 새로고침이나 창 닫기를 시도하면 브라우저 기본 경고를 표시

  })();




