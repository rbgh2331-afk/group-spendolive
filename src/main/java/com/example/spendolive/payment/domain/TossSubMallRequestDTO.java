package com.example.spendolive.payment.domain;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TossSubMallRequestDTO {
    private String refSellerId;       // 🌟 "SELLER_" + userId
    private String businessType;      // 🌟 "INDIVIDUAL"

    private AccountInfo account;      // 계좌 정보 객체
    private IndividualInfo individual; // 개인 정보 객체 (businessType이 INDIVIDUAL일 때 필수)

    @Getter
    @Builder
    public static class AccountInfo {
        private String bankCode;      // 🌟 bank -> bankCode로 변경 (예: "088")
        private String accountNumber; 
        private String holderName;    
    }

    @Getter
    @Builder
    public static class IndividualInfo {
        private String name;  
        private String email; 
        private String phone;         // 하이픈 없이 숫자만
    }
}
// 권한 문재로 현재는 사용 X