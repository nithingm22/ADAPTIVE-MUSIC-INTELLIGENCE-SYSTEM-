package com.amis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AMIS - Adaptive Music Intelligence System
 * Main entry point for the Spring Boot application.
 */
@SpringBootApplication
public class AmisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmisApplication.class, args);
        System.out.println("\n====================================================");
        System.out.println(" AMIS - Adaptive Music Intelligence System STARTED");
        System.out.println(" API running at: http://localhost:8080");
        System.out.println("====================================================\n");
    }
}
