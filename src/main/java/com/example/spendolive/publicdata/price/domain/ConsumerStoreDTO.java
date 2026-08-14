package com.example.spendolive.publicdata.price.domain;

public class ConsumerStoreDTO {

    // 가격정보의 entpId와 판매점 정보를 연결할 때 사용
    private String entpId;
    private String entpName;
    private String roadAddrBasic;

    public ConsumerStoreDTO() {}

    public ConsumerStoreDTO(String entpId, String entpName, String roadAddrBasic) {
        this.entpId = entpId;
        this.entpName = entpName;
        this.roadAddrBasic = roadAddrBasic;
    }

    public String getEntpId() { return entpId; }
    public void setEntpId(String entpId) { this.entpId = entpId; }
    public String getEntpName() { return entpName; }
    public void setEntpName(String entpName) { this.entpName = entpName; }
    public String getRoadAddrBasic() { return roadAddrBasic; }
    public void setRoadAddrBasic(String roadAddrBasic) { this.roadAddrBasic = roadAddrBasic; }
}
