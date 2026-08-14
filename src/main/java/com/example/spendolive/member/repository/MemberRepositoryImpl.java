package com.example.spendolive.member.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.domain.MemberTranVO;
import com.example.spendolive.member.domain.MemberVO;
import java.time.LocalDateTime;
@Repository
public class MemberRepositoryImpl implements MemberRepository{
    private final JdbcTemplate jdbcTemplate;
//insert
    private final String signup = "INSERT INTO member_tb(id, email, password, member_name, nickname, phone,login_type ,verify_type) values(?,?,?,?,?,?,?,?)";
    // 새로 연동한 계좌는 사용자가 직접 선택하기 전까지 주계좌가 아니므로 STATUS를 NO로 저장한다.
    private final String updatePinNO = "INSERT INTO member_account_tb(id, bank_code, account_number, fintech_use_num, open_bank_token, open_bank_user_seq, balance, ACCOUNT_HOLDER_NAM, STATUS) "
    + "values(?,?,?,?,?,?,?,?,'NO') ";
    private final String updateBillingKey = "INSERT INTO member_card_tb(id, card_number, card_company, billing_key) "
    +" values(?,?,?,?) ";
    // 거래 금액과 함께 거래 직후 잔액을 저장한다.
    private final String inserttrandetail = "INSERT INTO member_tran_tb(id, inout_type, tran_amt, tran_date, account_idx, balance_after) values(?,?,?,?,?,?)";

//select 


  private final String selectMemberAllSql =
    "SELECT member_id, id, email, password, member_name, nickname, "
  + "phone, login_type, TO_CHAR(blocked_until, 'YYYY-MM-DD HH24:MI:SS') AS blocked_until, warning_count, role, status, "
  + "verify_type, account_status, card_status, "
  + "TO_CHAR(created_at, 'YYYY-MM-DD') AS created_at, "
  + "TO_CHAR(updated_at, 'YYYY-MM-DD') AS updated_at, "
  + "TO_CHAR(warninged_at, 'YYYY-MM-DD') AS warninged_at, "
  + "TO_CHAR(last_login_at, 'YYYY-MM-DD') AS last_login_at "
  + "FROM member_tb ";
    private final String checkId = "select decode(count(*),1, 'false', 0, 'true') as id"
    +" from member_tb where id =? "+ "AND status = 'ACTIVE'";
    private final String checkEmail = "select decode(count(*),1, 'false', 0, 'true') as email"
    +" from member_tb where email =? "+ "AND status = 'ACTIVE'";
    private final String checkPhone = "select decode(count(*),1, 'false', 0, 'true') as phone"
    +" from member_tb where phone =? "+ "AND status = 'ACTIVE'";
   
    private final String selectMemberByIdSql =
    "SELECT member_id, id, email, password, member_name, nickname, "
  + "phone, login_type, TO_CHAR(blocked_until, 'YYYY-MM-DD HH24:MI:SS') AS blocked_until, warning_count, role, status, "
  + "verify_type, account_status, card_status, "
  + "TO_CHAR(created_at, 'YYYY-MM-DD') AS created_at, "
  + "TO_CHAR(updated_at, 'YYYY-MM-DD') AS updated_at, "
  + "TO_CHAR(warninged_at, 'YYYY-MM-DD') AS warninged_at, "
  + "TO_CHAR(last_login_at, 'YYYY-MM-DD') AS last_login_at "
  + "FROM member_tb "
  + "WHERE id = ? "
  + "AND status = 'ACTIVE'";

  // 주계좌가 설정된 경우 목록에서 먼저 보이도록 정렬한다.
  private final String selectMemberAccountById= "select ACCOUNT_HOLDER_NAM,ACCOUNT_IDX,ACCOUNT_NUMBER,BALANCE,BANK_CODE,FINTECH_USE_NUM,ID,OPEN_BANK_TOKEN,OPEN_BANK_USER_SEQ,REG_DATE,FROM_DATE,FROM_TIME,TO_DATE,TO_TIME,ACCOUNT_NAME,STATUS "
                                                + "from member_account_tb where id=? "
                                                + "order by case when status='YES' then 0 else 1 end, account_idx ";
    private final String selectMemberCardById= "select BILLING_KEY,CARD_COMPANY,CARD_IDX,CARD_NUMBER,CARD_NAME,ID,REG_DATE,STATUS "
                                                +"from member_card_tb where id=? ";
    // 다른 회원의 계좌가 조회되지 않도록 회원 아이디와 계좌 번호를 함께 검사한다.
    private final String selectTransactionsByAccount = """
            SELECT member_tran_idx,
                   account_idx,
                   id,
                   tran_amt,
                   inout_type,
                   tran_date,
                   balance_after,
                   TO_CHAR(reg_date, 'YYYY-MM-DD HH24:MI:SS') AS reg_date
            FROM member_tran_tb
            WHERE id = ?
              AND account_idx = ?
            ORDER BY tran_date DESC, member_tran_idx DESC
            """;
//update 
    private final String updatemember_account_Status = "UPDATE member_tb SET account_status = 'YES' WHERE id = ?";
    private final String updatemember_card_Status = "UPDATE member_tb SET card_status = 'YES' WHERE id = ?";

    private final String applyWarningPenalty = """
            UPDATE member_tb
            SET warning_count = warning_count + 1,
                warninged_at = SYSDATE,
                blocked_until = SYSDATE + ?
            WHERE id = ?
            """;
   
    private final String updatebalance="update member_account_tb set balance=(balance + ?) where account_idx=? ";
    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       로그인 회원의 특정 계좌 제목(account_name)만 수정하는 SQL이다.
       ========================================================= */
    private final String updateAccountName="update member_account_tb set account_name=? where id=? and account_idx=? ";

    /* [마이페이지 카드 이름 수정]
       회원 아이디와 카드 번호를 함께 조건으로 사용해 다른 회원 카드가 수정되지 않게 한다. */
    private final String updateCardName="update member_card_tb set card_name=? where id=? and card_idx=? ";

    // 선택한 계좌만 YES로 변경하고 같은 회원의 다른 계좌는 모두 NO로 변경하는 SQL이다.
    private final String updatePrimaryAccount = """
            UPDATE member_account_tb
            SET status = CASE
                             WHEN account_idx = ? THEN 'YES'
                             ELSE 'NO'
                         END
            WHERE id = ?
              AND EXISTS (
                  SELECT 1
                  FROM member_account_tb target_account
                  WHERE target_account.id = ?
                    AND target_account.account_idx = ?
              )
            """;
            private final String updatePrimaryCard = """
                UPDATE member_card_tb
                SET status = CASE
                                 WHEN card_idx = ? THEN 'YES'
                                 ELSE 'NO'
                             END
                WHERE id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM member_card_tb target_card
                      WHERE target_card.id = ?
                        AND target_card.card_idx = ?
                  )
                """;

               //delete
    private static final String deleteCard = "DELETE FROM member_card_tb WHERE card_idx = ? AND id = ?";
    public MemberRepositoryImpl(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public void insertNewMember(MemberVO member){
        jdbcTemplate.update(signup, member.getId(), member.getEmail(), member.getPassword(), member.getMember_name(), member.getNickname(), member.getPhone(),member.getLogin_type() ,"PHONE");
    }
    @Override
    public boolean checkId(String id){
        try {
            return jdbcTemplate.queryForObject(checkId, (rs, rowNum) -> {
                String ids = rs.getString("id");
                if(ids.equals("true")){
                    return  true;}
                return false;
                } ,id);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return false; 
        }
    }
    @Override
    public boolean checkEmail(String email){
        try {
            return jdbcTemplate.queryForObject(checkEmail, (rs, rowNum) -> {
                String emails = rs.getString("email");
                if(emails.equals("true")){
                    return  true;}
                return false;
                } ,email);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return false; 
        }
    }
    @Override
    public boolean checkPhone(String phone){
        try {
            return jdbcTemplate.queryForObject(checkPhone, (rs, rowNum) -> {
                String phones = rs.getString("phone");
                if(phones.equals("true")){
                    return  true;}
                return false;
                } ,phone);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return false; 
        }
    }
    @Override
	public void updateTossInfo(String userId, String card_num, String card_company, String billingkey)throws DataAccessException {
        jdbcTemplate.update(updateBillingKey,
        userId,
        card_num,
        card_company,
        billingkey
        );
    }
    
    @Override
    public void updateOpenBankingInfo(String userId, String accessToken, String userSeqNo, String fintech_num, String bank_code, String account_num, int balance,String accountHolderNam) throws DataAccessException {
        jdbcTemplate.update(updatePinNO,
        userId,
        bank_code,
        account_num,
        fintech_num,
        accessToken,
        userSeqNo,
        balance,
        accountHolderNam
        );
    }

    @Override
    public MemberVO selectMemberById(String id) throws DataAccessException {
        try {
            return jdbcTemplate.queryForObject(selectMemberByIdSql, (rs, rowNum) -> mapMember(rs), id);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void updateMyInfo(MemberVO memberVO, String newPassword) throws DataAccessException {
        boolean changePassword = newPassword != null && !newPassword.isBlank();

        if (changePassword) {
            String sql = """
                    UPDATE member_tb
                    SET member_name = ?,
                        nickname = ?,
                        email = ?,
                        phone = ?,
                        password = ?,
                        updated_at = SYSDATE
                    WHERE id = ?
                    """;
            jdbcTemplate.update(
                    sql,
                    memberVO.getMember_name(),
                    memberVO.getNickname(),
                    memberVO.getEmail(),
                    memberVO.getPhone(),
                    newPassword,
                    memberVO.getId()
            );
        } else {
            String sql = """
                    UPDATE member_tb
                    SET member_name = ?,
                        nickname = ?,
                        email = ?,
                        phone = ?,
                        updated_at = SYSDATE
                    WHERE id = ?
                    """;
            jdbcTemplate.update(
                    sql,
                    memberVO.getMember_name(),
                    memberVO.getNickname(),
                    memberVO.getEmail(),
                    memberVO.getPhone(),
                    memberVO.getId()
            );
        }
    }
    private MemberVO mapMember(ResultSet rs) throws SQLException {
        MemberVO member = new MemberVO();
        member.setMember_id(rs.getInt("member_id"));
        member.setWarning_count(rs.getInt("warning_count"));
        member.setId(rs.getString("id"));
        member.setEmail(rs.getString("email"));
        member.setLogin_type(rs.getString("login_type"));
        member.setVerify_type(rs.getString("verify_type"));
        member.setMember_name(rs.getString("member_name"));
        member.setNickname(rs.getString("nickname"));
        member.setPassword(rs.getString("password"));
        member.setPhone(rs.getString("phone"));
        member.setRole(rs.getString("role"));
        member.setStatus(rs.getString("status"));
        member.setCreated_at(rs.getString("created_at"));
        member.setUpdated_at(rs.getString("updated_at"));
        member.setBlocked_until(rs.getString("blocked_until"));
        member.setLast_login_at(rs.getString("last_login_at"));
        member.setVerify_type(rs.getString("verify_type"));
        member.setWarning_count(rs.getInt("warning_count"));
        member.setAccount_status(rs.getString("account_status"));
        member.setCard_status(rs.getString("card_status"));
        member.setWarninged_at(rs.getString("warninged_at"));
        return member;
    }

 
    

    /* =========================================================
       [추가 기능 구현] 아이디/비밀번호 찾기 Repository 구현부
       ---------------------------------------------------------
       이 아래 메서드들은 로그인 페이지의 아이디 찾기/비밀번호 찾기에서 새로 사용하는 SQL이다.
       공통 기준:
       - status = 'ACTIVE' 회원만 대상으로 한다.
       - 휴대폰 번호는 하이픈을 제거한 숫자 문자열로 비교한다.
       - 조회 결과가 없으면 예외를 화면까지 올리지 않고 null 또는 false로 반환한다.
       ========================================================= */

    @Override
    public String findIdByPhone(String phone) throws DataAccessException {
        // 화면에서 010-1234-5678 또는 01012345678 둘 다 입력할 수 있으므로 숫자만 남긴다.
        String normalizedPhone = normalizePhone(phone);
        // 아이디 찾기용 SQL: 휴대폰 번호가 일치하는 ACTIVE 회원의 id를 1건 조회한다.
        String sql = """
                SELECT id
                FROM member_tb
                WHERE REPLACE(phone, '-', '') = ?
                  AND status = 'ACTIVE'
                FETCH FIRST 1 ROWS ONLY
                """;
        try {
            return jdbcTemplate.queryForObject(sql, String.class, normalizedPhone);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public boolean existsActiveId(String id) throws DataAccessException {
        // 비밀번호 찾기용 SQL: 입력한 id가 ACTIVE 회원으로 존재하는지 확인한다.
        String sql = """
                SELECT COUNT(*)
                FROM member_tb
                WHERE id = ?
                  AND status = 'ACTIVE'
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public boolean existsActiveMemberByIdAndPhone(String id, String phone) throws DataAccessException {
        // 비밀번호 찾기용 SQL: id와 휴대폰 번호가 같은 회원 정보인지 확인한다.
        String normalizedPhone = normalizePhone(phone);
        String sql = """
                SELECT COUNT(*)
                FROM member_tb
                WHERE id = ?
                  AND REPLACE(phone, '-', '') = ?
                  AND status = 'ACTIVE'
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id, normalizedPhone);
        return count != null && count > 0;
    }

    @Override
    public void updatePasswordById(String id, String newPassword) throws DataAccessException {
        // 비밀번호 재설정용 SQL: 인증 완료된 id의 password와 updated_at을 갱신한다.
        String sql = """
                UPDATE member_tb
                SET password = ?,
                    updated_at = SYSDATE
                WHERE id = ?
                  AND status = 'ACTIVE'
                """;
        jdbcTemplate.update(sql, newPassword, id);
    }

    /*
     * [추가 유틸] 휴대폰 번호 정규화
     * DB에는 하이픈이 있거나 없는 값이 섞일 수 있고,
     * 화면에서도 두 형태 모두 입력될 수 있으므로 숫자만 남겨 비교한다. 이거 왜 여기에다 추가하셨죠?
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }
    @Override
    public void updateMember_account_status(String id){
        jdbcTemplate.update(updatemember_account_Status,id);
    }
    @Override
    public void updateMember_card_status(String id){
        jdbcTemplate.update(updatemember_card_Status,id);
    }
    @Override
    public List<MemberCardVO> selectCardById(String userId){
        try {
            return jdbcTemplate.query(selectMemberCardById, (rs, rowNum) -> {
            MemberCardVO card = new MemberCardVO();
            card.setCard_company(rs.getString("card_company"));
            card.setBilling_key(rs.getString("billing_key"));
            card.setCard_idx(rs.getInt("card_idx"));
            card.setCard_number(rs.getString("card_number"));
            card.setCard_name(rs.getString("card_name"));
            card.setId(rs.getString("id"));
            card.setReg_date(rs.getObject("reg_date", LocalDateTime.class));
            card.setStatus(rs.getString("status"));
            return card;
            },userId);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return null; 
        }
    }
    @Override
    public List<MemberAccountVO> selectAccountById(String userId){
        try {
            return jdbcTemplate.query(selectMemberAccountById, (rs, rowNum) -> {
            MemberAccountVO account = new MemberAccountVO();

            account.setAccount_idx(rs.getInt("account_idx"));
            account.setAccount_number(rs.getString("account_number"));
            account.setAccount_holder_nam(rs.getString("account_holder_nam"));
            account.setBalance(rs.getInt("balance"));
            account.setOpen_bank_user_seq(rs.getString("OPEN_BANK_USER_SEQ"));
            account.setBank_code(rs.getString("BANK_CODE"));
            account.setFintech_use_num(rs.getString("FINTECH_USE_NUM"));
            account.setId(rs.getString("id"));
            account.setOpen_bank_token(rs.getString("OPEN_BANK_TOKEN"));
            account.setReg_date(rs.getObject("reg_date", LocalDateTime.class));
            account.setAccount_name(rs.getString("account_name"));
            account.setStatus(rs.getString("status"));
            return account;
            },userId);
        }catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // ◀ [수정] 조회가 안 되면(로그인 실패) 에러를 터뜨리지 말고 null을 안전하게 리턴!
            return null; 
        }
    }
    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       계좌 제목 수정 SQL을 실행하고 실제 수정된 행 수를 반환한다.
       ========================================================= */
    @Override
    public int updateAccountName(String userId, int accountIdx, String accountName) {
        return jdbcTemplate.update(updateAccountName, accountName, userId, accountIdx);
    }

    // 로그인 회원이 선택한 계좌를 주계좌로 설정한다.
    @Override
    public int updatePrimaryAccount(String userId, int accountIdx) {
        return jdbcTemplate.update(
                updatePrimaryAccount,
                accountIdx,
                userId,
                userId,
                accountIdx
        );
    }
    @Override
    public int updatePrimaryCard(String userId, int cardIdx) {
        return jdbcTemplate.update(
                updatePrimaryCard,
                cardIdx,
                userId,
                userId,
                cardIdx
        );
    }

    @Override
    public int updateCardName(String userId, int cardIdx, String cardName) {
        return jdbcTemplate.update(updateCardName, cardName, userId, cardIdx);
    }
    @Override
    public void applyWarningPenalty(String userId, int penaltyDays){
        jdbcTemplate.update(applyWarningPenalty, penaltyDays, userId);
    }
    @Override
    public List<MemberVO> selectMemberAll() throws DataAccessException {
        return jdbcTemplate.query(selectMemberAllSql, (rs, rowNum) -> mapMember(rs));
    }
    @Override
    public void inserttrandetail(MemberTranVO tran){
        jdbcTemplate.update(
                inserttrandetail,
                tran.getId(),
                tran.getInout_type(),
                tran.getTran_amt(),
                tran.getTran_date(),
                tran.getAccount_idx(),
                tran.getBalance_after()
        );
    }
    @Override
    public void updatebalance(int tran_amt, int idx){
        jdbcTemplate.update(updatebalance, tran_amt,idx);
    }

    @Override
    public List<MemberTranVO> selectTransactionsByAccount(String userId, int accountIdx) {
        return jdbcTemplate.query(
                selectTransactionsByAccount,
                (rs, rowNum) -> {
                    MemberTranVO transaction = new MemberTranVO();
                    transaction.setMember_tran_idx(rs.getInt("member_tran_idx"));
                    transaction.setAccount_idx(rs.getInt("account_idx"));
                    transaction.setId(rs.getString("id"));
                    transaction.setTran_amt(rs.getInt("tran_amt"));
                    transaction.setInout_type(rs.getString("inout_type"));
                    transaction.setTran_date(rs.getString("tran_date"));
                    transaction.setBalance_after(rs.getObject("balance_after", Long.class));
                    transaction.setReg_date(rs.getString("reg_date"));
                    return transaction;
                },
                userId,
                accountIdx
        );
    }
    @Override
        public void deleteCard(int card_idx,String id) {
            jdbcTemplate.update(deleteCard, card_idx, id);
        }
}