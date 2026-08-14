package com.example.spendolive.payment.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.spendolive.member.service.MemberService;
import com.example.spendolive.ott.domain.OttRoomDTO;
import com.example.spendolive.ott.domain.OttRoomMemberDTO;
import com.example.spendolive.ott.domain.OttSettlementDTO;
import com.example.spendolive.payment.domain.PaymentAjaxResponse;
import com.example.spendolive.payment.domain.PaymentAmountDTO;
import com.example.spendolive.payment.domain.SettlementPaymentVO;
import com.example.spendolive.payment.exception.PaymentProcessException;
import com.example.spendolive.payment.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
@Controller("AdminPaymentController")
@RequestMapping(value="/admin/settlement")
public class AdminPaymentControllerImpl implements AdminPaymentController{
    private static final Logger log = LoggerFactory.getLogger(AdminPaymentControllerImpl.class);

    private final PaymentService paymentService;

    public AdminPaymentControllerImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    @Override
    @GetMapping("/list.do")
    public ModelAndView listUpSettlement(@RequestParam(value = "status", required = false) String status,HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        if(status==null){status = "READY";}
        
        try {
            List<OttRoomDTO> settlementList = paymentService.selectTodaysettlement(status);
            ModelAndView mav = layout("/WEB-INF/views/admin/settlement/settlement.jsp");
            mav.addObject("settlementList", settlementList);
            return mav;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "리스트업에 실패 하였습니다. ");
            return layout("/WEB-INF/views/admin/settlement/settlement.jsp");
        }
    }
    @Override
    @GetMapping("/paymentlist.do")
    public ModelAndView paymentlistUpSettlement(@RequestParam(value = "status", required = false) String status,HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        if(status==null){status = "READY";}
        
        try {
            List<OttRoomMemberDTO> paymentList = paymentService.selectTodaysettlementmember(status);
            ModelAndView mav = layout("/WEB-INF/views/admin/settlement/payment.jsp");
            mav.addObject("paymentList", paymentList);
            return mav;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "리스트업에 실패 하였습니다. ");
            return layout("/WEB-INF/views/admin/settlement/payment.jsp");
        }
    }
    @Override
    @GetMapping("/paymentdetaillist.do")
    public ModelAndView paymentdetaillistUpSettlement(@RequestParam(value = "status", required = false) String status,HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        if(status==null){status = "READY";}
        
        try {
            List<SettlementPaymentVO> paymentdetailList = paymentService.selectpaymentAll();
            ModelAndView mav = layout("/WEB-INF/views/admin/settlement/paymentdetail.jsp");
            mav.addObject("paymentdetailList", paymentdetailList);
            return mav;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("msg", "리스트업에 실패 하였습니다. ");
            return layout("/WEB-INF/views/admin/settlement/paymentdetail.jsp");
        }
    }
    @Override
    @PostMapping("/pay.do")
    public ResponseEntity<PaymentAjaxResponse> pay(@RequestParam("room_id") int room_id, HttpServletRequest request, HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) throws Exception {
        session = request.getSession();
        
        try {
            String msg = paymentService.updateExcrow(room_id);
            
            return ResponseEntity.ok(new PaymentAjaxResponse(
                    true,
                    "SETTLEMENT_COMPLETED",
                    msg,
                    "PAID",
                    null,
                    "/admin/settlement/list.do"));
        } catch (PaymentProcessException e) {
            return ResponseEntity
                    .status(resolveHttpStatus(e.getCode()))
                    .body(new PaymentAjaxResponse(
                            false,
                            e.getCode(),
                            e.getMessage(),
                            "FAILED",
                            null,
                            null));

        } catch (Exception e) {
            log.error("관리자 결제 상태 조회 중 오류가 발생했습니다.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentAjaxResponse(
                            false,
                            "SETTLEMENT_FAILED",
                            "송금 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "FAILED",
                            null,
                            null));
        }
    }
    private ModelAndView layout(String bodyPage) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("common/layout");
        mav.addObject("body_page", bodyPage);
        return mav;
    }
    @Override
    @PostMapping("/paymenting.do")
    public ResponseEntity<PaymentAjaxResponse> payment(
        @RequestParam("member_login_id") String member_login_id,@RequestParam("room_id") String room_idStr,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, RedirectAttributes redirectAttributes) throws Exception {


        int room_id = Integer.parseInt(room_idStr);

        try {
            // 사용자 결제와 동일한 PaymentService 금액 계산 규칙(1/N + 플랫폼 수수료 3%)을 사용한다.
            PaymentAmountDTO paymentAmount = paymentService.getPaymentAmount(room_id);
            paymentService.executeAutomaticPayment(
                    member_login_id,
                    paymentAmount.getTotalAmount(),
                    room_id,
                    paymentAmount.getFeeAmount(),
                    paymentAmount.getBaseAmount(),
                    paymentAmount.getSettlementId(),
                    paymentAmount.getHostLoginId());
            return ResponseEntity.ok(new PaymentAjaxResponse(
                    true,
                    "PAYMENT_COMPLETED",
                    "결제가 완료되었습니다.",
                    "PAID",
                    null,
                    "/admin/settlement/paymentlist.do"));
      
            
        } catch (PaymentProcessException e) {
            return ResponseEntity
                    .status(resolveHttpStatus(e.getCode()))
                    .body(new PaymentAjaxResponse(
                            false,
                            e.getCode(),
                            e.getMessage(),
                            "FAILED",
                            null,
                            null));

        } catch (Exception e) {
            log.error("관리자 결제 처리 중 오류가 발생했습니다.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentAjaxResponse(
                            false,
                            "PAYMENT_FAILED",
                            "결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "FAILED",
                            null,
                            null));
        }
    }
    @Override
    @PostMapping("/paymentlate.do")
    public ResponseEntity<PaymentAjaxResponse> paymentlate(
        @RequestParam("member_login_id") String member_login_id ,@RequestParam("room_id") int room_id,@RequestParam("pay_late_day") int pay_late_day,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, RedirectAttributes redirectAttributes) throws Exception {

        try {
            paymentService.updateTodaysettlementroommemberlate(room_id,member_login_id,pay_late_day);
            return ResponseEntity.ok(new PaymentAjaxResponse(
                true,
                "LATEDAY_COMPLETED",
                "연기가 완료되었습니다.",
                "COMPLETE",
                null,
                "/admin/settlement/paymentlist.do"));
            
        } catch (PaymentProcessException e) {
            return ResponseEntity
                    .status(resolveHttpStatus(e.getCode()))
                    .body(new PaymentAjaxResponse(
                            false,
                            e.getCode(),
                            e.getMessage(),
                            "FAILED",
                            null,
                            "/admin/settlement/paymentlist.do"));

        } catch (Exception e) {
            log.error("관리자 결제 목록 처리 중 오류가 발생했습니다.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentAjaxResponse(
                            false,
                            "LATEDAY_FAILED",
                            "정산 연기 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "FAILED",
                            null,
                            "/admin/settlement/paymentlist.do"));
        }
    }
    @Override
    @PostMapping("/cancelpaymenting.do")
    public ResponseEntity<PaymentAjaxResponse> calcelpayment(
            SettlementPaymentVO payment,
            HttpServletRequest request, HttpServletResponse response,
            HttpSession session, RedirectAttributes redirectAttributes) throws Exception {

        try {
            
            paymentService.executeRoomRefund(payment);
            return ResponseEntity.ok(new PaymentAjaxResponse(
                    true,
                    "REFUND_COMPLETED",
                    "취소가 완료되었습니다.",
                    "REFUNDED",
                    null,
                    "/admin/settlement/paymentdetaillist.do"));
        } catch (PaymentProcessException e) {
            return ResponseEntity
                    .status(resolveHttpStatus(e.getCode()))
                    .body(new PaymentAjaxResponse(
                            false,
                            e.getCode(),
                            e.getMessage(),
                            "FAILED",
                            null,
                            "/admin/settlement/paymentdetaillist.do"));

        } catch (Exception e) {
            log.error("관리자 결제 상세 처리 중 오류가 발생했습니다.", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaymentAjaxResponse(
                            false,
                            "REFUND_FAILED",
                            "결제 취소 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                            "FAILED",
                            null,
                            "/admin/settlement/paymentdetaillist.do"));
        }
    }
    private HttpStatus resolveHttpStatus(String code) {
        if ("PAYMENT_PROCESSING".equals(code)
                || "ROOM_FULL".equals(code)
                || "PAYMENT_NOT_ALLOWED".equals(code)) {
            return HttpStatus.CONFLICT;
        }

        if ("CARD_REQUIRED".equals(code)
                || "INVALID_PAYMENT_INFO".equals(code)
                || "HOST_CANNOT_PAY".equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    @Override
    @GetMapping(value = "/status.do", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public ResponseEntity<PaymentAjaxResponse> paymentStatus(
            @RequestParam(value = "room_id", required = false) Integer roomId,
            @RequestParam(value = "member_login_id", required = false) String memberLoginId,
            @RequestParam(value = "host_id", required = false) String hostId,
            @RequestParam(value = "payment_id", required = false) Integer paymentId,
            HttpServletRequest request,
            HttpSession session) throws Exception {

        String paymentStatus;
        if (hostId != null && !hostId.isBlank()) {
            if (roomId == null || roomId <= 0) {
                return invalidStatusRequest("송금 상태를 확인할 방 정보가 없습니다.");
            }
            paymentStatus = paymentService.selectEscrowStatus(roomId, hostId);
        } else if (memberLoginId != null && !memberLoginId.isBlank()) {
            if (roomId == null || roomId <= 0) {
                return invalidStatusRequest("결제 상태를 확인할 방 정보가 없습니다.");
            }
            paymentStatus = paymentService.getRoomPaymentStatus(memberLoginId, roomId);
        } else if (paymentId != null && paymentId > 0) {
            paymentStatus = paymentService.selectRefundStatus(paymentId);
        } else {
            return invalidStatusRequest("상태 확인에 필요한 결제 정보가 없습니다.");
        }

        if (isPaidStatus(paymentStatus)) {
            return ResponseEntity.ok(new PaymentAjaxResponse(
                    true,
                    "PAYMENT_COMPLETED",
                    "결제가 완료된 것을 확인했습니다.",
                    paymentStatus,
                    roomId,
                    null));
        }

        String message = "PROCESSING".equals(paymentStatus)
                ? "결제를 처리하고 있습니다."
                : "아직 결제가 완료되지 않았습니다.";

        return ResponseEntity.ok(new PaymentAjaxResponse(
                false,
                "PROCESSING".equals(paymentStatus)
                        ? "PAYMENT_PROCESSING"
                        : "PAYMENT_NOT_COMPLETED",
                message,
                paymentStatus,
                roomId,
                null));
    }

    private ResponseEntity<PaymentAjaxResponse> invalidStatusRequest(String message) {
        return ResponseEntity.badRequest().body(new PaymentAjaxResponse(
                false,
                "INVALID_PAYMENT_INFO",
                message,
                "FAILED",
                null,
                null));
    }

    private boolean isPaidStatus(String paymentStatus) {
        return "PAID".equals(paymentStatus)
                || "CONFIRMED".equals(paymentStatus);
    }
}