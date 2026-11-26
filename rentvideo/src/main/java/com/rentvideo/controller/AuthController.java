package com.rentvideo.controller;

import com.rentvideo.dto.LoginResponse;
import com.rentvideo.dto.RegisterRequest;
import com.rentvideo.dto.UserResponse;
import com.rentvideo.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** Basic Auth in, JWT out. */
    @PostMapping("/login")
    public LoginResponse login(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authService.login(authorization);
    }
}
