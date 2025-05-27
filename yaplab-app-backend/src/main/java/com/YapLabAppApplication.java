package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the YapLab application.
 * This class serves as the entry point for the Spring Boot application.
 * It initializes the application context and starts the embedded server.
 */
@SpringBootApplication
public class YapLabAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(YapLabAppApplication.class, args);
	}

}
