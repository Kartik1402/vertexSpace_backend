package com.example.vertexSpace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new building
 * SYSTEM_ADMIN only
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBuildingRequest {

    @NotBlank(message = "Building name is required")
    @Size(max = 100, message = "Building name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;
}
