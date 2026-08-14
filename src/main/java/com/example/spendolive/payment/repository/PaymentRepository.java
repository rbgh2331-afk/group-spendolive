package com.example.spendolive.payment.repository;

import java.util.List;

import org.springframework.dao.DataAccessException;

import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.payment.domain.EscrowPayoutVO;
import com.example.spendolive.payment.domain.PlatformRevenueVO;
import com.example.spendolive.payment.domain.SellerAccountVO;
import com.example.spendolive.payment.domain.SettlementPaymentVO;
import com.example.spendolive.payment.domain.SettlementRefundVO;

public interface PaymentRepository {
    List<OttRoomDTO> selectTodaysettlement(int today, int endday, String status) throws Exception;

    void updateEscrowStatus(int roomId);

    /** 기존 UNPAID 행은 갱신하고, 행이 없으면 새 결제 행을 등록합니다. */
    void updatePaymentStatus(SettlementPaymentVO paymentInfo);

    SettlementPaymentVO settlement_paymentByroomId(String userId, int roomId) throws DataAccessException;

    /** 방과 회원 기준으로 현재 결제 상태만 조회합니다. */
    String selectPaymentStatusByRoomAndMember(String userId, int roomId) throws DataAccessException;

    void insertEscrow(EscrowPayoutVO escrow);

    OttSettlementDTO settlementByroomId(int roomId) throws Exception;

    void insertPlatfoem_Revenue(PlatformRevenueVO revenueInfo);

    void insertSeller(SellerAccountVO sellerInfo);

    void updatSettlementStatus(int roomId);

    List<OttRoomMemberDTO> selectTodaysettlementMember(int today, int endday, String status) throws Exception;

    void updateReadyfromYet(int today, int endday) throws Exception;

    void updateReadyfromYettoroommember(int today, int endday) throws Exception;

    void updatSettlementStatusYETroommember(int day) throws Exception;

    void updatSettlementroommemberStatus(int roomId, String userId) throws Exception;

    void updateTodaysettlementroommemberlate(int roomId, String userId, int lateDay) throws Exception;

    void updatSettlementStatusYET(int day) throws Exception;
    List<SettlementPaymentVO> selectsettlement_paymentAll() throws DataAccessException;
    void updatePaymentstatusRefund(int payment_id) throws DataAccessException;
    void insertRefund(SettlementRefundVO refund) throws DataAccessException;
    boolean selectEscrowStatus(int room_id, String host_id)throws DataAccessException;
    String selectRefundStatus(int payment_id) throws DataAccessException;
    void deleteCard(int card_idx, String id) throws DataAccessException;
    void deleteAccount(int account_idx, String id) throws DataAccessException;
}
