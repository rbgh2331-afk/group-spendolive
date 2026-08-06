package com.example.spendolive.mypage.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spendolive.common.ajax.AjaxAuthSupport;
import com.example.spendolive.common.ajax.AjaxEndpoint;
import com.example.spendolive.common.ajax.AjaxResponse;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.service.MemberService;

import jakarta.servlet.http.HttpSession;

@RestController
@AjaxEndpoint
@RequestMapping("/spendolive/mypage/ajax")
/**
 * [마이페이지 AJAX 전용 Controller]
 * 회원정보·계좌·카드의 기존 MemberService를 재사용하고 화면 전체 이동 없이 처리 결과를 반환한다.
 * 계좌·카드 담당 로직의 Service와 Repository는 수정하지 않았다.
 */
public class MyPageAjaxController {

    private final MemberService memberService;

    public MyPageAjaxController(MemberService memberService) {
        this.memberService = memberService;
    }

    // [AJAX 변경] 이메일·전화 인증과 비밀번호 검증이 모두 끝난 뒤에만 회원정보를 저장한다.
    @PostMapping("/update.do")
    public ResponseEntity<?> updateProfile(@ModelAttribute MemberVO formMember,
                                           @RequestParam(value = "currentPassword", required = false) String currentPassword,
                                           @RequestParam(value = "passwordConfirm", required = false) String passwordConfirm,
                                           @RequestParam(value = "passwordChecked", required = false) String passwordChecked,
                                           HttpSession session) {
        MemberVO loginMember = AjaxAuthSupport.member(session);
        if (loginMember == null) return AjaxAuthSupport.unauthorized();
        try {
            MemberVO savedMember = memberService.getMemberById(loginMember.getId());
            if (savedMember == null) return AjaxAuthSupport.unauthorized();

            if (isChanged(formMember.getEmail(), savedMember.getEmail())
                    && !isVerified(session, "mypageEmailVerified", "mypageEmailVerifiedValue", formMember.getEmail())) {
                return ResponseEntity.badRequest().body(AjaxResponse.failure("EMAIL_NOT_VERIFIED", "변경한 이메일 인증을 완료해주세요."));
            }
            if (isChanged(formMember.getPhone(), savedMember.getPhone())
                    && !isVerified(session, "mypagePhoneVerified", "mypagePhoneVerifiedValue", formMember.getPhone())) {
                return ResponseEntity.badRequest().body(AjaxResponse.failure("PHONE_NOT_VERIFIED", "변경한 전화번호 인증을 완료해주세요."));
            }

            String newPassword = formMember.getPassword();
            boolean changePassword = newPassword != null && !newPassword.isBlank();
            if (changePassword) {
                if (currentPassword == null || !currentPassword.equals(savedMember.getPassword())) {
                    return ResponseEntity.badRequest().body(AjaxResponse.failure("PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다."));
                }
                if (passwordConfirm == null || !newPassword.equals(passwordConfirm)) {
                    return ResponseEntity.badRequest().body(AjaxResponse.failure("PASSWORD_CONFIRM_MISMATCH", "새 비밀번호 확인이 일치하지 않습니다."));
                }
                if (!"Y".equals(passwordChecked)) {
                    return ResponseEntity.badRequest().body(AjaxResponse.failure("PASSWORD_CHECK_REQUIRED", "비밀번호 확인 버튼을 먼저 눌러주세요."));
                }
            }

            formMember.setId(loginMember.getId());
            memberService.updateMyInfo(formMember, changePassword ? newPassword : null);
            MemberVO refreshed = memberService.getMemberById(loginMember.getId());
            if (refreshed != null) session.setAttribute("memberInfo", refreshed);
            clearVerification(session);
            return ResponseEntity.ok(AjaxResponse.success("회원정보가 수정되었습니다.",
                    Map.of("refreshUrl", "/spendolive/mypage.do#profile-edit")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "회원정보 수정 중 오류가 발생했습니다."));
        }
    }

    // [AJAX 변경] 계좌 제목을 검증한 뒤 기존 계좌명 수정 Service를 호출한다.
    @PostMapping("/account/name/update.do")
    public ResponseEntity<?> updateAccountName(@RequestParam("accountIdx") int accountIdx,
                                                @RequestParam("accountName") String accountName,
                                                HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();
        String safeName = accountName == null ? "" : accountName.trim();
        if (safeName.isBlank() || safeName.length() > 20) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "계좌 제목은 1~20자로 입력해주세요."));
        }
        try {
            memberService.updateAccountName(member.getId(), accountIdx, safeName);
            return ResponseEntity.ok(AjaxResponse.success("계좌 제목이 수정되었습니다.", Map.of("accountName", safeName, "refreshUrl", "/spendolive/mypage.do#asset-manage")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "계좌 제목 수정에 실패했습니다."));
        }
    }

    // [AJAX 변경] 주계좌 변경 완료 후 자산관리 영역 갱신 주소를 반환한다.
    @PostMapping("/account/primary/update.do")
    public ResponseEntity<?> updatePrimaryAccount(@RequestParam("accountIdx") int accountIdx, HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();
        try {
            memberService.updatePrimaryAccount(member.getId(), accountIdx);
            return ResponseEntity.ok(AjaxResponse.success("주계좌가 변경되었습니다.",
                    Map.of("refreshUrl", "/spendolive/mypage.do#asset-manage")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "주계좌 변경에 실패했습니다."));
        }
    }

    // [AJAX 변경] 카드 표시 이름을 검증한 뒤 로그인 회원이 소유한 카드만 수정한다.
    @PostMapping("/card/name/update.do")
    public ResponseEntity<?> updateCardName(@RequestParam("cardIdx") int cardIdx,
                                             @RequestParam("cardName") String cardName,
                                             HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();

        String safeName = cardName == null ? "" : cardName.trim();
        if (safeName.isBlank() || safeName.length() > 30) {
            return ResponseEntity.badRequest().body(
                    AjaxResponse.failure("INVALID_REQUEST", "카드 이름은 1~30자로 입력해주세요."));
        }

        try {
            memberService.updateCardName(member.getId(), cardIdx, safeName);
            return ResponseEntity.ok(AjaxResponse.success("카드 이름이 수정되었습니다.",
                    Map.of("cardName", safeName, "refreshUrl", "/spendolive/mypage.do#asset-manage")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    AjaxResponse.failure("SERVER_ERROR", "카드 이름 수정에 실패했습니다."));
        }
    }

    // [AJAX 변경] 주카드 변경 완료 후 자산관리 영역 갱신 주소를 반환한다.
    @PostMapping("/card/primary/update.do")
    public ResponseEntity<?> updatePrimaryCard(@RequestParam("cardIdx") int cardIdx, HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();
        try {
            memberService.updatePrimaryCard(member.getId(), cardIdx);
            return ResponseEntity.ok(AjaxResponse.success("주카드가 변경되었습니다.",
                    Map.of("refreshUrl", "/spendolive/mypage.do#asset-manage")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "주카드 변경에 실패했습니다."));
        }
    }
    // [AJAX 변경] 주카드 변경 완료 후 자산관리 영역 갱신 주소를 반환한다.
    @PostMapping("/card/delete.do")
    public ResponseEntity<?> deleteCard(@RequestParam("cardIdx") int cardIdx, HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();
        try {
            memberService.deleteCard(cardIdx,member.getId());
            return ResponseEntity.ok(AjaxResponse.success("카드가 삭제되었습니다.",
                    Map.of("refreshUrl", "/spendolive/mypage.do#asset-manage")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "카드가 삭제에 실패했습니다."));
        }
    }
    // 공백 차이로 불필요한 재인증이 발생하지 않도록 정리한 값끼리 비교한다.
    private boolean isChanged(String newValue, String oldValue) {
        return !(newValue == null ? "" : newValue.trim()).equals(oldValue == null ? "" : oldValue.trim());
    }

    // 인증 완료 플래그와 실제 인증한 값이 현재 요청값과 모두 일치하는지 확인한다.
    private boolean isVerified(HttpSession session, String flag, String value, String requestedValue) {
        return "Y".equals(session.getAttribute(flag)) && requestedValue != null && requestedValue.equals(session.getAttribute(value));
    }

    // 저장이 끝난 인증 정보는 세션에서 제거해 다음 수정에 재사용되지 않게 한다.
    private void clearVerification(HttpSession session) {
        String[] keys = {"mypageEmailCode", "mypageEmailTarget", "mypageEmailVerified", "mypageEmailVerifiedValue",
                "mypagePhoneCode", "mypagePhoneTarget", "mypagePhoneVerified", "mypagePhoneVerifiedValue"};
        for (String key : keys) session.removeAttribute(key);
    }
}
