package com.anubhav.Backend.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public String getMessage() {
        return "Backend is running successfully";
    }
}