package com.kcb.books.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the Bank Books API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI booksApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Books API")
                        .description("""
                                CRUD REST API for managing books.
                                
                                **Security Note:** Sensitive fields (email, phoneNumber) are automatically
                                masked in server logs using the `bank-masking-spring-boot-starter`.
                                Database values remain unmasked.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("David Ongora")
                                .email("ongoradavid5@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
