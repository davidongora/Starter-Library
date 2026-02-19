package com.kcb.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Book requests (create/update).
 *
 * <p>Note: {@code email} and {@code phoneNumber} are sensitive fields.
 * When this DTO is passed to the logger via the masking starter,
 * those fields will be automatically masked.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Book request payload")
public class BookRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Book title", example = "Clean Code")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    @Schema(description = "Author name", example = "Robert C. Martin")
    private String author;

    @Email(message = "Email must be valid")
    @Schema(description = "Contact email (sensitive – masked in logs)", example = "author@example.com")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Phone number must be valid")
    @Schema(description = "Contact phone number (sensitive – masked in logs)", example = "0712345678")
    private String phoneNumber;

    @Schema(description = "Publisher name", example = "Prentice Hall")
    private String publisher;
}
