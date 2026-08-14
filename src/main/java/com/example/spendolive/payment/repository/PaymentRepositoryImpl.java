package com.example.spendolive.payment.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.payment.domain.*;


@Repository
public class PaymentRepositoryImpl implements PaymentRepository{
    private final JdbcTemplate jdbcTemplate;
//insert

    // 사전 생성된 결제 행이 없는 예외적인 경우에만 새 결제 행을 등록합니다.
    private final String insertSuccessfulPayment = """
            INSERT INTO settlement_payment_tb (
                settlement_id,
                id,
                base_amount,
                fee_rate,
                fee_amount,
                total_amount,
                payment_status,
                card_number,
                card_company,
                paid_at,
                paymentKey,
                orderId,
                memo
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private final String insertEscrow = "INSERT INTO escrow_payout_tb (SETTLEMENT_ID, room_id, payer_id, host_id, amount, status, created_at)  "
    +" VALUES(?,?,?,?,?,?,?) ";
    private final String insertRevenue = "INSERT INTO platform_revenue_tb ("
    +"SETTLEMENT_ID, ROOM_ID, PAYER_ID, BASE_AMOUNT, FEE_RATE, FEE_AMOUNT, STATUS, created_at) "
    +" VALUES(?,?,?,?,?,?,?,?) ";
    private final String insertSeller = "INSERT INTO seller_account_tb ("
    +"member_id, bank_name, account_number, traceId) "
    +" VALUES(?,?,?,?) ";
    private final String insertRefund = "INSERT INTO settlement_refund_tb ("
    +"SETTLEMENT_ID, payment_id,member_login_id, REFUND_AMOUNT, refund_reason, refund_status, completed_at) "
    +" VALUES(?,?,?,?,?,?,?) ";
//select
    
    private final String settlement_paymentByroomId = "select "
    +"sp.payment_id, sp.settlement_id, sp.id, sp.base_amount, sp.fee_rate, sp.fee_amount, sp.total_amount, sp.payment_status, sp.card_number," 
    +"sp.card_company, sp.paid_at, sp.confirmed_at, sp.expired_at, sp.cancelled_at, sp.paymentKey, sp.orderId, sp.memo "
    +" from settlement_payment_tb sp JOIN settlement_tb st ON sp.settlement_id = st.settlement_id "
    +"where st.room_id =? AND sp.id = ? ORDER BY st.settlement_id DESC FETCH FIRST 1 ROW ONLY";
    private final String settlementByroomId = "select "
    +"st.settlement_id, st.room_id, st.total_price, r.member_limit, r.HOST_LOGIN_ID "
    +"from settlement_tb st "
    +"JOIN ott_room_tb r ON st.room_id = r.room_id where st.room_id =? "
    +"ORDER BY st.settlement_id DESC FETCH FIRST 1 ROW ONLY ";
    private final String selectPaymentStatusByRoomAndMember = """
            SELECT sp.payment_status
            FROM settlement_payment_tb sp
            JOIN settlement_tb st ON sp.settlement_id = st.settlement_id
            WHERE st.room_id = ?
              AND sp.id = ?
            ORDER BY st.settlement_id DESC
            FETCH FIRST 1 ROW ONLY
            """;
    private final String selectTodatSettlement = "SELECT r.ROOM_ID, r.HOST_LOGIN_ID, r.OTT_SERVICE_ID, r.ROOM_NAME, r.PLAN_NAME, r.TOTAL_PRICE, "
    + "r.BILLING_DAY, r.MEMBER_LIMIT, r.ROOM_MODE, r.STATUS, r.INVITE_CODE, r.CLOSE_REASON, r.CLOSE_NOTICE, "
    + "TO_CHAR(r.CLOSE_EFFECTIVE_DATE, 'YYYY-MM-DD') AS CLOSE_EFFECTIVE_DATE, "
    + "TO_CHAR(r.CLOSE_REQUESTED_AT, 'YYYY-MM-DD') AS CLOSE_REQUESTED_AT, "
    + "TO_CHAR(r.CLOSED_AT, 'YYYY-MM-DD') AS CLOSED_AT, "
    + "TO_CHAR(r.created_at, 'YYYY-MM-DD') AS created_at, "
    + "s.SETTLEMENT_STATUS "         
    + "from ott_room_tb r INNER JOIN settlement_tb s ON r.ROOM_ID = s.ROOM_ID where r.BILLING_DAY >=? AND r.BILLING_DAY <=? AND r.status IN ('ACTIVE', 'FIRST') "
    + "AND s.settlement_status =? ";
    private final String selectTodaySettlementmember = "SELECT FEE_AMOUNT,FEE_RATE,MEMBER_LOGIN_ID,PAY_AMOUNT,PAY_DAY,PAY_LATE_DAY,ROOM_ID, settlement_status, "
    + "TO_CHAR(JOINED_AT , 'YYYY-MM-DD') AS JOINED_AT "     
    + "from ott_room_member_tb where (pay_day + pay_late_day) >=? AND (pay_day + pay_late_day) <=? AND status= 'ACTIVE' "
    + "AND settlement_status =? and MEMBER_ROLE='MEMBER' AND room_id IN ( SELECT room_id from ott_room_tb where status ='ACTIVE' ) ";
    private final String settlement_paymentAll = "select "
    +"payment_id, settlement_id, id, base_amount, fee_rate, fee_amount, total_amount, payment_status, card_number," 
    +"card_company, paid_at, confirmed_at, expired_at, cancelled_at, paymentKey, orderId, memo "
    +" from settlement_payment_tb ";
    private final String selectEscrowStatus = "select "
    +"decode(count(*),1, 'false', 0, 'true') as status " 
    +" from escrow_payout_tb "
    +" where room_id =? and host_id=? and status='HELD' ";
    private final String selectRefundStatus = "select "
    +"REFUND_STATUS " 
    +" from settlement_refund_tb "
    +" where payment_id =? ";
//update
    private final String insertTodayexcrow = "UPDATE escrow_payout_tb set STATUS = 'RELEASED' ,PAYOUT_AT =sysdate where ROOM_ID =? ";
    private final String updateTodaysettlement = "UPDATE settlement_tb set SETTLEMENT_STATUS = 'DONE' where ROOM_ID =? ";
    private final String updateReadyfromYet="UPDATE settlement_tb set settlement_status = 'READY'  where settlement_status='YET'  AND room_id "
       + " IN (SELECT room_id FROM ott_room_tb WHERE BILLING_DAY >=? AND BILLING_DAY <=?) ";
    private final String updateCheckTodaysettlement = "UPDATE settlement_tb set SETTLEMENT_STATUS = 'YET' where SETTLEMENT_STATUS ='DONE' AND ROOM_ID IN (SELECT room_id FROM ott_room_tb WHERE billing_day < ?) ";
    private final String updateCheckTodayroommember = "UPDATE ott_room_member_tb set SETTLEMENT_STATUS = 'YET' where SETTLEMENT_STATUS ='DONE' and (pay_day + pay_late_day) <? ";
    private final String updateReadyfromYettoroommember="UPDATE ott_room_member_tb set settlement_status = 'READY'  where settlement_status='YET'  AND room_id "
    + " IN (SELECT room_id FROM ott_room_tb WHERE BILLING_DAY >=? AND BILLING_DAY <=?) ";
    private final String updateTodaysettlementroommemberstatus = "UPDATE ott_room_member_tb set SETTLEMENT_STATUS = 'DONE' where ROOM_ID =? and member_login_id =? ";
    private final String updateTodaysettlementroommemberlate = "UPDATE ott_room_member_tb set pay_late_day =? + 1 where ROOM_ID =? and member_login_id =? ";
    private final String updatePaymentstatusRefund = "UPDATE settlement_payment_tb set PAYMENT_STATUS =? where payment_id =? ";
    public PaymentRepositoryImpl(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
    //delete
    private static final String deleteCard = "DELETE FROM member_card_tb WHERE card_idx = ? AND id = ?";
    private static final String deleteAccount = "DELETE FROM member_account_tb WHERE account_idx = ? AND id = ?";
    //Select
    @Override
    public void updatePaymentStatus(SettlementPaymentVO paymentInfo) {
       
        jdbcTemplate.update(
                insertSuccessfulPayment,
                paymentInfo.getSettlement_id(),
                paymentInfo.getId(),
                paymentInfo.getBase_amount(),
                paymentInfo.getFee_rate(),
                paymentInfo.getFee_amount(),
                paymentInfo.getTotal_amount(),
                paymentInfo.getPayment_status(),
                paymentInfo.getCard_number(),
                paymentInfo.getCard_company(),
                paymentInfo.getPaid_at(),
                paymentInfo.getPaymentKey(),
                paymentInfo.getOrderId(),
                paymentInfo.getMemo());
    }
    @Override
    public SettlementPaymentVO settlement_paymentByroomId(String userId, int room_id) throws DataAccessException {
        try {
            return (SettlementPaymentVO) jdbcTemplate.queryForObject(settlement_paymentByroomId, (rs, rowNum) -> {
            SettlementPaymentVO settlementPaymentVO = new SettlementPaymentVO();
            settlementPaymentVO.setBase_amount(rs.getInt("base_amount"));
            settlementPaymentVO.setPaid_at(rs.getObject("paid_at", LocalDateTime.class));
            settlementPaymentVO.setConfirmed_at(rs.getObject("confirmed_at", LocalDateTime.class));
            settlementPaymentVO.setExpired_at(rs.getObject("expired_at", LocalDateTime.class));
            settlementPaymentVO.setCancelled_at(rs.getObject("cancelled_at", LocalDateTime.class));
            settlementPaymentVO.setCard_company(rs.getString("card_company"));
            settlementPaymentVO.setCard_number(rs.getString("card_number"));
            settlementPaymentVO.setFee_amount(rs.getInt("fee_amount"));
            settlementPaymentVO.setFee_rate(rs.getDouble("fee_rate"));
            settlementPaymentVO.setId(rs.getString("id"));
            settlementPaymentVO.setMemo(rs.getString("memo"));
            settlementPaymentVO.setOrderId(rs.getString("orderId"));
            settlementPaymentVO.setPaymentKey(rs.getString("paymentKey"));
            settlementPaymentVO.setPayment_id(rs.getInt("payment_id"));
            settlementPaymentVO.setPayment_status(rs.getString("payment_status"));
            settlementPaymentVO.setSettlement_id(rs.getInt("settlement_id"));
            settlementPaymentVO.setTotal_amount(rs.getInt("total_amount"));
         
            return settlementPaymentVO;
            }, room_id,userId);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return null; 
        }
    }
    @Override
    public String selectPaymentStatusByRoomAndMember(String userId, int room_id)
            throws DataAccessException {
        try {
            return jdbcTemplate.queryForObject(
                    selectPaymentStatusByRoomAndMember,
                    String.class,
                    room_id,
                    userId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public OttSettlementDTO settlementByroomId (int room_id) throws DataAccessException{
        try {
            return (OttSettlementDTO) jdbcTemplate.queryForObject(settlementByroomId, (rs, rowNum) -> {
            OttSettlementDTO set = new OttSettlementDTO();
            set.setSettlement_id(rs.getLong("settlement_id"));
            set.setRoom_id(rs.getLong("room_id"));
            set.setTotal_price(rs.getInt("total_price"));
            set.setMember_limit(rs.getInt("member_limit"));
            set.setHost_login_id(rs.getString("host_login_id"));
            return set;
        }, room_id);
    
    }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return null; 
        }
    }
    @Override
    public List<OttRoomDTO> selectTodaysettlement(int today,int endday, String status) throws Exception {
        try {
            return (List<OttRoomDTO>) jdbcTemplate.query(selectTodatSettlement, (rs, rowNum) -> {
            OttRoomDTO room = new OttRoomDTO();
            room.setBilling_day(rs.getInt("BILLING_DAY"));
            room.setClose_effective_date(rs.getString("CLOSE_EFFECTIVE_DATE"));
            room.setClose_notice(rs.getString("CLOSE_NOTICE"));
            room.setClose_reason(rs.getString("CLOSE_REASON"));
            room.setClose_requested_at(rs.getString("CLOSE_REQUESTED_AT"));
            room.setClosed_at(rs.getString("CLOSED_AT"));
            room.setCreated_at(rs.getString("CREATED_AT"));
            room.setHost_login_id(rs.getString("HOST_LOGIN_ID"));
            room.setInvite_code(rs.getString("INVITE_CODE"));
            room.setMember_limit(rs.getInt("member_limit"));
            room.setOtt_service_id(rs.getLong("OTT_SERVICE_ID"));
            room.setRoom_name(rs.getString("ROOM_NAME"));
            room.setPlan_name(rs.getString("PLAN_NAME"));
            room.setRoom_id(rs.getLong("room_id"));
            room.setRoom_mode(rs.getString("ROOM_MODE"));
            room.setStatus(rs.getString("STATUS"));
            room.setTotal_price(rs.getInt("TOTAL_PRICE"));
            room.setSettlement_status(rs.getString("settlement_status"));
            return room;
        }, today,endday, status);
    }catch (org.springframework.dao.EmptyResultDataAccessException e) {
        // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
        return null; 
    }
} 
@Override
public List<OttRoomMemberDTO> selectTodaysettlementMember(int today,int endday, String status) throws Exception {
    try {
        return (List<OttRoomMemberDTO>) jdbcTemplate.query(selectTodaySettlementmember, (rs, rowNum) -> {
            OttRoomMemberDTO mem = new OttRoomMemberDTO();
            mem.setFee_amount(rs.getInt("fee_amount"));
            mem.setRoom_id(rs.getLong("room_id"));
            mem.setJoined_at(rs.getString("joined_at"));
            mem.setSettlement_status(rs.getString("settlement_status"));
            mem.setMember_login_id(rs.getString("member_login_id"));
            mem.setPay_amount(rs.getInt("pay_amount"));
            mem.setFee_rate(rs.getDouble("fee_rate"));
            mem.setPay_late_day(rs.getInt("pay_late_day"));
            mem.setPay_day(rs.getInt("pay_day"));
        return mem;
    }, today,endday, status);
}catch (org.springframework.dao.EmptyResultDataAccessException e) {
    // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
    return null; 
}
} 

@Override
public List<SettlementPaymentVO> selectsettlement_paymentAll() throws DataAccessException {
    try {
        return jdbcTemplate.query(settlement_paymentAll, (rs, rowNum) -> {
        SettlementPaymentVO settlementPaymentVO = new SettlementPaymentVO();
        settlementPaymentVO.setBase_amount(rs.getInt("base_amount"));
        settlementPaymentVO.setPaid_at(rs.getObject("paid_at", LocalDateTime.class));
        settlementPaymentVO.setConfirmed_at(rs.getObject("confirmed_at", LocalDateTime.class));
        settlementPaymentVO.setExpired_at(rs.getObject("expired_at", LocalDateTime.class));
        settlementPaymentVO.setCancelled_at(rs.getObject("cancelled_at", LocalDateTime.class));
        settlementPaymentVO.setCard_company(rs.getString("card_company"));
        settlementPaymentVO.setCard_number(rs.getString("card_number"));
        settlementPaymentVO.setFee_amount(rs.getInt("fee_amount"));
        settlementPaymentVO.setFee_rate(rs.getDouble("fee_rate"));
        settlementPaymentVO.setId(rs.getString("id"));
        settlementPaymentVO.setMemo(rs.getString("memo"));
        settlementPaymentVO.setOrderId(rs.getString("orderId"));
        settlementPaymentVO.setPaymentKey(rs.getString("paymentKey"));
        settlementPaymentVO.setPayment_id(rs.getInt("payment_id"));
        settlementPaymentVO.setPayment_status(rs.getString("payment_status"));
        settlementPaymentVO.setSettlement_id(rs.getInt("settlement_id"));
        settlementPaymentVO.setTotal_amount(rs.getInt("total_amount"));
     
        return settlementPaymentVO;
        });
    }catch (org.springframework.dao.EmptyResultDataAccessException e) {
        // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
        return null; 
    }
}
    @Override
    public boolean selectEscrowStatus(int room_id,String host_id){
        try {
            return jdbcTemplate.queryForObject(selectEscrowStatus, (rs, rowNum) -> {
                String statuss = rs.getString("status");
                if(statuss.equals("true")){
                    return  true;}
                return false;
                } ,room_id,host_id);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return false; 
        }
    }
    @Override
    public String selectRefundStatus(int payment_id){
        try {
            return jdbcTemplate.queryForObject(selectRefundStatus, (rs, rowNum) -> {
                return rs.getString("REFUND_STATUS");
                } ,payment_id);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return null; 
        }
    }


    //Insert
    @Override
        public void insertRefund(SettlementRefundVO refund) {
            jdbcTemplate.update(insertRefund, refund.getSettlement_id(),refund.getPayment_id(), refund.getMember_login_id() , refund.getRefund_amount() ,refund.getRefund_reason(),refund.getRefund_status(), refund.getCompleted_at());
        }
    @Override
    public void insertEscrow(EscrowPayoutVO escrowInfo) {
        jdbcTemplate.update(insertEscrow, escrowInfo.getSettlement_id(),escrowInfo.getRoom_id(), escrowInfo.getPayer_id() ,escrowInfo.getHost_id(), escrowInfo.getAmount() ,escrowInfo.getStatus(), escrowInfo.getCreated_at());
    }
    @Override
    public void insertSeller(SellerAccountVO sellerInfo) {

        jdbcTemplate.update(insertSeller, sellerInfo.getMember_id(), sellerInfo.getBank_name(), sellerInfo.getAccount_number(),sellerInfo.getTraceId());
    }
    @Override
    public void insertPlatfoem_Revenue(PlatformRevenueVO revenueInfo) {
        jdbcTemplate.update(insertRevenue, revenueInfo.getSettlement_id(),revenueInfo.getRoom_id(), revenueInfo.getPayer_id() , revenueInfo.getBase_amount() ,revenueInfo.getFee_rate(), revenueInfo.getFee_amount(),revenueInfo.getStatus(),revenueInfo.getCreated_at());
    }
    //Update
   
    @Override
    public void updateEscrowStatus(int room_id) {
        jdbcTemplate.update(insertTodayexcrow, room_id);

    }
    @Override
    public void updatSettlementStatus(int room_id) {
        jdbcTemplate.update(updateTodaysettlement, room_id);
    }
    
    @Override
    public void updateReadyfromYet(int today,int endday) {
        jdbcTemplate.update(updateReadyfromYet, today,endday);
    }
    @Override
    public void updateReadyfromYettoroommember(int today,int endday) {
        jdbcTemplate.update(updateReadyfromYettoroommember, today,endday);
    }
    @Override
    public void updatSettlementStatusYETroommember(int day) {
        jdbcTemplate.update(updateCheckTodayroommember, day);
    }
    @Override
    public void updatSettlementroommemberStatus(int roomId,String userId) {
        jdbcTemplate.update(updateTodaysettlementroommemberstatus, roomId,userId);
    }
        @Override
        public void updateTodaysettlementroommemberlate(int roomId,String userId,int late_day) {
            jdbcTemplate.update(updateTodaysettlementroommemberlate, late_day,roomId,userId);
        }   
        @Override
        public void updatSettlementStatusYET(int day) {
            jdbcTemplate.update(updateCheckTodaysettlement, day);
        }
        @Override
        public void updatePaymentstatusRefund(int payment_id) {
            jdbcTemplate.update(updatePaymentstatusRefund, "REFUNDED", payment_id);
        }
        @Override
        public void deleteCard(int card_idx,String id) {
            jdbcTemplate.update(deleteCard, card_idx, id);
        }
        @Override
        public void deleteAccount(int account_idx,String id) {
            jdbcTemplate.update(deleteAccount, account_idx, id);
        }
}
