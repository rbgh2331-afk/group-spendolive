/* =========================================================
   OTT 채팅방
   - 페이지 data 속성에서 서버값을 읽는다.
   - 1.5초마다 메시지 목록을 갱신한다.
   ========================================================= */
(function initializeOttChatPage() {
    const page = document.getElementById('ottChatPage');

    if (!page) {
        return;
    }

    const msg = page.dataset.message || '';
    const room_id = page.dataset.roomId || '';
    const contextPath = page.dataset.contextPath || '';

    if (msg) {
        alert(msg);
    }
    const list = document.getElementById('chatMessageList');
    const form = document.getElementById('chatSendForm');
    const input = document.getElementById('chatMessageInput');
    const sendButton = form ? form.querySelector('button[type="submit"],input[type="submit"]') : null;
    // 채팅에는 전역 팝업을 쓰지 않고 별도 pending 상태로 연속 전송만 막는다
    let chatSendPending = false;

    if (!room_id || !list || !form || !input) {
        return;
    }

    // 메시지를 textContent로 화면에 출력
    function makeMessageRow(message) {
        const isSystem = message.system_yn === 'Y';
        const isMine = message.mine_yn === 'Y';

        const row = document.createElement('div');
        row.className = isSystem
            ? 'chat-message-row system'
            : 'chat-message-row ' + (isMine ? 'mine' : 'other');

        const bubble = document.createElement('div');
        bubble.className = isSystem ? 'chat-system-bubble' : 'chat-message-bubble';

        const sender = document.createElement('strong');
        sender.textContent = isSystem
            ? '시스템 알림'
            : (message.sender_name || '알 수 없음');

        const content = document.createElement('p');
        content.textContent = message.message_content || '';

        const time = document.createElement('small');
        time.textContent = message.created_at || '';

        bubble.appendChild(sender);
        bubble.appendChild(content);
        bubble.appendChild(time);
        row.appendChild(bubble);

        // 내 메시지와 시스템 메시지를 제외한 상대방 메시지만 신고 가능
        if (!isSystem && !isMine) {
            const reportLink = document.createElement('button');

            reportLink.type = 'button';
            reportLink.textContent = '신고하기';
            reportLink.dataset.reported_member_id = message.sender_id;
            reportLink.dataset.room_id = room_id;
            reportLink.dataset.chat_text = message.message_content || '';
            reportLink.className = 'btn btn-danger-outline mini reportSubmitButton';

            row.appendChild(reportLink);
        }

        return row;
    }

    // 최신 메시지 위치로 이동
    function scrollToBottom() {
        list.scrollTop = list.scrollHeight;
    }

    // AJAX로 채팅 목록 갱신
    function loadMessages() {
        fetch(contextPath + '/spendolive/ott/chat/messages.do?room_id=' + encodeURIComponent(room_id), {
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('채팅 목록 조회 실패: ' + response.status);
                }

                return response.json();
            })
            .then(function (messages) {
                const fragment = document.createDocumentFragment();

                if (!messages || messages.length === 0) {
                    const empty = document.createElement('div');
                    empty.className = 'empty-box chat-empty-box';
                    empty.textContent = '아직 대화가 없습니다. 첫 메시지를 보내보세요.';
                    fragment.appendChild(empty);
                } else {
                    messages.forEach(function (message) {
                        fragment.appendChild(makeMessageRow(message));
                    });
                }

                // 새 채팅 DOM이 정상적으로 모두 만들어진 뒤 기존 화면과 교체
                list.replaceChildren(fragment);
            })
            .catch(function (error) {
                // 갱신 실패 시 기존 채팅 화면은 그대로 유지
                console.error('[ott chat] 채팅 목록 갱신 실패', error);
            });
    }

    // AJAX로 메시지 전송
    // 채팅은 전역 로딩 팝업 대신 입력창 안에서 빠르게 이어져야 하므로 버튼 잠금만 적용
    form.addEventListener('submit', function (event) {
        event.preventDefault();

        if (!input.value.trim() || chatSendPending) {
            return;
        }

        chatSendPending = true;
        if (sendButton) sendButton.disabled = true;

        fetch(form.action, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            body: new FormData(form)
        })
            .then(function (response) {
                return response.json()
                    .catch(function () { return null; })
                    .then(function (result) { return { response: response, result: result }; });
            })
            .then(function (payload) {
                const response = payload.response;
                const result = payload.result;

                if (!response.ok || (result && result.success === false)) {
                    if (result && result.code === 'SESSION_EXPIRED' && result.redirectUrl) {
                        window.location.href = contextPath + result.redirectUrl;
                        return;
                    }
                    alert(result && result.message ? result.message : '메시지 전송에 실패했습니다.');
                    return;
                }

                input.value = '';
                loadMessages();
                input.focus();
            })
            .catch(function (error) {
                console.error('[ott chat] 메시지 전송 실패', error);
            })
            .finally(function () {
                chatSendPending = false;
                if (sendButton) sendButton.disabled = false;
            });
    });

    // 1.5초마다 새 메시지 조회
    scrollToBottom();
    setInterval(loadMessages, 1500);
})();


/* =========================================================
   OTT 가족방 초대 공유
   - URL 복사, QR 표시, 카카오톡 공유를 처리한다.
   ========================================================= */
// 가족방 초대 공유 - URL 복사, QR, 카카오톡 공유 처리
(function () {
    var page = document.getElementById('ottFriendsPage');

    if (!page) {
        return;
    }

    var kakaoJavascriptKey = page.dataset.kakaoKey || '';
    var contextPath = page.dataset.contextPath || '';

    if (window.Kakao && kakaoJavascriptKey && !window.Kakao.isInitialized()) {
        window.Kakao.init(kakaoJavascriptKey);
    }

    // 초대 URL 복사
    function copyText(text) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(text);
        }

        var temp = document.createElement('textarea');
        temp.value = text;
        temp.style.position = 'fixed';
        temp.style.left = '-9999px';
        document.body.appendChild(temp);
        temp.focus();
        temp.select();
        document.execCommand('copy');
        document.body.removeChild(temp);
        return Promise.resolve();
    }

    // 카카오 공유 데이터 생성
    function buildSharePayload(room_name, inviteUrl) {
        return {
            objectType: 'feed',
            content: {
                title: room_name,
                description: 'SpendOlive 가족방 초대 링크입니다. 링크를 열면 결제 화면으로 이동합니다.',
                imageUrl: window.location.origin + contextPath + '/resources/images/logo.png',
                link: {
                    mobileWebUrl: inviteUrl,
                    webUrl: inviteUrl
                }
            },
            buttons: [
                {
                    title: '결제하러 가기',
                    link: {
                        mobileWebUrl: inviteUrl,
                        webUrl: inviteUrl
                    }
                }
            ]
        };
    }

    // 카카오 공유 실패 시 URL 복사
    function shareKakao(room_name, inviteUrl) {
        if (!kakaoJavascriptKey) {
            return copyText(inviteUrl).then(function () {
                alert('카카오 JavaScript 키가 아직 설정되지 않아 초대 URL을 대신 복사했습니다.');
            });
        }

        if (!window.Kakao || !window.Kakao.isInitialized()) {
            return copyText(inviteUrl).then(function () {
                alert('카카오 SDK가 연결되지 않아 초대 URL을 대신 복사했습니다.');
            });
        }

        try {
            var payload = buildSharePayload(room_name, inviteUrl);

            if (window.Kakao.Share && window.Kakao.Share.sendDefault) {
                window.Kakao.Share.sendDefault(payload);
                return Promise.resolve();
            }

            if (window.Kakao.Link && window.Kakao.Link.sendDefault) {
                window.Kakao.Link.sendDefault(payload);
                return Promise.resolve();
            }
        } catch (e) {
            console.error(e);
        }

        return copyText(inviteUrl).then(function () {
            alert('카카오톡 공유를 실행하지 못해 초대 URL을 대신 복사했습니다.');
        });
    }

    // 초대 공유 버튼 이벤트 연결
    document.querySelectorAll('.invite-share-box').forEach(function (box) {
        var input = box.querySelector('.invite-url-input');
        var copyBtn = box.querySelector('.invite-copy-btn');
        var qrBtn = box.querySelector('.invite-qr-btn');
        var kakaoBtn = box.querySelector('.invite-kakao-btn');
        var qrBox = box.querySelector('.invite-qr-box');
        var qrImg = qrBox ? qrBox.querySelector('img') : null;
        var room_name = box.dataset.roomName || 'SpendOlive 가족방';

        if (copyBtn && input) {
            copyBtn.addEventListener('click', function () {
                copyText(input.value).then(function () {
                    alert('초대 URL을 복사했습니다.');
                });
            });
        }

        if (qrBtn && input && qrBox && qrImg) {
            qrBtn.addEventListener('click', function () {
            if (!qrImg.getAttribute('src')) {
                qrImg.src =
                    'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data='
                    + encodeURIComponent(input.value);
            }
                qrBox.classList.toggle('show');
            });
        }

        if (kakaoBtn && input) {
            kakaoBtn.addEventListener('click', function () {
                shareKakao(room_name, input.value);
            });
        }
    });
})();
