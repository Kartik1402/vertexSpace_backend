package com.example.vertexSpace.controller;
import com.example.vertexSpace.dto.AuthResponse;
import com.example.vertexSpace.dto.LoginRequest;
import com.example.vertexSpace.dto.RegisterRequest;
import com.example.vertexSpace.dto.UserProfileDTO;
import com.example.vertexSpace.security.UserPrincipal;
import com.example.vertexSpace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints
 * Milestone 1: Auth APIs
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/auth/me
     * Get current authenticated user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserProfileDTO profile = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(profile);
    }
}

