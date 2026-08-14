package com.example.spendolive.member.controller;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.domain.MemberAjaxResponse;
import com.example.spendolive.member.domain.MemberVO;
public interface MemberController {
	public ResponseEntity<MemberAjaxResponse> sendEmail(@RequestParam("email") String email, HttpServletRequest request) throws Exception;
	public ResponseEntity<MemberAjaxResponse> login(@RequestParam Map<String, String> loginMap,HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ModelAndView loginForm(@RequestParam String log, HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ResponseEntity<MemberAjaxResponse>  addMember(@ModelAttribute("member") MemberVO member,
            HttpServletRequest request, HttpServletResponse response,RedirectAttributes redirectAttributes) throws Exception;
	public ModelAndView memberForm(HttpServletRequest request, HttpServletResponse response) throws Exception;
	public ResponseEntity<MemberAjaxResponse> sendSms(@RequestParam("phone") String phone, HttpServletRequest request) throws Exception;
	public boolean verifySms(@RequestParam("inputCode") String inputCode, HttpServletRequest request) throws Exception;
	public boolean verifyEmail(@RequestParam("inputCode") String inputCode, HttpServletRequest request) throws Exception;
	public ResponseEntity<MemberAjaxResponse> checkId(@RequestParam("id") String id) throws Exception;
	public boolean checkEmail(@RequestParam("email") String email) throws Exception;
	public boolean checkPhone(@RequestParam("phone") String phone) throws Exception;
	public ModelAndView kakaoCallback(@RequestParam(value = "code", required = false) String code, HttpServletRequest request,RedirectAttributes redirectAttributes) throws Exception;
	public ModelAndView openBankingCallback(@RequestParam("code") String code,@RequestParam("state") String state, HttpServletRequest request, HttpServletResponse response ,HttpSession session,RedirectAttributes redirectAttributes) throws Exception;
	public String openBankingAuth() throws Exception;
	public String openBankingIntegratedAuth() throws UnsupportedEncodingException;
	public ModelAndView openBankingIntegratedcallback(
    @RequestParam("code") String code,
    @RequestParam("state") String state,
    HttpServletRequest request, HttpServletResponse response,
    HttpSession session,RedirectAttributes redirectAttributes) throws UnsupportedEncodingException;
}
