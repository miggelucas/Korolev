package br.ufpe.cin.taes2.korolev_engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI korolevOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Korolev Engine API")
                        .description("Runtime variability control engine for Software Product Lines. "
                                + "Validates feature flag configurations against SPL constraints "
                                + "(hierarchy, mandatory, cross-tree requires, mutual exclusion) "
                                + "before persisting changes.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact()
                                .name("UFPE — CIn — TAES2")
                                .url("https://portal.cin.ufpe.br"))
                );
    }
}
