package com.example.spendolive.mypage.service;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.spendolive.Expense.service.ExpenseService;
import com.example.spendolive.inquiry.service.FileStorageService;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberCardVO;
import com.example.spendolive.member.service.MemberService;
import com.example.spendolive.mypage.domain.MyPageDTO;
import com.example.spendolive.mypage.repository.MyPageReportRepository;
import com.example.spendolive.mypage.repository.MyPageRepository;
import com.example.spendolive.ott.service.OttService;

@Service
public class MyPageServiceImpl implements MyPageService {

    private final MemberService memberService;
    private final MyPageRepository myPageRepository;
    private final MyPageReportRepository myPageReportRepository;
    private final OttService ottService;
    private final ExpenseService expenseService;
    private final FileStorageService fileStorageService;

    public MyPageServiceImpl(MemberService memberService,
                             MyPageRepository myPageRepository,
                             MyPageReportRepository myPageReportRepository,
                             OttService ottService,
                             ExpenseService expenseService,
                             FileStorageService fileStorageService) {
        this.memberService = memberService;
        this.myPageRepository = myPageRepository;
        this.myPageReportRepository = myPageReportRepository;
        this.ottService = ottService;
        this.expenseService = expenseService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public MyPageDTO getMyPage(String loginId) throws Exception {
    
        MemberVO memberInfo = memberService.getMemberById(loginId);
    
        /* =========================================================
           [마이페이지 계좌·카드 연결 추가 시작]
           담당자가 만든 계좌·카드 목록 조회 메서드를 호출한다.
           조회 결과가 null 또는 빈 목록이어도 마이페이지가 500 오류 없이 열리게 처리한다.
           첫 번째 계좌는 오픈뱅킹 연결 여부 확인용으로만 사용한다.
           ========================================================= */
        List<MemberAccountVO> accountInfoList = memberService.getAccountById(loginId);
        List<MemberCardVO> cardInfoList = memberService.getCardById(loginId);

        accountInfoList = accountInfoList == null ? List.of() : accountInfoList;
        cardInfoList = cardInfoList == null ? List.of() : cardInfoList;
        MemberAccountVO linkedAccountInfo = accountInfoList.isEmpty() ? null : accountInfoList.get(0);
        /* [마이페이지 계좌·카드 연결 추가 끝] */
        MyPageDTO myPage = new MyPageDTO();
    
        myPage.setMemberInfo(memberInfo);
        myPage.setProfileInitial(makeProfileInitial(memberInfo));
        // 메인·지출관리와 같은 ExpenseService 결과를 합산한다.
        // 반복 지출은 화면에서 자동 생성되므로 DB SUM만 사용하면 마이페이지 금액과 달라질 수 있다.
        int thisMonthExpenseTotal = memberInfo == null
                ? 0
                : expenseService.getExpenseList(
                        Long.valueOf(memberInfo.getMember_id()),
                        YearMonth.now().toString()
                  ).stream()
                   .mapToInt(expense -> expense.getAmount() == null ? 0 : expense.getAmount())
                   .sum();

        // 현재 달 예산을 조회해 지출 대비 사용률을 계산한다.
        int thisMonthBudget = memberInfo == null
                ? 0
                : expenseService.getMonthlyBudget(
                        Long.valueOf(memberInfo.getMember_id()),
                        YearMonth.now().toString()
                );

        int thisMonthBudgetPercent = thisMonthBudget <= 0
                ? 0
                : (int) Math.round(thisMonthExpenseTotal * 100.0 / thisMonthBudget);

        // 계산 결과를 마이페이지 DTO에 저장한다.
        myPage.setThisMonthExpenseTotal(thisMonthExpenseTotal);
        myPage.setThisMonthBudget(thisMonthBudget);
        myPage.setThisMonthBudgetPercent(thisMonthBudgetPercent);
    
        myPage.setAccountConnected(isAccountConnected(linkedAccountInfo));
    
        myPage.setOpenBankUserSeq(
                linkedAccountInfo == null
                        ? null
                        : linkedAccountInfo.getOpen_bank_user_seq()
        );
        /* [마이페이지 계좌·카드 연결 추가] JSP로 전달할 전체 목록을 DTO에 저장한다. */
        myPage.setAccountList(accountInfoList);
        myPage.setCardList(cardInfoList);
    
        myPage.setWarning_count(Math.max(
                memberInfo == null ? 0 : memberInfo.getWarning_count(),
                myPageReportRepository.selectwarning_count(loginId)
        ));
    
        myPage.setMyReportCount(
                myPageReportRepository.selectMyReportCount(loginId)
        );
    
        myPage.setMyReportList(
                myPageReportRepository.selectMyReportList(loginId)
        );
    
        myPage.setFriendRoomList(
                ottService.getFriendRooms(loginId)
        );
    
        myPage.setHostedRecruitRoomList(
                ottService.getHostedRecruitRooms(loginId)
        );
    
        myPage.setJoinedRecruitRoomList(
                ottService.getJoinedRecruitRooms(loginId)
        );
    
        return myPage;
    }


    @Override
    @Transactional
    public void withdrawMember(String loginId) throws Exception {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("회원탈퇴 대상 아이디가 없습니다.");
        }

        int updatedCount = myPageRepository.withdrawMember(loginId);
        if (updatedCount == 0) {
            throw new IllegalStateException("이미 탈퇴했거나 존재하지 않는 회원입니다.");
        }
    }

    // [회원탈퇴 개선] 사용자 본인 탈퇴는 활성 OTT 관계와 처리 중 환불이 모두 없을 때만 진행한다.
    @Override
    @Transactional
    public MyPageDTO withdrawSelfMember(String loginId) throws Exception {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("회원탈퇴 대상 아이디가 없습니다.");
        }

        // [회원탈퇴 개선] 동일 회원의 중복 탈퇴 요청을 막고 기존 member_id를 익명화 과정에 사용한다.
        int memberId = myPageRepository.selectActiveMemberIdForUpdate(loginId);

        // [회원탈퇴 개선] 기존 OttService 조회 로직을 재사용해 가족방과 외부 모집방을 함께 검사한다.
        int ownedRoomCount = (int) ottService.getHostedRooms(loginId).stream()
                .filter(room -> !"CLOSED".equals(room.getStatus()) && !"END".equals(room.getStatus()))
                .count();

        int joinedRoomCount = (int) ottService.getMyRooms(loginId).stream()
                .filter(room -> !loginId.equals(room.getHost_login_id()))
                .filter(room -> !"CLOSED".equals(room.getStatus()) && !"END".equals(room.getStatus()))
                .count();

        int pendingRefundCount = myPageRepository.selectPendingRefundCount(loginId);

        MyPageDTO result = new MyPageDTO();
        result.setOwnedRoomCount(ownedRoomCount);
        result.setJoinedRoomCount(joinedRoomCount);
        result.setPendingRefundCount(pendingRefundCount);

        // [회원탈퇴 개선] 제한 사유가 있으면 데이터 변경 없이 개수만 Controller에 반환한다.
        if (!result.isWithdrawEligible()) {
            return result;
        }

        // [회원탈퇴 정책 변경] 문의 DB 행을 삭제하기 전에 문의 번호를 보관해 실제 첨부파일 폴더도 함께 정리한다.
        List<Integer> inquiryIdList = myPageRepository.selectInquiryIds(loginId);

        // [회원탈퇴 개선] 기존 아이디와 이메일을 비워 즉시 신규 회원가입에 사용할 수 있게 한다.
        String temporaryId = makeTemporaryId(memberId);
        String anonymousId = makeAnonymousId(memberId, loginId);
        String anonymousEmail = "deleted_" + memberId + "_" + loginId + "@spendolive.local";

        myPageRepository.withdrawSelfMember(memberId, loginId, temporaryId, anonymousId, anonymousEmail);

        // [회원탈퇴 정책 변경] DB 트랜잭션이 정상 커밋된 뒤에만 문의 첨부파일 디렉토리를 삭제한다.
        deleteInquiryFilesAfterCommit(inquiryIdList);
        return result;
    }

    // [회원탈퇴 정책 변경] DB 롤백 시 파일만 먼저 사라지는 일을 막기 위해 커밋 이후 실제 파일을 정리한다.
    private void deleteInquiryFilesAfterCommit(List<Integer> inquiryIdList) {
        if (inquiryIdList == null || inquiryIdList.isEmpty()) {
            return;
        }

        Runnable deleteFiles = () -> inquiryIdList.forEach(fileStorageService::deleteInquiryFiles);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteFiles.run();
                }
            });
            return;
        }

        // [회원탈퇴 정책 변경] 트랜잭션 동기화가 없는 예외적인 실행 환경에서는 DB 처리 직후 파일을 정리한다.
        deleteFiles.run();
    }

    // [회원탈퇴 개선] 임시 회원 아이디는 외래키 이동 중에만 사용하며 VARCHAR2(20)를 넘지 않게 한다.
    private String makeTemporaryId(int memberId) {
        String temporaryId = "TMP_" + memberId;
        if (temporaryId.length() > 20) {
            throw new IllegalStateException("회원탈퇴 임시 아이디 길이가 허용 범위를 초과했습니다.");
        }
        return temporaryId;
    }

    // [회원탈퇴 개선] 최종 아이디 형식은 LEAVE_회원번호_기존아이디이며 전체 길이는 20자로 제한한다.
    private String makeAnonymousId(int memberId, String loginId) {
        String fixedPart = "LEAVE_" + memberId + "_";
        int availableLength = 20 - fixedPart.length();
        if (availableLength < 1) {
            throw new IllegalStateException("회원탈퇴 익명 아이디 길이가 허용 범위를 초과했습니다.");
        }

        String limitedLoginId = loginId.length() > availableLength ? loginId.substring(0, availableLength) : loginId;
        return fixedPart + limitedLoginId;
    }

    private boolean isAccountConnected(MemberAccountVO accountInfo) {
        return accountInfo != null
                && accountInfo.getOpen_bank_token() != null
                && !accountInfo.getOpen_bank_token().isBlank()
                && accountInfo.getOpen_bank_user_seq() != null
                && !accountInfo.getOpen_bank_user_seq().isBlank();
    }

    private String makeProfileInitial(MemberVO memberInfo) {
        if (memberInfo == null) {
            return "회";
        }

        String name = memberInfo.getMember_name();
        if (name != null && !name.isBlank()) {
            return name.substring(0, 1);
        }

        String nickname = memberInfo.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname.substring(0, 1);
        }

        return "회";
    }
}
