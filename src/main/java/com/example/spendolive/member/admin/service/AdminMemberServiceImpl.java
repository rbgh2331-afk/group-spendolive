package com.example.spendolive.member.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.repository.MemberRepository;

/**
 * 관리자 회원 관리 서비스 구현체
 */
@Service
public class AdminMemberServiceImpl implements AdminMemberService{
    private final MemberRepository memberRepository;

    public AdminMemberServiceImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
    @Override
    @Transactional
    public List<MemberVO> selectMemberAll() throws Exception{
        return memberRepository.selectMemberAll();
    }
}
