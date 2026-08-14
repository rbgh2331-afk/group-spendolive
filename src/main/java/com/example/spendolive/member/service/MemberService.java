package com.example.spendolive.member.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.domain.MemberTranVO;
import com.example.spendolive.member.domain.MemberVO;

public interface MemberService {
    List<MemberAccountVO> getAccountById(String id) throws Exception;

    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       담당자가 만든 카드 Repository 조회를 마이페이지 Service에서 사용할 수 있게 연결한다.
       ========================================================= */
    List<MemberCardVO> getCardById(String id) throws Exception;

    /* 마이페이지 계좌 목록에서 계좌 제목을 수정한다. */
    void updateAccountName(String id, int accountIdx, String accountName) throws Exception;

    // 마이페이지에서 선택한 계좌를 주계좌로 변경한다.
    void updatePrimaryAccount(String id, int accountIdx) throws Exception;

    // 마이페이지에서 선택한 계좌의 거래내역을 최신순으로 조회한다.
    List<MemberTranVO> getTransactionsByAccount(String id, int accountIdx) throws Exception;

    void addMember(MemberVO memberVO) throws Exception;

    String sendVerificationEmail(String toEmail) throws Exception;

    String sendSmsVerification(String toNumber) throws Exception;

    boolean checkId(String id) throws Exception;

    Map<String, String> getKakaoUserInfo(String code) throws Exception;

    boolean checkEmail(String email) throws Exception;

    boolean checkPhone(String phone) throws Exception;

    void registerOpenBankingToken(String code,
                                  String userId,
                                  HttpHeaders headers,
                                  ResponseEntity<Map> response,
                                  MemberVO memberVO) throws Exception;

    void registerOpenBankingIntegratedToken(MemberVO memberVO,
                                            MemberAccountVO accountVO) throws Exception;

    // 마이페이지에서 로그인한 회원 정보를 조회하고 수정한다.
    MemberVO getMemberById(String id) throws Exception;

    void updateMyInfo(MemberVO memberVO, String newPassword) throws Exception;
    boolean matchesPassword(String rawPassword, String encodedPassword);

    // 로그인 페이지의 아이디·비밀번호 찾기 기능에서 사용한다.
    String findIdByPhone(String phone) throws Exception;

    boolean existsActiveId(String id) throws Exception;

    boolean existsActiveMemberByIdAndPhone(String id, String phone) throws Exception;

    void updatePasswordById(String id, String newPassword) throws Exception;
    void updatePrimaryCard(String id, int cardIdx) throws Exception;

    // 마이페이지에서 카드 표시 이름을 수정한다.
    void updateCardName(String id, int cardIdx, String cardName) throws Exception;
    public void deleteCard(int card_idx,String id) throws Exception;
}
