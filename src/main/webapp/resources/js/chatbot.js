(function () {
  const toggleBtn = document.getElementById('chatbotToggle');
  const panel = document.getElementById('chatbotPanel');
  const closeBtn = document.getElementById('chatbotClose');
  const body = document.getElementById('chatbotBody');
  const input = document.getElementById('chatbotInput');
  const sendBtn = document.getElementById('chatbotSend');

  function toggle() {
    const isOpen = panel.classList.toggle('show');

    if (isOpen) {
      input.focus();
    }
  }

  function close() {
    panel.classList.remove('show');
  }

  function addMessage(text, sender, fallback) {
    const div = document.createElement('div');
    div.className = 'chatbot-msg ' + sender + (fallback ? ' fallback' : '');
    div.textContent = text;
    body.appendChild(div);
    body.scrollTop = body.scrollHeight;
    return div;
  }

  async function send() {
    const question = input.value.trim();
    if (!question) return;
    addMessage(question, 'user');
    input.value = '';

    const typing = addMessage('답변을 찾는 중...', 'bot typing');

    try {
     /* [AJAX] POST /chatbot/ask.do
         - 사용자가 입력한 질문 텍스트를 JSON body로 그대로 서버에 전달
         - 서버는 FAQ 등과 매칭해서 { answer, matched } 형태로 응답한다고 가정:
           matched === false면 "찾은 답변이 없어 기본 안내 문구를 준 것"이라는 뜻이라
           addMessage 세 번째 인자(fallback)를 true로 넘겨 스타일을 다르게 표시함
         - 대화창 안에 이미 "답변을 찾는 중..." 말풍선이 있어서 fetchWithLoading()의
           전역 스피너는 쓰지 않고 순정 fetch를 그대로 씀 */
      const res = await fetch(`/spendolive/chatbot/ask.do`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        skipGlobalLoading: true,  // 대화창 안에 자체 "답변을 찾는 중..." 버블이 이미 있어서 전역 스피너는 제외
        body: JSON.stringify({ question })
      });
      const data = await res.json();
      typing.remove();
      addMessage(data.answer, 'bot', !data.matched);
    } catch (e) {
      typing.remove();
      addMessage('일시적인 오류가 발생했어요. 잠시 후 다시 시도해주세요.', 'bot', true);
      console.error('[chatbot] 요청 실패', e);
    }
  }

  toggleBtn.addEventListener('click', toggle);
  closeBtn.addEventListener('click', close);
  sendBtn.addEventListener('click', send);
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') send();
  });
})();
