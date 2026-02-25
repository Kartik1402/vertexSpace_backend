package com.example.vertexSpace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields in JSON
public class ErrorResponse {

    private final Instant timestamp;         // e.g., Instant.now()
    private final int status;                // HTTP status code (e.g., 400)
    private final String error;              // short label (e.g., "Validation Failed")
    private final String message;            // human-readable message
    private final Map<String, String> fieldErrors; // only for @Valid errors (nullable)
}
