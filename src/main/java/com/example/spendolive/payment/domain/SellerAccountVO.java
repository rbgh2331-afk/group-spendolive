package com.example.spendolive.payment.domain;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                // Getter, Setter, toString, hashCode, equals 자동 생성
@NoArgsConstructor   // 기본 생성자 생성
@AllArgsConstructor  // 모든 필드를 포함한 생성자 생성
@Builder             // 빌더 패턴 지원 (토스 API나 서비스단에서 객체 생성할 때 개편함)
public class SellerAccountVO {

    private Long seller_idx;         // SELLER_IDX (NUMBER -> 자바에서는 Long이 정석!)
    private String member_id;        // MEMBER_ID (VARCHAR2)
    private String bank_name;        // BANK_NAME (VARCHAR2)
    private String account_number;   // ACCOUNT_NUMBER (VARCHAR2)
    private String traceId;         // traceId (VARCHAR2) - 금결원 거래고유번호나 토스 서브몰 ID 킵하는 용도
    private Date reg_date;           // REG_DATE (DATE -> java.util.Date)  YYYY/mm/DD

}
// 권한 문재로 현재는 사용 X
