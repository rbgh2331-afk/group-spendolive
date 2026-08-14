package com.example.spendolive.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.spendolive.payment.domain.PaymentAjaxResponse;
import com.example.spendolive.payment.domain.SettlementPaymentVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * 관리자 결제 및 정산 관리 요청 처리 컨트롤러
 */
public interface AdminPaymentController {
    public ModelAndView listUpSettlement(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<PaymentAjaxResponse> pay(@RequestParam("room_id") int room_id, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ModelAndView paymentlistUpSettlement(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<PaymentAjaxResponse> payment(
        @RequestParam("userId") String userId, @RequestParam("roomId") String roomIdStr,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<PaymentAjaxResponse> paymentlate(
        @RequestParam("member_login_id") String member_login_id , @RequestParam("room_id") int room_id, @RequestParam("pay_late_day") int pay_late_day,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
    public ResponseEntity<PaymentAjaxResponse> calcelpayment(
        SettlementPaymentVO payment,
        HttpServletRequest request, HttpServletResponse response,
        HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
        public ModelAndView paymentdetaillistUpSettlement(@RequestParam(value = "status", required = false) String status, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception;
        public ResponseEntity<PaymentAjaxResponse> paymentStatus(
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "member_login_id", required = false) String memberLoginId,
            @RequestParam(value = "host_id", required = false) String hostId,
            @RequestParam(value = "payment_id", required = false) Integer paymentId,
            HttpServletRequest request,
            HttpSession session) throws Exception;

}
