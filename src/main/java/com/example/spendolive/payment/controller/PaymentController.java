package com.example.spendolive.payment.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.spendolive.payment.domain.PaymentAjaxResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface PaymentController {
    ModelAndView detail(
        @RequestParam("room_id") int roomId,
        HttpServletRequest request,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes) throws Exception;

String tossCallback(
        @RequestParam("customerKey") String customerKey,
        @RequestParam("authKey") String authKey,
        HttpServletRequest request,
        HttpServletResponse response,
        HttpSession session,
        RedirectAttributes redirectAttributes) throws Exception;

ResponseEntity<PaymentAjaxResponse> payment(
        @RequestParam("roomId") int roomId,
        HttpServletRequest request,
        HttpSession session) throws Exception;

ResponseEntity<PaymentAjaxResponse> paymentStatus(
        @RequestParam("room_id") int room_id,
        HttpServletRequest request,
        HttpSession session) throws Exception;

String tossCallback(RedirectAttributes redirectAttributes) throws Exception;
ResponseEntity<PaymentAjaxResponse> updatePrimaryCard(@RequestParam("card_Idx") String card_idxstr,@RequestHeader(value = "Referer", required = false) String referer,HttpServletRequest request,HttpSession session) throws Exception;
}
