package com.kcb.books.controller;

import com.kcb.books.dto.BookRequestDto;
import com.kcb.books.dto.BookResponseDto;
import com.kcb.books.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Book CRUD operations.
 *
 * <p>Base path: {@code /books}
 *
 * <p>Note: Masking is handled at the service layer, not here.
 * The controller is responsible only for HTTP semantics — routing,
 * status codes, and request/response mapping.
 *
 * <p><strong>Design decision:</strong> Keeping the controller thin and delegating
 * all business logic to the service layer is a SOLID principle (SRP). This also
 * ensures masking is applied even if the service is called from non-HTTP contexts.
 */
@Slf4j
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "CRUD operations for Books")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "Create a new book",
            description = "Creates a new book entry. Sensitive fields (email, phoneNumber) are masked in logs.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book created successfully",
                    content = @Content(schema = @Schema(implementation = BookResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookRequestDto request) {
        log.debug("POST /books received");
        BookResponseDto response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all books")
    @ApiResponse(responseCode = "200", description = "List of all books")
    @GetMapping
    public ResponseEntity<List<BookResponseDto>> getAllBooks() {
        log.debug("GET /books received");
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @Operation(summary = "Get a book by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(
            @Parameter(description = "Book ID") @PathVariable("id") Long id) {
        log.debug("GET /books/{} received", id);
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @Operation(summary = "Update a book by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDto> updateBook(
            @Parameter(description = "Book ID") @PathVariable("id") Long id,
            @Valid @RequestBody BookRequestDto request) {
        log.debug("PUT /books/{} received", id);
        return ResponseEntity.ok(bookService.updateBook(id, request));
    }

    @Operation(summary = "Delete a book by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(
            @Parameter(description = "Book ID") @PathVariable("id") Long id) {
        log.debug("DELETE /books/{} received", id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
