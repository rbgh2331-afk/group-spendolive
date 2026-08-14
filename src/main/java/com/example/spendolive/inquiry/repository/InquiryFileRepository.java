package com.example.spendolive.inquiry.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.spendolive.inquiry.domain.InquiryFileVO;


@Repository
public class InquiryFileRepository {
    private static final Logger log = LoggerFactory.getLogger(InquiryFileRepository.class);

    // ────────────────────────────────────────────────────────────
    // SQL 정의
    // ────────────────────────────────────────────────────────────

    // findByInquiryId / findById에서 공통으로 쓰는 SELECT 컬럼 목록
    private static final String SELECT_COLUMNS = """
            file_id, inquiry_id, origin_name, saved_name, file_size,
            file_path, TO_CHAR(reg_date, 'YYYY.MM.DD') AS reg_date
        """;

    // 첨부파일 등록
    private static final String INSERT_SQL = """
            INSERT INTO inquiry_file_tb(file_id, inquiry_id, origin_name, saved_name, file_path, file_size, reg_date)
            VALUES(inquiry_file_seq.NEXTVAL, ?, ?, ?, ?, ?, SYSDATE)
        """;

    // 특정 문의의 첨부파일 목록
    private static final String FIND_BY_INQUIRY_ID_SQL = """
            SELECT """ + SELECT_COLUMNS + """
            FROM inquiry_file_tb
            WHERE inquiry_id = ?
            ORDER BY file_id
        """;

    // 첨부파일 단건 조회 (다운로드/미리보기용)
    private static final String FIND_BY_ID_SQL = """
            SELECT """ + SELECT_COLUMNS + """
            FROM inquiry_file_tb
            WHERE file_id = ?
        """;

    // ────────────────────────────────────────────────────────────
    // 필드 / 생성자 / RowMapper
    // ────────────────────────────────────────────────────────────

    private final JdbcTemplate jdbcTemplate;

    public InquiryFileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private InquiryFileVO mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        InquiryFileVO file = new InquiryFileVO();
        file.setFile_id(rs.getInt("file_id"));
        file.setInquiry_id(rs.getInt("inquiry_id"));
        file.setOrigin_name(rs.getString("origin_name"));
        file.setSaved_name(rs.getString("saved_name"));
        file.setFile_path(rs.getString("file_path"));
        file.setFile_size(rs.getLong("file_size"));
        file.setReg_date(rs.getString("reg_date"));
        return file;
    }

    // ────────────────────────────────────────────────────────────
    // 조회 / 등록 메서드
    // ────────────────────────────────────────────────────────────

    /* ─── 첨부파일 등록 ───────────────────────────────────── */
    public void insertFile(InquiryFileVO file) {
        try {
            jdbcTemplate.update(INSERT_SQL,
                    file.getInquiry_id(), file.getOrigin_name(), file.getSaved_name(),
                    file.getFile_path(), file.getFile_size());
        } catch (DataAccessException e) {
            log.error("{}", "[InquiryFileRepository.insertFile] DB 오류: " + e.getMessage(), e);
            throw e;
        }
    }

    /* ─── 특정 문의의 첨부파일 목록 ───────────────────────── */
    public List<InquiryFileVO> findByInquiryId(int inquiryId) {
        try {
            return jdbcTemplate.query(FIND_BY_INQUIRY_ID_SQL, (rs, rowNum) -> mapRow(rs), inquiryId);
        } catch (DataAccessException e) {
            log.error("{}", "[InquiryFileRepository.findByInquiryId] DB 오류: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /* ─── 첨부파일 단건 조회 (다운로드/미리보기용) ────────── */
    public InquiryFileVO findById(int fileId) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_SQL, (rs, rowNum) -> mapRow(rs), fileId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error("{}", "[InquiryFileRepository.findById] DB 오류: " + e.getMessage(), e);
            return null;
        }
    }
}