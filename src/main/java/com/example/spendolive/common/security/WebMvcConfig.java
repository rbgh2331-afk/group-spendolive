package com.example.spendolive.common.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginCheckInterceptor loginCheckInterceptor;

    public WebMvcConfig(LoginCheckInterceptor loginCheckInterceptor) {
        this.loginCheckInterceptor = loginCheckInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns(
                        "/spendolive/mypage.do",
                        "/spendolive/mypage/**",
                        "/spendolive/expense/**",
                        "/spendolive/ott/friends.do",
                        "/spendolive/ott/recruit.do",
                        "/spendolive/ott/ajax/**",
                        "/spendolive/ott/chat/**",
                        "/spendolive/ott/settlement/**",
                        "/spendolive/ott/room/**",
                        "/report/**",
                        "/payment/**")
                .excludePathPatterns("/payment/fail.do");
    }
}
