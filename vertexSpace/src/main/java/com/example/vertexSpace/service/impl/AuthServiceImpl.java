package com.example.vertexSpace.service.impl;
import com.example.vertexSpace.dto.AuthResponse;
import com.example.vertexSpace.dto.LoginRequest;
import com.example.vertexSpace.dto.RegisterRequest;
import com.example.vertexSpace.dto.UserProfileDTO;
import com.example.vertexSpace.entity.Department;
import com.example.vertexSpace.entity.User;
import com.example.vertexSpace.enums.Role;
import com.example.vertexSpace.exception.DuplicateResourceException;
import com.example.vertexSpace.exception.ResourceNotFoundException;
import com.example.vertexSpace.repository.DepartmentRepository;
import com.example.vertexSpace.repository.UserRepository;
import com.example.vertexSpace.security.JwtTokenProvider;
import com.example.vertexSpace.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of authentication service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Validation 1: Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Validation 2: Check if department exists
        Department department = departmentRepository.findByCode(request.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getCode()));

        // Create new user entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setRole(Role.USER);  // Default role (cannot self-register as admin)
        user.setDepartment(department);
        user.setIsActive(true);

        // Save to database
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        // Auto-login: Generate JWT token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        // Update last login
        userRepository.updateLastLogin(savedUser.getId(), Instant.now());

        // Build response
        UserProfileDTO userProfile = mapToUserProfileDTO(savedUser);

        return new AuthResponse(
                token,
                jwtTokenProvider.getJwtExpiration(),
                userProfile
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Authenticate user (throws BadCredentialsException if invalid)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(authentication);

        // Get user details
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Update last login timestamp
        userRepository.updateLastLogin(user.getId(), Instant.now());

        log.info("User logged in successfully: {}", user.getId());

        // Build response
        UserProfileDTO userProfile = mapToUserProfileDTO(user);

        return new AuthResponse(
                token,
                jwtTokenProvider.getJwtExpiration(),
                userProfile
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return mapToUserProfileDTO(user);
    }

    public UUID getUserIdByEmail(String email) {
        return userRepository.findIdByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found for email: " + email));
    }
    /**
     * Helper: Map User entity to UserProfileDTO
     */
    private UserProfileDTO mapToUserProfileDTO(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getDepartment().getId(),
                user.getDepartment().getName(),
                user.getIsActive()
        );
    }
}

