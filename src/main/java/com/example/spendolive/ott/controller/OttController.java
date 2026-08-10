package com.example.spendolive.ott.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.domain.OttChatMessageDTO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.ott.service.OttService;

// 사용자 OTT 요청 처리 - 화면 요청을 Service로 전달
@Controller
@RequestMapping("/spendolive")
public class OttController {

    private static final int RECRUIT_PAGE_SIZE = 10;

    private final OttService ottService;

    @Value("${kakao.javascript.key:}")
    private String kakaoJavascriptKey;

    public OttController(OttService ottService) {
        this.ottService = ottService;
    }

    // OTT 메인 화면 - 공유 가능한 OTT 목록, 전체 모집방 수, 참여방 수 조회
    @GetMapping("/ott.do")
    public String ottMain(Model model, HttpSession session) {
        String loginId = getLoginId(session);
        session.removeAttribute("log");
        model.addAttribute("serviceList", ottService.getShareableServices());
        model.addAttribute("recruitRoomCount", ottService.getRecruitRoomCount());
        
        // [내 담당 로그인 공통화] OTT 진입 로그인 안내는 공통 JS에서 처리하고 Controller는 화면 데이터만 구성한다.
        model.addAttribute("myRoomCount", ottService.getMyRoomCount(loginId));
        model.addAttribute("body_page", "/WEB-INF/views/ott/ott.jsp");
        return "common/layout";
    }

    // 가족·지인 공유방 화면 - 참여방, 방장 방, 정산 내역 조회
    @GetMapping("/ott/friends.do")
    public String friends(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);
        // 계좌 상태는 null 여부가 아니라 실제 연동 완료 값인 "YES"로 검사한다.
        // DB 기본값이 "NO"이므로 null만 검사하면 미연동 회원도 통과할 수 있다.
        if (!hasLinkedAccount(session)) {
            redirectAttributes.addFlashAttribute("msg", "OTT 관련 기능은 계좌 연동이 필요합니다.");
            return "redirect:/spendolive/main.do";
        }
        addCommonOttModel(model, loginId);
        model.addAttribute("myRoomList", ottService.getFriendRooms(loginId));
        model.addAttribute("hostedRoomList", ottService.getHostedFriendRooms(loginId));
        model.addAttribute("settlementList", ottService.getFriendSettlements(loginId));
        model.addAttribute("body_page", "/WEB-INF/views/ott/ottFriends.jsp");
        return "common/layout";
    }

    // 가족·지인 공유방 생성 - 생성자를 방장 멤버로 등록
    @PostMapping("/ott/friends/create.do")
    public String createFriendRoom(@ModelAttribute OttRoomDTO roomDTO,HttpSession session,RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);

        // 화면 주소를 거치지 않고 생성 URL을 직접 호출하는 경우도 막는다.
        if (!hasLinkedAccount(session)) {
            redirectAttributes.addFlashAttribute("msg", "OTT 관련 기능은 계좌 연동이 필요합니다.");
            return "redirect:/spendolive/main.do";
        }

        ottService.createFriendRoom(roomDTO, loginId);

        // 방 생성 완료 알림
        redirectAttributes.addFlashAttribute(
            "msg",
            "가족·지인 공유방을 개설했습니다."
        );
        return "redirect:/spendolive/ott/friends.do?result=created";
    }

    // 외부 모집방 화면 - 검색, 참여방, 정산 데이터 조회
    @GetMapping("/ott/recruit.do")
    public String recruit(@RequestParam(value = "tab", required = false, defaultValue = "all") String tab,
                          @RequestParam(value = "ott_service_id", required = false) String ott_service_id,
                          @RequestParam(value = "roomNameKeyword", required = false) String roomNameKeyword,
                          @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          HttpSession session) {
        String loginId = getLoginId(session);
        // OTT 화면과 생성·참가 요청에서 같은 계좌 연동 기준을 사용한다.
        if (!hasLinkedAccount(session)) {
            redirectAttributes.addFlashAttribute("msg", "OTT 관련 기능은 계좌 연동이 필요합니다.");
            return "redirect:/spendolive/main.do";
        }
        Long selectedOttServiceId = parseOttServiceId(ott_service_id);
        int totalRecruitRoomCount = ottService.getRecruitRoomCount(selectedOttServiceId, roomNameKeyword);
        int totalPages = totalRecruitRoomCount == 0
                ? 0
                : (totalRecruitRoomCount + RECRUIT_PAGE_SIZE - 1) / RECRUIT_PAGE_SIZE;
        int currentPage = totalPages == 0
                ? 1
                : Math.min(Math.max(page, 1), totalPages);

        addCommonOttModel(model, loginId);
        model.addAttribute("tab", tab);
        model.addAttribute("selectedOttServiceId", selectedOttServiceId);
        model.addAttribute("roomNameKeyword", roomNameKeyword);
        model.addAttribute("recruitRoomList", ottService.getRecruitRooms(
                loginId,
                selectedOttServiceId,
                roomNameKeyword,
                currentPage,
                RECRUIT_PAGE_SIZE));
        model.addAttribute("totalRecruitRoomCount", totalRecruitRoomCount);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("hostedRoomList", ottService.getHostedRecruitRooms(loginId));
        model.addAttribute("joinedRoomList", ottService.getJoinedRecruitRooms(loginId));
        model.addAttribute("hostedRoomMemberList", ottService.getHostedRoomMembers(loginId));
        model.addAttribute("settlementList", ottService.getRecruitSettlements(loginId));
        model.addAttribute("body_page", "/WEB-INF/views/ott/ottRecruit.jsp");
        return "common/layout";
    }

    // 외부 모집방 생성 - 방 정보와 READY 정산 데이터 생성
    @PostMapping("/ott/recruit/create.do")
    public String createRecruitRoom(@ModelAttribute OttRoomDTO roomDTO, HttpSession session, RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);
        // DB 기본값 "NO"를 확실히 차단하기 위해 "YES"만 허용한다.
        if (!hasLinkedAccount(session)) {
            redirectAttributes.addFlashAttribute("msg", "OTT 관련 기능은 계좌 연동이 필요합니다.");
            return "redirect:/spendolive/main.do";
        }
        ottService.createRecruitRoom(roomDTO, loginId);

        // 방 생성 완료 알림
        redirectAttributes.addFlashAttribute(
            "msg",
            "외부 모집방을 개설했습니다."
        );
        return "redirect:/spendolive/ott/recruit.do?tab=all&result=created";
    }

    // 빠른 참가 처리 - 참가 가능한 가장 오래된 모집방 조회
    @PostMapping("/ott/recruit/quick-join.do")
    public String quickJoinRecruitRoom(@RequestParam(value = "ott_service_id", required = false) String ott_service_id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);

        // 빠른 참가도 OTT 기능이므로 동일한 계좌 연동 검사를 적용한다.
        if (!hasLinkedCard(session)) {
            redirectAttributes.addFlashAttribute("msg", "OTT 관련 기능은 카드 등록이 필요합니다.");
            return "redirect:/spendolive/main.do";
        }

        // parseOttServiceId()는 OttController 안에 이미 있는 변환용 메서드
        Long selectedOttServiceId = parseOttServiceId(ott_service_id);

        if (selectedOttServiceId == null) {
            redirectAttributes.addFlashAttribute("msg", "빠른 참가를 하려면 OTT 종류를 먼저 선택해 주세요.");
            return "redirect:/spendolive/ott/recruit.do?tab=all";
        }

        // 선택한 OTT 기준으로 참가 가능한 가장 오래된 빈 방의 roomId를 찾는다.
        // 여기서는 아직 DB에 참여자로 저장하지 않는다.
        // 결제 흐름을 거쳐야 하므로 "방 찾기"만 한다.
        Long room_id = ottService.findQuickJoinRecruitRoomId(selectedOttServiceId, loginId);

        // 참가 가능한 방이 없는 경우
        if (room_id == null) {
            redirectAttributes.addFlashAttribute("msg", "참가 가능한 모집방이 없습니다.");
            return "redirect:/spendolive/ott/recruit.do?tab=all&ott_service_id=" + selectedOttServiceId;
        }

        // 기존 신청하기와 동일한 결제 흐름으로 이동
        // 일반 신청하기도 최종적으로 roomId를 결제쪽으로 넘긴다.
        // 빠른 참가는 서버에서 자동으로 찾은 roomId를 넘긴다는 점만 다르다.
        // 결제쪽은 기존 room_id 기반 결제 로직을 그대로 사용하면 된다.
        return redirectToRoomPayment(room_id, "RECRUIT", null);
    }

    // 가족방 초대 입장 - 초대 코드 확인 후 결제 화면 이동
    @GetMapping("/ott/friends/invite.do")
    public String enterFriendRoomByInvite(@RequestParam("code") String invite_code) {
        OttRoomDTO room = ottService.getRoomByInviteCode(invite_code);

        if (room == null || !"FRIEND".equals(room.getRoom_mode()) || "CLOSED".equals(room.getStatus())) {
            return "redirect:/spendolive/ott.do?error=invalidInvite";
        }

        return redirectToRoomPayment(room.getRoom_id(), "FRIEND", room.getInvite_code());
    }

    // 채팅방 화면 - 참여 권한 확인 후 메시지 조회
    @GetMapping("/ott/chat/room.do")
    public String chatRoom(@RequestParam("room_id") Long room_id, Model model, HttpSession session) {
        String loginId = getLoginId(session);

        OttRoomDTO chatRoom = ottService.getChatRoom(room_id, loginId);
        if (chatRoom == null) {
            return "redirect:/spendolive/ott.do?error=noChatAccess";
        }

        model.addAttribute("loginId", loginId);
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("chatMessageList", ottService.getChatMessages(room_id, loginId));
        ottService.markChatRoomAsRead(room_id, loginId);
        model.addAttribute("body_page", "/WEB-INF/views/ott/ottChatRoom.jsp");
        return "common/layout";
    }

    // 채팅 메시지 조회 - AJAX 요청에 메시지 목록 반환
    @GetMapping("/ott/chat/messages.do")
    @ResponseBody
    public List<OttChatMessageDTO> chatMessages(@RequestParam("room_id") Long room_id, HttpSession session) {
        String loginId = getLoginId(session);

        List<OttChatMessageDTO> messages = ottService.getChatMessages(room_id, loginId);
        ottService.markChatRoomAsRead(room_id, loginId);
        return messages;
    }

    // 채팅 메시지 전송 - 참여 권한 확인 후 메시지 저장
    @PostMapping("/ott/chat/send.do")
    public String sendChatMessage(@RequestParam("room_id") Long room_id,
                                  @RequestParam("message_content") String message_content,
                                  HttpSession session) {
        String loginId = getLoginId(session);

        ottService.sendChatMessage(room_id, loginId, message_content);
        return "redirect:/spendolive/ott/chat/room.do?room_id=" + room_id;
    }

    // 정산 요청 처리 - 방장의 다음 회차 정산 생성
    @PostMapping("/ott/settlement/request.do")
    public String requestSettlement(@RequestParam("room_id") Long room_id,
                                    @RequestParam(value = "settlement_month", required = false) String settlement_month,
                                    @RequestParam(value = "due_date", required = false) String due_date,
                                    @RequestParam(value = "returnPage", required = false, defaultValue = "recruit") String returnPage,
                                    HttpSession session) {
        String loginId = getLoginId(session);

        if (settlement_month == null || settlement_month.isBlank()) {
            settlement_month = YearMonth.now().plusMonths(1).toString();
        }

        ottService.requestSettlement(room_id, loginId, settlement_month);

        if ("friends".equals(returnPage)) {
            return "redirect:/spendolive/ott/friends.do?result=settlementRequested";
        }

        return "redirect:/spendolive/ott/recruit.do?tab=settlement&result=settlementRequested";
    }

    // 정산 결제 처리 - 로그인 사용자의 결제 상태를 PAID로 변경
    @PostMapping("/ott/settlement/pay.do")
    public String paySettlement(@RequestParam("payment_id") Long payment_id,
                                @RequestParam(value = "returnPage", required = false, defaultValue = "recruit") String returnPage,
                                HttpSession session) {
        String loginId = getLoginId(session);

        ottService.markPaymentPaid(payment_id, loginId);

        if ("friends".equals(returnPage)) {
            return "redirect:/spendolive/ott/friends.do?result=paid";
        }

        return "redirect:/spendolive/ott/recruit.do?tab=settlement&result=paid";
    }

    // 방 종료 예약 - 현재 이용 기간 종료일 기준으로 예약
    @PostMapping("/ott/room/close-request.do")
    public String closeRoom(@RequestParam("room_id") Long room_id,
                            @RequestParam(value = "close_notice", required = false) String close_notice,
                            @RequestParam(value = "close_reason", required = false) String close_reason,
                            @RequestParam(value = "returnPage", required = false, defaultValue = "friends") String returnPage,
                            HttpSession session) {
        String loginId = getLoginId(session);

        ottService.requestRoomClose(room_id, loginId, close_notice, close_reason);

        if ("recruit".equals(returnPage)) {
            return "redirect:/spendolive/ott/recruit.do?tab=apply&result=closeRequested";
        }

        return "redirect:/spendolive/ott/friends.do?result=closeRequested";
    }


    // 탈퇴 예약 - 참여자의 다음 이용 회차 탈퇴 예약
    @PostMapping("/ott/room/leave-reserve.do")
    public String reserveRoomLeave(@RequestParam("room_id") Long room_id,
                                   @RequestParam(value = "returnPage", required = false, defaultValue = "recruit") String returnPage,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);

        String msg = ottService.reserveRoomLeave(room_id, loginId);
        redirectAttributes.addFlashAttribute("msg", msg);
        return redirectAfterRoomAction(returnPage);
    }

    // 탈퇴 예약 취소
    @PostMapping("/ott/room/leave-cancel.do")
    public String cancelRoomLeave(@RequestParam("room_id") Long room_id,
                                  @RequestParam(value = "returnPage", required = false, defaultValue = "recruit") String returnPage,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        String loginId = getLoginId(session);

        String msg = ottService.cancelRoomLeave(room_id, loginId);
        redirectAttributes.addFlashAttribute("msg", msg);
        return redirectAfterRoomAction(returnPage);
    }


    // 방 관리 화면 이동 경로 처리
    private String redirectAfterRoomAction(String returnPage) {
        if ("friends".equals(returnPage)) {
            return "redirect:/spendolive/ott/friends.do";
        }
        return "redirect:/spendolive/ott/recruit.do?tab=manage";
    }

    // 결제 화면 이동 URL 생성
    private String redirectToRoomPayment(Long room_id, String room_mode, String invite_code) {
        String redirectUrl = "redirect:/payment/detail.do?room_id=" + room_id + "&room_mode=" + room_mode;

        if (invite_code != null && !invite_code.isBlank()) {
            redirectUrl += "&invite_code=" + invite_code;
        }

        return redirectUrl;
    }

    // OTT 서비스 ID 변환 - 빈 값이나 잘못된 값은 null 처리
    private Long parseOttServiceId(String ott_service_id) {
        if (ott_service_id == null || ott_service_id.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(ott_service_id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // OTT 공통 화면 데이터 설정
    private void addCommonOttModel(Model model, String loginId) {
        YearMonth nextMonth = YearMonth.now().plusMonths(1);
        LocalDate today = LocalDate.now();

        model.addAttribute("loginId", loginId);
        model.addAttribute("serviceList", ottService.getShareableServices());
        model.addAttribute("selectedSettlementMonth", nextMonth.toString());
        model.addAttribute("today", today.toString());
        model.addAttribute("kakaoJavascriptKey", kakaoJavascriptKey == null ? "" : kakaoJavascriptKey);
        model.addAttribute("settlementGuide", "OTT별 최고 멤버십 기준 금액을 N분의 1로 나누고 서비스 수수료 3%를 더해 정산합니다. 결제 마감일은 이용 시작일 7일 전으로 자동 계산됩니다.");
    }

    // OTT 계좌 연동 여부 공통 검사
    // MEMBER_TB.ACCOUNT_STATUS의 연동 완료 값은 "YES"이므로 그 값만 허용한다.
    private boolean hasLinkedAccount(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        return memberInfo != null && "YES".equals(memberInfo.getAccount_status());
    }
    private boolean hasLinkedCard(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");
        return memberInfo != null && "YES".equals(memberInfo.getCard_status());
    }

    // [내 담당 로그인 공통화] 로그인 여부 판단은 공통 JS에서 처리하고 Controller에서는 세션 사용자 ID만 사용한다.
    private String getLoginId(HttpSession session) {
        MemberVO memberInfo = (MemberVO) session.getAttribute("memberInfo");

        if (memberInfo.getId() != null && !memberInfo.getId().isBlank()) {
            return memberInfo.getId();
        }

        return String.valueOf(memberInfo.getMember_id());
    }
}
