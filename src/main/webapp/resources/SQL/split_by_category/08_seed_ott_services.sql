
/* =========================================================
   1. OTT 서비스 기본 데이터
   =========================================================
   역할:
   - OTT 메인/모집글 작성 화면에서 선택할 서비스 목록을 미리 넣습니다.
   - 사용자가 요금제명, 총금액, 최대인원을 직접 고르는 방식이 아니라
     서비스별 최고 멤버십 기준값을 사용합니다.

   컬럼 설명:
   - service_name       : 화면에 표시되는 OTT 이름
   - default_price      : 총 분담 기준 금액 = base_price + 추가 비용 총액
   - fixed_plan_name    : 고정 사용할 최고 멤버십 이름
   - base_price         : 최고 멤버십 기본 월 구독료
   - extra_member_fee   : 추가 IP/추가 멤버 1명당 비용. 없으면 0
   - extra_member_count : 추가 비용을 적용할 인원 수. 없으면 0
   - max_member_limit   : 최대 공유 인원
   - platform_fee_rate  : 서비스 수수료율. 현재 3%
   - share_yn           : SpendOlive에서 공유방 생성 가능 여부
   - risk_level         : 공유 위험도. 현재 지원 OTT는 LOW로 설정
   - block_reason       : 공유 불가 사유. 지원 OTT는 NULL

   금액 예시:
   - Netflix: 17,000원 + 5,000원 * 2명 = 27,000원
   - Disney+: 13,900원 + 4,000원 * 1명 = 17,900원
   - 1인 결제 금액은 Java에서 default_price / max_member_limit 후 3% 수수료를 더해 계산합니다.
   ========================================================= */
INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'Netflix',
    27000,
    '프리미엄',
    17000,
    5000,
    2,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'Disney+',
    17900,
    '프리미엄',
    13900,
    4000,
    1,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'TVING',
    22000,
    '프리미엄',
    17000,
    5000,
    1,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'Wavve',
    13900,
    '프리미엄',
    13900,
    0,
    0,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'Watcha',
    12900,
    '프리미엄',
    12900,
    0,
    0,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

INSERT INTO ott_service_tb(
    service_name,
    default_price,
    fixed_plan_name,
    base_price,
    extra_member_fee,
    extra_member_count,
    max_member_limit,
    platform_fee_rate,
    share_yn,
    risk_level,
    block_reason
) VALUES (
    'Laftel',
    14900,
    '프리미엄',
    14900,
    0,
    0,
    4,
    3,
    'Y',
    'LOW',
    NULL
);

COMMIT;
