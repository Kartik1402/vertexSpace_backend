package com.example.vertexSpace.service;
import com.example.vertexSpace.dto.AuthResponse;
import com.example.vertexSpace.dto.LoginRequest;
import com.example.vertexSpace.dto.RegisterRequest;
import com.example.vertexSpace.dto.UserProfileDTO;
import com.example.vertexSpace.exception.DuplicateResourceException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

/**
 * Service interface for authentication operations
 */
public interface AuthService {

    /**
     * Register a new user
     * @throws DuplicateResourceException if email already exists
     * @throws ResourceNotFoundException if department doesn't exist
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticate user and generate JWT token
     * @throws BadCredentialsException if credentials are invalid
     */
    AuthResponse login(LoginRequest request);

    /**
     * Get current authenticated user's profile
     * @throws ResourceNotFoundException if user not found
     */
    UserProfileDTO getCurrentUser(UUID userId);

    UUID getUserIdByEmail(String currentUserEmail);
}

