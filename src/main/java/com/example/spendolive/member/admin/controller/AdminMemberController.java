package com.example.spendolive.member.admin.controller;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberAjaxResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
/**
 * 관리자 회원 관리 요청 규격을 정의하는 컨트롤러 인터페이스
 */
public interface AdminMemberController {
    public ModelAndView listUpSettlement(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<MemberAjaxResponse> withdrawMember(@RequestParam("id") String id, HttpSession session) throws Exception;
}
