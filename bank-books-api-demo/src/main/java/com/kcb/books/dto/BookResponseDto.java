package com.kcb.books.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Book responses.
 *
 * <p>This DTO represents the response payload returned to API clients.
 * Values here are unmasked (as stored in the database).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Book response payload")
public class BookResponseDto {

    @Schema(description = "Unique book identifier", example = "1")
    private Long id;

    @Schema(description = "Book title", example = "Clean Code")
    private String title;

    @Schema(description = "Author name", example = "Robert C. Martin")
    private String author;

    @Schema(description = "Contact email", example = "author@example.com")
    private String email;

    @Schema(description = "Contact phone number", example = "0712345678")
    private String phoneNumber;

    @Schema(description = "Publisher name", example = "Prentice Hall")
    private String publisher;

    @Schema(description = "Record creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Record last updated timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
