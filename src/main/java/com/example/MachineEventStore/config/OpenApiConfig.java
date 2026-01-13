package com.example.MachineEventStore.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Machine Event Store API")
                        .version("1.0.0")
                        .description(
                                "Backend system for monitoring factory machines and processing events. " +
                                        "Handles batch event ingestion, deduplication, updates, and provides statistics " +
                                        "on machine health and defect rates."
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://machineeventstore-production.up.railway.app")
                                .description("Production Server")
                ));
    }
}
