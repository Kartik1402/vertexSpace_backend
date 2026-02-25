package com.example.vertexSpace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ⭐ ADD THIS
public class VertexSpaceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VertexSpaceApplication.class, args);
	}

}
