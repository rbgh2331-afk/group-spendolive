package com.example.spendolive.member.repository;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.domain.MemberTranVO;
import com.example.spendolive.member.domain.MemberVO;

public interface MemberRepository {

    public void insertNewMember(MemberVO memberVO) throws DataAccessException;
    public boolean checkId(String id) throws DataAccessException;
    public boolean checkPhone(String phone) throws DataAccessException;
    public boolean checkEmail(String email) throws DataAccessException;
    public void updateOpenBankingInfo(String userId, String accessToken, String userSeqNo, String fintech_num, String bank_code, String account_num, int balance, String accountHolderNam)throws DataAccessException;
    public void updateTossInfo(String userId, String card_num, String card_company, String billingkey)throws DataAccessException;
    public MemberVO selectMemberById(String id) throws DataAccessException;
    public void updateMyInfo(MemberVO memberVO, String newPassword) throws DataAccessException;
    public void updateMember_account_status(String id)throws DataAccessException;
    public void updateMember_card_status(String id)throws DataAccessException;
    public List<MemberCardVO> selectCardById(String userId)throws DataAccessException;
    public List<MemberAccountVO> selectAccountById(String userId)throws DataAccessException;
    public int updatePrimaryCard(String userId, int cardIdx) throws DataAccessException;

    // 로그인 회원이 소유한 카드의 표시 이름만 수정
    public int updateCardName(String userId, int cardIdx, String cardName) throws DataAccessException;
    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       계좌 제목 수정 요청을 Repository 구현체로 전달하는 메서드이다.
       userId 조건을 함께 사용해 다른 회원의 계좌가 수정되지 않도록 한다.
       ========================================================= */
    public int updateAccountName(String userId, int accountIdx, String accountName)throws DataAccessException;

    // 선택한 계좌만 주계좌(YES)로 바꾸고 같은 회원의 나머지 계좌는 NO로 변경
    public int updatePrimaryAccount(String userId, int accountIdx)throws DataAccessException;
    public void applyWarningPenalty(String userId, int penaltyDays)throws DataAccessException;
    public List<MemberVO> selectMemberAll() throws DataAccessException;

    /* =========================================================
       아이디/비밀번호 찾기 Repository 메서드
       ---------------------------------------------------------
       member_tb를 직접 조회/수정하는 구간이다.
       휴대폰 번호는 하이픈 유무와 상관없이 비교하기 위해 구현체에서 숫자만 남겨 비교한다.
       ========================================================= */
    // 휴대폰 번호로 ACTIVE 회원의 id 조회
    public String findIdByPhone(String phone) throws DataAccessException;

    // 입력한 id가 ACTIVE 회원으로 존재하는지 확인
    public boolean existsActiveId(String id) throws DataAccessException;

    // id + 휴대폰 번호가 같은 ACTIVE 회원 정보인지 확인
    public boolean existsActiveMemberByIdAndPhone(String id, String phone) throws DataAccessException;

    // 인증 완료 후 비밀번호 변경
    public void updatePasswordById(String id, String newPassword) throws DataAccessException;
    public void inserttrandetail(MemberTranVO tran)throws DataAccessException;
    public void updatebalance(int tran_amt, int idx) throws DataAccessException;

    // 로그인 회원이 소유한 특정 계좌의 거래내역을 최신순으로 조회
    public List<MemberTranVO> selectTransactionsByAccount(String userId, int accountIdx)
            throws DataAccessException;
    public void deleteCard(int card_idx, String id)throws DataAccessException;
}
