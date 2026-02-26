package com.example.vertexSpace.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBuildingRequest {

    @Size(max = 100, message = "Building name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @Size(max = 20, message = "ZIP code must not exceed 20 characters")
    private String zipCode;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    private Boolean isActive;
}
