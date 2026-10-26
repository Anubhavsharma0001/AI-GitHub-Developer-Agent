package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.UserResponse;
import com.anubhav.Backend.entity.User;
import com.anubhav.Backend.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}