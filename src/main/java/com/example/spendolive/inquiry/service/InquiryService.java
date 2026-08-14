package com.example.spendolive.inquiry.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.spendolive.inquiry.domain.InquiryFileVO;
import com.example.spendolive.inquiry.domain.InquiryVO;
import com.example.spendolive.inquiry.repository.InquiryFileRepository;
import com.example.spendolive.inquiry.repository.InquiryRepository;
import com.example.spendolive.notification.domain.NotificationType;
import com.example.spendolive.notification.service.NotificationService;

@Service
public class InquiryService {

    private static final int PAGE_SIZE = 5; // 사용자(내 문의 조회) 목록 전용 페이지당 개수 — 관리자 목록과 공유하지 않음

    // 관리자 문의 목록 전용: 20개 이하면 페이지네이션 없이 전부 표시, 넘으면 20개씩 페이지 분리
    private static final int ADMIN_PAGE_SIZE = 20;
    private static final int ADMIN_PAGINATION_THRESHOLD = 20;

    private final InquiryRepository inquiryRepository;
    private final InquiryFileRepository inquiryFileRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public InquiryService(InquiryRepository inquiryRepository,
                           InquiryFileRepository inquiryFileRepository,
                           FileStorageService fileStorageService,
                           NotificationService notificationService) {
        this.inquiryRepository = inquiryRepository;
        this.inquiryFileRepository = inquiryFileRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    /**
     * 문의 등록 + 첨부파일 저장을 한 트랜잭션으로 처리
     * 파일 검증(FileStorageService.storeFiles)에서 예외가 나면 inquiry insert도 롤백된다.
     */
    @Transactional
    public void writeInquiry(InquiryVO inquiry, MultipartFile[] attachments) {
        int inquiry_id = inquiryRepository.insertInquiry(inquiry);
        inquiry.setInquiry_id(inquiry_id);

        List<InquiryFileVO> files = fileStorageService.storeFiles(inquiry_id, attachments);
        for (InquiryFileVO file : files) {
            inquiryFileRepository.insertFile(file);
        }

        // 문의 접수 완료 알림
        // 딱 맞는 전용 타입이 없어서 NotificationType.PERSONAL(일반 개인 알림)을 재사용함
        // 제목=문의 자체 제목, 본문=안내문구만 (공지 알림 표시 패턴과 통일)
        notificationService.createNotification(
                inquiry.getId(),
                NotificationType.PERSONAL,
                inquiry.getTitle(),
                "문의가 접수되었습니다. 답변까지 영업일 기준 1~2일 소요됩니다.",
                "/spendolive/inquiry/list.do");
    }

    /**
     * @param status null이면 전체, 아니면 WAIT/DONE/REVIEW 중 하나 (DB 저장값, 대문자)로 필터링
     */
    public List<InquiryVO> getMyInquiryList(String id, int page, String status) {
        int totalCount = inquiryRepository.countBymember_id(id, status);
        if (totalCount == 0) {
            return Collections.emptyList();
        }

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        int safePage = Math.min(Math.max(page, 1), totalPages);
        int offset = (safePage - 1) * PAGE_SIZE;
        List<InquiryVO> list = inquiryRepository.findBymember_id(id, status, offset, PAGE_SIZE);

        // 각 문의에 첨부파일 목록을 채워 넣는다 (목록 카드에서 썸네일 표시용)
        for (InquiryVO inquiry : list) {
            inquiry.setFiles(inquiryFileRepository.findByInquiryId(inquiry.getInquiry_id()));
        }
        return list;
    }

    public int getMyInquiryTotalPages(String id, String status) {
        int totalCount = inquiryRepository.countBymember_id(id, status);
        if (totalCount == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalCount / PAGE_SIZE);
    }

    public InquiryVO getInquiryDetail(int inquiry_id) {
        InquiryVO inquiry = inquiryRepository.findById(inquiry_id);
        if (inquiry != null) {
            inquiry.setFiles(inquiryFileRepository.findByInquiryId(inquiry_id));
        }
        return inquiry;
    }

    /**
     * 첨부파일 미리보기/다운로드 요청 시 접근 권한을 확인
     * 관리자는 전체 열람 가능, 일반 회원은 본인 문의의 첨부파일만 열람 가능
     */
    public InquiryFileVO getInquiryFile(int file_id, String memberId, boolean isAdmin) {
        InquiryFileVO file = inquiryFileRepository.findById(file_id);

        if (file == null) {
            return null;
        }
        if (isAdmin) {
            return file;
        }

        InquiryVO inquiry = inquiryRepository.findById(file.getInquiry_id());
        if (inquiry == null || !inquiry.getId().equals(memberId)) {

            return null;
        }
        return file;
    }

    /* ─── 관리자용: 전체 회원 문의 목록/답변 ─────────────────── */

    /**
     * @param status null이면 전체, 아니면 WAIT/DONE/REVIEW 중 하나로 필터링
     */
    public List<InquiryVO> getAllInquiriesForAdmin(String status, int page) {
        int totalCount = inquiryRepository.countAllForAdmin(status);
        if (totalCount == 0) {
            return Collections.emptyList();
        }

        List<InquiryVO> list;
        if (totalCount <= ADMIN_PAGINATION_THRESHOLD) {
            list = inquiryRepository.findAllForAdmin(status, 0, totalCount);
        } else {
            int safePage = Math.max(page, 1);
            int offset = (safePage - 1) * ADMIN_PAGE_SIZE;
            list = inquiryRepository.findAllForAdmin(status, offset, ADMIN_PAGE_SIZE);
        }

        // 관리자 목록에서 클릭 시 팝업으로 바로 상세(첨부파일 포함)를 보여주기 위해
        // 사용자 목록(getMyInquiryList)과 동일하게 각 문의에 첨부파일을 채워 넣는다
        for (InquiryVO inquiry : list) {
            inquiry.setFiles(inquiryFileRepository.findByInquiryId(inquiry.getInquiry_id()));
        }
        return list;
    }

    public int getAdminInquiryTotalPages(String status) {
        int totalCount = inquiryRepository.countAllForAdmin(status);
        if (totalCount <= ADMIN_PAGINATION_THRESHOLD) {
            return 1;
        }
        return (int) Math.ceil((double) totalCount / ADMIN_PAGE_SIZE);
    }

    /** 화면에 표시할 "몇 번째 문의인지" 계산용 (inquiry_id는 삭제된 데이터 때문에 듬성듬성 빌 수 있어서 따로 계산) */
    public int getAdminInquiryTotalCount(String status) {
        return inquiryRepository.countAllForAdmin(status);
    }

    public int getAdminPageSize() {
        return ADMIN_PAGE_SIZE;
    }

    /** 답변 등록/수정 + 상태 변경(보통 DONE, 검토만 하고 싶으면 REVIEW로도 가능) + 문의 작성자에게 답변 완료 알림 발송 */
    public void replyToInquiry(int inquiryId, String replyContent, String status) {
        inquiryRepository.replyToInquiry(inquiryId, replyContent, status);

        InquiryVO inquiry = inquiryRepository.findById(inquiryId);
        if (inquiry != null && inquiry.getId() != null && !inquiry.getId().isBlank()) {
            // 제목=문의 자체 제목, 본문=안내문구만 (공지 알림 표시 패턴과 통일)
            notificationService.createNotification(
                    inquiry.getId(),
                    NotificationType.INQUIRY_REPLY,
                    inquiry.getTitle(),
                    "문의하신 내용에 관리자 답변이 등록되었습니다.",
                    "/spendolive/inquiry/list.do"
            );
        }
    }

    /**
     * 본인 문의 수정. 아직 답변이 안 달린(WAIT) 문의만 수정 가능.
     * @return true면 수정 성공, false면 본인 문의가 아니거나 이미 답변이 달려서 수정 불가
     */
    public boolean updateInquiry(InquiryVO inquiry) {
        int updated = inquiryRepository.updateInquiry(inquiry);
        return updated > 0;
    }

    /**
     * 본인 문의 삭제. 아직 답변이 안 달린(WAIT) 문의만 삭제 가능.
     * DB 삭제가 성공한 경우에만 디스크의 첨부파일도 같이 정리한다.
     * @return true면 삭제 성공, false면 본인 문의가 아니거나 이미 답변이 달려서 삭제 불가
     */
    @Transactional
    public boolean deleteInquiry(int inquiryId, String memberId) {
        int deleted = inquiryRepository.deleteInquiry(inquiryId, memberId);
        if (deleted > 0) {
            fileStorageService.deleteInquiryFiles(inquiryId);
            return true;
        }
        return false;
    }
}
