package com.retail.userservice.controller;

import com.retail.userservice.dto.AuthResponse;
import com.retail.userservice.dto.LoginRequest;
import com.retail.userservice.dto.RegisterRequest;
import com.retail.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints.
 *
 * These endpoints are PUBLIC (no JWT required) -- configured in SecurityConfig.
 *
 * @RestController = @Controller + @ResponseBody
 *   - @Controller: This class handles HTTP requests
 *   - @ResponseBody: Return values are serialized to JSON (not rendered as views)
 *
 * @RequestMapping("/api/auth"): All endpoints in this controller start with /api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/auth/register
     *
     * Creates a new user account and returns a JWT token.
     *
     * @Valid triggers validation of the RegisterRequest annotations
     * (@NotBlank, @Email, @Size). If validation fails, Spring automatically
     * returns a 400 Bad Request with error details.
     */
    @PostMapping(path = "/register", version = "1")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     *
     * Authenticates a user and returns a JWT token.
     */
    @PostMapping(path = "/login", version = "1")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
