package com.example.spendolive.notice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.spendolive.notice.domain.NoticeDTO;
import com.example.spendolive.notice.repository.NoticeRepository;

/**
 * NoticeService 구현체.
 * 대부분의 메서드는 NoticeRepository 호출을 그대로 위임만 함.
 * 로직다운 로직이 있는 곳은 두 곳: 관리자 페이지네이션 계산(getNoticeListForAdmin/getNoticeAdminTotalPages),
 * 공지 등록 시 전체 알림 발송(insertNotice).
 */
@Service
public class NoticeServiceImpl implements NoticeService {
    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);

    // 관리자 공지 목록 전용: 20개 이하면 페이지네이션 없이 전부 표시, 넘으면 20개씩 페이지 분리
    private static final int ADMIN_PAGE_SIZE = 20;
    private static final int ADMIN_PAGINATION_THRESHOLD = 20;

    private final NoticeRepository noticeRepository;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 NoticeRepository를 자동으로 넣어줌
    public NoticeServiceImpl(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @Override
    public List<NoticeDTO> getNoticeList(String id) {
        return noticeRepository.findAll(id);
    }

    // 전체 개수가 기준(ADMIN_PAGINATION_THRESHOLD) 이하면 페이지 계산 없이 그냥 전부 반환.
    // 기준을 넘으면 요청받은 page를 1 미만이 안 되게 보정한 뒤, 그 페이지에 해당하는
    // offset만큼 건너뛰고 ADMIN_PAGE_SIZE(20)개만 잘라서 반환
    @Override
    public List<NoticeDTO> getNoticeListForAdmin(int page) {
        int totalCount = noticeRepository.countAll();
        if (totalCount <= ADMIN_PAGINATION_THRESHOLD) {
            return noticeRepository.findAllPaged(0, Math.max(totalCount, 1));
        }
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * ADMIN_PAGE_SIZE;
        return noticeRepository.findAllPaged(offset, ADMIN_PAGE_SIZE);
    }

    // 위 getNoticeListForAdmin과 같은 기준(ADMIN_PAGINATION_THRESHOLD)으로 총 페이지 수만 계산
    @Override
    public int getNoticeAdminTotalPages() {
        int totalCount = noticeRepository.countAll();
        if (totalCount <= ADMIN_PAGINATION_THRESHOLD) {
            return 1;
        }
        return (int) Math.ceil((double) totalCount / ADMIN_PAGE_SIZE);
    }

    @Override
    public NoticeDTO getNoticeDetail(int notice_id) {
        return noticeRepository.findById(notice_id);
    }

    @Override
    public NoticeDTO getNoticeDetailForUser(int notice_id, String id) {
        return noticeRepository.findByIdWithStar(notice_id, id);
    }

    @Override
    public int getNoticeCount() {
        return noticeRepository.countAll();
    }

    @Override
    public int getPinnedCount() {
        return noticeRepository.countPinned();
    }

    @Override
    public List<NoticeDTO> getImportantList(String id) {
    return noticeRepository.findImportantList(id);
    }

    @Override
    public void readNotice(int notice_id, String id) {
        noticeRepository.insertNoticeRead(notice_id, id);
    }


    @Override
    public List<NoticeDTO> getUnreadNoticeList(String id) {
    return noticeRepository.findUnreadBymember_id(id);
    }

    @Override
    public void toggleNoticeStar(int notice_id, String id) {
        noticeRepository.toggleNoticeStar(notice_id, id);
    }

    /**
     * 공지 등록 + 전체 회원 알림 발송.
     */
    @Override
    public int insertNotice(NoticeDTO notice) {
        int newId = noticeRepository.insertNotice(notice);
        if (newId > 0) {
            try {
                noticeRepository.insertNoticeAlertForAll(notice.getTitle(), String.valueOf(newId));
            } catch (Exception e) {
                log.error("{}", "[NoticeServiceImpl] 알림 발송 실패 (공지는 등록됨): " + e.getMessage(), e);
            }
        }
        return newId;
    }

    @Override
    public void updateNotice(NoticeDTO notice) {
        noticeRepository.updateNotice(notice);
    }

    @Override
    public void deleteNotice(int notice_id) {
        noticeRepository.deleteNotice(notice_id);
    }
}