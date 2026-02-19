package com.kcb.books.service;

import com.kcb.books.dto.BookRequestDto;
import com.kcb.books.dto.BookResponseDto;
import com.kcb.books.entity.Book;
import com.kcb.books.exception.BookNotFoundException;
import com.kcb.books.mapper.BookMapper;
import com.kcb.books.repository.BookRepository;
import com.kcb.masking.annotation.LogMasked;
import com.kcb.masking.service.ObjectMaskingService;
import com.kcb.masking.wrapper.MaskedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Book CRUD operations.
 *
 * <p><strong>Masking integration:</strong>
 * <ul>
 *   <li>Uses {@link MaskedObject} wrapper for lazy masked logging of DTOs.</li>
 *   <li>{@code @LogMasked} on methods enables AOP-intercepted masked parameter logging.</li>
 *   <li>Database values remain unmasked; only log output is masked.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;
    private final ObjectMaskingService objectMaskingService;

    /**
     * Creates a new book record.
     *
     * <p>Demonstrates masking: the log statement uses {@link MaskedObject} to ensure
     * sensitive fields (email, phoneNumber) appear masked, while the database stores
     * the full unmasked values.
     *
     * @param requestDto the book data
     * @return the saved book as a response DTO
     */
    @LogMasked
    @Transactional
    public BookResponseDto createBook(BookRequestDto requestDto) {
        // - Masked log – email/phoneNumber are masked here
        log.info("Creating book: {}", MaskedObject.of(requestDto, objectMaskingService));

        Book book = bookMapper.toEntity(requestDto);
        Book savedBook = bookRepository.save(book);

        // - Response DTO is logged with masked sensitive fields
        BookResponseDto response = bookMapper.toResponse(savedBook);
        log.info("Book created successfully with id={}: {}", savedBook.getId(),
                MaskedObject.of(response, objectMaskingService));
        return response;
    }

    /**
     * Retrieves all books.
     *
     * @return list of all books as response DTOs
     */
    public List<BookResponseDto> getAllBooks() {
        log.info("Fetching all books");
        List<Book> books = bookRepository.findAll();
        log.info("Found {} books", books.size());
        return books.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single book by its ID.
     *
     * @param id the book ID
     * @return the book as a response DTO
     * @throws BookNotFoundException if no book exists with the given ID
     */
    public BookResponseDto getBookById(Long id) {
        log.info("Fetching book with id={}", id);
        Book book = findBookOrThrow(id);
        BookResponseDto response = bookMapper.toResponse(book);
        log.info("Found book: {}", MaskedObject.of(response, objectMaskingService));
        return response;
    }

    /**
     * Updates an existing book.
     *
     * @param id         the ID of the book to update
     * @param requestDto the updated data
     * @return the updated book as a response DTO
     * @throws BookNotFoundException if no book exists with the given ID
     */
    @LogMasked
    @Transactional
    public BookResponseDto updateBook(Long id, BookRequestDto requestDto) {
        log.info("Updating book id={} with: {}", id, MaskedObject.of(requestDto, objectMaskingService));
        Book book = findBookOrThrow(id);
        bookMapper.updateEntity(requestDto, book);
        Book updated = bookRepository.save(book);
        BookResponseDto response = bookMapper.toResponse(updated);
        log.info("Book updated: {}", MaskedObject.of(response, objectMaskingService));
        return response;
    }

    /**
     * Deletes a book by its ID.
     *
     * @param id the ID of the book to delete
     * @throws BookNotFoundException if no book exists with the given ID
     */
    @Transactional
    public void deleteBook(Long id) {
        log.info("Deleting book with id={}", id);
        // Verify the book exists before deletion (throws BookNotFoundException if not found)
        findBookOrThrow(id);
        bookRepository.deleteById(id);
        log.info("Book with id={} deleted successfully", id);
    }

    private Book findBookOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
