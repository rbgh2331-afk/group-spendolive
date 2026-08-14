package com.example.spendolive.publicdata.price.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.spendolive.common.ajax.AjaxAuthSupport;
import com.example.spendolive.common.ajax.AjaxEndpoint;
import com.example.spendolive.common.ajax.AjaxResponse;
import com.example.spendolive.member.domain.MemberVO;
import com.example.spendolive.publicdata.price.domain.ConsumerPriceComparisonDTO;
import com.example.spendolive.publicdata.price.domain.ConsumerProductDTO;
import com.example.spendolive.publicdata.price.service.ConsumerPriceService;

import jakarta.servlet.http.HttpSession;

@AjaxEndpoint
@RestController
@RequestMapping("/spendolive/publicdata/consumer-price")
public class ConsumerPriceController {

    private final ConsumerPriceService consumerPriceService;

    public ConsumerPriceController(ConsumerPriceService consumerPriceService) {
        this.consumerPriceService = consumerPriceService;
    }

    // 사용자가 입력한 상품명과 일치하는 상품을 최대 20개 반환
    @GetMapping("/products.do")
    public ResponseEntity<?> searchProducts(@RequestParam(value = "keyword", required = false) String keyword,
                                            HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();

        try {
            List<ConsumerProductDTO> products = consumerPriceService.searchProducts(keyword);
            String message = products.isEmpty() ? "검색된 상품이 없습니다." : "검색된 상품을 선택해주세요.";
            return ResponseEntity.ok(AjaxResponse.success(message, products));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AjaxResponse.failure("PUBLIC_API_ERROR", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AjaxResponse.failure("PUBLIC_API_ERROR", "생필품 상품정보를 불러오지 못했습니다."));
        }
    }

    // 최근 조사일의 판매점별 가격과 최저·평균·최고 가격을 반환
    @GetMapping("/prices.do")
    public ResponseEntity<?> comparePrices(@RequestParam(value = "goodId", required = false) String goodId,
                                           @RequestParam(value = "goodName", required = false) String goodName,
                                           HttpSession session) {
        MemberVO member = AjaxAuthSupport.member(session);
        if (member == null) return AjaxAuthSupport.unauthorized();

        try {
            ConsumerPriceComparisonDTO comparison = consumerPriceService.comparePrices(goodId, goodName);
            return ResponseEntity.ok(AjaxResponse.success("최근 조사 가격을 불러왔습니다.", comparison));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(AjaxResponse.failure("INVALID_REQUEST", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(AjaxResponse.failure("PUBLIC_API_ERROR", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AjaxResponse.failure("PUBLIC_API_ERROR", "판매점별 가격정보를 불러오지 못했습니다."));
        }
    }
}
