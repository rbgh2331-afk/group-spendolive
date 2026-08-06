package com.example.spendolive.weather.service;

import com.example.spendolive.weather.util.GridConverter;
import com.example.spendolive.weather.util.WeatherTimeUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String serviceKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    /**
     * 위경도 기반 조회 (Geolocation으로 받은 좌표를 격자로 변환해서 조회)
     */
    public List<JsonObject> getForecastByLatLon(double lat, double lon) {
        GridConverter.GridXY grid = GridConverter.convertToGrid(lat, lon);
        return getForecastByGrid(grid.nx, grid.ny);
    }

    /**
     * 격자좌표(nx, ny) 직접 조회 (지역 선택 드롭다운에서 사용)
     */
    public List<JsonObject> getForecastByGrid(int nx, int ny) {
        String[] baseDateTime = WeatherTimeUtil.getBaseDateTime();

        URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDateTime[0])
                .queryParam("base_time", baseDateTime[1])
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .build(true)
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(uri, String.class);

        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();

            JsonObject header = root.getAsJsonObject("response").getAsJsonObject("header");
            String resultCode = header.get("resultCode").getAsString();
            if (!"00".equals(resultCode)) {
                String msg = header.get("resultMsg").getAsString();
                throw new RuntimeException("기상청 API 오류: " + resultCode + " - " + msg);
            }

            JsonArray items = root.getAsJsonObject("response")
                    .getAsJsonObject("body")
                    .getAsJsonObject("items")
                    .getAsJsonArray("item");

            List<JsonObject> result = new ArrayList<>();
            items.forEach(el -> result.add(el.getAsJsonObject()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("날씨 응답 파싱 실패", e);
        }
    }
}
