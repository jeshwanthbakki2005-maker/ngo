package com.ngo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth Service routes
                .route("auth-signup", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))
                
                // NFSA Service routes
                .route("nfsa-service", r -> r.path("/api/nfsa/**")
                        .uri("lb://nfsa-service"))
                
                // Donor Service routes
                .route("donor-service", r -> r.path("/api/donor/**")
                        .uri("lb://donor-service"))
                
                // Beneficiary Service routes
                .route("beneficiary-service", r -> r.path("/api/beneficiary/**")
                        .uri("lb://beneficiary-service"))
                
                // Donation Service routes
                .route("donation-service", r -> r.path("/api/donation/**")
                        .uri("lb://donation-service"))
                
                // Allocation Service routes
                .route("allocation-service", r -> r.path("/api/allocation/**")
                        .uri("lb://allocation-service"))
                
                // Report Service routes
                .route("report-service", r -> r.path("/api/report/**")
                        .uri("lb://report-service"))
                
                // Audit Service routes
                .route("audit-service", r -> r.path("/api/audit/**")
                        .uri("lb://audit-service"))
                
                .build();
    }
}
