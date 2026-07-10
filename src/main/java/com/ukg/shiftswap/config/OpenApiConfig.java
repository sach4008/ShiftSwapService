package com.ukg.shiftswap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String IDENTITY_SCHEME = "X-User-Id";

    @Bean
    public OpenAPI shiftSwapOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shift Swap Service")
                        .description("Request, approve, reject, and cancel shift swaps between employees.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(IDENTITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(IDENTITY_SCHEME)
                        .description("Local identity stand-in — the numeric id of the calling employee")))
                .addSecurityItem(new SecurityRequirement().addList(IDENTITY_SCHEME));
    }
}
