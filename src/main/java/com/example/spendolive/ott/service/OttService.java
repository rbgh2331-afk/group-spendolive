package com.example.spendolive.ott.service;

import java.util.List;

import com.example.spendolive.ott.domain.OttChatMessageDTO;
import com.example.spendolive.ott.domain.OttChatRoomDTO;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttServiceDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;

// 사용자 OTT 서비스 인터페이스 - 방, 정산, 채팅 기능 정의
public interface OttService {

    // 공유 가능한 OTT와 고정 요금 규칙 조회
    List<OttServiceDTO> getShareableServices();

    // 로그인 사용자 기준으로 전체 외부 모집방 조회
    List<OttRoomDTO> getRecruitRooms(String loginId);

    // OTT 종류와 방 제목 검색 조건을 적용해 외부 모집방 조회
    List<OttRoomDTO> getRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword);

    // 검색 조건과 페이지 범위를 적용해 외부 모집방 조회
    List<OttRoomDTO> getRecruitRooms(String loginId, Long ott_service_id, String roomNameKeyword,
            int page, int pageSize);

    // 사용자가 참여하거나 만든 가족·지인 방 조회
    List<OttRoomDTO> getFriendRooms(String loginId);

    // 사용자가 방장인 가족·지인 방 조회
    List<OttRoomDTO> getHostedFriendRooms(String loginId);

    // 사용자가 방장인 외부 모집방 조회
    List<OttRoomDTO> getHostedRecruitRooms(String loginId);

    // 사용자가 일반 멤버로 참여 중인 외부 모집방 조회
    List<OttRoomDTO> getJoinedRecruitRooms(String loginId);

    // 방 모드와 관계없이 사용자가 속한 모든 방 조회
    List<OttRoomDTO> getMyRooms(String loginId);

    // 방 모드와 관계없이 사용자가 만든 모든 방 조회
    List<OttRoomDTO> getHostedRooms(String loginId);

    // 방장 화면에 표시할 방별 참여자와 탈퇴 예약 상태 조회
    List<OttRoomMemberDTO> getHostedRoomMembers(String loginId);

    // 사용자가 관련된 전체 정산 내역 조회
    List<OttSettlementDTO> getMySettlements(String loginId);

    // 가족·지인 방 정산 내역만 조회
    List<OttSettlementDTO> getFriendSettlements(String loginId);

    // 외부 모집방 정산 내역만 조회
    List<OttSettlementDTO> getRecruitSettlements(String loginId);

    // 방장이 확인할 참여자별 결제 상태 조회
    List<OttSettlementDTO> getHostedSettlementPayments(String loginId, String room_mode);

    // 사용자가 접근 가능한 채팅방 요약 목록 조회
    List<OttChatRoomDTO> getMyChatRooms(String loginId);

    // 채팅 접근 권한을 확인한 뒤 방의 메시지 목록 조회
    List<OttChatMessageDTO> getChatMessages(Long room_id, String loginId);

    // 채팅방 화면에 필요한 방 정보 조회
    OttRoomDTO getChatRoom(Long room_id, String loginId);

    // 현재 종료되지 않은 외부 모집방 수 조회
    int getRecruitRoomCount();

    // 검색 조건에 맞는 종료되지 않은 외부 모집방 수 조회
    int getRecruitRoomCount(Long ott_service_id, String roomNameKeyword);

    // 사용자가 ACTIVE 상태로 참여 중인 방 수 조회
    int getMyRoomCount(String loginId);

    // 사용자의 전체 읽지 않은 채팅 수 조회
    int getUnreadChatCount(String loginId);

    // 선택 월에 사용자가 관련된 실제 OTT 정산 회차 수 조회
    int getMySettlementCount(String loginId, String settlement_month);

    // 가족·지인 방을 생성하고 생성자를 방장으로 등록
    void createFriendRoom(OttRoomDTO roomDTO, String loginId);

    // 외부 모집방과 결제 연결용 READY 정산을 함께 생성
    void createRecruitRoom(OttRoomDTO roomDTO, String loginId);

    // 선택한 OTT에서 가장 오래된 참가 가능 모집방 조회
    Long findQuickJoinRecruitRoomId(Long ott_service_id, String loginId);

    // 가족방 초대 코드에 해당하는 방 조회
    OttRoomDTO getRoomByInviteCode(String invite_code);

    // 방장 권한과 정산 기간을 검증해 새로운 정산 회차 생성
    void requestSettlement(Long room_id, String hostId, String settlement_month);

    // 로그인 사용자의 미결제 건을 PAID로 변경
    void markPaymentPaid(Long payment_id, String loginId);

    // 결제 완료 후 사용자를 ACTIVE 멤버로 확정
    void completePaidRoomEntry(Long room_id, String loginId);

    // 현재 이용 기간 종료일 기준으로 방 종료를 예약
    void requestRoomClose(Long room_id, String hostId, String close_notice, String close_reason);

    // 일반 멤버의 탈퇴 가능 회차를 계산해 탈퇴 예약 등록
    String reserveRoomLeave(Long room_id, String loginId);

    // 처리 전인 탈퇴 예약 취소
    String cancelRoomLeave(Long room_id, String loginId);

    // 탈퇴, 미결제 만료, 방 종료 예약을 일괄 처리
    void processScheduledOttJobs();

    // 채팅 메시지 전송 - 참여 권한 확인 후 메시지 저장
    String sendChatMessage(Long room_id, String sender_id, String message_content);

    // 사용자의 채팅방 마지막 읽은 시각 갱신
    void markChatRoomAsRead(Long room_id, String loginId);
}
