package dev.toqash.travelplannerbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Metadata for the generated OpenAPI document (served at /v3/api-docs, rendered at /swagger-ui.html).
@Configuration
public class OpenApiConfig {
    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI travelPlannerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Planner API")
                        .version("v1")
                        .description("Plan trips and generate day-by-day itineraries with AI. "
                                + "Errors use RFC 9457 problem details with a stable 'code' field."))
                // Every endpoint needs HTTP Basic unless it says otherwise; this enables Swagger UI's Authorize button.
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}