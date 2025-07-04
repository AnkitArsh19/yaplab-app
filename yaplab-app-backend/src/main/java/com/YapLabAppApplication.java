package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the YapLab application.
 * This class serves as the entry point for the Spring Boot application.
 * It initializes the application context and starts the embedded server.
 * Enable scheduling enables the use of scheduled tasks in the application.
 */
@SpringBootApplication
@EnableScheduling
public class YapLabAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(YapLabAppApplication.class, args);
	}

}
