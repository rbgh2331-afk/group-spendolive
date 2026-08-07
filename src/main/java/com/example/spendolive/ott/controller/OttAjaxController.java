package com.example.spendolive.ott.controller;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spendolive.common.ajax.AjaxDuplicateGuard;
import com.example.spendolive.common.ajax.AjaxEndpoint;
import com.example.spendolive.common.ajax.AjaxResponse;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.service.OttService;

import jakarta.servlet.http.HttpSession;

@RestController
@AjaxEndpoint
@RequestMapping("/spendolive/ott/ajax")
/**
 * [OTT 사용자 기능 AJAX 전용 Controller]
 * 방 생성·빠른 참가 확인·정산·나가기 처리를 기존 OttService로 실행하고 JSON으로 결과를 반환한다.
 * 결제사 외부 화면 이동은 AJAX로 대체하지 않고, 빠른 참가 확인이 끝난 뒤 기존 결제 화면으로 이동한다.
 */
public class OttAjaxController {

    private final OttService ottService;
    private final AjaxDuplicateGuard duplicateGuard;

    public OttAjaxController(OttService ottService, AjaxDuplicateGuard duplicateGuard) {
        this.ottService = ottService;
        this.duplicateGuard = duplicateGuard;
    }

    // [AJAX 변경] 가족·지인 공유방 생성 요청의 중복 실행과 계좌 미연동을 차단한다.
    @PostMapping("/friends/create.do")
    public ResponseEntity<?> createFriendRoom(@ModelAttribute OttRoomDTO roomDTO, HttpSession session) {
        MemberVO member = requireLinkedMember(session);
        if (!"YES".equals(member.getAccount_status())) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("ACCOUNT_REQUIRED", "OTT 기능은 계좌 연동이 필요합니다."));
        }
        String key = "friend-create:" + member.getId();
        if (!duplicateGuard.tryAcquire(key, Duration.ofSeconds(5))) return duplicateResponse();
        try {
            ottService.createFriendRoom(roomDTO, member.getId());
            return ResponseEntity.ok(AjaxResponse.success("가족·지인 공유방을 개설했습니다.",
                    Map.of("refreshUrl", "/spendolive/ott/friends.do")));
        } catch (Exception e) {
            duplicateGuard.release(key);
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "공유방 개설에 실패했습니다. 입력값을 확인해주세요."));
        }
    }

    // [AJAX 변경] 외부 모집방 생성 요청의 중복 실행과 계좌 미연동을 차단한다.
    @PostMapping("/recruit/create.do")
    public ResponseEntity<?> createRecruitRoom(@ModelAttribute OttRoomDTO roomDTO, HttpSession session) {
        MemberVO member = requireLinkedMember(session);
        if (!"YES".equals(member.getAccount_status())) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("ACCOUNT_REQUIRED", "OTT 기능은 계좌 연동이 필요합니다."));
        }
        String key = "recruit-create:" + member.getId();
        if (!duplicateGuard.tryAcquire(key, Duration.ofSeconds(5))) return duplicateResponse();
        try {
            ottService.createRecruitRoom(roomDTO, member.getId());
            return ResponseEntity.ok(AjaxResponse.success("외부 모집방을 개설했습니다.",
                    Map.of("refreshUrl", "/spendolive/ott/recruit.do?tab=all")));
        } catch (Exception e) {
            duplicateGuard.release(key);
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "모집방 개설에 실패했습니다. 입력값을 확인해주세요."));
        }
    }

    // [AJAX 변경] 참가자를 바로 저장하지 않고 참가 가능한 방만 확인한 뒤 결제 주소를 반환한다.
    @PostMapping("/recruit/quick-join.do")
    public ResponseEntity<?> quickJoin(@RequestParam(value = "ott_service_id", required = false) Long ottServiceId,
                                       HttpSession session) {
        MemberVO member = requireLinkedMember(session);
        if (!"YES".equals(member.getAccount_status())) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("ACCOUNT_REQUIRED", "OTT 기능은 계좌 연동이 필요합니다."));
        }
        if (ottServiceId == null) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "빠른 참가를 하려면 OTT 종류를 선택해주세요."));
        }
        try {
            Long roomId = ottService.findQuickJoinRecruitRoomId(ottServiceId, member.getId());
            if (roomId == null) {
                return ResponseEntity.status(404).body(AjaxResponse.failure("NOT_FOUND", "참가 가능한 모집방이 없습니다."));
            }
            return ResponseEntity.ok(AjaxResponse.success("참가 가능한 방을 찾았습니다.",
                    Map.of("redirectUrl", "/payment/detail.do?room_id=" + roomId + "&room_mode=RECRUIT")));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "참가 가능한 방을 확인하지 못했습니다."));
        }
    }

    // [AJAX 변경] 동일 결제 ID의 연속 처리를 차단하고 실제 저장 후에만 화면 갱신 주소를 반환한다.
    @PostMapping("/settlement/pay.do")
    public ResponseEntity<?> paySettlement(@RequestParam("payment_id") Long paymentId,
                                           @RequestParam(value = "returnPage", defaultValue = "recruit") String returnPage,
                                           HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        String key = "settlement-pay:" + member.getId() + ':' + paymentId;
        if (!duplicateGuard.tryAcquire(key, Duration.ofSeconds(10))) return duplicateResponse();
        try {
            ottService.markPaymentPaid(paymentId, member.getId());
            return ResponseEntity.ok(AjaxResponse.success("정산 결제가 완료 처리되었습니다.",
                    Map.of("refreshUrl", "friends".equals(returnPage)
                            ? "/spendolive/ott/friends.do"
                            : "/spendolive/ott/recruit.do?tab=settlement")));
        } catch (Exception e) {
            duplicateGuard.release(key);
            return ResponseEntity.internalServerError().body(AjaxResponse.failure("SERVER_ERROR", "정산 결제 처리에 실패했습니다."));
        }
    }

    // [AJAX 변경] 방 종료 예약은 동일 방에 대한 연속 요청을 막은 뒤 기존 Service를 호출한다.
    @PostMapping("/room/close-request.do")
    public ResponseEntity<?> closeRoom(@RequestParam("room_id") Long roomId,
                                       @RequestParam(value = "close_notice", required = false) String closeNotice,
                                       @RequestParam(value = "close_reason", required = false) String closeReason,
                                       @RequestParam(value = "returnPage", defaultValue = "friends") String returnPage,
                                       HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        String key = "room-close:" + member.getId() + ':' + roomId;
        if (!duplicateGuard.tryAcquire(key, Duration.ofSeconds(5))) return duplicateResponse();
        try {
            ottService.requestRoomClose(roomId, member.getId(), closeNotice, closeReason);
            return ResponseEntity.ok(AjaxResponse.success("방 종료 예약이 처리되었습니다.",
                    Map.of("refreshUrl", "recruit".equals(returnPage)
                            ? "/spendolive/ott/recruit.do?tab=manage"
                            : "/spendolive/ott/friends.do")));
        } catch (Exception e) {
            duplicateGuard.release(key);
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "방 종료 예약에 실패했습니다."));
        }
    }

    // [AJAX 변경] 나가기 예약 결과 문구와 현재 화면을 다시 불러올 주소를 반환한다.
    @PostMapping("/room/leave-reserve.do")
    public ResponseEntity<?> reserveLeave(@RequestParam("room_id") Long roomId,
                                          @RequestParam(value = "returnPage", defaultValue = "recruit") String returnPage,
                                          HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        try {
            String message = ottService.reserveRoomLeave(roomId, member.getId());
            return ResponseEntity.ok(AjaxResponse.success(message,
                    Map.of("refreshUrl", refreshRoomUrl(returnPage))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "나가기 예약에 실패했습니다."));
        }
    }

    // [AJAX 변경] 나가기 예약 취소 결과 문구와 현재 화면 갱신 주소를 반환한다.
    @PostMapping("/room/leave-cancel.do")
    public ResponseEntity<?> cancelLeave(@RequestParam("room_id") Long roomId,
                                         @RequestParam(value = "returnPage", defaultValue = "recruit") String returnPage,
                                         HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        try {
            String message = ottService.cancelRoomLeave(roomId, member.getId());
            return ResponseEntity.ok(AjaxResponse.success(message,
                    Map.of("refreshUrl", refreshRoomUrl(returnPage))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", "나가기 예약 취소에 실패했습니다."));
        }
    }

    // [내 담당 로그인 공통화] 로그인 판정은 공통 JS에서 처리하고 AJAX 요청에서는 세션 회원정보만 사용한다.
    private MemberVO requireLinkedMember(HttpSession session) {
        return (MemberVO) session.getAttribute("memberInfo");
    }

    // 가족방과 외부 모집방에서 각각 돌아가야 할 부분 갱신 주소를 구분한다.
    private String refreshRoomUrl(String returnPage) {
        return "friends".equals(returnPage) ? "/spendolive/ott/friends.do" : "/spendolive/ott/recruit.do?tab=manage";
    }

    // 중복 요청은 성공처럼 처리하지 않고 HTTP 409와 공통 실패 코드를 반환한다.
    private ResponseEntity<AjaxResponse<Void>> duplicateResponse() {
        return ResponseEntity.status(409).body(AjaxResponse.failure("DUPLICATE_REQUEST", "이미 처리 중인 요청입니다. 잠시 후 다시 시도해주세요."));
    }
}
