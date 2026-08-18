package com.example.spendolive.member.admin.service;

import java.util.List;

import com.example.spendolive.member.domain.MemberVO;

/**
 * 관리자 회원 관리 서비스
 */
public interface AdminMemberService {
    public List<MemberVO> selectMemberAll() throws Exception;
}
