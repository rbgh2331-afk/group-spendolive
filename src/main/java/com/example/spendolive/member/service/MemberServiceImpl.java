package com.example.spendolive.member.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.domain.MemberTranVO;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.exception.MemberProcessException;
import com.example.spendolive.member.repository.MemberRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;

import tools.jackson.databind.ObjectMapper;

@Service
public class MemberServiceImpl implements MemberService {
    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    private final JavaMailSender mailSender;

    public MemberServiceImpl(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    @Value("${kakao.client.id}")
    private String restApiKey;

    @Value("${kakao.redirect.uri}")
    private String redirectUri;

    @Value("${openbanking.client-id}")
    private String openbankingclientId;

    @Value("${openbanking.redirect-uri}")
    private String openbankingredirectUri;

    @Value("${openbanking.client-secret}")
    private String openbankingclientSecret;

    @Value("${solapi.api-key}")
    private String solapiapikey;

    @Value("${solapi.secret-key}")
    private String solapisecretkey;
    @Value("${openbanking.code}")
    private String useCode;
    @Value("${openbanking.integrated-redirect-uri}")
    private String openbankingIntegratedredirectUri;

    @Override
    public void addMember(MemberVO memberVO) throws Exception {
        String rawPassword = memberVO.getPassword();
        if (rawPassword == null || rawPassword.equals("")) {
            throw new RuntimeException("비밀번호를 입력해 주세요");
        }
        String encodedPassword = passwordEncoder.encode(rawPassword);
        memberVO.setPassword(encodedPassword);
        memberRepository.insertNewMember(memberVO);
    }

    @Override
    public String sendVerificationEmail(String toEmail) throws Exception {

        String verificationCode = String.valueOf(100000 + new Random().nextInt(900000));

        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
            java.util.Properties props =
                    ((org.springframework.mail.javamail.JavaMailSenderImpl) mailSender)
                            .getJavaMailProperties();
            props.put("mail.smtp.localhost", "127.0.0.1");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("rbgh2331@gmail.com");
        message.setTo(toEmail);
        message.setSubject("[SpendOlive] 회원가입 인증번호 안내");
        message.setText(
                "안녕하세요. SpendOlive입니다.\n\n"
                        + "회원가입을 완료하기 위한 인증번호는 [ "
                        + verificationCode
                        + " ] 입니다.\n"
                        + "타인에게 노출되지 않도록 주의해 주세요."
        );

        try {
            mailSender.send(message);
            return verificationCode;
        } catch (Exception e) {
            log.error("{}", "이메일 발송 에러: " + e.getMessage(), e);
            throw new RuntimeException("이메일 전송 중 에러 발생", e);
        }

    }

    @Override
    public String sendSmsVerification(String toNumber) throws Exception {
        String verificationCode = String.valueOf(100000 + new Random().nextInt(900000));

        DefaultMessageService messageService =  SolapiClient.INSTANCE.createInstance(solapiapikey, solapisecretkey);
        Message message = new Message();
        message.setFrom("01023310468");
        message.setTo(toNumber);
        message.setText("★ 발송 메세지: [SpendOlive] 가입 인증번호는 [" + verificationCode + "] 입니다.");

        try {
            messageService.send(message);
            return verificationCode;
        } catch (SolapiMessageNotReceivedException exception) {
        // 문자 발송 실패 상세 목록은 개인정보가 포함될 수 있어 그대로 출력하지 않는다
        log.error("문자 인증 발송에 실패했습니다.", exception);
        throw new RuntimeException("문자 전송 중 오류 발생", exception);
        } catch (Exception exception) {
        log.error("문자 인증 발송 중 오류가 발생했습니다.", exception);
        throw new RuntimeException("문자 전송 중 오류 발생", exception);
        }

    }
    @Override
    public boolean checkId(String id) {
        try{
        if (memberRepository.checkId(id)) {
            return true;
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean checkEmail(String email) {
        return memberRepository.checkEmail(email);
    }

    @Override
    public boolean checkPhone(String phone) {
        return memberRepository.checkPhone(phone);
    }

    @Override
    public Map<String, String> getKakaoUserInfo(String code) throws Exception {
        String accessToken = getAccessToken(code);
        return getUserInfo(accessToken);
    }

    private String getAccessToken(String code) throws Exception {
        String accessToken = "";
        URL url = new URL("https://kauth.kakao.com/oauth/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        conn.setDoOutput(true);

        try (BufferedWriter bw =
                     new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=").append(restApiKey);
            sb.append("&redirect_uri=").append(redirectUri);
            sb.append("&code=").append(code);
            bw.write(sb.toString());
            bw.flush();
        }

        if (conn.getResponseCode() == HttpStatus.OK.value()) {
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }

                JsonElement element = JsonParser.parseString(result.toString());
                accessToken = element.getAsJsonObject().get("access_token").getAsString();
            }
        } else {
            throw new Exception("카카오 토큰 발급 실패: 상태 코드 " + conn.getResponseCode());
        }

        return accessToken;
    }

    private Map<String, String> getUserInfo(String accessToken) throws Exception {
        Map<String, String> userInfoMap = new HashMap<>();
        URL url = new URL("https://kapi.kakao.com/v2/user/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        if (conn.getResponseCode() == HttpStatus.OK.value()) {
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line);
                }

                JsonObject userInfo = JsonParser.parseString(result.toString()).getAsJsonObject();
                String id = userInfo.get("id").getAsString();

                JsonObject properties = userInfo.getAsJsonObject("properties");
                String nickname =
                        properties != null && properties.get("nickname") != null
                                ? properties.get("nickname").getAsString()
                                : "카카오회원";

                userInfoMap.put("id", id);
                userInfoMap.put("nickname", nickname);
                userInfoMap.put("password", "KAKAO");
            }
        } else {
            throw new Exception("카카오 유저 정보 요청 실패: 상태 코드 " + conn.getResponseCode());
        }

        return userInfoMap;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrimaryCard(String id, int cardIdx) throws Exception {
        int updatedCount = memberRepository.updatePrimaryCard(id, cardIdx);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("주카드로 설정할 카드를 찾을 수 없습니다.");
        }
    }

    @Override
    public void updateCardName(String id, int cardIdx, String cardName) throws Exception {
        int updatedCount = memberRepository.updateCardName(id, cardIdx, cardName);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("이름을 수정할 카드를 찾을 수 없습니다.");
        }
    }
    @Override
    public MemberVO getMemberById(String id) throws Exception {
        return memberRepository.selectMemberById(id);
    }

    @Override
    public List<MemberAccountVO> getAccountById(String id) throws Exception {
        return memberRepository.selectAccountById(id);
    }

    /* =========================================================
       [마이페이지 계좌·카드 연결 추가 시작]
       1) 등록 카드 목록 조회
       2) 로그인 회원이 소유한 계좌의 제목 수정
       ========================================================= */
    @Override
    public List<MemberCardVO> getCardById(String id) throws Exception {
        return memberRepository.selectCardById(id);
    }

    @Override
    public void updateAccountName(String id, int accountIdx, String accountName) throws Exception {
        int updatedCount = memberRepository.updateAccountName(id, accountIdx, accountName);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("수정할 계좌를 찾을 수 없습니다.");
        }
    }

    // 선택한 계좌를 주계좌로 바꾸며 다른 계좌의 주계좌 상태도 함께 해제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrimaryAccount(String id, int accountIdx) throws Exception {
        int updatedCount = memberRepository.updatePrimaryAccount(id, accountIdx);
        if (updatedCount == 0) {
            throw new IllegalArgumentException("주계좌로 설정할 계좌를 찾을 수 없습니다.");
        }
    }

    // 회원 아이디와 계좌 번호를 함께 사용해 본인 계좌 거래내역만 조회
    @Override
    public List<MemberTranVO> getTransactionsByAccount(String id, int accountIdx) throws Exception {
        return memberRepository.selectTransactionsByAccount(id, accountIdx);
    }
    /* */

    @Override
    public void updateMyInfo(MemberVO memberVO, String newPassword) throws Exception {
        String encodedPassword = null;
        if (newPassword != null && !newPassword.isBlank()) {
            encodedPassword = passwordEncoder.encode(newPassword);
        }
        memberRepository.updateMyInfo(memberVO, encodedPassword);
    }

    @Override
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return rawPassword != null
                && encodedPassword != null
                && passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void registerOpenBankingToken(
            String code,
            String userId,
            HttpHeaders headers,
            ResponseEntity<Map> response,
            MemberVO memberVO
    ) throws Exception {
//토큰 발급
        String tokenUrl = "https://testapi.openbanking.or.kr/oauth/2.0/token";
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", openbankingclientId);
        params.add("client_secret", openbankingclientSecret);
        params.add("redirect_uri", openbankingredirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<String> tokenResponse =
                restTemplate.postForEntity(tokenUrl, request, String.class);

        if (tokenResponse.getStatusCode() != HttpStatus.OK || tokenResponse.getBody() == null) {
            throw new RuntimeException("금융결제원 토큰 발급 실패");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> resultMap = objectMapper.readValue(tokenResponse.getBody(), Map.class);

        String accessToken = (String) resultMap.get("access_token");
        String userSeqNo = (String) resultMap.get("user_seq_no");

        if (accessToken == null || userSeqNo == null) {
            throw new RuntimeException("금융결제원 토큰 응답에 필수 정보가 없습니다.");
        }

        // Access Token과 사용자 일련번호는 인증정보이므로 로그로 남기지 않는다
//계좌 조회
        String accountUrl =
                "https://testapi.openbanking.or.kr/v2.0/account/list?user_seq_no="
                        + userSeqNo
                        + "&include_account_num=Y";

        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        response = restTemplate.exchange(accountUrl, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("등록 계좌 조회에 실패했습니다.");
        }

        List<Map<String, Object>> resList =
                (List<Map<String, Object>>) response.getBody().get("res_list");

        if (resList == null || resList.isEmpty()) {
            throw new RuntimeException("등록된 계좌 정보가 없습니다.");
        }

//잔액 조회
            Map<String, Object> account = resList.get(0);
            String fintech_use_num = (String) account.get("fintech_use_num");
            String accountNum = (String) account.get("account_num_masked");
            String bankCode = (String) account.get("bank_code_std");
            String accountHolderName = (String) account.get("account_holder_name");
            String tranDtime =
            java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String uniqueNine = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
            String bankTranId = useCode + "U" + uniqueNine;
            String balanceUrl =
            "https://testapi.openbanking.or.kr/v2.0/account/balance/fintech_use_num"
                    + "?bank_tran_id=" + bankTranId
                    + "&fintech_use_num="
                    + fintech_use_num
                    + "&tran_dtime="
                    + tranDtime;

    HttpEntity<String> balanceEntity = new HttpEntity<>(headers);
    ResponseEntity<Map> balanceResponse =
            restTemplate.exchange(balanceUrl, HttpMethod.GET, balanceEntity, Map.class);

    int balance = 0;

    if (balanceResponse.getStatusCode() == HttpStatus.OK
            && balanceResponse.getBody() != null) {
        Object balanceAmt = balanceResponse.getBody().get("balance_amt");
        if (balanceAmt != null) {
            balance = Integer.parseInt(String.valueOf(balanceAmt));
        }
    }
            // 핀테크 이용번호, 은행코드, 계좌번호 등 금융 식별정보는 로그로 출력하지 않는다
            memberRepository.updateOpenBankingInfo(
                userId,
                accessToken,
                userSeqNo,
                fintech_use_num,
                bankCode,
                accountNum,
                balance,
                accountHolderName
        );
        memberRepository.updateMember_account_status(userId);

        // 토스 지급대행은 보안키 지원 문제로 현재 API 요청을 생략하는 구조
        /*
        paymentService.registerSubMall(
                userId,
                bankCode,
                accountNum,
                accountHolderName,
                memberVO
        );
         */
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    @SuppressWarnings("unchecked")
    public void registerOpenBankingIntegratedToken(MemberVO memberVO, MemberAccountVO accountVO) throws Exception {
        try {

            RestTemplate restTemplate = new RestTemplate();
            String fintech_use_num = accountVO.getFintech_use_num();
            String uniqueNine = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
            String bankTranId = useCode + "U" + uniqueNine;
            String from_date = accountVO.getFrom_date();
            String from_time = accountVO.getFrom_time();
            String to_date = accountVO.getTo_date();
            String to_time = accountVO.getTo_time();
            String accessToken = accountVO.getOpen_bank_token();
            HttpHeaders headers = new HttpHeaders();
            String accountUrl =
                    "https://testapi.openbanking.or.kr/v2.0/account/transaction_list/fin_num?bank_tran_id=" + bankTranId + "&fintech_use_num=" + fintech_use_num + "&inquiry_type=A"+ "&inquiry_base=T" + "&from_date="+from_date+"&from_time="+from_time+"&to_date="+to_date+"&to_time="+to_time+"&sort_order=D&tran_dtime="+to_date+to_time;
            headers.set("Authorization", "Bearer " + accessToken);
            HttpHeaders accountHeaders = new HttpHeaders();
            accountHeaders.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(accountHeaders);
            ResponseEntity<Map> response = restTemplate.exchange(accountUrl, HttpMethod.GET, entity, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new MemberProcessException("SELECT_FAILED", "거래내역 조회에 실패했습니다.");
        }
        response.getBody().get("res_list");
        String rsp_code = (String) response.getBody().get("rsp_code");
        }catch (Exception e) {
            throw new MemberProcessException("CONNECTION_FAILED", "서버 연결 실패했습니다.");

        }
         /*
         권한 문제로 임시데이터로 처리
        List<Map<String, Object>> resList = (List<Map<String, Object>>) response.getBody().get("res_list");

        if (resList == null || resList.isEmpty()) {
            return; // 결제내역 없음
        }
        for(Map<String, Object> account : resList){
        String tran_date = (String) account.get("tran_date");
        String tran_time = (String) account.get("tran_time");
        String inout_type = (String) account.get("inout_type");
        String tran_type = (String) account.get("tran_type");
        String print_content = (String) account.get("print_content");
        String tran_amt = (String) account.get("tran_amt");
        String after_balance_amt = (String) account.get("after_balance_amt");
        }
        */
        try {
        String[] inout_type = {"출금", "입금"};
        int[] tran_amt = {10000, 20000, 30000, 40000, 5000, 7000, 7500, 100000, 150000, 2000000};
        int amt_number = new java.util.Random().nextInt(10);
        int type_nember = new java.util.Random().nextInt(2);
        String id = memberVO.getId();
        int idx = accountVO.getAccount_idx();
        MemberTranVO tran = new MemberTranVO();
        String tranDtime =
            java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 출금은 음수, 입금은 양수로 통일해 현재 잔액과 거래 후 잔액을 계산
        int signedAmount = tran_amt[amt_number];
        if (inout_type[type_nember].equals("출금")) {
            signedAmount = signedAmount * -1;
        }

        int balanceAfter = accountVO.getBalance() + signedAmount;

        tran.setId(id);
        tran.setInout_type(inout_type[type_nember]);
        tran.setAccount_idx(idx);
        tran.setTran_amt(signedAmount);
        tran.setTran_date(tranDtime);
        tran.setBalance_after(Long.valueOf(balanceAfter));

        // 거래 당시 잔액을 거래 테이블에 저장한 뒤 계좌의 현재 잔액을 갱신
        memberRepository.inserttrandetail(tran);
        memberRepository.updatebalance(signedAmount, idx);
        accountVO.setBalance(balanceAfter);
        }catch (Exception e) {
            throw new MemberProcessException("INSERT_FAILED", "서버 오류.");

        }

    }

    @Override
    public String findIdByPhone(String phone) throws Exception {
        return memberRepository.findIdByPhone(phone);
    }

    @Override
    public boolean existsActiveId(String id) throws Exception {
        return memberRepository.existsActiveId(id);
    }

    @Override
    public boolean existsActiveMemberByIdAndPhone(String id, String phone) throws Exception {
        return memberRepository.existsActiveMemberByIdAndPhone(id, phone);
    }

    @Override
    public void updatePasswordById(String id, String newPassword) throws Exception {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        memberRepository.updatePasswordById(id, passwordEncoder.encode(newPassword));
    }
    @Override
    public void deleteCard(int card_idx, String id) throws Exception {
        try{
        memberRepository.deleteCard(card_idx, id);
        }catch (Exception e) {
            throw new MemberProcessException(
                    "DELETE_FAILED",
                    "카드 삭제에 실패하였습니다.",
                    e);
        }
    }
}
