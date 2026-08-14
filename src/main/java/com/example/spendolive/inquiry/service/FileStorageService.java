package com.example.spendolive.inquiry.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.spendolive.inquiry.domain.InquiryFileVO;

/*
 * 문의 첨부파일을 실제 디스크에 저장하는 서비스.
 * 저장 경로는 application.properties의 app.upload.inquiry-dir 로 설정 가능
 * (기본값: 프로젝트 루트 기준 uploads/inquiry)
 *
 * 예) application.properties
 *   app.upload.inquiry-dir=/data/spendolive/uploads/inquiry
 *   spring.servlet.multipart.max-file-size=5MB
 *   spring.servlet.multipart.max-request-size=15MB
 */
@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final List<String> ALLOWED_EXT = List.of("png", "jpg", "jpeg", "gif", "pdf");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB, inquiryWrite.jsp 안내 문구와 동일
    private static final int MAX_FILE_COUNT = 5;                // inquiryWrite.jsp 안내 문구와 동일

    private final String uploadDir;

    public FileStorageService(@Value("${app.upload.inquiry-dir:uploads/inquiry}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    /**
     * 첨부파일들을 디스크에 저장하고, DB insert에 쓸 InquiryFileVO 목록을 만들어 반환한다.
     * DB insert 자체는 호출부(InquiryService)에서 트랜잭션 안에서 처리한다.
     *
     * @param inquiry_id  이미 생성된 문의 번호 (inquiry_tb PK)
     * @param attachments 폼에서 넘어온 첨부파일 배열 (null/빈 파일 섞여 있어도 됨)
     */
    public List<InquiryFileVO> storeFiles(int inquiry_id, MultipartFile[] attachments) {
        List<InquiryFileVO> result = new ArrayList<>();
        if (attachments == null || attachments.length == 0) {
            return result;
        }

        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : attachments) {
            if (file != null && !file.isEmpty()) {
                validFiles.add(file);
            }
        }
        if (validFiles.isEmpty()) {
            return result;
        }
        if (validFiles.size() > MAX_FILE_COUNT) {
            throw new IllegalArgumentException("첨부파일은 최대 " + MAX_FILE_COUNT + "개까지 업로드할 수 있습니다.");
        }

        // 저장 전에 전체 파일 먼저 검증 (하나라도 규격 위반이면 아무 것도 저장하지 않음)
        for (MultipartFile file : validFiles) {
            validateFile(file);
        }

        try {
            Path targetDir = Path.of(uploadDir, String.valueOf(inquiry_id));
            Files.createDirectories(targetDir);

            for (MultipartFile file : validFiles) {
                String origin_name = file.getOriginalFilename();
                String ext = extractExtension(origin_name);
                String saved_name = UUID.randomUUID() + "." + ext;

                Path targetPath = targetDir.resolve(saved_name);
                file.transferTo(targetPath);

                InquiryFileVO fileVO = new InquiryFileVO();
                fileVO.setInquiry_id(inquiry_id);
                fileVO.setOrigin_name(origin_name);
                fileVO.setSaved_name(saved_name);
                fileVO.setFile_path(targetPath.toString());
                fileVO.setFile_size(file.getSize());
                result.add(fileVO);
            }
        } catch (IOException e) {
            throw new RuntimeException("첨부파일 저장 중 오류가 발생했습니다.", e);
        }

        return result;
    }

    /**
     * 문의 삭제 시 호출. inquiry_id 하위 디렉토리(첨부파일이 저장된 폴더)를 통째로 지운다.
     * DB의 inquiry_file_tb 행은 ON DELETE CASCADE로 이미 지워진 상태이므로,
     * 여기서는 디스크에 남은 실제 파일만 정리하면 된다. 실패해도 문의 삭제 자체는 이미 끝난 뒤라
     * 예외를 던지지 않고 로그만 남긴다(고아 파일이 남는 것보다 삭제 자체가 실패하는 게 더 나쁨).
     */
    public void deleteInquiryFiles(int inquiry_id) {
        Path targetDir = Path.of(uploadDir, String.valueOf(inquiry_id));
        if (!Files.exists(targetDir)) {
            return;
        }
        try (var paths = Files.walk(targetDir)) {
            paths.sorted(java.util.Comparator.reverseOrder()) // 파일 먼저, 디렉토리는 마지막에 삭제
                 .forEach(p -> {
                     try {
                         Files.deleteIfExists(p);
                     } catch (IOException e) {
                         log.error("{}", "[FileStorageService.deleteInquiryFiles] 파일 삭제 실패: " + p + " - " + e.getMessage(), e);
                     }
                 });
        } catch (IOException e) {
            log.error("{}", "[FileStorageService.deleteInquiryFiles] 디렉토리 정리 실패: inquiry_id=" + inquiry_id + " - " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 \"" + file.getOriginalFilename() + "\"의 용량이 5MB를 초과합니다.");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (ext.isEmpty() || !ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + file.getOriginalFilename());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}