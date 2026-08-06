package com.example.spendolive.payment.service;

import java.util.List;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.payment.domain.PaymentAmountDTO;
import com.example.spendolive.payment.domain.SettlementPaymentVO;

public interface PaymentService {
    // public boolean processWithdraw(SettlementPaymentVO paymentInfo, MemberVO memberInfo) throws Exception;
    List<OttRoomDTO> selectTodaysettlement(String status) throws Exception;

    void issueAndSaveBillingKey(String customerKey, String authKey, String userId) throws Exception;

    void executeAutomaticPayment(
            String userId,
            int total,
            int roomId,
            int fee,
            int base,
            int settlementId,
            String hostId) throws Exception;

    /** 결제 화면과 실제 결제에 공통으로 사용할 최신 금액을 서버에서 계산합니다. */
    PaymentAmountDTO getPaymentAmount(int roomId) throws Exception;

    /** 중복 결제를 막은 뒤 Toss 자동결제를 실행합니다. */
    PaymentAmountDTO executeRoomPayment(String userId, int roomId) throws Exception;

    /** Ajax 통신이 끊긴 경우 현재 결제 상태를 다시 확인합니다. */
    String getRoomPaymentStatus(String userId, int roomId) throws Exception;

    SettlementPaymentVO getSettlement_PaymentByRoomId(String userId, int roomId) throws Exception;

    OttSettlementDTO selectMySettlements(int roomId) throws Exception;

    void registerSubMall(
            String userId,
            String bankCode,
            String accNum,
            String holderName,
            MemberVO memberVO) throws Exception;

    String updateExcrow(int roomId) throws Exception;

    List<OttRoomMemberDTO> selectTodaysettlementmember(String status) throws Exception;

    boolean processWithdraw(SettlementPaymentVO paymentInfo, MemberVO memberInfo) throws Exception;

    void updateTodaysettlementroommemberlate(int roomId, String userId, int lateDay) throws Exception;

    OttRoomDTO selectRoomByRoomId(int roomId) throws Exception;
    
    void updatePaymentstatusRefund(SettlementPaymentVO payment) throws Exception;
    boolean cancelApprovedPayment(String paymentKey) throws Exception;
    List<SettlementPaymentVO> selectpaymentAll() throws Exception;
    String selectEscrowStatus(int room_id, String host_id)throws Exception;
        String selectRefundStatus(int payment_id) throws Exception;
    void executeRoomRefund(SettlementPaymentVO payment) throws Exception;
    void deleteCard(int card_idx,String id) throws Exception;
    void deleteAccount(int account_idx,String id) throws Exception;
}
