package com.example.spendolive.admin.dashboard.repository;

import com.example.spendolive.admin.dashboard.domain.AdminDashboardDTO;

/**
 * 관리자 대시보드 통계 조회 저장소
 */
public interface AdminDashboardRepository {

    AdminDashboardDTO selectDashboardSummary();
}
