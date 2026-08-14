package com.example.spendolive.member.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.member.admin.service.AdminMemberService;
import com.example.spendolive.member.domain.MemberAjaxResponse;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.mypage.service.MyPageService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller("AdminMemberController")
@RequestMapping(value="/admin/member")
public class AdminMemberControllerImpl implements AdminMemberController{
    private final AdminMemberService adminmemberService;
    private final MyPageService myPageService;

    public AdminMemberControllerImpl(AdminMemberService adminmemberService, MyPageService myPageService) {
        this.adminmemberService = adminmemberService;
        this.myPageService = myPageService;
    }

    @Override
    @GetMapping("/list.do")
    public ModelAndView listUpSettlement(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        if (status==null) {status = "READY";}

        try {
            List<MemberVO> memberList = adminmemberService.selectMemberAll();
            session.setAttribute("memberList", memberList);
            return layout("/WEB-INF/views/admin/member/member.jsp");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "리스트업에 실패 하였습니다. ");
            return layout("/WEB-INF/views/admin/member/member.jsp");
        }
    }

    // 관리자 회원관리 화면에서만 호출되는 강제탈퇴 API입니다
    // SecurityConfig의 /admin/** 권한 검사와 Controller의 role 검사를 함께 적용
    @Override
    @PostMapping("/withdraw.do")
    @ResponseBody
    public ResponseEntity<MemberAjaxResponse> withdrawMember(@RequestParam("id") String id, HttpSession session) throws Exception {
        MemberVO admin = session == null ? null : (MemberVO) session.getAttribute("memberInfo");
        if (admin == null || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MemberAjaxResponse(
                    false,
                    "FORBIDDEN",
                    "관리자 권한이 필요한 기능입니다.",
                    "FAILED",
                    id,
                    "/spendolive/main.do"));
        }

        try {
            myPageService.withdrawMember(id);
            return ResponseEntity.ok(new MemberAjaxResponse(
                    true,
                    "WITHDRAW_COMPLETED",
                    "탈퇴가 완료되었습니다.",
                    "SUCCESS",
                    id,
                    "/admin/member/list.do"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MemberAjaxResponse(
                    false,
                    "WITHDRAW_FAILED",
                    "탈퇴에 실패하였습니다.",
                    "FAILED",
                    id,
                    "/admin/member/list.do"));
        }
    }

    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
}
