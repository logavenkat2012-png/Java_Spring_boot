package com.example.rentvideo.controller;

import com.example.rentvideo.dto.RegisterRequest;
import com.example.rentvideo.dto.UserResponse;
import com.example.rentvideo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Public: anyone can register. */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.register(request)));
    }

    /**
     * Login with Basic Auth (Authorization: Basic base64(email:password)).
     * Returns the profile of the authenticated user; invalid credentials give 401.
     */
    @PostMapping("/login")
    public UserResponse login(Authentication authentication) {
        return UserResponse.from(userService.getByEmail(authentication.getName()));
    }
}
