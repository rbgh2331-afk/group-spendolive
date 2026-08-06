package com.example.spendolive.weather.controller;

import com.example.spendolive.weather.service.WeatherService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class WeatherAjaxController {

    private final WeatherService weatherService;

    public WeatherAjaxController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 지역 선택: nx, ny 파라미터로 조회
     * 내 위치 사용: lat, lon 파라미터로 조회
     * 둘 다 오면 nx/ny를 우선한다.
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

        JsonArray array = new JsonArray();
        items.forEach(array::add);
        return array.toString();
    }
}
