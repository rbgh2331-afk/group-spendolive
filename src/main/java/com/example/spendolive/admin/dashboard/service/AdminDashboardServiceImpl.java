package com.example.spendolive.admin.dashboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spendolive.admin.dashboard.domain.AdminDashboardDTO;
import com.example.spendolive.admin.dashboard.repository.AdminDashboardRepository;

/**
 * 관리자 대시보드 통계 조회 서비스 구현체
 */
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardRepository adminDashboardRepository;

    public AdminDashboardServiceImpl(AdminDashboardRepository adminDashboardRepository) {
        this.adminDashboardRepository = adminDashboardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboardSummary() {
        return adminDashboardRepository.selectDashboardSummary();
    }
}
