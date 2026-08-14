package com.example.spendolive.ott.admin.controller;

import java.util.List;

import com.example.spendolive.ott.admin.service.AdminOttService;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.ott.domain.OttServiceDTO;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 OTT 관리 Controller
 *
 * 패키지 위치:
 * com.example.spendolive.ott.admin.controller
 *
 * 역할:
 * - 관리자 OTT 관리 URL 매핑
 * - 로그인/관리자 권한 확인
 * - Service 호출
 * - 화면 이동 처리
 *
 * 주의:
 * - SQL은 Controller에 작성하지 않습니다.
 * - DB 처리는 Repository에서 담당
 */
@Controller
@RequestMapping({"/admin/ott", "/spendolive/admin/ott"})
public class AdminOttController {

    private final AdminOttService adminOttService;

    public AdminOttController(AdminOttService adminOttService) {
        this.adminOttService = adminOttService;
    }

    private boolean isAdmin(HttpSession session) {
        MemberVO member = (MemberVO) session.getAttribute("memberInfo");
        return member != null && "ADMIN".equals(member.getRole());
    }

    private ModelAndView layout() {
        ModelAndView mav = new ModelAndView("common/layout");
        mav.addObject("body_page", "/WEB-INF/views/admin/ott/ott.jsp");
        return mav;
    }

    @GetMapping("/list.do")
    public ModelAndView list(HttpSession session) {
        if (!isAdmin(session)) {
            return new ModelAndView("redirect:/spendolive/main.do");
        }

        List<OttServiceDTO> serviceList = adminOttService.getOttServiceList();

        ModelAndView mav = layout();
        mav.addObject("serviceList", serviceList);
        return mav;
    }

    @GetMapping("/edit.do")
    public ModelAndView edit(@RequestParam("ott_service_id") Long ott_service_id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return new ModelAndView("redirect:/spendolive/main.do");
        }

        OttServiceDTO editService = adminOttService.getOttService(ott_service_id);

        if (editService == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "존재하지 않는 OTT 항목입니다.");
            return new ModelAndView("redirect:/admin/ott/list.do");
        }

        ModelAndView mav = layout();
        mav.addObject("serviceList", adminOttService.getOttServiceList());
        mav.addObject("editService", editService);
        return mav;
    }

    @PostMapping("/insert.do")
    public ModelAndView insert(@ModelAttribute OttServiceDTO ottService,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return new ModelAndView("redirect:/spendolive/main.do");
        }

        try {
            adminOttService.addOttService(ottService);
            redirectAttributes.addFlashAttribute("msg", "OTT 항목이 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "OTT 항목 등록 중 오류가 발생했습니다: " + e.getMessage());
        }

        return new ModelAndView("redirect:/admin/ott/list.do");
    }

    @PostMapping("/update.do")
    public ModelAndView update(@ModelAttribute OttServiceDTO ottService,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return new ModelAndView("redirect:/spendolive/main.do");
        }

        try {
            boolean updated = adminOttService.modifyOttService(ottService);

            if (updated) {
                redirectAttributes.addFlashAttribute("msg", "OTT 항목이 수정되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "수정할 OTT 항목을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "OTT 항목 수정 중 오류가 발생했습니다: " + e.getMessage());
        }

        return new ModelAndView("redirect:/admin/ott/list.do");
    }

    @PostMapping("/delete.do")
    public ModelAndView delete(@RequestParam("ott_service_id") Long ott_service_id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return new ModelAndView("redirect:/spendolive/main.do");
        }

        try {
            boolean hidden = adminOttService.hideOttService(ott_service_id);

            if (hidden) {
                redirectAttributes.addFlashAttribute("msg", "OTT 항목이 숨김 처리되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("errorMsg", "삭제 또는 숨김 처리할 OTT 항목을 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "OTT 항목 삭제 처리 중 오류가 발생했습니다.");
        }

        return new ModelAndView("redirect:/admin/ott/list.do");
    }
}
