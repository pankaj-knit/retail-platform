package com.retail.userservice.controller;

import com.retail.userservice.dto.UserProfileResponse;
import com.retail.userservice.dto.UserProfileResponseV2;
import com.retail.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for user profile endpoints.
 *
 * These endpoints REQUIRE authentication (JWT token in Authorization header).
 * Spring Security enforces this based on the rules in SecurityConfig.
 *
 * The Authentication object is automatically injected by Spring Security.
 * It contains the user's email (set by our JwtAuthenticationFilter).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/me
     *
     * Returns the profile of the currently authenticated user.
     * The email is extracted from the JWT token (via SecurityContext).
     */
    @GetMapping(path = "/me", version = "1")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse profile = userService.getProfile(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping(path = "/me", version = "2")
    public ResponseEntity<UserProfileResponseV2> getMyProfileV2(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponseV2 profile = userService.getProfileV2(email);
        return ResponseEntity.ok(profile);
    }
}
