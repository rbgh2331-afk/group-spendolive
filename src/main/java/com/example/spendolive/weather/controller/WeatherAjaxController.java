package com.example.spendolive.weather.controller;

import com.example.spendolive.weather.service.WeatherService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 헤더 날씨 위젯(weather.js)이 호출하는 기상청 단기예보 조회 API.
 * - 응답을 DTO가 아니라 JsonObject 리스트 → JsonArray 문자열로 직접 조립해서 반환함
 *   (별도 응답 DTO 클래스 없이 WeatherService가 만든 JsonObject를 그대로 씀).
 */
@RestController
public class WeatherAjaxController {

    private final WeatherService weatherService;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 WeatherService 구현체를 자동으로 넣어줌
    public WeatherAjaxController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 지역 선택: nx, ny 파라미터로 조회
     * 내 위치 사용: lat, lon 파라미터로 조회
     * 둘 다 오면 nx/ny를 우선한다.
     *
     * nx/ny도 lat/lon도 하나도 안 오면 IllegalArgumentException을 던짐 - 이걸 잡아서
     * 400 등으로 응답 변환해주는 @ExceptionHandler가 따로 없어 보이니, 프론트(weather.js)가
     * 이 파라미터들을 빠뜨리지 않고 항상 보내는지 확인 필요 (안 그러면 500 에러로 나감)
     */
    @GetMapping(value = "/ajax/weather.do", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getWeather(@RequestParam(required = false) Integer nx,
                              @RequestParam(required = false) Integer ny,
                              @RequestParam(required = false) Double lat,
                              @RequestParam(required = false) Double lon) {

        List<JsonObject> items;

        if (nx != null && ny != null) {
            items = weatherService.getForecastByGrid(nx, ny);
        } else if (lat != null && lon != null) {
            items = weatherService.getForecastByLatLon(lat, lon);
        } else {
            throw new IllegalArgumentException("nx/ny 또는 lat/lon 파라미터가 필요합니다.");
        }

        // JsonObject 리스트를 JsonArray로 옮겨 담아서 문자열로 직렬화.
        // produces가 APPLICATION_JSON이라 반환 타입은 String이어도 브라우저는 JSON으로 받음
        JsonArray array = new JsonArray();
        items.forEach(array::add);
        return array.toString();
    }
}