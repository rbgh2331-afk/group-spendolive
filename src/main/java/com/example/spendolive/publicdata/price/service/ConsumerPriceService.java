package com.example.spendolive.publicdata.price.service;

import java.util.List;

import com.example.spendolive.publicdata.price.domain.ConsumerPriceComparisonDTO;
import com.example.spendolive.publicdata.price.domain.ConsumerProductDTO;

public interface ConsumerPriceService {

    // 상품명으로 한국소비자원 상품 목록을 검색한다
    List<ConsumerProductDTO> searchProducts(String keyword) throws Exception;

    // 최근 조사일 기준 판매점별 가격을 비교
    ConsumerPriceComparisonDTO comparePrices(String goodId, String goodName) throws Exception;
}
