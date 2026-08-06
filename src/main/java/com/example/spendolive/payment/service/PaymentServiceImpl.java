package com.example.spendolive.payment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.repository.MemberRepository;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.ott.repository.OttRepository;
import com.example.spendolive.ott.service.OttService;
import com.example.spendolive.payment.domain.*;
import com.example.spendolive.payment.exception.PaymentProcessException;
import com.example.spendolive.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService{
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private OttRepository ottRepository;
    @Autowired
    private OttService ottService;
    private final StringRedisTemplate redisTemplate;
    @Value("${openbanking.useorg-code}")
    private String useorgCode;

    @Value("${openbanking.cntr-account-num}")
    private String cntrAccountNum;

    @Value("${openbanking.cntr-bank-code}")
    private String cntrBankCode;

    @Value("${openbanking.cntr-account-holder}")
    private String cntrAccountHolder;
    @Value("${toss.secret-key}")
    private String secretKey;

    // 프로젝트에서 사용하는 플랫폼 수수료율입니다.
    private static final int PLATFORM_FEE_RATE = 3;

    // 같은 서버에서 동일 회원이 동일 방 결제를 동시에 요청하는 것을 차단합니다.
    private final Set<String> processingPayments = ConcurrentHashMap.newKeySet();
    private final Set<String> processingRefunds = ConcurrentHashMap.newKeySet();
    

    @Override
    @Transactional(rollbackFor = Exception.class) //금결원 출금이체 프로세스 (권한 문제로 홀딩)
    public boolean processWithdraw(SettlementPaymentVO paymentInfo, MemberVO memberInfo) throws Exception {
        /*
        // 1. 금결원 출금이체 API 주소
        String apiUrl = "https://testapi.openbanking.or.kr/v2.0/transfer/withdraw/fintech_use_num";

        // 2. RestTemplate 및 헤더 세팅 (Bearer 토큰 필수)
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // 출금이체는 JSON 바디로 요청
        headers.set("Authorization", "Bearer " + memberInfo.getOpen_bank_token());

        // 3. 거래고유번호(bank_tran_id) 생성 -> 이용기관코드(10) + U + 난수(9) = 총 20자리
        String uniqueId = UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
        String bankTranId = useorgCode + "U" + uniqueId;

        // 4. 요청 시간 생성 (YYYYMMDDHHMMSS)
        String tranDtime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 5. 금결원 규격에 맞는 JSON 바디 조립 (Map 활용)
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("bank_tran_id", bankTranId);
        bodyMap.put("cntr_account_type", "C");                  // N: 계좌 형식
        bodyMap.put("cntr_account_num", cntrAccountNum);        // 우리 수납 계좌
        bodyMap.put("dps_print_text", "SpendOlive입금");        // 우리 통장에 찍힐 문구
        
   
        bodyMap.put("fintech_use_num", "199000000000000000000001"); 
        
        bodyMap.put("wd_print_text", "스펜드올리브출금");          // 파티원 통장에 찍힐 문구
        bodyMap.put("tran_amt", paymentInfo.getTotal_amount()); // 출금할 금액 (이용료 + 수수료)
        bodyMap.put("tran_dtime", tranDtime);
        bodyMap.put("req_client_name", "홍길동");             // 파티원 이름
        bodyMap.put("req_client_num", memberInfo.getId());      // 파티원 ID
        bodyMap.put("req_client_fintech_use_num", "199000000000000000000001");
        bodyMap.put("recv_client_name", cntrAccountHolder);     // 수취인 성명 (나)
        bodyMap.put("recv_client_bank_code", cntrBankCode);     // 수취인 은행 코드 (내 은행)
        bodyMap.put("recv_client_account_num", cntrAccountNum); // 수취인 계좌 (내 계좌)

        // 6. API 요청 날리기
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bodyMap, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> resultMap = mapper.readValue(response.getBody(), Map.class);
            
            // 금결원 응답 코드 응답 (기본적으로 "A0000" 이 성공입니다)
            String rspCode = (String) resultMap.get("rsp_code");
            
            if ("A0000".equals(rspCode)) {
                // 🚀 [성공] 1단계: 팀원별 입금 장부(SettlementPayment) 상태를 PAID로 변경
                paymentInfo.setPayment_status("PAID");
                paymentInfo.setPaid_at(LocalDateTime.now());
                paymentRepository.updatePaymentStatus(paymentInfo); // 레포지토리에 반영

                // 🚀 [성공] 2단계: 먹튀 방지를 위해 에스크로(Escrow) 금고 테이블에 돈 묶어두기
                EscrowPayoutVO escrow = new EscrowPayoutVO();
                escrow.setSettlement_id(paymentInfo.getSettlement_id());
                // 방의 룸 ID와 방장 ID는 원래 룸 정보에서 꺼내와야 하므로 데이터 바인딩 필요
                escrow.setRoom_id(1); // 예시 ID
                escrow.setPayerId(paymentInfo.getId());
                escrow.setHostId("방장ID_조회필요");
                escrow.setAmount(paymentInfo.getBase_amount()); // 수수료 뺀 원금 보관
                escrow.setStatus("HELD"); // 보관 상태로 지정
                
                paymentRepository.insertEscrow(escrow); // 에스크로 인서트
                
                System.out.println("출금이체 및 장부 업데이트 완료! 파티원: " + paymentInfo.getId());
                return true;
            } else {
                System.out.println("금결원 거절 사유: " + resultMap.get("rsp_message"));
                return false;
            }
        }
     */   
        return false;
    }
      // 카드 등록 프로세스
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issueAndSaveBillingKey(String customerKey, String authKey, String userId) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        
        // 1. 최신 2024-06-01 버전 규격 엔드포인트 주소
        String url = "https://api.tosspayments.com/v1/billing/authorizations/issue";
        
        // 토스 규격대로 뒤에 콜론(:)을 붙이고 Base64로 인코딩
        String rawKey = secretKey.trim() + ":";
        String encodedSecretKey = Base64.getEncoder().encodeToString(rawKey.getBytes());

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedSecretKey); 
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 바디 설정
        Map<String, String> body = new HashMap<>();
        body.put("customerKey", customerKey);
        body.put("authKey", authKey);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            System.out.println("[토스 응답 바디] : " + response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                Map<String, Object> resBody = mapper.readValue(response.getBody(), Map.class);
                String billingKey = (String) resBody.get("billingKey"); 
                Map<String, Object> cardInfo = (Map<String, Object>) resBody.get("card");
                String card_num = null;
                String card_company = null;
                if (cardInfo != null) {
                    card_num = (String) cardInfo.get("number"); 
                    card_company = (String) cardInfo.get("issuerCode"); 
                }
                memberRepository.updateTossInfo(userId, card_num, card_company, billingKey);
                memberRepository.updateMember_card_status(userId);
            }
        } catch (Exception e) {
            System.out.println("[최종 에러 디버깅] : " + e.getMessage());
            throw new RuntimeException("토스 통신 실패: " + e.getMessage());
        }
    }
    /**
     * 결제 화면과 실제 승인 요청에서 공통으로 사용할 최신 금액을 계산합니다.
     */
    @Override
    public PaymentAmountDTO getPaymentAmount(int roomId) throws Exception {
        if (roomId <= 0) {
            throw new PaymentProcessException(
                    "INVALID_PAYMENT_INFO",
                    "결제할 방 정보가 올바르지 않습니다.");
        }

        OttSettlementDTO settlementInfo = paymentRepository.settlementByroomId(roomId);
        OttRoomDTO roomInfo = selectRoomByRoomId(roomId);

        if (settlementInfo == null || roomInfo == null) {
            throw new PaymentProcessException(
                    "INVALID_PAYMENT_INFO",
                    "해당 방의 결제 정보를 찾을 수 없습니다.");
        }

        if ("CLOSE_REQUESTED".equals(roomInfo.getStatus())
                || "CLOSED".equals(roomInfo.getStatus())) {
            throw new PaymentProcessException(
                    "PAYMENT_NOT_ALLOWED",
                    "종료되었거나 종료 예정인 방은 결제할 수 없습니다.");
        }

        int memberLimit = settlementInfo.getMember_limit();
        Integer totalPrice = settlementInfo.getTotal_price();

        if (memberLimit <= 0 || totalPrice == null || totalPrice <= 0) {
            throw new PaymentProcessException(
                    "INVALID_PAYMENT_INFO",
                    "결제 금액 또는 모집 인원 정보가 올바르지 않습니다.");
        }

        // 나눗셈으로 소수점이 생기면 부족 결제를 막기 위해 원 단위로 올림합니다.
        int baseAmount = (int) Math.ceil(totalPrice / (double) memberLimit);
        int feeAmount = (int) Math.round(baseAmount * (PLATFORM_FEE_RATE / 100.0));
        int totalAmount = baseAmount + feeAmount;
        //자동결제일 계산 로직
        int billingDay = roomInfo.getBilling_day() == null
                ? 1
                : roomInfo.getBilling_day();
        int automaticPaymentDay = billingDay - 10;
        if (automaticPaymentDay <= 0) {
            automaticPaymentDay += 30;
        }

        return new PaymentAmountDTO(
                roomId,
                settlementInfo.getSettlement_id().intValue(),
                roomInfo.getRoom_name(),
                settlementInfo.getHost_login_id(),
                memberLimit,
                baseAmount,
                PLATFORM_FEE_RATE,
                feeAmount,
                totalAmount,
                automaticPaymentDay);
    }

    /**
     * 중복 결제와 방 상태를 확인한 뒤 Toss 자동결제를 실행합니다.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentAmountDTO executeRoomPayment(String userId, int roomId) throws Exception {
        if (userId == null || userId.isBlank()) {
            throw new PaymentProcessException(
                    "LOGIN_REQUIRED",
                    "로그인이 필요합니다.");
        }

        String processingKey = createProcessingKey(userId, roomId);
        if (!processingPayments.add(processingKey)) {
            throw new PaymentProcessException(
                    "PAYMENT_PROCESSING",
                    "이미 결제를 처리하고 있습니다. 잠시만 기다려주세요.");
        }

        try {
            String currentPaymentStatus =
                    paymentRepository.selectPaymentStatusByRoomAndMember(userId, roomId);

            if (isPaidStatus(currentPaymentStatus)) {
                throw new PaymentProcessException(
                        "PAID",
                        "이미 결제가 완료된 방입니다. 참여한 방으로 이동합니다.");
            }

            if ("CANCELLED".equals(currentPaymentStatus)
                    || "EXPIRED".equals(currentPaymentStatus)
                    || "REFUNDED".equals(currentPaymentStatus)) {
                throw new PaymentProcessException(
                        "PAYMENT_NOT_ALLOWED",
                        "현재 결제 상태에서는 다시 결제할 수 없습니다.");
            }

            PaymentAmountDTO paymentAmount = getPaymentAmount(roomId);

            if (userId.equals(paymentAmount.getHostLoginId())) {
                throw new PaymentProcessException(
                        "HOST_CANNOT_PAY",
                        "방장은 자신이 만든 방에 참여 결제를 할 수 없습니다.");
            }
           
       
            String redisKey = "room:" + roomId + ":seats";
            if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                redisKey, 
                String.valueOf(Math.max(paymentAmount.getMemberLimit() - ottRepository.countActiveRoomMembers((long) roomId), 0))))) {
                    redisTemplate.expire(redisKey, 1, TimeUnit.HOURS);
            }
            Long remainingSeats = redisTemplate.opsForValue().decrement(redisKey);
            if (remainingSeats == null ||remainingSeats < 0) {
                // 자리가 없으므로 내가 깎은 1을 다시 원복하고 튕겨냄
                redisTemplate.opsForValue().increment(redisKey);
                throw new PaymentProcessException("ROOM_FULL", "이미 정원이 찬 방입니다.");
            }
            try{
            executeAutomaticPayment(
                    userId,
                    paymentAmount.getTotalAmount(),
                    roomId,
                    paymentAmount.getFeeAmount(),
                    paymentAmount.getBaseAmount(),
                    paymentAmount.getSettlementId(),
                    paymentAmount.getHostLoginId());
            ottService.completePaidRoomEntry((long) roomId, userId);
            return paymentAmount;
            }catch(Exception e){
                redisTemplate.opsForValue().increment(redisKey);
                e.printStackTrace();
                if (e instanceof PaymentProcessException) {
                    throw e;
                }
                throw new PaymentProcessException("PAYMENT_FAILED", "결제 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        } finally {
            
            processingPayments.remove(processingKey);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeRoomRefund(SettlementPaymentVO payment) throws Exception {
        int payment_id = payment.getPayment_id();
        String paymentkey = payment.getPaymentKey();
        if (!processingRefunds.add(paymentkey)) {
            throw new PaymentProcessException(
                    "REFUND_PROCESSING",
                    "이미 취소를 처리하고 있습니다. 잠시만 기다려주세요.");
        }

        try {
            String currentPaymentStatus =paymentRepository.selectRefundStatus(payment_id);

            if (isPaidStatus(currentPaymentStatus)) {
                throw new PaymentProcessException(
                        "REFUNDED",
                        "이미 취소가 완료된 건입니다.");
            }

            if ("FAILED".equals(currentPaymentStatus)) {
                throw new PaymentProcessException(
                        "REFUND_NOT_ALLOWED",
                        "현재 상태에서는 다시 취소할 수 없습니다.");
            }
            try{
                if(!cancelApprovedPayment(paymentkey)){
                String message = "결제 정보 저장에 실패해 Toss 승인을 취소했습니다.";
                throw new PaymentProcessException(
                        "REFUND_FAILED",
                        message,
                        null);
                }
            try{
            updatePaymentstatusRefund(payment);
            }catch(Exception a){
                throw new PaymentProcessException("REFUND_FAILED", "결제 취소 후 서버 오류가 발생했습니다 송금 결제 내역을 확인해 주세요 " + a.getMessage());
            }
            }catch(Exception e){
                e.printStackTrace();
                if (e instanceof PaymentProcessException) {
                    throw e;
                }
                throw new PaymentProcessException("REFUND_FAILED", "결제 취소 처리 중 오류가 발생했습니다: " + e.getMessage());
            }
        } finally {
            processingRefunds.remove(paymentkey);
        }
    }
    /** Ajax 응답이 끊겼을 때 DB와 서버 처리 상태를 다시 확인합니다. */
    @Override
    public String getRoomPaymentStatus(String userId, int roomId) throws Exception {
        if (userId == null || userId.isBlank() || roomId <= 0) {
            return "UNPAID";
        }

        if (processingPayments.contains(createProcessingKey(userId, roomId))) {
            return "PROCESSING";
        }

        String paymentStatus =
                paymentRepository.selectPaymentStatusByRoomAndMember(userId, roomId);
        return paymentStatus == null ? "UNPAID" : paymentStatus;
    }

    // 결제 프로세스 검토 완료
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAutomaticPayment(
            String userId,
            int amount,
            int roomId,
            int fee,
            int base,
            int settlementId,
            String hostId) throws Exception {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(
                0,
                new org.springframework.http.converter.StringHttpMessageConverter(
                        java.nio.charset.StandardCharsets.UTF_8));

        List<MemberCardVO> cardVoList = memberRepository.selectCardById(userId);
        String billingKey = null;
        if (cardVoList != null) {
            for (MemberCardVO card : cardVoList) {
                if (card == null
                        || card.getBilling_key() == null
                        || card.getBilling_key().isBlank()) {
                    continue;
                }

                // 주 결제 카드가 지정되어 있으면 가장 먼저 사용합니다.
                if ("YES".equals(card.getStatus())) {
                    billingKey = card.getBilling_key();
                    break;
                }
            }
        }

        if (billingKey == null || billingKey.isBlank()) {
            throw new PaymentProcessException(
                    "CARD_REQUIRED",
                    "등록된 주 결제 카드가 없습니다. 카드를 먼저 등록해주세요.");
        }

        String url = "https://api.tosspayments.com/v1/billing/" + billingKey;
        String rawKey = secretKey.trim() + ":";
        String encodedSecretKey = Base64.getEncoder().encodeToString(rawKey.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String orderId = "SPENDOLIVE_"
                + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        Map<String, Object> body = new HashMap<>();
        body.put("customerKey", userId);
        body.put("amount", amount);
        body.put("orderId", orderId);
        body.put("orderName", "SpendOlive OTT 정산");

        tools.jackson.databind.ObjectMapper jsonMapper =
                new tools.jackson.databind.ObjectMapper();
        String jsonBody = jsonMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK
                    || response.getBody() == null) {
                throw new PaymentProcessException(
                        "PAYMENT_FAILED",
                        "Toss 결제 승인 응답을 받지 못했습니다.");
            }

            Map<String, Object> resBody = jsonMapper.readValue(response.getBody(), Map.class);
            Map<String, Object> cardInfo = (Map<String, Object>) resBody.get("card");

            String paymentKey = (String) resBody.get("paymentKey");
            String status = (String) resBody.get("status");
            String responseOrderId = (String) resBody.get("orderId");
            int totalAmount = ((Number) resBody.get("totalAmount")).intValue();
            String approvedAtStr = (String) resBody.get("approvedAt");
            LocalDateTime approvedAt =
                    OffsetDateTime.parse(approvedAtStr).toLocalDateTime();

            String cardNumber = cardInfo == null
                    ? null
                    : (String) cardInfo.get("number");
            String cardCompany = cardInfo == null
                    ? null
                    : (String) cardInfo.get("issuerCode");

            if (!"DONE".equals(status)) {
                throw new PaymentProcessException(
                        "PAYMENT_FAILED",
                        "결제가 완료되지 않은 상태입니다: " + status);
            }

            if (totalAmount != amount) {
                cancelApprovedPayment(paymentKey);
                throw new PaymentProcessException(
                        "PAYMENT_AMOUNT_MISMATCH",
                        "승인 금액이 달라 결제를 즉시 취소했습니다.");
            }

            SettlementPaymentVO paymentInfo = new SettlementPaymentVO();
            paymentInfo.setId(userId);
            paymentInfo.setOrderId(responseOrderId);
            paymentInfo.setPaymentKey(paymentKey);
            paymentInfo.setTotal_amount(totalAmount);
            paymentInfo.setCard_number(cardNumber);
            paymentInfo.setCard_company(cardCompany);
            paymentInfo.setPaid_at(approvedAt);
            paymentInfo.setPayment_status("PAID");
            paymentInfo.setBase_amount(base);
            paymentInfo.setFee_amount(fee);
            paymentInfo.setFee_rate((double) PLATFORM_FEE_RATE);
            paymentInfo.setMemo("OTT 사용료");
            paymentInfo.setSettlement_id(settlementId);

            EscrowPayoutVO escrowInfo = new EscrowPayoutVO();
            escrowInfo.setAmount(base);
            escrowInfo.setCreated_at(approvedAt);
            escrowInfo.setHost_id(hostId);
            escrowInfo.setPayer_id(userId);
            escrowInfo.setRoom_id(roomId);
            escrowInfo.setSettlement_id(settlementId);
            escrowInfo.setStatus("HELD");

            PlatformRevenueVO revenueInfo = new PlatformRevenueVO();
            revenueInfo.setBase_amount(base);
            revenueInfo.setCreated_at(approvedAt);
            revenueInfo.setFee_amount(fee);
            revenueInfo.setFee_rate((double) PLATFORM_FEE_RATE);
            revenueInfo.setPayer_id(userId);
            revenueInfo.setRoom_id(roomId);
            revenueInfo.setSettlement_id(settlementId);
            revenueInfo.setStatus("EARNED");

            try {
                // Toss 승인 후 DB 저장에 실패하면 아래에서 즉시 승인 취소를 요청합니다.
                paymentRepository.updatePaymentStatus(paymentInfo);
                paymentRepository.insertEscrow(escrowInfo);
                paymentRepository.insertPlatfoem_Revenue(revenueInfo);
                paymentRepository.updatSettlementroommemberStatus(roomId, userId);
            } catch (Exception databaseException) {
                boolean cancelled = cancelApprovedPayment(paymentKey);

                String message = cancelled
                        ? "결제 정보 저장에 실패해 Toss 승인을 취소했습니다."
                        : "결제 정보 저장과 Toss 승인 취소에 실패했습니다. 관리자 확인이 필요합니다.";

                throw new PaymentProcessException(
                        "PAYMENT_SAVE_FAILED",
                        message,
                        databaseException);
            }

            

        } catch (PaymentProcessException e) {
            throw e;

        } catch (HttpClientErrorException e) {
            System.err.println("Toss 결제 거절 응답: " + e.getResponseBodyAsString());
            throw new PaymentProcessException(
                    "PAYMENT_FAILED",
                    "카드 승인에 실패했습니다. 카드 상태와 한도를 확인해주세요.",
                    e);

        } catch (Exception e) {
            System.err.println("자동결제 승인 실패: " + e.getMessage());
            throw new PaymentProcessException(
                    "PAYMENT_FAILED",
                    "자동결제 시스템 오류로 승인이 실패했습니다.",
                    e);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SettlementPaymentVO> selectpaymentAll() throws Exception { 
        return paymentRepository.selectsettlement_paymentAll();
       
    }
    @Override
    public boolean cancelApprovedPayment(String paymentKey) throws Exception{
        String cancelUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";
        RestTemplate restTemplate = new RestTemplate();
        String myRealSecretKey = secretKey; 
        String rawKey = myRealSecretKey.trim() + ":";
        String encodedSecretKey = Base64.getEncoder().encodeToString(rawKey.getBytes());
        Map<String, Object> cancelBody = new HashMap<>();
        cancelBody.put("cancelReason", "서버 오류로 인한 취소");//취소 이유
        tools.jackson.databind.ObjectMapper jsonMapper = new tools.jackson.databind.ObjectMapper();
        String cancelJson = jsonMapper.writeValueAsString(cancelBody);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> cancelEntity = new HttpEntity<>(cancelJson, headers);

 
        try{
          
            ResponseEntity<String> cancelResponse = restTemplate.postForEntity(cancelUrl, cancelEntity, String.class);
            if (cancelResponse.getStatusCode() == HttpStatus.OK && cancelResponse.getBody() != null) {
                tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                Map<String, Object> resBody = mapper.readValue(cancelResponse.getBody(), Map.class);
                String canceledAtStr = (String) resBody.get("approvedAt");
                System.out.println("결제 취소 성공 확인 paymentKey: " + paymentKey + " | 승인시간: " + canceledAtStr);
            }
            return cancelResponse.getStatusCode() == HttpStatus.OK;
        } catch (Exception cancelException) {
            System.err.println("Toss 결제 취소 실패: " + cancelException.getMessage());
            return false;
        }
    }
    /** Toss 승인은 끝났지만 DB 저장이 실패했을 때 결제를 즉시 취소합니다.  검토 완료*/
   
    // 서버 내 OTT방 결제 진행중인지 확인 변수 생성 안에 값이 있다면 진행중인 것
    private String createProcessingKey(String userId, int roomId) {
        return userId + "#" + roomId;
    }
    // 결제가 완료 된 상태를 검증 하기 위한 유효성 평가 메서드 중복 결제를 막기 위한 메서드
    private boolean isPaidStatus(String paymentStatus) {
        return "PAID".equals(paymentStatus)
                || "CONFIRMED".equals(paymentStatus);
    }
    
    @Override
    public SettlementPaymentVO getSettlement_PaymentByRoomId(String userId, int room_id) throws Exception {
        return paymentRepository.settlement_paymentByroomId(userId, room_id);
    }
    @Override
    public OttSettlementDTO selectMySettlements(int room_id)  throws Exception{
        return paymentRepository.settlementByroomId(room_id);
    }
    //토스 정산금 보낼 셀러 등록 프로세스(권한 문제로 보류)
    @Override
    @Transactional
    public void registerSubMall(String userId, String bankCode, String accNum, String holderName, MemberVO memberVO) {
    /*
    // 1. v1 정산 API 주소
    String TOSS_API_URL = "https://api.tosspayments.com/v1/payouts/sub-malls"; 
    
    try {
        
        RestTemplate restTemplate = new RestTemplate();
        String name = memberVO.getMember_name();
        
        String cleanNum = accNum.replace("***", "000").replace("-", ""); 

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("subMallId", "SELLER_" + userId);   // 고유 식별자
        requestBody.put("companyName", name);               // 상호명
        requestBody.put("representativeName", name);        // 대표자명
        requestBody.put("identityNumber", "0001013111111"); // 예시: 주민번호 앞자리6자리 + 뒷자리7자리 총 13자리
        requestBody.put("type", "INDIVIDUAL");
        Map<String, String> accountInfo = new HashMap<>();
        accountInfo.put("bank", "국민"); 
        accountInfo.put("accountNumber", cleanNum);
        accountInfo.put("holderName", name);
        requestBody.put("account", accountInfo);

        // 헤더 세팅 
        HttpHeaders headers = new HttpHeaders();
        String rawKey = secretKey.trim() + ":";
        String encodedSecretKey = Base64.getEncoder().encodeToString(rawKey.getBytes());
        
        headers.set("Authorization", "Basic " + encodedSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON); // text/plain 대신 무조건 JSON!


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            TOSS_API_URL,
            HttpMethod.POST,
            entity,
            String.class
        );
        
        
        if (response.getStatusCode() == HttpStatus.OK) {
            tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
            Map<String, Object> resBody = mapper.readValue(response.getBody(), Map.class);
            

            SellerAccountVO seller = SellerAccountVO.builder()
                    .member_id(userId)
                    .bank_name((String) resBody.get("bank"))
                    .account_number((String) resBody.get("accountNumber"))
                    .traceId(UUID.randomUUID().toString()) // v1은 traceId를 안 주므로 내부용 임의 생성
                    .build();
                    
            try{
            String traceId ="1123412312321413243142sadsadadsdsadasd";        
            SellerAccountVO sellerInfo = SellerAccountVO.builder()
            .member_id(userId)
            .bank_name(bankCode)
            .account_number(accNum)
            .traceId(traceId)
            .build();
            paymentRepository.insertSeller(seller);
            }catch(Exception e){
                //취소 api요청
                throw new RuntimeException("서버 오류 로 송금을 취소합니다");
            }
        }

    } catch (HttpClientErrorException e) {

        throw new RuntimeException("토스 서브몰 등록 중 API 검증 오류 발생");
    } catch (Exception e) {

        throw new RuntimeException("토스 서브몰 등록 중 시스템 오류 발생");
    } */
}

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OttRoomDTO> selectTodaysettlement(String status) throws Exception {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int endday = YearMonth.from(today).lengthOfMonth();
        if(day == endday && endday<31){
            endday =31;
      
        }else{
            endday = day;
            
        }
        paymentRepository.updatSettlementStatusYET(endday); // 빌링데이가 지난 데이터는 다시 상태를YET으로 
        paymentRepository.updateReadyfromYet(day,endday); //오늘 정산금 송금 할 내역들은 YET에서 READY로
 
            
        return paymentRepository.selectTodaysettlement(day,endday,status);
       
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OttRoomMemberDTO> selectTodaysettlementmember(String status) throws Exception {
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();
        int endday = YearMonth.from(today).lengthOfMonth();
       if(day == endday && endday<31){
            endday =31;
      
        }else{
            endday = day;
            
        }

            paymentRepository.updatSettlementStatusYETroommember(day); // 페이데이가 지난 데이터는 다시 상태를YET으로 
            paymentRepository.updateReadyfromYettoroommember(day,endday); //오늘 정산금 정산 할 내역들은 YET에서 READY로
            return paymentRepository.selectTodaysettlementMember(day,endday,status);
        }
    
    //정산금 송금 후 상태값 변경 프로세스
    @Override
    @Transactional
    public String updateExcrow(int room_id) throws Exception {


        try{
        //추후 사업자 등록 후 토스지급대행 , 금결원 출금이체 api 사용 메서드      
        }catch(Exception e){

            //api사용중 오류 시 바로 예외처리 db저장 x
            
            return "송금중 문제가 생겼습니다. ";
            }
        try{
            paymentRepository.updateEscrowStatus(room_id);
            paymentRepository.updatSettlementStatus(room_id);
            String status = "ACTIVE";
            Long roomid = Long.valueOf(room_id);
            ottRepository.updateRoomStatus(roomid, status);
        }catch(Exception e){
            throw new RuntimeException("DB 업데이트 실패: 송금 완료 후 서버 쪽에서 오류가 생겼습니다.");
        }
        return "송금을 정상적으로 완료 하였습니다.";
    }
    
    @Override
    public void updateTodaysettlementroommemberlate(int roomId,String userId,int late_day) throws Exception {
        try{
        paymentRepository.updateTodaysettlementroommemberlate(roomId, userId,late_day);
        }catch(Exception e){
            throw new PaymentProcessException(
                    "LATEDAY_FAILED",
                    "정산 연기에 실패하였습니다.",
                    e);
        }
    }
    @Override
    public OttRoomDTO selectRoomByRoomId(int roomId) throws Exception {
        String roomidStr = String.valueOf(roomId);
        Long roomid = Long.parseLong(roomidStr);
        return ottRepository.selectRoom(roomid);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentstatusRefund(SettlementPaymentVO payment) throws Exception {
        int payment_id = payment.getPayment_id();
        
        int refund_amount = payment.getTotal_amount();
        String id = payment.getId();
        int settlement_id = payment.getSettlement_id();
        LocalDateTime created_at = LocalDateTime.now();
        String paymentkey = payment.getPaymentKey();
        paymentRepository.updatePaymentstatusRefund(payment_id);
        SettlementRefundVO refund = new SettlementRefundVO();
        refund.setMember_login_id(id);
        refund.setCompleted_at(created_at);
        refund.setPayment_id(payment_id);
        refund.setRefund_amount(refund_amount);
        refund.setRefund_reason("PAYMENT_CANCEL");
        refund.setRefund_status("COMPLETED");
        refund.setSettlement_id(settlement_id);
        paymentRepository.insertRefund(refund);
    }
    @Override
    public String selectEscrowStatus(int room_id, String host_id) {
        try{
        if (paymentRepository.selectEscrowStatus(room_id,host_id)) {
            return "PAID";
            }
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        return "UNPAID";
    }
    @Override
    public String selectRefundStatus(int payment_id) {
        try{
            String status = paymentRepository.selectRefundStatus(payment_id);
            return status;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
        
    }
    
}    

