package com.example.spendolive.common.error;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class ErrorCustomController {

    @GetMapping("/400")
    public String error400() {
        return "error/400"; // /WEB-INF/views/error/400.jsp 로 이동
    }

    @GetMapping("/403")
    public String error403() {
        return "error/403"; // /WEB-INF/views/error/403.jsp 로 이동
    }

    @GetMapping("/404")
    public String error404() {
        return "error/404"; // /WEB-INF/views/error/404.jsp 로 이동
    }

    @GetMapping("/500")
    public String error500() {
        return "error/500"; // /WEB-INF/views/error/500.jsp 로 이동
    }
}
