package com.example.vertexSpace.dto;

import com.example.vertexSpace.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private UUID id;
    private String email;
    private String displayName;
    private Role role;
    private UUID departmentId;
    private String departmentName;
    private Boolean isActive;
}

