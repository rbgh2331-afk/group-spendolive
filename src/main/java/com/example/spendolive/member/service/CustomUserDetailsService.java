package com.example.spendolive.member.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.member.repository.MemberRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository; // 작성해두신 DAO 주입

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        
        // 1. DB에서 ID로 회원 조회 (비밀번호 비교 X)
        MemberVO member = memberRepository.selectMemberById(id);

        // 2. 만약 없는 아이디라면 에러 던지기
        if (member == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + id);
        }

        // 3. 찾은 회원 정보(아이디, 암호문, 권한)를 시큐리티 규격에 맞게 포장해서 리턴!
        return User.builder()
                .username(member.getId())
                .password(member.getPassword()) // DB의 암호화된 비밀번호
                .roles(member.getRole() != null ? member.getRole() : "USER")
                .build();
    }
}