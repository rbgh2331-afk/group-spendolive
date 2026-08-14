package com.example.spendolive.ott.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.spendolive.ott.admin.repository.AdminOttRepository;
import com.example.spendolive.ott.domain.OttServiceDTO;

/**
 * 관리자 OTT 관리 Service 구현체
 *
 * 패키지 위치:
 * com.example.spendolive.ott.admin.service
 *
 * 역할:
 * - 입력값 정리
 * - 기본값 처리
 * - default_price 자동 계산
 * - 삭제 대신 숨김 처리 정책
 */
@Service
public class AdminOttServiceImpl implements AdminOttService {

    private final AdminOttRepository adminOttRepository;

    public AdminOttServiceImpl(AdminOttRepository adminOttRepository) {
        this.adminOttRepository = adminOttRepository;
    }

    @Override
    public List<OttServiceDTO> getOttServiceList() {
        return adminOttRepository.selectOttServiceList();
    }

    @Override
    public OttServiceDTO getOttService(Long ott_service_id) {
        if (ott_service_id == null) {
            return null;
        }

        return adminOttRepository.selectOttService(ott_service_id);
    }

    @Override
    public void addOttService(OttServiceDTO ottService) {
        prepareForSave(ottService);
        adminOttRepository.insertOttService(ottService);
    }

    @Override
    public boolean modifyOttService(OttServiceDTO ottService) {
        if (ottService.getOtt_service_id() == null) {
            throw new IllegalArgumentException("수정할 OTT 항목 ID가 없습니다.");
        }

        prepareForSave(ottService);
        return adminOttRepository.updateOttService(ottService) > 0;
    }

    @Override
    public boolean hideOttService(Long ott_service_id) {
        if (ott_service_id == null) {
            throw new IllegalArgumentException("숨김 처리할 OTT 항목 ID가 없습니다.");
        }

        return adminOttRepository.hideOttService(ott_service_id) > 0;
    }

    private void prepareForSave(OttServiceDTO dto) {
        dto.setService_name(normalizeRequired(dto.getService_name(), "OTT 이름"));
        dto.setFixed_plan_name(normalizeDefault(dto.getFixed_plan_name(), "프리미엄"));

        if (!"N".equals(dto.getShare_yn())) {
            dto.setShare_yn("Y");
        }

        dto.setRisk_level(normalizeNullable(dto.getRisk_level()));
        dto.setBlock_reason(normalizeNullable(dto.getBlock_reason()));

        if (dto.getBase_price() < 0) {
            throw new IllegalArgumentException("최고 멤버십 가격은 0원 이상이어야 합니다.");
        }

        if (dto.getExtra_member_fee() < 0) {
            throw new IllegalArgumentException("추가 멤버 비용은 0원 이상이어야 합니다.");
        }

        if (dto.getExtra_member_count() < 0) {
            throw new IllegalArgumentException("추가 멤버 수는 0명 이상이어야 합니다.");
        }

        if (dto.getMax_member_limit() <= 0) {
            dto.setMax_member_limit(4);
        }

        if (dto.getPlatform_fee_rate() < 0) {
            dto.setPlatform_fee_rate(0.0);
        }

        /*
         * 최종 기준금액을 입력하지 않았으면 자동 계산
         * base_price + extra_member_fee * extra_member_count
         */
        if (dto.getDefault_price() <= 0) {
            int calculatedPrice = dto.getBase_price() + (dto.getExtra_member_fee() * dto.getExtra_member_count());
            dto.setDefault_price(calculatedPrice);
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }

    private String normalizeDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
