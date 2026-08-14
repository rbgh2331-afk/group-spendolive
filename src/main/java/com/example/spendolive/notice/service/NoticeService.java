package com.example.spendolive.notice.service;

import java.util.List;

import com.example.spendolive.notice.domain.NoticeDTO;

/**
 * 공지사항 관련 비즈니스 로직 인터페이스.
 * 구현체가 실제로 NoticeRepository를 호출해서 처리함.
 * - 사용자 화면(목록/상세/중요공지/안읽음)과 관리자 화면(페이지네이션 목록/등록/수정/삭제)이 섞여 있음.
 * - 로그인 회원 여부에 따라 read_yn(읽음)/star_yn(찜) 같은 개인화 값이 같이 조회되는 메서드들이 있음.
 */
public interface NoticeService {

    // 사용자 화면(noticeCenter.jsp) 전체 목록. id가 null이면 비로그인(개인화 값 없이 조회)
    List<NoticeDTO> getNoticeList(String id);

    // 관리자 공지 목록 전용 (페이지네이션): 20개 이하면 전체, 넘으면 20개씩 페이지 분리
    List<NoticeDTO> getNoticeListForAdmin(int page);
    int getNoticeAdminTotalPages();

    // 공지 상세 - 개인화 값(찜 여부 등) 없이 공지 원문만 조회
    NoticeDTO getNoticeDetail(int notice_id);

    // 사용자 상세 페이지 전용: 로그인 회원 기준 찜 여부(star_yn)까지 포함해서 조회
    NoticeDTO getNoticeDetailForUser(int notice_id, String id);

    // 전체 공지 개수 (페이지네이션 계산 등에 씀)
    int getNoticeCount();

    // 상단 고정(pinned_yn='Y') 공지 개수
    int getPinnedCount();

    // 중요(고정) 공지만 모아서 조회. id가 null이면 비로그인
    List<NoticeDTO> getImportantList(String id);

    // 로그인 회원이 해당 공지를 읽음 처리 (공지 상세 진입 시 호출)
    void readNotice(int notice_id, String id);

    // 로그인 회원 기준 아직 안 읽은 공지만 조회
    List<NoticeDTO> getUnreadNoticeList(String id);

    // 찜(star) 상태를 토글(켜져 있으면 끄고, 꺼져 있으면 켬)
    void toggleNoticeStar(int notice_id, String id);

    // 관리자 기능
    // 반환값은 새로 등록된 공지의 notice_id
    int insertNotice(NoticeDTO notice);
    void updateNotice(NoticeDTO notice);
    void deleteNotice(int notice_id);
}
