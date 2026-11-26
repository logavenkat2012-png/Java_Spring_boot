package com.rentvideo.service;

import com.rentvideo.dto.LoginResponse;
import com.rentvideo.dto.RegisterRequest;
import com.rentvideo.dto.UserResponse;
import com.rentvideo.entity.Role;
import com.rentvideo.entity.User;
import com.rentvideo.exception.ConflictException;
import com.rentvideo.repository.UserRepository;
import com.rentvideo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setRole(req.role() == null ? Role.CUSTOMER : req.role());
        return UserResponse.from(userRepository.save(user));
    }

    /** Verifies "Authorization: Basic base64(email:password)" and issues a JWT for subsequent calls. */
    public LoginResponse login(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            throw new BadCredentialsException("Basic authentication header required");
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(authorizationHeader.substring(6).trim()),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Malformed Basic authentication header");
        }
        int idx = decoded.indexOf(':');
        if (idx < 0) {
            throw new BadCredentialsException("Malformed Basic authentication header");
        }
        String email = decoded.substring(0, idx).trim().toLowerCase();
        String password = decoded.substring(idx + 1);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        UserDetails principal = (UserDetails) auth.getPrincipal();
        String role = principal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        return new LoginResponse(jwtService.generateToken(principal), "Bearer",
                jwtService.getExpirationSeconds(), role);
    }
}
