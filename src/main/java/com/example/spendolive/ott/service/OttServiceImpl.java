package com.example.spendolive.ott.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.repository.MemberRepository;
import com.example.spendolive.ott.domain.OttChatMessageDTO;
import com.example.spendolive.ott.domain.OttChatRoomDTO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttServiceDTO;
import com.example.spendolive.notification.service.OttAlarmService;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.ott.repository.OttRepository;

// 사용자 OTT 비즈니스 로직 - 조건 판단, 계산, 처리 순서와 트랜잭션 담당
@Service
public class OttServiceImpl implements OttService {

    private static final double PLATFORM_FEE_RATE = 3.0;
    private static final int PAYMENT_CLOSE_DAYS_BEFORE = 7;

    private static final DateTimeFormatter BLOCKED_UNTIL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BLOCKED_UNTIL_DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OttRepository ottRepository;
    private final OttAlarmService ottAlarmService;
    private final MemberRepository memberRepository;

    // OTT 데이터와 알림, 회원 제재 정보를 생성자에서 한 번 주입받아 모든 OTT 처리에서 재사용
    public OttServiceImpl(OttRepository ottRepository, OttAlarmService ottAlarmService, MemberRepository memberRepository) {
        this.ottRepository = ottRepository;
        this.ottAlarmService = ottAlarmService;
        this.memberRepository = memberRepository;
    }

    // =========================================================
    // 1. 화면 조회
    // =========================================================

    // 공유 가능한 OTT 서비스와 요금 규칙 조회
    @Override
    public List<OttServiceDTO> getShareableServices() {
        return enrichServices(ottRepository.selectShareableServices());
    }

    // 검색 조건에 맞는 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> getRecruitRooms(String loginId) {
        return enrichRooms(ottRepository.selectRecruitRooms(loginId));
    }

    // 검색 조건에 맞는 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> getRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword) {
        return enrichRooms(ottRepository.selectRecruitRooms(
                loginId,
                normalizeOttServiceId(ott_service_id),
                normalizeKeyword(roomNameKeyword)));
    }

    // 검색 조건과 페이지 범위를 적용한 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> getRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword,
            int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        int offset = (safePage - 1) * safePageSize;

        return enrichRooms(ottRepository.selectRecruitRooms(
                loginId,
                normalizeOttServiceId(ott_service_id),
                normalizeKeyword(roomNameKeyword),
                offset,
                safePageSize));
    }

    // 내가 참여 중인 가족·지인 공유방 목록 조회
    @Override
    public List<OttRoomDTO> getFriendRooms(String loginId) {
        return enrichRooms(ottRepository.selectFriendRooms(loginId));
    }

    // 내가 방장인 가족·지인 공유방 목록 조회
    @Override
    public List<OttRoomDTO> getHostedFriendRooms(String loginId) {
        return enrichRooms(ottRepository.selectHostedFriendRooms(loginId));
    }

    // 내가 방장인 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> getHostedRecruitRooms(String loginId) {
        return enrichRooms(ottRepository.selectHostedRecruitRooms(loginId));
    }

    // 내가 일반 멤버로 참여 중인 외부인 모집방 목록 조회
    @Override
    public List<OttRoomDTO> getJoinedRecruitRooms(String loginId) {
        return enrichRooms(ottRepository.selectJoinedRecruitRooms(loginId));
    }

    // 내가 참여 중인 전체 OTT 방 목록 조회
    @Override
    public List<OttRoomDTO> getMyRooms(String loginId) {
        return enrichRooms(ottRepository.selectMyRooms(loginId));
    }

    // 내가 방장인 전체 OTT 방 목록 조회
    @Override
    public List<OttRoomDTO> getHostedRooms(String loginId) {
        return enrichRooms(ottRepository.selectHostedRooms(loginId));
    }

    // 내가 방장인 방의 참여자 목록 조회
    @Override
    public List<OttRoomMemberDTO> getHostedRoomMembers(String loginId) {
        return ottRepository.selectHostedRoomMembers(loginId);
    }

    // 내 전체 OTT 정산 내역 조회
    @Override
    public List<OttSettlementDTO> getMySettlements(String loginId) {
        return ottRepository.selectMySettlements(loginId);
    }

    // 가족·지인 공유방 정산 내역 조회
    @Override
    public List<OttSettlementDTO> getFriendSettlements(String loginId) {
        return ottRepository.selectMySettlements(loginId, "FRIEND");
    }

    // 외부인 모집방 정산 내역 조회
    @Override
    public List<OttSettlementDTO> getRecruitSettlements(String loginId) {
        return ottRepository.selectMySettlements(loginId, "RECRUIT");
    }

    // 내가 방장인 방의 멤버별 결제 내역 조회
    @Override
    public List<OttSettlementDTO> getHostedSettlementPayments(String loginId, String room_mode) {
        return ottRepository.selectHostedSettlementPayments(loginId, room_mode);
    }

    // 내가 참여 중인 OTT 채팅방 목록 조회
    @Override
    public List<OttChatRoomDTO> getMyChatRooms(String loginId) {
        return ottRepository.selectMyChatRooms(loginId);
    }

    // 권한 확인 후 선택한 채팅방 메시지 목록 조회
    @Override
    public List<OttChatMessageDTO> getChatMessages(Long room_id, String loginId) {
        if (!isValidLogin(loginId) || room_id == null || !ottRepository.canUseChatRoom(room_id, loginId)) {
            return Collections.emptyList();
        }
        return ottRepository.selectChatMessages(room_id, loginId);
    }

    // 권한 확인 후 선택한 채팅방 정보 조회
    @Override
    public OttRoomDTO getChatRoom(Long room_id, String loginId) {
        if (!isValidLogin(loginId) || room_id == null || !ottRepository.canUseChatRoom(room_id, loginId)) {
            return null;
        }
        return enrichRoom(ottRepository.selectChatRoom(room_id, loginId));
    }

    // 현재 모집 중인 외부인 방 개수 조회
    @Override
    public int getRecruitRoomCount() {
        return ottRepository.countRecruitRooms();
    }

    // 검색 조건에 맞는 종료되지 않은 외부인 모집방 수 조회
    @Override
    public int getRecruitRoomCount(Long ott_service_id, String roomNameKeyword) {
        return ottRepository.countRecruitRooms(
                normalizeOttServiceId(ott_service_id),
                normalizeKeyword(roomNameKeyword));
    }

    // 내가 참여 중인 OTT 방 개수 조회
    @Override
    public int getMyRoomCount(String loginId) {
        return ottRepository.countMyRooms(loginId);
    }

    // 내가 읽지 않은 OTT 채팅 메시지 개수 조회
    @Override
    public int getUnreadChatCount(String loginId) {
        return ottRepository.countUnreadChatMessages(loginId);
    }

    // 메인 대시보드에서 선택 월의 실제 OTT 정산 회차 수 조회
    @Override
    public int getMySettlementCount(String loginId, String settlement_month) {
        if (!isValidLogin(loginId) || settlement_month == null || settlement_month.isBlank()) {
            return 0;
        }
        return ottRepository.countMySettlements(loginId, settlement_month);
    }

    // =========================================================
    // 2. 방 생성과 참가
    // =========================================================

    // 가족·지인 공유방 생성 후 방장 등록과 시스템 메시지 처리
    @Override
    @Transactional
    public void createFriendRoom(OttRoomDTO roomDTO, String loginId) {
        if (roomDTO == null || !isValidLogin(loginId)) {
            return;
        }

        roomDTO.setRoom_mode("FRIEND");
        prepareRoomDefaultValues(roomDTO);
        prepareRoomIdentity(roomDTO);

        Long room_id = ottRepository.insertRoom(roomDTO, loginId, "ACTIVE");
        ottRepository.insertHostMember(room_id, loginId);
        insertSystemChatMessage(room_id, loginId, roomDTO.getRoom_name() + " 공유방이 만들어졌습니다.");
        createReadySettlement(room_id, loginId);
    }

    // 외부인 모집방 생성 후 방장 등록과 READY 정산 생성
    @Override
    @Transactional
    public void createRecruitRoom(OttRoomDTO roomDTO, String loginId) {
        if (roomDTO == null || !isValidLogin(loginId)) {
            return;
        }

        roomDTO.setRoom_mode("RECRUIT");
        prepareRoomDefaultValues(roomDTO);
        prepareRoomIdentity(roomDTO);

        Long room_id = ottRepository.insertRoom(roomDTO, loginId, "RECRUITING");
        ottRepository.insertHostMember(room_id, loginId);
        insertSystemChatMessage(room_id, loginId, roomDTO.getRoom_name() + " 공유방이 만들어졌습니다.");
        createReadySettlement(room_id, loginId);
    }

    // 빠른 참가가 가능한 가장 오래된 외부인 모집방 조회
    @Override
    public Long findQuickJoinRecruitRoomId(Long ott_service_id, String loginId) {
        if (normalizeOttServiceId(ott_service_id) == null || !isValidLogin(loginId)) {
            return null;
        }
        return ottRepository.selectOldestAvailableRecruitRoomId(ott_service_id, loginId);
    }

    // 초대 코드로 가족·지인 공유방 조회
    @Override
    public OttRoomDTO getRoomByInviteCode(String invite_code) {
        return enrichRoom(ottRepository.selectRoomByInviteCode(normalizeKeyword(invite_code)));
    }

    // =========================================================
    // 3. 정산과 결제
    // =========================================================

    // 정산 조건을 확인하고 정산 회차와 멤버별 결제 건 생성
    @Override
    @Transactional
    public void requestSettlement(Long room_id, String hostId, String settlement_month) {
        if (room_id == null || !isValidLogin(hostId)) {
            return;
        }

        OttRoomDTO room = ottRepository.selectRoom(room_id);
        if (!canHostManageRoom(room, hostId) || isClosingRoom(room)) {
            return;
        }

        YearMonth targetMonth = parseSettlementMonth(settlement_month);

        // 과거 이용 회차를 새로 열면 날짜와 결제 흐름이 꼬일 수 있으므로 생성하지 않는다
        if (targetMonth.isBefore(YearMonth.now())) {
            return;
        }

        LocalDate service_start_date = resolveBillingDate(targetMonth, room.getBilling_day());
        LocalDate service_end_date = service_start_date.plusMonths(1).minusDays(1);
        LocalDate payment_start_date = service_start_date.minusMonths(1);
        LocalDate payment_close_date = service_start_date.minusDays(PAYMENT_CLOSE_DAYS_BEFORE);

        // pay_day도 정산 회차의 실제 결제 마감일과 같은 기준(이용 시작 7일 전)으로 맞춘다
        // 월말·2월은 LocalDate 계산 결과의 일자를 사용하므로 30일 고정 보정이 필요 없다
        ottRepository.updateActiveMemberPayDay(room_id, payment_close_date.getDayOfMonth());

        LocalDate replace_start_date = payment_close_date.plusDays(1);
        LocalDate replace_end_date = service_start_date.minusDays(1);

        List<OttRoomMemberDTO> members = ottRepository.selectActiveMembers(room_id);
        int total_price = 0;
        int total_fee = 0;
        int total_pay_amount = 0;

        for (OttRoomMemberDTO member : members) {
            if ("HOST".equals(member.getMember_role())) {
                continue;
            }
            total_price += nullToZero(member.getShare_amount());
            total_fee += nullToZero(member.getFee_amount());
            total_pay_amount += nullToZero(member.getPay_amount());
        }

        OttSettlementDTO settlement = createSettlementDTO(
                room_id,
                targetMonth.toString(),
                total_price,
                total_fee,
                total_pay_amount,
                payment_start_date,
                payment_close_date,
                service_start_date,
                service_end_date,
                replace_start_date,
                replace_end_date,
                "PAYMENT_OPEN");

        Long settlement_id = ottRepository.selectSettlementId(room_id, targetMonth.toString());
        if (settlement_id == null) {
            settlement_id = ottRepository.insertSettlement(settlement);
        } else {
            // 방 생성 시 만들어 둔 READY 정산을 새로 INSERT하지 않고 PAYMENT_OPEN으로 갱신
            settlement.setSettlement_id(settlement_id);
            ottRepository.updateSettlement(settlement);
        }

        for (OttRoomMemberDTO member : members) {
            if ("HOST".equals(member.getMember_role())) {
                continue;
            }

            String memo = room.getRoom_name() + " " + targetMonth + " 이용분 정산";
            ottRepository.insertSettlementPaymentIfAbsent(settlement_id, member, memo);

            // 알림 제목·본문·이동 주소는 OttAlarmService에서 한 곳으로 관리한다
            ottAlarmService.notifySettlementOpened(
                    member.getMember_login_id(),
                    room.getRoom_name(),
                    targetMonth.toString(),
                    nullToZero(member.getPay_amount()),
                    payment_close_date,
                    room_id);
        }

        ottRepository.updateRoomStatus(room_id, "PAYMENT_OPEN");
        insertSystemChatMessage(room_id, hostId,
                targetMonth + " 이용분 결제가 열렸습니다. 결제 마감일은 " + payment_close_date + "입니다.");
    }

    // 결제 소유자를 확인하고 결제 건을 PAID 상태로 변경
    @Override
    @Transactional
    public void markPaymentPaid(Long payment_id, String loginId) {
        if (payment_id == null || !isValidLogin(loginId)) {
            return;
        }

        Map<String, Object> data = ottRepository.selectPaymentMap(payment_id);
        if (data == null) {
            return;
        }

        String payerId = mapString(data, "ID");
        String paymentStatus = mapString(data, "PAYMENT_STATUS");
        String roomStatus = mapString(data, "ROOM_STATUS");
        if (!loginId.equals(payerId)
                || !"UNPAID".equals(paymentStatus)
                || "CLOSE_REQUESTED".equals(roomStatus)
                || "CLOSED".equals(roomStatus)) {
            return;
        }

        int updated = ottRepository.updatePaymentPaid(payment_id, loginId);
        if (updated == 0) {
            return;
        }

        Long room_id = mapLong(data, "ROOM_ID");
        String hostId = mapString(data, "HOST_LOGIN_ID");
        String room_name = mapString(data, "ROOM_NAME");
        String settlement_month = mapString(data, "SETTLEMENT_MONTH");
        Integer total_amount = mapInteger(data, "TOTAL_AMOUNT");

        // 결제 완료 사실을 방장에게 알리고, 금액이 null이면 0원으로 안전하게 처리
        ottAlarmService.notifyPaymentCompletedToHost(
                hostId,
                loginId,
                room_name,
                settlement_month,
                nullToZero(total_amount),
                room_id);

    }

    // 결제 완료 사용자를 정원 확인 후 ACTIVE 멤버로 입장 처리
    @Override
    @Transactional
    public void completePaidRoomEntry(Long room_id, String loginId) {
        if (room_id == null || !isValidLogin(loginId)) {
            return;
        }

        OttRoomDTO room = ottRepository.selectRoom(room_id);
        if (room == null || loginId.equals(room.getHost_login_id()) || isClosingRoom(room)) {
            return;
        }

        String currentStatus = ottRepository.selectRoomMemberStatus(room_id, loginId);
        if ("ACTIVE".equals(currentStatus)) {
            return;
        }

        if (ottRepository.countActiveRoomMembers(room_id) >= room.getMember_limit()) {
            return;
        }

        int share_amount = safeDivide(room.getTotal_price(), room.getMember_limit());

        // 멤버 결제일도 정산 결제 마감일과 동일하게 이용 시작 7일 전으로 계산
        // 단순히 billing_day에서 숫자를 빼지 않고 LocalDate를 사용해
        // 2월·윤년·31일이 없는 달까지 정확하게 처리
        int pay_day = resolveNextPaymentCloseDate(
                LocalDate.now(),
                room.getBilling_day()).getDayOfMonth();

        int fee_amount = calculateFeeAmount(share_amount, PLATFORM_FEE_RATE);
        int pay_amount = share_amount + fee_amount;

        if (currentStatus == null) {
            ottRepository.insertActiveRoomMember(
                    room_id,
                    loginId,
                    share_amount,
                    PLATFORM_FEE_RATE,
                    fee_amount,
                    pay_amount,
                    pay_day);
        } else {
            ottRepository.reactivateRoomMember(
                    room_id,
                    loginId,
                    share_amount,
                    PLATFORM_FEE_RATE,
                    fee_amount,
                    pay_amount,
                    pay_day);
        }

        if (ottRepository.countActiveRoomMembers(room_id) >= room.getMember_limit()) {
            ottRepository.updateRoomStatus(room_id, "FIRST");//FIRST고정 수정 X
        }

        // 참여 완료 채팅과 알림을 등록한다. 본인은 완료 알림을 받고, 기존 멤버는 입장 알림을 받는다
        String member_name = ottRepository.selectMemberDisplayName(loginId);
        insertSystemChatMessage(room_id, loginId,
                member_name + "님이 결제를 완료하고 공유방에 입장했습니다.");

        ottAlarmService.notifyRoomJoinedSelf(
                loginId,
                room.getRoom_name(),
                room_id);

        for (String memberId : ottRepository.selectActiveRoomMemberIds(room_id)) {
            if (loginId.equals(memberId)) {
                continue;
            }
            ottAlarmService.notifyRoomMemberJoined(
                    memberId,
                    member_name,
                    room.getRoom_name(),
                    room_id);
        }
    }

    // =========================================================
    // 4. 방 종료와 나가기 예약
    // =========================================================

    // 방장 권한을 확인하고 방 종료 예정일과 사유 저장
    @Override
    @Transactional
    public void requestRoomClose(Long room_id, String hostId, String close_notice, String close_reason) {
        if (room_id == null || !isValidLogin(hostId)) {
            return;
        }

        OttRoomDTO room = ottRepository.selectRoom(room_id);
        if (!canHostManageRoom(room, hostId) || isClosingRoom(room)) {
            return;
        }

        LocalDate close_effective_date = getNextBillingDate(LocalDate.now(), room.getBilling_day());
        String notice = normalizeCloseNotice(close_notice);
        String reason = normalizeCloseReason(close_reason);

        int updated = ottRepository.updateRoomCloseRequest(
                room_id, hostId, close_effective_date, reason, notice);
        if (updated == 0) {
            return;
        }

        String targetMonth = YearMonth.from(close_effective_date).toString();
        ottRepository.insertRefundsForRoomClose(room_id, close_effective_date, targetMonth);
        ottRepository.cancelUnpaidFuturePayments(room_id, close_effective_date, targetMonth);
        ottRepository.markFutureSettlementsCancelled(room_id, close_effective_date, targetMonth);

        String message = "파티장이 방 삭제를 요청했습니다. 기존 참여자는 "
                + close_effective_date.minusDays(1)
                + "까지 이용할 수 있으며, 이미 결제된 다음 이용분은 자동 환불 처리됩니다.";
        insertSystemChatMessage(room_id, hostId, message);
        // 방장을 제외한 현재 참여자에게 종료 예정일과 안내 문구를 전달
        for (String memberId : ottRepository.selectActiveRoomMemberIds(room_id)) {
            if (hostId.equals(memberId)) {
                continue;
            }
            ottAlarmService.notifyRoomCloseScheduled(
                    memberId,
                    room.getRoom_name(),
                    close_effective_date,
                    notice,
                    room_id);
        }

        // 종료를 신청한 방장 본인에게도 접수 완료 알림을 남긴다
        ottAlarmService.notifySelfLeaveRequested(
                hostId,
                room.getRoom_name(),
                close_effective_date,
                room_id);
    }

    // 결제 상태와 날짜 규칙을 확인하여 방 나가기 예약
    @Override
    @Transactional
    public String reserveRoomLeave(Long room_id, String loginId) {
        if (room_id == null || !isValidLogin(loginId)) {
            return "나가기 예약을 처리할 수 없습니다.";
        }

        OttRoomDTO room = ottRepository.selectRoom(room_id);
        if (room == null || isClosingRoom(room)) {
            return "이미 종료되었거나 종료 예정인 방입니다.";
        }
        if (loginId.equals(room.getHost_login_id())) {
            return "파티장은 나가기 예약을 할 수 없습니다. 방 삭제 요청 기능을 사용해 주세요.";
        }
        if (!ottRepository.isActiveNormalMember(room_id, loginId)) {
            return "현재 참여 중인 일반 참여자만 나가기 예약을 할 수 있습니다.";
        }
        if (ottRepository.hasReservedLeave(room_id, loginId)) {
            return "이미 나가기 예약이 되어 있습니다.";
        }

        LocalDate today = LocalDate.now();
        LocalDate nextBillingDate = getNextBillingDate(today, room.getBilling_day());
        LocalDate leave_scheduled_date = nextBillingDate.minusDays(PAYMENT_CLOSE_DAYS_BEFORE);

        if (!today.isBefore(leave_scheduled_date)) {
            return "이미 다음 결제 준비 기간이라 이번 회차 나가기 예약은 불가능합니다. 다음 결제일 이후 다시 예약할 수 있습니다.";
        }
        if (ottRepository.hasPaidUpcomingPayment(room_id, loginId, leave_scheduled_date)) {
            return "이미 다음 이용분 결제가 완료되어 이번 회차 나가기 예약은 불가능합니다.";
        }

        int updated = ottRepository.reserveRoomLeave(room_id, loginId, leave_scheduled_date);
        if (updated == 0) {
            return "나가기 예약을 처리하지 못했습니다.";
        }

        insertSystemChatMessage(room_id, loginId,
                loginId + "님이 " + leave_scheduled_date + " 나가기 예약을 했습니다.");
        // 참여자의 나가기 신청 사실을 방장에게 알린다
        ottAlarmService.notifyMemberLeaveRequestedToHost(
                room.getHost_login_id(),
                loginId,
                room.getRoom_name(),
                leave_scheduled_date,
                room_id);

        // 신청한 참여자 본인에게도 예약 완료 알림을 남긴다
        ottAlarmService.notifySelfLeaveRequested(
                loginId,
                room.getRoom_name(),
                leave_scheduled_date,
                room_id);

        return "나가기 예약이 완료되었습니다. " + leave_scheduled_date + "에 자동으로 방에서 나가집니다.";
    }

    // 미처리 방 나가기 예약 취소
    @Override
    @Transactional
    public String cancelRoomLeave(Long room_id, String loginId) {
        if (room_id == null || !isValidLogin(loginId)) {
            return "나가기 예약 취소를 처리할 수 없습니다.";
        }

        OttRoomDTO room = ottRepository.selectRoom(room_id);
        if (room == null || "CLOSED".equals(room.getStatus())) {
            return "이미 종료된 방입니다.";
        }
        if (loginId.equals(room.getHost_login_id())) {
            return "파티장은 나가기 예약 취소 대상이 아닙니다.";
        }

        int updated = ottRepository.cancelRoomLeave(room_id, loginId);
        if (updated == 0) {
            return "취소할 나가기 예약이 없습니다.";
        }

        insertSystemChatMessage(room_id, loginId, loginId + "님이 나가기 예약을 취소했습니다.");
        return "나가기 예약이 취소되었습니다.";
    }

    // 나가기 예약·미결제 만료·방 종료 예약 작업 실행
    @Override
    @Transactional
    public void processScheduledOttJobs() {
        ottRepository.processLeaveReservations();
        ottRepository.expireOverduePayments();
        ottRepository.closeEffectiveRooms();
    }

    // =========================================================
    // 5. 채팅
    // =========================================================

    // 채팅 권한과 메시지 내용을 검증한 뒤 메시지 등록
    @Override
    public String sendChatMessage(Long room_id, String sender_id, String message_content) {
        if (room_id == null || !isValidLogin(sender_id)
                || !ottRepository.canUseChatRoom(room_id, sender_id)) {
            return null;
        }

        String restrictionMessage = getChatRestrictionMessage(sender_id);
        if (restrictionMessage != null) {
            return restrictionMessage;
        }

        String normalizedMessage = normalizeChatMessage(message_content);
        if (normalizedMessage == null) {
            return null;
        }
        ottRepository.insertChatMessage(room_id, sender_id, normalizedMessage);
        return null;
    }

    // 경고 패널티 기간이 남아 있으면 메시지 전송만 막고 채팅방 조회는 허용한다
    private String getChatRestrictionMessage(String sender_id) {
        MemberVO member = memberRepository.selectMemberById(sender_id);
        if (member == null || member.getBlocked_until() == null || member.getBlocked_until().isBlank()) {
            return null;
        }

        LocalDateTime blockedUntil;
        try {
            blockedUntil = LocalDateTime.parse(member.getBlocked_until(), BLOCKED_UNTIL_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }

        if (!blockedUntil.isAfter(LocalDateTime.now())) {
            return null;
        }

        return "경고 " + member.getWarning_count() + "회로 인해 "
                + blockedUntil.format(BLOCKED_UNTIL_DISPLAY_FORMATTER)
                + "까지 채팅 이용이 제한됩니다.";
    }

    // 채팅 권한 확인 후 마지막 읽은 시각 갱신
    @Override
    public void markChatRoomAsRead(Long room_id, String loginId) {
        if (room_id == null || !isValidLogin(loginId)
                || !ottRepository.canUseChatRoom(room_id, loginId)) {
            return;
        }
        ottRepository.markChatRoomAsRead(room_id, loginId);
    }

    // =========================================================
    // 6. Service 전용 비즈니스 규칙
    // =========================================================

    // OTT 서비스 목록에 화면 표시용 고정 요금 정보 보완
    private List<OttServiceDTO> enrichServices(List<OttServiceDTO> services) {
        for (OttServiceDTO service : services) {
            int share_amount = safeDivide(service.getDefault_price(), service.getMax_member_limit());
            int fee_amount = calculateFeeAmount(share_amount, service.getPlatform_fee_rate());
            service.setShare_amount(share_amount);
            service.setFee_amount(fee_amount);
            service.setPer_person_amount(share_amount + fee_amount);
        }
        return services;
    }

    // OTT 방 목록에 화면 표시용 서비스 규칙 정보 보완
    private List<OttRoomDTO> enrichRooms(List<OttRoomDTO> rooms) {
        for (OttRoomDTO room : rooms) {
            enrichRoom(room);
        }
        return rooms;
    }

    // OTT 방 한 건에 화면 표시용 서비스 규칙 정보 보완
    private OttRoomDTO enrichRoom(OttRoomDTO room) {
        if (room == null) {
            return null;
        }
        int share_amount = safeDivide(room.getTotal_price(), room.getMember_limit());
        int fee_amount = calculateFeeAmount(share_amount, PLATFORM_FEE_RATE);
        room.setShare_amount(share_amount);
        room.setPlatform_fee_rate(PLATFORM_FEE_RATE);
        room.setFee_amount(fee_amount);
        room.setPer_person_amount(share_amount + fee_amount);
        return room;
    }

    private void createReadySettlement(Long room_id, String hostId) {
        OttRoomDTO room = ottRepository.selectRoom(room_id);

        if (!canHostManageRoom(room, hostId)) {
            return;
        }

        LocalDate today = LocalDate.now();
        YearMonth targetMonth = YearMonth.now().plusMonths(1);

        if (ottRepository.existsSettlement(
                room_id,
                targetMonth.toString())) {
            return;
        }

        LocalDate service_start_date =
                resolveBillingDate(
                        targetMonth,
                        room.getBilling_day());

        LocalDate service_end_date =
                service_start_date.plusMonths(1).minusDays(1);

        LocalDate payment_start_date = today;

        LocalDate normal_payment_close_date =
                service_start_date.minusDays(
                        PAYMENT_CLOSE_DAYS_BEFORE);

        LocalDate payment_close_date;

        /*
         * 원래 결제 마감일이 이미 지났다면
         * 첫 정산에 한해서 서비스 시작 전날까지 결제 가능하게 한다.
         */
        if (normal_payment_close_date.isBefore(today)) {
            payment_close_date =
                    service_start_date.minusDays(1);
        } else {
            payment_close_date =
                    normal_payment_close_date;
        }

        LocalDate replace_start_date =
                payment_close_date.plusDays(1);

        LocalDate replace_end_date =
                service_start_date.minusDays(1);

        /*
         * 첫 정산의 결제 마감일이 서비스 시작 전날이면
         * 대체 모집 기간이 존재하지 않는다.
         */
        if (replace_start_date.isAfter(replace_end_date)) {
            replace_start_date = null;
            replace_end_date = null;
        }

        OttSettlementDTO settlement = createSettlementDTO(
                room_id,
                targetMonth.toString(),
                room.getTotal_price(),
                0,
                0,
                payment_start_date,
                payment_close_date,
                service_start_date,
                service_end_date,
                replace_start_date,
                replace_end_date,
                "READY");

        ottRepository.insertSettlement(settlement);
    }

    private OttSettlementDTO createSettlementDTO(
            Long room_id,
            String settlement_month,
            int total_price,
            int total_fee,
            int total_pay_amount,
            LocalDate payment_start_date,
            LocalDate payment_close_date,
            LocalDate service_start_date,
            LocalDate service_end_date,
            LocalDate replace_start_date,
            LocalDate replace_end_date,
            String status) {
        OttSettlementDTO settlement = new OttSettlementDTO();
        settlement.setRoom_id(room_id);
        settlement.setSettlement_month(settlement_month);
        settlement.setTotal_price(total_price);
        settlement.setTotal_fee(total_fee);
        settlement.setTotal_pay_amount(total_pay_amount);
        // 대체 모집 기간이 없는 첫 회차는 replace 날짜가 null일 수 있다
        // 날짜를 바로 toString()하면 NullPointerException이 발생하므로 공통 변환 메서드를 사용
        settlement.setDue_date(toDateString(payment_close_date));
        settlement.setPayment_start_date(toDateString(payment_start_date));
        settlement.setPayment_close_date(toDateString(payment_close_date));
        settlement.setService_start_date(toDateString(service_start_date));
        settlement.setService_end_date(toDateString(service_end_date));
        settlement.setReplace_start_date(toDateString(replace_start_date));
        settlement.setReplace_end_date(toDateString(replace_end_date));
        settlement.setStatus(status);
        return settlement;
    }

    // LocalDate를 DB 저장용 문자열로 변환
    // 날짜가 존재하지 않는 선택 항목은 null을 그대로 반환
    private String toDateString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    // 방 생성 전에 방 유형과 OTT 요금 기본값 적용
    private void prepareRoomDefaultValues(OttRoomDTO roomDTO) {
        applyFixedOttPlanRule(roomDTO);

        if (roomDTO.getBilling_day() == null
                || roomDTO.getBilling_day() < 1
                || roomDTO.getBilling_day() > 31) {
            roomDTO.setBilling_day(1);
        }
    }

    // 방 이름과 초대 코드를 생성하여 방 식별 정보 설정
    private void prepareRoomIdentity(OttRoomDTO roomDTO) {
        roomDTO.setPlan_name(normalizePlanName(roomDTO.getPlan_name()));
        roomDTO.setRoom_mode(normalizeRoomMode(roomDTO.getRoom_mode()));
        roomDTO.setInvite_code(makeInviteCode());

        if (roomDTO.getRoom_name() == null || roomDTO.getRoom_name().isBlank()) {
            String service_name = ottRepository.selectServiceName(roomDTO.getOtt_service_id());
            roomDTO.setRoom_name(service_name + " - " + roomDTO.getPlan_name() + " - 모집");
        } else {
            roomDTO.setRoom_name(roomDTO.getRoom_name().trim());
        }
    }

    // OTT별 고정 요금과 인원 제한 규칙 적용
    private void applyFixedOttPlanRule(OttRoomDTO roomDTO) {
        OttServiceDTO serviceRule = ottRepository.selectOttServiceRule(roomDTO.getOtt_service_id());

        if (serviceRule == null) {
            roomDTO.setPlan_name(normalizePlanName(roomDTO.getPlan_name()));
            if (roomDTO.getTotal_price() == null || roomDTO.getTotal_price() < 0) {
                roomDTO.setTotal_price(0);
            }
            if (roomDTO.getMember_limit() == null || roomDTO.getMember_limit() < 1) {
                roomDTO.setMember_limit(4);
            }
            return;
        }

        roomDTO.setPlan_name(serviceRule.getFixed_plan_name());
        roomDTO.setMember_limit(serviceRule.getMax_member_limit());
        if ("FRIEND".equals(roomDTO.getRoom_mode())) {
            roomDTO.setTotal_price(serviceRule.getBase_price());
        } else {
            roomDTO.setTotal_price(serviceRule.getDefault_price());
        }
    }

    // 실제 회원 ID를 사용하여 시스템 채팅 메시지 등록
    private void insertSystemChatMessage(Long room_id, String sender_id, String message_content) {
        // sender_id는 member_tb.id FK이므로 실제 회원 ID를 사용하고 접두어로 시스템 메시지를 구분한다
        ottRepository.insertChatMessage(room_id, sender_id, "[SYSTEM] " + message_content);
    }

    // 채팅 메시지 공백과 SYSTEM 접두어를 안전하게 정리
    private String normalizeChatMessage(String message_content) {
        if (message_content == null || message_content.trim().isEmpty()) {
            return null;
        }

        String normalized = message_content.trim();
        if (normalized.startsWith("[SYSTEM]")) {
            normalized = normalized.replaceFirst("^\\[SYSTEM\\]\\s*", "").trim();
        }
        return normalized.isEmpty() ? null : normalized;
    }

    // 로그인 사용자가 해당 방을 관리할 방장인지 확인
    private boolean canHostManageRoom(OttRoomDTO room, String hostId) {
        return room != null && hostId != null && hostId.equals(room.getHost_login_id());
    }

    // 방이 종료 요청 또는 종료 상태인지 확인
    private boolean isClosingRoom(OttRoomDTO room) {
        return room == null
                || "CLOSE_REQUESTED".equals(room.getStatus())
                || "CLOSED".equals(room.getStatus());
    }

    // 로그인 ID가 유효한 문자열인지 확인
    private boolean isValidLogin(String loginId) {
        return loginId != null && !loginId.isBlank();
    }

    // 0 이하 OTT 서비스 ID를 미선택 값으로 변환
    private Long normalizeOttServiceId(Long ott_service_id) {
        return ott_service_id == null || ott_service_id <= 0 ? null : ott_service_id;
    }

    // 빈 검색어를 null로 변환하고 앞뒤 공백 제거
    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.trim().isEmpty() ? null : keyword.trim();
    }

    // 요금제명이 없을 때 기본 요금제명 적용
    private String normalizePlanName(String plan_name) {
        return plan_name == null || plan_name.isBlank() ? "프리미엄" : plan_name.trim();
    }

    // 방 유형을 FRIEND 또는 RECRUIT 값으로 정규화
    private String normalizeRoomMode(String room_mode) {
        return "FRIEND".equals(room_mode) ? "FRIEND" : "RECRUIT";
    }

    // 종료 안내 문구의 기본값 적용
    private String normalizeCloseNotice(String close_notice) {
        return close_notice == null || close_notice.isBlank()
                ? "파티장 요청으로 이번 이용 기간 종료 후 공유방이 종료됩니다."
                : close_notice.trim();
    }

    // 종료 사유의 기본값 적용
    private String normalizeCloseReason(String close_reason) {
        return close_reason == null || close_reason.isBlank() ? "파티장 요청" : close_reason.trim();
    }

    // 정산 월 문자열을 YearMonth로 변환
    private YearMonth parseSettlementMonth(String settlement_month) {
        if (settlement_month != null && !settlement_month.isBlank()) {
            try {
                return YearMonth.parse(settlement_month, DateTimeFormatter.ofPattern("yyyy-MM"));
            } catch (DateTimeParseException ignored) {
                // 잘못된 입력은 다음 달 정산으로 처리
            }
        }
        return YearMonth.now().plusMonths(1);
    }

    /**
     * 오늘 이후 가장 가까운 결제 마감일을 계산
     * 결제 마감일은 서비스 이용 시작일의 7일 전으로 통일한다.
     */
    private LocalDate resolveNextPaymentCloseDate(LocalDate today, Integer billing_day) {
        YearMonth serviceMonth = YearMonth.from(today);
        LocalDate serviceStartDate = resolveBillingDate(serviceMonth, billing_day);
        LocalDate paymentCloseDate = serviceStartDate.minusDays(PAYMENT_CLOSE_DAYS_BEFORE);

        if (paymentCloseDate.isBefore(today)) {
            serviceStartDate = resolveBillingDate(serviceMonth.plusMonths(1), billing_day);
            paymentCloseDate = serviceStartDate.minusDays(PAYMENT_CLOSE_DAYS_BEFORE);
        }

        return paymentCloseDate;
    }

    // 정산 월과 결제일을 기준으로 실제 결제 날짜 계산
    private LocalDate resolveBillingDate(YearMonth month, Integer billing_day) {
        int day = billing_day == null ? 1 : billing_day;
        day = Math.max(1, Math.min(day, month.lengthOfMonth()));
        return month.atDay(day);
    }

    // 현재 날짜를 기준으로 다음 결제 예정일 계산
    private LocalDate getNextBillingDate(LocalDate today, Integer billing_day) {
        YearMonth month = YearMonth.from(today);
        LocalDate candidate = resolveBillingDate(month, billing_day);
        return candidate.isAfter(today) ? candidate : resolveBillingDate(month.plusMonths(1), billing_day);
    }

    // 0 또는 null 값을 방지하며 인원별 분담금 계산
    private int safeDivide(Integer total_price, Integer member_limit) {
        if (total_price == null || member_limit == null || member_limit <= 0) {
            return 0;
        }
        return (int) Math.ceil(total_price / (double) member_limit);
    }

    // 분담금과 수수료율을 기준으로 플랫폼 수수료 계산
    private int calculateFeeAmount(Integer share_amount, Double fee_rate) {
        if (share_amount == null || share_amount <= 0) {
            return 0;
        }
        double rate = fee_rate == null ? PLATFORM_FEE_RATE : fee_rate;
        return (int) Math.round(share_amount * (rate / 100.0));
    }

    // null 정수 값을 0으로 변환
    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    // 가족·지인 공유방에 사용할 초대 코드 생성
    private String makeInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    // 결제 조회 Map에서 문자열 값을 안전하게 변환
    private String mapString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            value = map.get(key.toLowerCase());
        }
        return value == null ? null : value.toString();
    }

    // 결제 조회 Map에서 Long 값을 안전하게 변환
    private Long mapLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            value = map.get(key.toLowerCase());
        }
        return value == null ? null : ((Number) value).longValue();
    }

    // 결제 조회 Map에서 Integer 값을 안전하게 변환
    private Integer mapInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            value = map.get(key.toLowerCase());
        }
        return value == null ? 0 : ((Number) value).intValue();
    }
}
