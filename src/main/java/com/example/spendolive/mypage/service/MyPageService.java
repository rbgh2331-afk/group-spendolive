package com.example.spendolive.mypage.service;

import com.example.spendolive.mypage.domain.MyPageDTO;

public interface MyPageService {
    MyPageDTO getMyPage(String loginId) throws Exception;
    void withdrawMember(String loginId) throws Exception;

    // [회원탈퇴 개선] 본인 자진탈퇴는 OTT·환불 조건 검사와 개인정보 정리를 함께 처리한다.
    MyPageDTO withdrawSelfMember(String loginId) throws Exception;
}
