package com.example.spendolive.publicdata.price.domain;

import java.util.ArrayList;
import java.util.List;

public class ConsumerPriceComparisonDTO {

    // [생필품 가격 비교] 선택한 상품과 실제 가격 조사일을 화면에 전달한다.
    private String goodId;
    private String goodName;
    private String inspectDay;

    // [생필품 가격 비교] 판매점 가격을 기준으로 계산한 요약값이다.
    private int lowestPrice;
    private int averagePrice;
    private int highestPrice;

    // [생필품 가격 비교] 낮은 가격순으로 정렬한 판매점 목록이다.
    private List<StorePrice> stores = new ArrayList<>();

    public ConsumerPriceComparisonDTO() {}

    public ConsumerPriceComparisonDTO(String goodId, String goodName, String inspectDay, int lowestPrice, int averagePrice, int highestPrice, List<StorePrice> stores) {
        this.goodId = goodId;
        this.goodName = goodName;
        this.inspectDay = inspectDay;
        this.lowestPrice = lowestPrice;
        this.averagePrice = averagePrice;
        this.highestPrice = highestPrice;
        this.stores = stores;
    }

    public String getGoodId() { return goodId; }
    public void setGoodId(String goodId) { this.goodId = goodId; }
    public String getGoodName() { return goodName; }
    public void setGoodName(String goodName) { this.goodName = goodName; }
    public String getInspectDay() { return inspectDay; }
    public void setInspectDay(String inspectDay) { this.inspectDay = inspectDay; }
    public int getLowestPrice() { return lowestPrice; }
    public void setLowestPrice(int lowestPrice) { this.lowestPrice = lowestPrice; }
    public int getAveragePrice() { return averagePrice; }
    public void setAveragePrice(int averagePrice) { this.averagePrice = averagePrice; }
    public int getHighestPrice() { return highestPrice; }
    public void setHighestPrice(int highestPrice) { this.highestPrice = highestPrice; }
    public List<StorePrice> getStores() { return stores; }
    public void setStores(List<StorePrice> stores) { this.stores = stores; }

    public static class StorePrice {

        // [생필품 가격 비교] 판매점별 가격과 할인 여부를 화면에 표시한다.
        private String entpId;
        private String storeName;
        private String roadAddress;
        private int price;
        private String plusOneYn;
        private String discountYn;

        public StorePrice() {}

        public StorePrice(String entpId, String storeName, String roadAddress, int price, String plusOneYn, String discountYn) {
            this.entpId = entpId;
            this.storeName = storeName;
            this.roadAddress = roadAddress;
            this.price = price;
            this.plusOneYn = plusOneYn;
            this.discountYn = discountYn;
        }

        public String getEntpId() { return entpId; }
        public void setEntpId(String entpId) { this.entpId = entpId; }
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public String getRoadAddress() { return roadAddress; }
        public void setRoadAddress(String roadAddress) { this.roadAddress = roadAddress; }
        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }
        public String getPlusOneYn() { return plusOneYn; }
        public void setPlusOneYn(String plusOneYn) { this.plusOneYn = plusOneYn; }
        public String getDiscountYn() { return discountYn; }
        public void setDiscountYn(String discountYn) { this.discountYn = discountYn; }
    }
}
