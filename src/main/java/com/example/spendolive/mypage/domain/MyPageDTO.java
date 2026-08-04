package com.example.spendolive.mypage.domain;

import java.util.List;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.ott.domain.OttRoomDTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPageDTO {

    private MemberVO memberInfo;
    private String profileInitial;
    private int thisMonthExpenseTotal;

    // 마이페이지에 표시할 이번 달 예산과 사용률
    private int thisMonthBudget;
    private int thisMonthBudgetPercent;

    private boolean accountConnected;
    private String openBankUserSeq;

    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       담당자 로직에서 조회한 계좌·카드 목록을 Controller와 JSP까지 전달한다.
       ========================================================= */
    private List<MemberAccountVO> accountList;
    private List<MemberCardVO> cardList;
    
    private int warning_count;
    private int myReportCount;
    private List<MyPageReportDTO> myReportList;
    private List<OttRoomDTO> friendRoomList;
    private List<OttRoomDTO> hostedRecruitRoomList;
    private List<OttRoomDTO> joinedRecruitRoomList;

    /* [회원탈퇴 개선] 운영 중인 방, 참여 중인 방, 처리 중인 환불 건수를 한 번에 전달한다. */
    private int ownedRoomCount;
    private int joinedRoomCount;
    private int pendingRefundCount;

    /* [회원탈퇴 개선] 세 가지 제한 조건이 모두 없을 때만 자진탈퇴를 허용한다. */
    public boolean isWithdrawEligible() {
        return ownedRoomCount == 0 && joinedRoomCount == 0 && pendingRefundCount == 0;
    }
}
