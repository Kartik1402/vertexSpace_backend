package com.example.vertexSpace.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;  // Milliseconds
    private UserProfileDTO user;

    public AuthResponse(String accessToken, Long expiresIn, UserProfileDTO user) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
