package com.example.mcp_service;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;

@SpringBootApplication
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> securityCustomizer(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return http -> http
                .with(mcpServerOAuth2(), a -> a.authorizationServer(issuerUri).resourcePath("/mcp"));
    }
}

record DogAdoptionAppointment(
        Instant when, String dogName, String clientName
) {
}

@Service
class DogAdoptionScheduler {

    @McpTool(description = """
            schedule an appointment to pick up or adopt 
            a dog from a Pooch Palace location
            """)
    DogAdoptionAppointment schedule(@McpToolParam(description = "the name of the dog") String dogName) {
        var user = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        var when = Instant
                .now()
                .plus(3, ChronoUnit.DAYS);
        var dogAdoptionAppointment = new DogAdoptionAppointment(when, dogName, user);
        IO.println(dogAdoptionAppointment);
        return dogAdoptionAppointment;
    }
}