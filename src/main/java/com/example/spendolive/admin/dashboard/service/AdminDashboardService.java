package com.example.spendolive.admin.dashboard.service;

import com.example.spendolive.admin.dashboard.domain.AdminDashboardDTO;

/**
 * 관리자 대시보드 통계 조회 서비스
 */
public interface AdminDashboardService {

    AdminDashboardDTO getDashboardSummary();
}
