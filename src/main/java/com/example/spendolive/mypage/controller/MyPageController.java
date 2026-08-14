package com.example.spendolive.mypage.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberAccountVO;
import com.example.spendolive.member.domain.MemberTranVO;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.mypage.domain.MyPageDTO;
import com.example.spendolive.member.service.MemberService;
import com.example.spendolive.mypage.service.MyPageService;

@Controller
@RequestMapping("/spendolive")
public class MyPageController {
    private static final Logger log = LoggerFactory.getLogger(MyPageController.class);

    /* =========================================================
       [마이페이지 계좌·카드 연결 추가]
       DB의 은행코드를 JSP에서 사용자에게 보여줄 은행명으로 변환한다.
       ========================================================= */
    private static final Map<String, String> BANK_NAME_MAP = Map.ofEntries(
            Map.entry("002", "산업은행"),
            Map.entry("003", "기업은행"),
            Map.entry("004", "KB국민은행"),
            Map.entry("007", "Sh수협은행"),
            Map.entry("011", "NH농협은행"),
            Map.entry("020", "우리은행"),
            Map.entry("023", "SC제일은행"),
            Map.entry("027", "한국씨티은행"),
            Map.entry("031", "대구은행"),
            Map.entry("032", "부산은행"),
            Map.entry("034", "광주은행"),
            Map.entry("035", "제주은행"),
            Map.entry("037", "전북은행"),
            Map.entry("039", "경남은행"),
            Map.entry("081", "하나은행"),
            Map.entry("088", "신한은행"),
            Map.entry("089", "케이뱅크"),
            Map.entry("090", "카카오뱅크"),
            Map.entry("092", "토스뱅크")
    );

    /* =========================================================
       [마이페이지 카드사명 표시]
       MEMBER_CARD_TB.CARD_COMPANY에는 토스 issuerCode 원본을 유지하고,
       마이페이지 화면에서만 사용자에게 읽기 쉬운 카드사명으로 변환한다.
       CARD_NAME은 사용자가 수정하는 카드 별칭이므로 기존 기능을 그대로 유지한다.
       ========================================================= */
    private static final Map<String, String> CARD_COMPANY_NAME_MAP = Map.ofEntries(
            Map.entry("3K", "기업 BC"),
            Map.entry("46", "광주은행"),
            Map.entry("71", "롯데카드"),
            Map.entry("30", "한국산업은행"),
            Map.entry("31", "BC카드"),
            Map.entry("51", "삼성카드"),
            Map.entry("38", "새마을금고"),
            Map.entry("41", "신한카드"),
            Map.entry("62", "신협"),
            Map.entry("36", "씨티카드"),
            Map.entry("33", "우리BC카드(BC 매입)"),
            Map.entry("W1", "우리카드(우리 매입)"),
            Map.entry("37", "우체국예금보험"),
            Map.entry("39", "저축은행중앙회"),
            Map.entry("35", "전북은행"),
            Map.entry("42", "제주은행"),
            Map.entry("15", "카카오뱅크"),
            Map.entry("3A", "케이뱅크"),
            Map.entry("24", "토스뱅크"),
            Map.entry("21", "하나카드"),
            Map.entry("61", "현대카드"),
            Map.entry("11", "KB국민카드"),
            Map.entry("91", "NH농협카드"),
            Map.entry("34", "Sh수협은행")
    );

    private final MyPageService myPageService;
    private final MemberService memberService;

    public MyPageController(MyPageService myPageService, MemberService memberService) {
        this.myPageService = myPageService;
        this.memberService = memberService;
    }

    @RequestMapping(value = "/mypage.do", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView mypage(HttpSession session) throws Exception {
        MemberVO sessionMember = (MemberVO) session.getAttribute("memberInfo");

        
        MyPageDTO myPage = myPageService.getMyPage(sessionMember.getId());
        MemberVO memberInfo = myPage.getMemberInfo();
        if (memberInfo == null) {
            memberInfo = sessionMember;
            myPage.setMemberInfo(memberInfo);
        } else {
            session.setAttribute("memberInfo", memberInfo);
        }
        session.removeAttribute("log");
        ModelAndView mav = layout("/WEB-INF/views/member/mypage.jsp");
        mav.addObject("memberInfo", memberInfo);
        mav.addObject("profileInitial", myPage.getProfileInitial());
        mav.addObject("thisMonthExpenseTotal", myPage.getThisMonthExpenseTotal());

        // 이번 달 예산과 사용률을 마이페이지 JSP에 전달한다.
        mav.addObject("thisMonthBudget", myPage.getThisMonthBudget());
        mav.addObject("thisMonthBudgetPercent", myPage.getThisMonthBudgetPercent());
        mav.addObject("accountConnected", myPage.isAccountConnected());
        mav.addObject("openBankUserSeq", myPage.getOpenBankUserSeq());
        /* =========================================================
           [마이페이지 계좌·카드 연결 추가]
           JSP에서 계좌·카드 목록, 상단 현재 계좌, 은행명을 사용할 수 있게 전달한다.
           ========================================================= */
        mav.addObject("accountList", myPage.getAccountList());
        mav.addObject("cardList", myPage.getCardList());
        // STATUS가 YES인 주계좌만 상단 계좌관리 카드에 전달한다.
        mav.addObject("currentAccount", findPrimaryAccount(myPage));
        mav.addObject("bankNameMap", BANK_NAME_MAP);
        // CARD_COMPANY 원본 코드는 유지하고 JSP에서 카드사명으로 표시한다.
        mav.addObject("cardCompanyNameMap", CARD_COMPANY_NAME_MAP);
        mav.addObject("warning_count", myPage.getWarning_count());
        mav.addObject("myReportCount", myPage.getMyReportCount());
        mav.addObject("myReportList", myPage.getMyReportList());
        mav.addObject("friendRoomList", myPage.getFriendRoomList());
        mav.addObject("hostedRecruitRoomList", myPage.getHostedRecruitRoomList());
        mav.addObject("joinedRecruitRoomList", myPage.getJoinedRecruitRoomList());
        return mav;
    }

    @PostMapping("/mypage/update.do")
    public ModelAndView updateMyInfo(@ModelAttribute MemberVO formMember,
                                     @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                     @RequestParam(value = "passwordConfirm", required = false) String passwordConfirm,
                                     @RequestParam(value = "passwordChecked", required = false) String passwordChecked,
                                     HttpSession session) {
        ModelAndView mav = new ModelAndView();
        // [내 담당 로그인 공통화] 화면 진입 로그인 안내는 공통 JS에서 처리하므로 Controller의 중복 로그인 redirect 검사는 제거한다.
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        try {
            MemberVO savedMember = memberService.getMemberById(loginMember.getId());
            if (savedMember == null) {
                mav.setViewName("redirect:/spendolive/mypage.do?profileError=memberNotFound#profile-edit");
                return mav;
            }

            if (isChanged(formMember.getEmail(), savedMember.getEmail()) && !isVerified(session, "mypageEmailVerified", "mypageEmailVerifiedValue", formMember.getEmail())) {
                mav.setViewName("redirect:/spendolive/mypage.do?profileError=emailNotVerified#profile-edit");
                return mav;
            }

            if (isChanged(formMember.getPhone(), savedMember.getPhone()) && !isVerified(session, "mypagePhoneVerified", "mypagePhoneVerifiedValue", formMember.getPhone())) {
                mav.setViewName("redirect:/spendolive/mypage.do?profileError=phoneNotVerified#profile-edit");
                return mav;
            }

            String newPassword = formMember.getPassword();
            boolean changePassword = newPassword != null && !newPassword.isBlank();
            if (changePassword) {
                if (currentPassword == null || currentPassword.isBlank() || !memberService.matchesPassword(currentPassword, savedMember.getPassword())) {
                    mav.setViewName("redirect:/spendolive/mypage.do?profileError=currentPasswordMismatch#profile-edit");
                    return mav;
                }

                if (passwordConfirm == null || !newPassword.equals(passwordConfirm)) {
                    mav.setViewName("redirect:/spendolive/mypage.do?profileError=passwordMismatch#profile-edit");
                    return mav;
                }

                if (!"Y".equals(passwordChecked)) {
                    mav.setViewName("redirect:/spendolive/mypage.do?profileError=passwordCheckRequired#profile-edit");
                    return mav;
                }
            }

            formMember.setId(loginMember.getId());
            memberService.updateMyInfo(formMember, changePassword ? newPassword : null);

            MemberVO refreshedMember = memberService.getMemberById(loginMember.getId());
            if (refreshedMember != null) {
                session.setAttribute("memberInfo", refreshedMember);
            }

            clearMyPageVerificationSession(session);
            mav.setViewName("redirect:/spendolive/mypage.do?profileUpdated=Y");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?profileError=updateFailed#profile-edit");
        }

        return mav;
    }


    /* =========================================================
       [마이페이지 계좌·카드 연결 추가 시작]
       마이페이지 계좌 목록의 제목 수정 요청을 처리한다.
       로그인 확인, 공백·20자 제한 검사 후 Service를 호출한다.
       ========================================================= */
    @PostMapping("/mypage/account/name/update.do")
    public ModelAndView updateAccountName(@RequestParam("accountIdx") int accountIdx,
                                          @RequestParam("accountName") String accountName,
                                          HttpSession session) {
        ModelAndView mav = new ModelAndView();
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        String safeAccountName = accountName == null ? "" : accountName.trim();
        if (safeAccountName.isBlank() || safeAccountName.length() > 20) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=invalidAccountName#asset-manage");
            return mav;
        }

        try {
            memberService.updateAccountName(loginMember.getId(), accountIdx, safeAccountName);
            mav.setViewName("redirect:/spendolive/mypage.do?accountNameUpdated=Y#asset-manage");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=accountNameUpdateFailed#asset-manage");
        }

        return mav;
    }
    /* [마이페이지 계좌·카드 연결 추가 끝] */

    /* [기존 일반 POST 호환]
       JavaScript가 비활성화되거나 AJAX 공통 스크립트 로드에 실패해도 카드 이름을 수정할 수 있게 한다. */
    @PostMapping("/mypage/card/name/update.do")
    public ModelAndView updateCardName(@RequestParam("cardIdx") int cardIdx,
                                       @RequestParam("cardName") String cardName,
                                       HttpSession session) {
        ModelAndView mav = new ModelAndView();
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        String safeCardName = cardName == null ? "" : cardName.trim();
        if (safeCardName.isBlank() || safeCardName.length() > 30) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=invalidCardName#asset-manage");
            return mav;
        }

        try {
            memberService.updateCardName(loginMember.getId(), cardIdx, safeCardName);
            mav.setViewName("redirect:/spendolive/mypage.do?cardNameUpdated=Y#asset-manage");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=cardNameUpdateFailed#asset-manage");
        }
        return mav;
    }

    // 마이페이지 계좌 목록에서 선택한 계좌를 주계좌로 변경한다.
    @PostMapping("/mypage/account/primary/update.do")
    public ModelAndView updatePrimaryAccount(@RequestParam("accountIdx") int accountIdx,
                                             HttpSession session) {
        ModelAndView mav = new ModelAndView();
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        try {
            memberService.updatePrimaryAccount(loginMember.getId(), accountIdx);
            mav.setViewName("redirect:/spendolive/mypage.do?primaryAccountUpdated=Y#asset-manage");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=primaryAccountUpdateFailed#asset-manage");
        }

        return mav;
    }

    /* [기존 일반 POST 호환]
       AJAX 사용이 불가능한 경우에도 주카드 변경이 가능하도록 기존 주소를 유지한다. */
    @PostMapping("/mypage/card/primary/update.do")
    public ModelAndView updatePrimaryCard(@RequestParam("cardIdx") int cardIdx, HttpSession session) {
        ModelAndView mav = new ModelAndView();
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");
        try {
            memberService.updatePrimaryCard(loginMember.getId(), cardIdx);
            mav.setViewName("redirect:/spendolive/mypage.do?primaryCardUpdated=Y#asset-manage");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?assetError=primaryCardUpdateFailed#asset-manage");
        }
        return mav;
    }

    /* =========================================================
       [계좌 거래내역 Ajax 조회]
       선택한 계좌의 거래내역과 거래 직후 잔액을 최신순으로 반환한다.
       Repository에서 회원 아이디와 계좌 번호를 함께 검사한다.
       ========================================================= */
    @GetMapping("/mypage/account/transactions.do")
    @ResponseBody
    public ResponseEntity<?> getAccountTransactions(@RequestParam("accountIdx") int accountIdx,
                                                     HttpSession session) {
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        try {
            List<MemberTranVO> transactionList =
                    memberService.getTransactionsByAccount(loginMember.getId(), accountIdx);
            return ResponseEntity.ok(transactionList);
        } catch (Exception e) {
            log.error("거래내역 조회 중 오류가 발생했습니다.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "거래내역 조회에 실패했습니다."));
        }
    }

    @PostMapping("/mypage/withdraw.do")
    public ModelAndView withdrawMember(@RequestParam(value = "withdrawConfirm", required = false) String withdrawConfirm,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        ModelAndView mav = new ModelAndView();
        MemberVO loginMember = (MemberVO) session.getAttribute("memberInfo");

        if (withdrawConfirm == null || !"탈퇴합니다".equals(withdrawConfirm.trim())) {
            mav.setViewName("redirect:/spendolive/mypage.do?withdrawError=confirmRequired#withdraw-section");
            return mav;
        }

        try {
            // [회원탈퇴 개선] 본인 탈퇴 전용 서비스에서 방·참여·환불 조건을 먼저 확인한다.
            MyPageDTO result = myPageService.withdrawSelfMember(loginMember.getId());

            // [회원탈퇴 개선] 탈퇴 불가 사유를 한 번에 표시할 수 있도록 건수를 Flash Attribute로 전달한다.
            if (!result.isWithdrawEligible()) {
                redirectAttributes.addFlashAttribute("withdrawBlocked", true);
                redirectAttributes.addFlashAttribute("ownedRoomCount", result.getOwnedRoomCount());
                redirectAttributes.addFlashAttribute("joinedRoomCount", result.getJoinedRoomCount());
                redirectAttributes.addFlashAttribute("pendingRefundCount", result.getPendingRefundCount());
                mav.setViewName("redirect:/spendolive/mypage.do#withdraw-section");
                return mav;
            }

            // [회원탈퇴 개선] 모든 조건을 통과하고 익명화가 완료된 경우에만 현재 세션을 종료한다.
            session.invalidate();
            mav.setViewName("redirect:/member/loginForm.do?withdraw=Y");
        } catch (Exception e) {
            mav.setViewName("redirect:/spendolive/mypage.do?withdrawError=failed#withdraw-section");
        }

        return mav;
    }

    @PostMapping("/mypage/email/send.do")
    @ResponseBody
    public String sendMyPageEmailCode(@RequestParam("email") String email, HttpSession session) {
        try {
            String verificationCode = memberService.sendVerificationEmail(email);
            session.setAttribute("mypageEmailCode", verificationCode);
            session.setAttribute("mypageEmailTarget", email);
            session.removeAttribute("mypageEmailVerified");
            session.removeAttribute("mypageEmailVerifiedValue");
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    @PostMapping("/mypage/email/verify.do")
    @ResponseBody
    public boolean verifyMyPageEmailCode(@RequestParam("email") String email,
                                         @RequestParam("inputCode") String inputCode,
                                         HttpSession session) {
        String originalCode = (String) session.getAttribute("mypageEmailCode");
        String targetEmail = (String) session.getAttribute("mypageEmailTarget");

        if (originalCode != null && originalCode.equals(inputCode) && targetEmail != null && targetEmail.equals(email)) {
            session.setAttribute("mypageEmailVerified", "Y");
            session.setAttribute("mypageEmailVerifiedValue", email);
            session.removeAttribute("mypageEmailCode");
            session.removeAttribute("mypageEmailTarget");
            return true;
        }

        return false;
    }

    @PostMapping("/mypage/phone/send.do")
    @ResponseBody
    public String sendMyPagePhoneCode(@RequestParam("phone") String phone, HttpSession session) {
        try {
            String verificationCode = memberService.sendSmsVerification(phone);
            session.setAttribute("mypagePhoneCode", verificationCode);
            session.setAttribute("mypagePhoneTarget", phone);
            session.removeAttribute("mypagePhoneVerified");
            session.removeAttribute("mypagePhoneVerifiedValue");
            return "SUCCESS";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    @PostMapping("/mypage/phone/verify.do")
    @ResponseBody
    public boolean verifyMyPagePhoneCode(@RequestParam("phone") String phone,
                                         @RequestParam("inputCode") String inputCode,
                                         HttpSession session) {
        String originalCode = (String) session.getAttribute("mypagePhoneCode");
        String targetPhone = (String) session.getAttribute("mypagePhoneTarget");

        if (originalCode != null && originalCode.equals(inputCode) && targetPhone != null && targetPhone.equals(phone)) {
            session.setAttribute("mypagePhoneVerified", "Y");
            session.setAttribute("mypagePhoneVerifiedValue", phone);
            session.removeAttribute("mypagePhoneCode");
            session.removeAttribute("mypagePhoneTarget");
            return true;
        }

        return false;
    }

    private boolean isChanged(String newValue, String oldValue) {
        String safeNewValue = newValue == null ? "" : newValue.trim();
        String safeOldValue = oldValue == null ? "" : oldValue.trim();
        return !safeNewValue.equals(safeOldValue);
    }

    private boolean isVerified(HttpSession session, String flagName, String valueName, String requestValue) {
        String verifiedFlag = (String) session.getAttribute(flagName);
        String verifiedValue = (String) session.getAttribute(valueName);
        return "Y".equals(verifiedFlag) && verifiedValue != null && verifiedValue.equals(requestValue);
    }

    private void clearMyPageVerificationSession(HttpSession session) {
        session.removeAttribute("mypageEmailCode");
        session.removeAttribute("mypageEmailTarget");
        session.removeAttribute("mypageEmailVerified");
        session.removeAttribute("mypageEmailVerifiedValue");
        session.removeAttribute("mypagePhoneCode");
        session.removeAttribute("mypagePhoneTarget");
        session.removeAttribute("mypagePhoneVerified");
        session.removeAttribute("mypagePhoneVerifiedValue");
    }

    /* =========================================================
       [마이페이지 주계좌 조회 추가]
       계좌 목록 중 STATUS가 YES인 계좌만 상단 계좌관리 카드에 표시한다.
       주계좌를 아직 선택하지 않았다면 null을 반환한다.
       ========================================================= */
    private MemberAccountVO findPrimaryAccount(MyPageDTO myPage) {
        if (myPage == null || myPage.getAccountList() == null) {
            return null;
        }

        return myPage.getAccountList()
                .stream()
                .filter(account -> "YES".equals(account.getStatus()))
                .findFirst()
                .orElse(null);
    }

    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
}
