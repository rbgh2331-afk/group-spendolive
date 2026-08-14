package com.example.spendolive.publicdata.price.domain;

public class ConsumerStoreDTO {

    // 가격정보의 entpId와 판매점 정보를 연결할 때 사용
    private String entpId;
    private String entpName;
    private String roadAddrBasic;
    private String xMapCoord;
    private String yMapCoord;

    public ConsumerStoreDTO() {}

    public ConsumerStoreDTO(String entpId, String entpName, String roadAddrBasic, String xMapCoord, String yMapCoord) {
        this.entpId = entpId;
        this.entpName = entpName;
        this.roadAddrBasic = roadAddrBasic;
        this.xMapCoord = xMapCoord;
        this.yMapCoord = yMapCoord;
    }

    public String getEntpId() { return entpId; }
    public void setEntpId(String entpId) { this.entpId = entpId; }
    public String getEntpName() { return entpName; }
    public void setEntpName(String entpName) { this.entpName = entpName; }
    public String getRoadAddrBasic() { return roadAddrBasic; }
    public void setRoadAddrBasic(String roadAddrBasic) { this.roadAddrBasic = roadAddrBasic; }
    public String getXMapCoord() { return xMapCoord; }
    public void setXMapCoord(String xMapCoord) { this.xMapCoord = xMapCoord; }
    public String getYMapCoord() { return yMapCoord; }
    public void setYMapCoord(String yMapCoord) { this.yMapCoord = yMapCoord; }
}
