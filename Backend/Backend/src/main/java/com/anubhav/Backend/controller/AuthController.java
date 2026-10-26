package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.LoginRequest;
import com.anubhav.Backend.dto.LoginResponse;
import com.anubhav.Backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}