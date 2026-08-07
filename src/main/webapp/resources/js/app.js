/* SpendOlive Complete Fixed JS */
const contextPath = window.contextPath;
let currentMonth = 6;

function openModal(id){const el=document.getElementById(id);if(el)el.classList.add("show")}
function closeModal(id){const el=document.getElementById(id);if(el)el.classList.remove("show")}

function addExpense(){
  const amount=Number(document.getElementById("expenseAmount")?.value||0);
  const tbody=document.getElementById("expenseRows");
  if(!tbody){alert("지출관리 페이지에서 사용할 수 있는 기능입니다.");return}
  const tr=document.createElement("tr");
  tr.innerHTML=`<td>오늘</td><td>새 지출</td><td>변동지출</td><td>식비</td><td>${amount.toLocaleString("ko-KR")}원</td>`;
  tbody.prepend(tr);
  alert("지출 내역이 등록되었습니다.");
}

function changeMonth(diff){
  currentMonth+=diff;
  if(currentMonth<1)currentMonth=12;
  if(currentMonth>12)currentMonth=1;
  const title=document.getElementById("calendarTitle");
  if(title) title.textContent=`2026년 ${currentMonth}월`;
}

function kakaoLogin(){alert("카카오톡 로그인 연동 예정입니다.")}
function authLogin(){alert("로그인 처리 예정입니다.")}
function authSignup(){alert("회원가입 처리 예정입니다.")}

function setAuthMessage(id,message,type){
  const el=document.getElementById(id);
  if(!el)return;
  el.textContent=message;
  el.className="auth-result-text "+type;
}
var isIdVerified = false; 
document.addEventListener("DOMContentLoaded",()=>{
  document.querySelectorAll(".modal").forEach((modal)=>{
    modal.addEventListener("click",(event)=>{
      if(event.target===modal) modal.classList.remove("show");
    });
  });
});
var isEmailVerified = false; 


// 2. 사용자가 입력한 6자리 인증번호를 확인
function verifyEmail() {
  const inputCode = $('#emailAuthCode').val().trim();
  if (!inputCode) {
      alert('인증번호를 입력해 주세요.');
      return;
  }

  $.ajax({
      url: contextPath + "/member/verifyEmail.do", // 컨트롤러 매핑 주소
      type: 'POST',
      data: { inputCode: inputCode },
      success: function(isSuccess) {
        if (isSuccess) {
            $('#emailAuthResult').text('✓ 이메일 인증이 완료되었습니다.').css('color', '#4CAF50');
            $('#email').attr('readonly', true);
            $('#emailAuthCode').attr('readonly', true);
            isEmailVerified = true; // 인증 성공 플래그 true
        } else {
            $('#emailAuthResult').text('✗ 인증번호가 일치하지 않습니다. 다시 확인해 주세요.').css('color', '#FF3B30');
            isEmailVerified = false;
        }
    },
    error: function() {

        alert('오류가 발생했습니다.');
    }
  });
}

// 글로벌 변수로 휴대폰 인증 여부 체크용 플래그 선언
let isPhoneVerified = false;

// 2. 사용자가 입력한 가상 인증번호 검증
function verifySms() {
  const inputCode = $('#phoneAuthCode').val();
  if (!inputCode) {
      alert('인증번호를 입력해 주세요.');
      return;
  }

  $.ajax({
      url: contextPath +"/member/verifySms.do", // 컨트롤러 매핑 주소
      type: 'POST',
      data: { inputCode: inputCode },
      success: function(isSuccess) {
          if (isSuccess) {
              $('#phoneAuthResult').text('✓ 휴대폰 인증이 완료되었습니다.').css('color', '#4CAF50');
              $('#phone').attr('readonly', true);
              $('#phoneAuthCode').attr('readonly', true);
              isPhoneVerified = true; // 인증 성공 플래그 true
          } else {
              $('#phoneAuthResult').text('✗ 인증번호가 일치하지 않습니다. 다시 확인해 주세요.').css('color', '#FF3B30');
              isPhoneVerified = false;
          }
      },
      error: function() {
          alert('인증 확인 중 오류가 발생했습니다.');
      }
  });
}
// 3. 회원가입 버튼 누를 때 최종 방어선 함수
function joinCheck() {
if(!isEmailVerified) {
    alert("이메일 인증을 완료해야 회원가입이 가능합니다.");
    return false;    
}
if (!isPhoneVerified) {
  alert('전화번호 인증을 완료해 주세요.');
  return false;
}
if (isIdVerified) {
  alert('아이디 중복확인을 완료해 주세요.');
  return false;
}
if($("#password").val() !== $("#passwordCheck").val()) {
    alert("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    return false;
}
return true;
}
function joinCheckKakao() {
  if(!isEmailVerified) {
      alert("이메일 인증을 완료해야 회원가입이 가능합니다.");
      return false;    
  }
  if (!isPhoneVerified) {
    alert('전화번호 인증을 완료해 주세요.');
    return false;
  }
  return true;
}
// 3. 최종 회원가입 서브밋 전 벨리데이션 체크 (joinCheck 함수가 있다면 추가)

/* =========================================================
 OTT 고정 최고 멤버십 자동 계산
 ========================================================= */
(function () {
function toNumber(value) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatWon(value) {
  return toNumber(value).toLocaleString('ko-KR') + '원';
}

function updateFixedPlanForm(form) {
  const select = form.querySelector('.ott-service-select');
  const option = select && select.selectedOptions ? select.selectedOptions[0] : null;
  const preview = form.querySelector('.ott-fixed-plan-preview');
  const room_mode = form.dataset.room_mode || 'RECRUIT';
  const isFriendRoom = room_mode === 'FRIEND';

  if (!option || !option.value) {
    form.querySelector('.ott-plan-input')?.setAttribute('value', '');
    form.querySelector('.ott-total-price-input')?.setAttribute('value', '');
    form.querySelector('.ott-member-limit-input')?.setAttribute('value', '');
    if (preview) {
      preview.innerHTML = '<strong>OTT를 선택하면 최고 멤버십 기준 금액이 자동 적용됩니다.</strong><p>구독종류, 전체 구독료, 최대 인원은 직접 입력하지 않고 서비스 규칙으로 고정됩니다.</p>';
    }
    return;
  }

  const service_name = option.dataset.service_name || option.textContent.trim();
  const plan = option.dataset.plan || '프리미엄';
  const base_price = toNumber(option.dataset.base_price);
  const extraFee = toNumber(option.dataset.extraFee);
  const extraCount = toNumber(option.dataset.extraCount);
  const total_price = toNumber(option.dataset.total_price);
  const member_limit = toNumber(option.dataset.member_limit);
  const share_amount = toNumber(option.dataset.share_amount);
  const fee_amount = toNumber(option.dataset.fee_amount);
  const personAmount = toNumber(option.dataset.personAmount);

  const planInput = form.querySelector('.ott-plan-input');
  const totalInput = form.querySelector('.ott-total-price-input');
  const memberInput = form.querySelector('.ott-member-limit-input');

  if (planInput) planInput.value = plan;
  if (totalInput) totalInput.value = total_price;
  if (memberInput) memberInput.value = member_limit;

  const displayTotalPrice = isFriendRoom ? base_price : total_price;
  const displayShareAmount = member_limit > 0 ? Math.floor(displayTotalPrice / member_limit) : 0;
  const displayFeeAmount = Math.floor(displayShareAmount * 0.03);
  const displayPersonAmount = displayShareAmount + displayFeeAmount;
  
  const extraText = isFriendRoom
    ? '가족/지인 공유방은 추가 IP 비용을 제외합니다.'
    : (extraFee > 0 && extraCount > 0
        ? `추가 계정 ${extraCount}명 × ${formatWon(extraFee)} 포함`
        : '추가 계정 비용 없음');

  if (preview) {
    preview.innerHTML = `
      <strong>${service_name} · ${plan}</strong>
      <div class="ott-plan-preview-grid">
        <span><b>기본 구독료</b>${formatWon(base_price)}</span>
        <span><b>${isFriendRoom ? '공유 기준' : '추가 비용'}</b>${extraText}</span>
        <span><b>N분의 1 기준 금액</b>${formatWon(displayTotalPrice)} / ${member_limit}명</span>
        <span><b>1인 결제금액</b>${formatWon(displayPersonAmount)} <small>분담금 ${formatWon(displayShareAmount)} + 수수료 ${formatWon(displayFeeAmount)}(3%)</small></span>
      </div>
    `;
  }
}

document.addEventListener('DOMContentLoaded', function () {
  document.querySelectorAll('.ott-fixed-plan-form').forEach(function (form) {
    const select = form.querySelector('.ott-service-select');
    updateFixedPlanForm(form);
    if (select) {
      select.addEventListener('change', function () {
        updateFixedPlanForm(form);
      });
    }
  });
});
})();
// 알림 배지는 bellIcon.js 에서 처리

//팝업
function openWithdrawModal() {
  const modal = document.getElementById('withdrawModal');
  const confirmInput = document.getElementById('withdrawConfirm');
  if (!modal) {
      return;
  }

  modal.classList.add('show');
  modal.setAttribute('aria-hidden', 'false');
  if (confirmInput) {
      confirmInput.value = '';
      setTimeout(function () {
          confirmInput.focus();
      }, 80);
  }
}
function closeWithdrawModal() {
  const modal = document.getElementById('withdrawModal');
  if (!modal) {
      return;
  }

  modal.classList.remove('show');
  modal.setAttribute('aria-hidden', 'true');
}
function checkKakaoPassword() {
  const pwInput = document.getElementById('loginPw');
  const password = pwInput ? pwInput.value : '';
  // 대소문자 구분 없이 "KAKAO"만 입력되었는지 확인
  if (password.trim().toLowerCase() === 'kakao') {
    alert("'KAKAO'는 입력할 수 없는 비밀번호입니다.");
    return false; // 로그인 진행 차단
  }

  return true; // 로그인 계속 진행
}
document.addEventListener('DOMContentLoaded', function() {

  // ==========================================
  // 1. 폰트 패널 열기 / 닫기 기능
  // ==========================================
  const fontBtn = document.getElementById('fontToggle');
  const panel = document.getElementById('fontPanel');
  const closeBtn = document.getElementById('fontClose');
  const input = document.getElementById('fontInput');

  function openPanel() {
      if (panel) panel.classList.add('show');
      if (fontBtn) fontBtn.classList.add('hide');
      if (input) input.focus();
  }

  function closePanel() {
      if (panel) panel.classList.remove('show');
      if (fontBtn) fontBtn.classList.remove('hide');
  }

  if (fontBtn) fontBtn.addEventListener('click', openPanel);
  if (closeBtn) closeBtn.addEventListener('click', closePanel);



  
  // ==========================================
  // 2. 글자 크기 및 폰트 설정 기능
  // ==========================================
  const htmlTag = document.documentElement;
  const btnUp = document.getElementById('btn-font-up');
  const btnDown = document.getElementById('btn-font-down');
  const fontCards = document.querySelectorAll('.font-card');

  const CONFIG = {
      default: 16,
      step: 3,
      min: 10,
      max: 30
  };

  // [요청하신 Jua 폰트 추가]
  const FONT_MAP = {
      'system': "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
      'jua': '"Jua", sans-serif', // <-- Jua 폰트 반영
      'sans-serif': "'Noto Sans KR', 'Malgun Gothic', '맑은 고딕', sans-serif",
      'serif': "'Noto Serif KR', 'Batang', '바탕', serif",
      'monospace': "'D2Coding', 'Courier New', monospace"
  };

  const savedSize = localStorage.getItem('userFontSize');
  const savedFont = localStorage.getItem('userFontFamily') || 'system';

  let currentSize = savedSize ? parseInt(savedSize, 10) : CONFIG.default;

  // --- A. 글자 크기 적용 ---
  function applyFontSize(size) {
      htmlTag.style.fontSize = size + 'px';
      localStorage.setItem('userFontSize', size);
      updateButtonStatus(size);
  }

  // --- B. 폰트 종류 적용 ---
  function applyFontFamily(fontKey) {
    const selectedFont = FONT_MAP[fontKey] || FONT_MAP['system'];
    
    // <html> 태그와 <body> 태그 모두에 폰트를 인라인으로 강제 적용
    htmlTag.style.setProperty('font-family', selectedFont, 'important');
    if (document.body) {
        document.body.style.setProperty('font-family', selectedFont, 'important');
    }

    localStorage.setItem('userFontFamily', fontKey);

    fontCards.forEach(card => {
      if (card.getAttribute('data-font') === fontKey) {
          card.classList.add('active');
      } else {
          card.classList.remove('active');
      }
  });
}

  function updateButtonStatus(size) {
      if (btnUp) {
          btnUp.disabled = (size >= CONFIG.max);
          btnUp.style.opacity = (size >= CONFIG.max) ? '0.5' : '1';
          btnUp.style.cursor = (size >= CONFIG.max) ? 'not-allowed' : 'pointer';
      }
      if (btnDown) {
          btnDown.disabled = (size <= CONFIG.min);
          btnDown.style.opacity = (size <= CONFIG.min) ? '0.5' : '1';
          btnDown.style.cursor = (size <= CONFIG.min) ? 'not-allowed' : 'pointer';
      }
  }

  // --- C. 초기 실행 ---
  applyFontSize(currentSize);
  applyFontFamily(savedFont);

  // --- D. 이벤트 리스너 ---
  if (btnUp) {
      btnUp.addEventListener('click', function(e) {
          e.preventDefault();
          let nextSize = currentSize + CONFIG.step;
          if (nextSize > CONFIG.max) nextSize = CONFIG.max;

          if (nextSize !== currentSize) {
              currentSize = nextSize;
              applyFontSize(currentSize);
          }
      });
  }

  if (btnDown) {
      btnDown.addEventListener('click', function(e) {
          e.preventDefault();
          let nextSize = currentSize - CONFIG.step;
          if (nextSize < CONFIG.min) nextSize = CONFIG.min;

          if (nextSize !== currentSize) {
              currentSize = nextSize;
              applyFontSize(currentSize);
          }
      });
  }

  fontCards.forEach(card => {
    card.addEventListener('click', function(e) {
        e.preventDefault();
        const fontKey = this.getAttribute('data-font');
        applyFontFamily(fontKey);
    });
});
});
//ajax 모달
let isProcessing = false;
function showStatusModal(prefix,state, modalTitle, modalMessage, option) {
    const overlay = document.getElementById(prefix +'StatusOverlay');
    const spinner = document.getElementById(prefix +'StatusSpinner');
    const icon = document.getElementById(prefix +'StatusIcon');
    const title = document.getElementById(prefix +'StatusTitle');
    const message = document.getElementById(prefix +'StatusMessage');
    const actions = document.getElementById(prefix +'StatusActions');
    const closeButton = document.getElementById(prefix +'StatusCloseButton');
    const actionButton = document.getElementById(prefix +'StatusActionButton');
    const settings = option || {};
    if (!overlay) return;
    if (!title) return;
      overlay.hidden = false;
      title.textContent = modalTitle;
      message.textContent = modalMessage;
      overlay.dataset.state = state;

    if (spinner) spinner.hidden = state !== 'processing';
    if (icon) {
        icon.hidden = state === 'processing';
        icon.textContent = state === 'success' ? '✓' : '!';
    }
    if (actions) actions.hidden = state === 'processing';
    if (closeButton) closeButton.hidden = state === 'success' || settings.hideClose === true;
      window.modalActionHandler = typeof settings.onAction === 'function' ? settings.onAction : null;

        if (actionButton) {
            if (settings.actionText && window.modalActionHandler) {
            actionButton.textContent = settings.actionText;
            actionButton.hidden = false;
            } else {
            actionButton.hidden = true;
            }
        }
  }
  window.addEventListener('beforeunload', function (event) {
    if (!isProcessing) {
        return;
    }
    event.preventDefault();
    event.returnValue = '';
});
  async function readJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return {
            success: false,
            code: 'INVALID_RESPONSE',
            message: '서버 응답을 확인할 수 없습니다.'
        };
    }
}

window.executeRequest = async function(options,prefix) {
  const {
      button,           // 클릭된 타겟 버튼 (disabled 처리용)
      confirmMessage,   // confirm 창 메시지
      requestUrl,       // 요청 API 경로
      bodyData,         // URLSearchParams 객체
      checkStatusFunc,  // 예외 발생 시 실행할 폴링 함수 () => checkPaymentStatus(id)
      modalTitle = '결제를 처리하고 있습니다.',
      modalDesc = '창을 닫거나 새로고침하지 말아주세요.'
  } = options;

  // 1. 중복 진행 방지
  if (isProcessing) return;

  // 2. 사용자 확인
  if (confirmMessage && !window.confirm(confirmMessage)) return;

  isProcessing = true;
  if (button) button.disabled = true;

  // 3. 상태 모달 열기
  showStatusModal(prefix, 'processing', modalTitle, modalDesc);

  // 4. 타임아웃 컨트롤러 설정 (30초)
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), 30000);

  try {
      const response = await fetch(contextPath + requestUrl, {
          method: 'POST',
          credentials: 'same-origin',
          headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
          },
          body: bodyData.toString(),
          signal: controller.signal
      });

      const result = await readJson(response);

      // Success
      if (response.ok && result.success) {
          isProcessing = false;
          moveAfterSuccess(result, prefix);
          return;
      }

      // Response Error
      showFailure(button, result, prefix);

  } catch (error) {
      // Network or Timeout Exception -> Fallback Check
      showStatusModal(
        prefix,
          'processing',
          '송금 결과를 확인하고 있습니다.',
          '통신이 잠시 끊겨 실제 결제 상태를 다시 확인합니다.'
      );

      const statusResult = await checkStatusFunc();

      if (statusResult && statusResult.success) {
          isProcessing = false;
          moveAfterSuccess(statusResult,prefix);
          return;
      }

      showFailure(button, statusResult || {
          message: '송금 결과를 확인하지 못했습니다. 송금 내역을 확인한 뒤 다시 시도해주세요.'
      });

  } finally {
      window.clearTimeout(timeoutId);
  }
}
function wait(milliseconds) {
  return new Promise(resolve => window.setTimeout(resolve, milliseconds));
}

function hideStatusModal(prefix) {
  // 결제/정산 진행 중일 때는 닫기 방지
  if (isProcessing) return;

  const overlay = document.getElementById(prefix + 'StatusOverlay');
  if (overlay) {
      overlay.hidden = true;
  }
  window.modalActionHandler = null;
}
function moveAfterSuccess(result,prefix) {
  showStatusModal(
      prefix,
      'success',
      result.message,
      result.message || '참여한 방으로 이동합니다.',
      { hideClose: true }
  );

  window.setTimeout(function () {
      window.location.href = result.redirectUrl;
  }, 1200);
}
function showFailure(targetButton, result, prefix = 'payment') {
  isProcessing = false;
  if (targetButton) {
      targetButton.disabled = false;
  }

  if (result && result.code === 'LOGIN_REQUIRED') {
      showStatusModal(prefix, 'error', '로그인이 필요합니다.', result.message || '다시 로그인해주세요.', {
          actionText: '로그인 화면으로',
          onAction: () => {
              window.location.href = result.redirectUrl || (contextPath + '/member/loginForm.do');
          }
      });
      return;
  }

  if (result && result.code === 'CARD_REQUIRED') {
      showStatusModal(prefix, 'error', '결제 카드가 필요합니다.', result.message || '카드를 먼저 등록해주세요.', {
          actionText: '카드 등록하기',
          onAction: () => {
              if (typeof window.requestBillingAuth === 'function') {
                  window.requestBillingAuth();
              } else {
                  window.location.href = contextPath + '/spendolive/mypage.do';
              }
          }
      });
      return;
  }
  if (result && result.code === 'REPORTED_FAILED') {
    showStatusModal(prefix, 'error', '이미 신고가 완료된 건 입니다.', result.message || '이미 완료된 건 입니다.', {
        actionText: '메인 화면으로',
        onAction: () => {
            if (typeof window.requestBillingAuth === 'function') {
                window.requestBillingAuth();
            } else {
                window.location.href = contextPath + '/spendolive/main.do';
            }
        }
    });
    return;
}

  const defaultTitle = prefix === 'payment' ? '결제를 완료하지 못했습니다.' : '실패하였습니다.';
  showStatusModal(
      prefix,
      'error',
      defaultTitle,
      result && result.message ? result.message : '잠시 후 다시 시도해주세요.'
  );
}