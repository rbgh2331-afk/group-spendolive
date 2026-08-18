    /* =========================================================
       메인 대시보드 실제 슬롯머신식 숫자 롤링 효과
       ---------------------------------------------------------
       이전 방식:
       - 숫자 텍스트 자체를 빠르게 바꿔서 "막 바뀌는" 느낌이었다.

       현재 방식:
       - 각 숫자를 세로 reel(릴) 구조로 만든다.
       - 릴 안의 숫자가 위로 굴러가다가 최종 숫자에서 멈춘다.
       - GIF 슬롯머신처럼 보이도록 overflow:hidden + translateY를 사용한다.

       비로그인:
       - 고정/변동/OTT 금액을 랜덤 생성
       - 총지출 = 고정 + 변동 + OTT
       - OTT 정산 건수 = OTT지출 금액 기준
       - 슬롯처럼 굴러간 뒤 3초 멈추고 다시 반복

       로그인:
       - Controller가 DB에서 가져온 선택 월 고정/변동/OTT 금액 사용
       - OTT 정산 건수도 DB의 실제 정산 회차 수 사용
       - 총지출 = 고정 + 변동 + OTT
       - 처음 한 번 슬롯처럼 굴러간 뒤 실제 DB 값에서 멈춤
       ========================================================= */
    (function () {
        const dashboardData = document.getElementById('mainDashboardData');

        if (!dashboardData) {
            return;
        }

        const elements = {
            totalStat: document.getElementById('mainTotalStat'),
            ottSettlementStat: document.getElementById('mainOttSettlementStat'),
            dashboardTotal: document.getElementById('dashboardTotalAmount'),
            spendRing: document.getElementById('spendRing'),
            budgetPercent: document.getElementById('budgetPercentText'),
            fixedAmount: document.getElementById('fixedExpenseAmount'),
            variableAmount: document.getElementById('variableExpenseAmount'),
            ottAmount: document.getElementById('ottExpenseAmount')
        };

        const isLogin = dashboardData.dataset.login === 'true';
        const budget = Number(dashboardData.dataset.budget || 1700000);

        /*
         * 원형 그래프 부드러운 애니메이션용 변수
         *
         * 기존 방식은 conic-gradient 값을 바로 바꿔서 그래프가 딱딱 끊겨 보였다.
         * 현재 방식은 requestAnimationFrame으로 현재 퍼센트에서 목표 퍼센트까지
         * 조금씩 값을 올리거나 내려서 자연스럽게 차오르는 느낌을 만든다.
         */
        let currentRingPercent = 72;
        let ringAnimationFrameId = null;

        function toNumber(value) {
            const parsed = Number(value || 0);
            return Number.isFinite(parsed) ? parsed : 0;
        }

        function formatWon(value) {
            return toNumber(value).toLocaleString('ko-KR') + '원';
        }

        function formatWonWithSymbol(value) {
            return '₩' + toNumber(value).toLocaleString('ko-KR');
        }

        function randomStepAmount(min, max, step) {
            const count = Math.floor((max - min) / step);
            return min + Math.floor(Math.random() * (count + 1)) * step;
        }

        /*
         * OTT 정산 건수 계산 규칙
         *
         * 15,000원 이상 ~ 32,000원 미만 → 1건
         * 32,000원 이상 ~ 47,000원 미만 → 2건
         * 47,000원 이상 ~ 62,000원 미만 → 3건
         * 62,000원 이상 ~ 78,000원 미만 → 4건
         * 78,000원 이상                  → 5건
         */
        function getOttSettlementCount(ottAmount) {
            if (ottAmount <= 0) return 0;
            if (ottAmount < 32000) return 1;
            if (ottAmount < 47000) return 2;
            if (ottAmount < 62000) return 3;
            if (ottAmount < 78000) return 4;
            return 5;
        }

        function getBudgetPercent(total) {
            if (budget <= 0) return 0;

            // 텍스트에는 실제 사용률을 표시
            // 예산을 초과하면 120%, 150%처럼 100%를 넘겨 보여준다
            return Math.round((total / budget) * 100);
        }

        function makeRandomSummary() {
            const fixed = randomStepAmount(500000, 700000, 10000);
            const variable = randomStepAmount(300000, 650000, 10000);
            const ott = randomStepAmount(15000, 100000, 1000);

            // 총합은 반드시 고정 + 변동 + OTT로 계산
            const total = fixed + variable + ott;
            const percent = getBudgetPercent(total);
            const ottSettlementCount = getOttSettlementCount(ott);

            return { fixed, variable, ott, total, percent, ottSettlementCount };
        }

        function makeDbSummary() {
            const fixed = toNumber(dashboardData.dataset.fixed);
            const variable = toNumber(dashboardData.dataset.variable);
            const ott = toNumber(dashboardData.dataset.ott);

            // 로그인 상태에서도 총지출은 세 항목을 다시 더해서 화면 불일치를 막는다
            const total = fixed + variable + ott;
            const percent = getBudgetPercent(total);

            // 로그인 사용자는 금액 구간 추정이 아니라 Controller가 전달한 실제 정산 수를 사용
            const ottSettlementCount = toNumber(
                dashboardData.dataset.ottSettlementCount
            );

            return { fixed, variable, ott, total, percent, ottSettlementCount };
        }

        function drawRing(percent) {
            if (!elements.spendRing) return;

            // 원형 그래프만 최대 100%로 제한한다. 텍스트는 실제 초과율을 유지
            const safePercent = Math.max(0, Math.min(percent, 100));

            elements.spendRing.style.background =
                'conic-gradient(var(--olive) 0 ' + safePercent + '%, var(--olive-soft) ' + safePercent + '% 100%)';
        }

        function easeOutCubic(progress) {
            return 1 - Math.pow(1 - progress, 3);
        }

        function updateRing(targetPercent) {
            if (!elements.spendRing) return;

            const startPercent = currentRingPercent;
            const endPercent = Math.max(0, Math.min(toNumber(targetPercent), 100));
            const duration = 1350;
            const startTime = performance.now();

            if (ringAnimationFrameId !== null) {
                cancelAnimationFrame(ringAnimationFrameId);
            }

            elements.spendRing.classList.add('slot-ring-rolling');

            function animate(now) {
                const elapsed = now - startTime;
                const progress = Math.min(elapsed / duration, 1);
                const easedProgress = easeOutCubic(progress);
                const nextPercent = startPercent + (endPercent - startPercent) * easedProgress;

                currentRingPercent = nextPercent;
                drawRing(nextPercent);

                if (progress < 1) {
                    ringAnimationFrameId = requestAnimationFrame(animate);
                    return;
                }

                currentRingPercent = endPercent;
                drawRing(endPercent);
                ringAnimationFrameId = null;
                elements.spendRing.classList.remove('slot-ring-rolling');
            }

            ringAnimationFrameId = requestAnimationFrame(animate);
        }

        /**
         * 숫자 하나를 슬롯머신 릴로 만든다.
         *
         * 예: 최종 숫자가 7이면
         * 0 1 2 3 4 5 6 7 8 9 0 1 2 ... 7
         * 이런 긴 세로 줄을 만들고, translateY로 위로 올려 7에서 멈춘다.
         */
        function createDigitReel(targetDigit, index) {
            const digit = Number(targetDigit);
            const digitBox = document.createElement('span');
            const reel = document.createElement('span');

            digitBox.className = 'slot-digit-box';
            reel.className = 'slot-digit-reel';

            // 숫자마다 멈추는 타이밍을 살짝 다르게 해서 실제 슬롯처럼 보이게 한다
            reel.style.transitionDuration = (1.05 + index * 0.045) + 's';
            reel.style.transitionDelay = (index * 0.025) + 's';

            // 0~9를 여러 번 반복해서 굴러가는 길이를 만든다
            // 마지막에 targetDigit를 붙여 그 숫자에서 정확히 멈추게 한다
            const numbers = [];
            for (let round = 0; round < 4; round++) {
                for (let n = 0; n <= 9; n++) {
                    numbers.push(n);
                }
            }
            numbers.push(digit);

            numbers.forEach(function (number) {
                const item = document.createElement('span');
                item.className = 'slot-digit-item';
                item.textContent = number;
                reel.appendChild(item);
            });

            digitBox.appendChild(reel);

            // 화면에 붙은 다음 프레임에서 transform을 줘야 transition이 동작한다
            requestAnimationFrame(function () {
                requestAnimationFrame(function () {
                    const finalIndex = numbers.length - 1;

                    reel.style.transform =
                        'translateY(-' + finalIndex + 'em)';
                });
            });

            return digitBox;
        }

        /**
         * 텍스트를 슬롯머신 숫자 형태로 출력한다.
         *
         * 숫자: 세로로 굴러가는 릴 처리
         * 콤마, 원, ₩, %, 건: 고정 문자 처리
         */
        function renderSlotText(element, text) {
            if (!element) return;

            element.classList.add('slot-number');
            element.innerHTML = '';

            const chars = String(text).split('');
            let digitIndex = 0;

            chars.forEach(function (char) {
                if (/\d/.test(char)) {
                    element.appendChild(createDigitReel(char, digitIndex));
                    digitIndex++;
                } else {
                    const staticChar = document.createElement('span');
                    staticChar.className = 'slot-static-char';
                    staticChar.textContent = char;
                    element.appendChild(staticChar);
                }
            });
        }

        function renderSummaryWithSlot(summary) {
            renderSlotText(elements.totalStat, formatWon(summary.total));
            renderSlotText(elements.ottSettlementStat, summary.ottSettlementCount + '건');
            renderSlotText(elements.dashboardTotal, formatWonWithSymbol(summary.total));
            renderSlotText(elements.budgetPercent, summary.percent + '%');
            renderSlotText(elements.fixedAmount, formatWon(summary.fixed));
            renderSlotText(elements.variableAmount, formatWon(summary.variable));
            renderSlotText(elements.ottAmount, formatWon(summary.ott));
            updateRing(summary.percent);
        }

        function startGuestRolling() {
            const summary = makeRandomSummary();
            renderSummaryWithSlot(summary);

            // 릴이 멈춘 뒤 3초 정도 최종 숫자를 보여주고 다시 돌린다
            setTimeout(startGuestRolling, 4600);
        }

        if (isLogin) {
            // 로그인 상태는 DB 실제 값으로 한 번만 슬롯머신처럼 돌리고 멈춘다
            renderSummaryWithSlot(makeDbSummary());
        } else {
            // 비로그인 상태는 시연용 랜덤 값으로 계속 반복한다
            startGuestRolling();
        }
    })();
