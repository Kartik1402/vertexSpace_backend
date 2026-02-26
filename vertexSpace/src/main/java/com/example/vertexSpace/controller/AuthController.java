package com.example.vertexSpace.controller;
import com.example.vertexSpace.dto.AuthResponse;
import com.example.vertexSpace.dto.LoginRequest;
import com.example.vertexSpace.dto.RegisterRequest;
import com.example.vertexSpace.dto.UserProfileDTO;
import com.example.vertexSpace.dto.response.LogoutResponse;
import com.example.vertexSpace.security.UserPrincipal;
import com.example.vertexSpace.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.hibernate.query.sqm.tree.SqmNode.log;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserProfileDTO profile = authService.getCurrentUser(userPrincipal.getId());
        return ResponseEntity.ok(profile);
    }
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(Authentication authentication, HttpServletRequest request) {
        // Stateless JWT: server can't invalidate the token without a revocation store.
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal up) {
            // Optional: log logout event here if you want
             log.info("Logout: userId={}, email={}");
        }
        return ResponseEntity.ok(new LogoutResponse("Logged out. Please delete token on client."));
    }
}


