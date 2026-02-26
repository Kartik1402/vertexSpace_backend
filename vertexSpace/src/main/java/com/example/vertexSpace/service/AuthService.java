package com.example.vertexSpace.service;
import com.example.vertexSpace.dto.AuthResponse;
import com.example.vertexSpace.dto.LoginRequest;
import com.example.vertexSpace.dto.RegisterRequest;
import com.example.vertexSpace.dto.UserProfileDTO;
import com.example.vertexSpace.exception.DuplicateResourceException;
import com.example.vertexSpace.exception.ResourceNotFoundException;

import java.util.UUID;
public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileDTO getCurrentUser(UUID userId);
    UUID getUserIdByEmail(String currentUserEmail);
}

