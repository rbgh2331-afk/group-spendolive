package com.example.spendolive.weather.util;

/**
 * 위도/경도(lat, lon) → 기상청 격자 좌표(nx, ny) 변환기.
 *
 * 기상청 단기예보 API는 위경도가 아니라 자체 격자 좌표(nx, ny)로 지역을 지정해야 해서,
 * 프론트에서 브라우저 GPS로 받은 위경도를 이 좌표로 바꿔줘야 함(weather.js "내 위치 사용" 기능용).
 *
 * 아래 변환 공식은 직접 짠 로직이 아니라 기상청이 공식 배포하는 격자 변환 알고리즘
 * (람베르트 정각원추도법, Lambert Conformal Conic Projection)을 그대로 옮긴 것.
 * 상수값(RE/GRID/SLAT1/SLAT2/OLON/OLAT/XO/YO)도 기상청이 정해준 고정값이라 임의로 바꾸면 안 됨.
 */
public class WeatherGridConverter {

    private static final double RE = 6371.00877;   // 지구 반경(km)
    private static final double GRID = 5.0;         // 격자 간격(km) - 기상청 격자 한 칸의 실제 거리
    private static final double SLAT1 = 30.0;       // 투영 표준위도 1
    private static final double SLAT2 = 60.0;       // 투영 표준위도 2
    private static final double OLON = 126.0;       // 기준점(원점) 경도
    private static final double OLAT = 38.0;        // 기준점(원점) 위도
    private static final double XO = 43;            // 기준점의 X좌표(GRID 단위)
    private static final double YO = 136;           // 기준점의 Y좌표(GRID 단위)

    // 변환 결과를 담는 단순 값 객체(불변) - nx/ny가 기상청 API에 그대로 넘길 격자 좌표
    public static class GridXY {
        public final int nx;
        public final int ny;
        public GridXY(int nx, int ny) { this.nx = nx; this.ny = ny; }
    }

    /**
     * 위도(lat)/경도(lon)를 기상청 격자 좌표(nx, ny)로 변환.
     * 기상청이 배포한 변환 공식을 그대로 구현한 것이라, 중간 변수명(sn/sf/ro/ra/theta 등)도
     * 원본 공식의 수학 기호를 그대로 따름 - 값 하나하나의 의미보다는
     * "표준 공식을 그대로 옮겼다"는 것만 알면 됨(임의로 수정하면 좌표가 틀어짐).
     */
    public static GridXY convertToGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;  // 도(degree) → 라디안 변환 계수

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        // 원추 투영의 축척계수(sn), 인자(sf), 기준점까지의 극좌표 반경(ro) 계산
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        // 입력받은 lat/lon을 같은 투영법으로 극좌표 반경(ra)·각도(theta)로 변환
        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;      // 경도차가 ±180도를 넘으면 보정
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        // 극좌표(ra, theta)를 최종 격자 좌표(nx, ny)로 변환, 기준점(XO, YO)만큼 평행이동
        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new GridXY(nx, ny);
    }
}