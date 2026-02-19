package com.kcb.books;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Bank Books API Demo application.
 *
 * <p>This application demonstrates the use of the {@code bank-masking-spring-boot-starter}
 * for automatic sensitive data masking in logs.
 *
 * <p>Access the API documentation at: http://localhost:8080/swagger-ui.html
 */
@SpringBootApplication
public class BankBooksApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankBooksApiApplication.class, args);
    }
}
