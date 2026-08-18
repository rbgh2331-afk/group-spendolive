package com.example.spendolive.publicdata.price.domain;

public class ConsumerProductDTO {

    // 한국소비자원 상품아이디다
    private String goodId;

    // 사용자가 선택할 상품명이다
    private String goodName;

    // 상품 용량과 단위를 검색 결과에 함께 표시
    private String goodTotalCnt;
    private String goodTotalDivCode;

    public ConsumerProductDTO() {}

    public ConsumerProductDTO(String goodId, String goodName, String goodTotalCnt, String goodTotalDivCode) {
        this.goodId = goodId;
        this.goodName = goodName;
        this.goodTotalCnt = goodTotalCnt;
        this.goodTotalDivCode = goodTotalDivCode;
    }

    public String getGoodId() { return goodId; }
    public void setGoodId(String goodId) { this.goodId = goodId; }
    public String getGoodName() { return goodName; }
    public void setGoodName(String goodName) { this.goodName = goodName; }
    public String getGoodTotalCnt() { return goodTotalCnt; }
    public void setGoodTotalCnt(String goodTotalCnt) { this.goodTotalCnt = goodTotalCnt; }
    public String getGoodTotalDivCode() { return goodTotalDivCode; }
    public void setGoodTotalDivCode(String goodTotalDivCode) { this.goodTotalDivCode = goodTotalDivCode; }
}
