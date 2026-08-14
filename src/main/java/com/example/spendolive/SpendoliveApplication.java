package com.example.spendolive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpendOlive 애플리케이션 실행 진입점
 */
@EnableScheduling
@SpringBootApplication
public class SpendoliveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpendoliveApplication.class, args);
    }

}
