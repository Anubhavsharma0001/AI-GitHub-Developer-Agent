package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.ApiResponse;
import com.anubhav.Backend.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final HelloService helloService;

    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/api/hello")
    public ApiResponse hello() {

        return new ApiResponse(
                "AI GitHub Developer Agent",
                "ONLINE",
                helloService.getMessage()
        );
    }
}