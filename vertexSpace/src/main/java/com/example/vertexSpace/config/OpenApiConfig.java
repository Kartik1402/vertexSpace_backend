package com.example.vertexSpace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:VertexSpace Booking System}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // ============================================================
                // API INFORMATION
                // ============================================================
                .info(new Info()
                        .title("VertexSpace Booking System API")
                        .version("3.0.0")  // Updated for Milestone 3
                        .description(""" 
                                """)
                        .contact(new Contact()
                                .name("VertexSpace Development Team")
                                .email("support@vertexspace.com")
                                .url("https://vertexspace.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )

                // ============================================================
                // SERVER URLS
                // ============================================================
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api-dev.vertexspace.com")
                                .description("Development Server (if deployed)"),
                        new Server()
                                .url("https://api.vertexspace.com")
                                .description("Production Server (if applicable)")
                ))

                // ============================================================
                // SECURITY CONFIGURATION
                // ============================================================
                .components(new Components()
                        .addSecuritySchemes("bearer-auth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("""
                                    JWT Authentication - Include your token in all authenticated requests.
                                    
                                    **How to authorize in Swagger:**
                                    
                                    1. Login via `POST /api/v1/auth/login`
                                    2. Copy the `token` from response (starts with `eyJ...`)
                                    3. Click the **"Authorize"** button (🔓 padlock icon) above
                                    4. Paste your token in the "Value" field
                                    5. Click "Authorize" then "Close"
                                    
                                    ⚠️ **Important**: Paste ONLY the token value, not "Bearer <token>"
                                    
                                    **Example token:**
                                    ```
                                    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJlbXBsb3llZUBkZWxvaXR0ZS5jb20iLCJpYXQiOjE3MDg1...
                                    ```
                                    
                                    **After authorization, all requests will include:**
                                    ```
                                    Authorization: Bearer <your-token>
                                    ```
                                    """)
                        )
                )

                .addSecurityItem(new SecurityRequirement().addList("bearer-auth"));
    }

}
