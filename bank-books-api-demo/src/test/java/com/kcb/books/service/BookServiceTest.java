package com.kcb.books.service;

import com.kcb.books.dto.BookRequestDto;
import com.kcb.books.dto.BookResponseDto;
import com.kcb.books.entity.Book;
import com.kcb.books.exception.BookNotFoundException;
import com.kcb.books.mapper.BookMapper;
import com.kcb.books.repository.BookRepository;
import com.kcb.masking.annotation.MaskStyle;
import com.kcb.masking.config.MaskingProperties;
import com.kcb.masking.service.MaskingService;
import com.kcb.masking.service.ObjectMaskingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ObjectMaskingService objectMaskingService;

    @InjectMocks
    private BookService bookService;

    private BookRequestDto requestDto;
    private Book savedBook;
    private BookResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = BookRequestDto.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .email("robert@example.com")
                .phoneNumber("0712345678")
                .publisher("Prentice Hall")
                .build();

        savedBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .email("robert@example.com")
                .phoneNumber("0712345678")
                .publisher("Prentice Hall")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        responseDto = BookResponseDto.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .email("robert@example.com")
                .phoneNumber("0712345678")
                .publisher("Prentice Hall")
                .build();
    }

    @Test
    @DisplayName("createBook: should save and return response DTO")
    void createBook_shouldSaveAndReturn() {
        when(bookMapper.toEntity(requestDto)).thenReturn(savedBook);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(bookMapper.toResponse(savedBook)).thenReturn(responseDto);
        when(objectMaskingService.toMaskedString(any())).thenReturn("{}");

        BookResponseDto result = bookService.createBook(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("getAllBooks: should return list of books")
    void getAllBooks_shouldReturnList() {
        when(bookRepository.findAll()).thenReturn(List.of(savedBook));
        when(bookMapper.toResponse(savedBook)).thenReturn(responseDto);

        List<BookResponseDto> result = bookService.getAllBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("getBookById: should return book when found")
    void getBookById_shouldReturnBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedBook));
        when(bookMapper.toResponse(savedBook)).thenReturn(responseDto);
        when(objectMaskingService.toMaskedString(any())).thenReturn("{}");

        BookResponseDto result = bookService.getBookById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getBookById: should throw BookNotFoundException when not found")
    void getBookById_shouldThrow_whenNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(999L))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("updateBook: should update existing book")
    void updateBook_shouldUpdateBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedBook));
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(responseDto);
        when(objectMaskingService.toMaskedString(any())).thenReturn("{}");
        doNothing().when(bookMapper).updateEntity(any(BookRequestDto.class), any(Book.class));

        BookResponseDto result = bookService.updateBook(1L, requestDto);

        assertThat(result).isNotNull();
        verify(bookMapper).updateEntity(requestDto, savedBook);
        verify(bookRepository).save(savedBook);
    }

    @Test
    @DisplayName("updateBook: should throw BookNotFoundException when not found")
    void updateBook_shouldThrow_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(99L, requestDto))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("deleteBook: should delete existing book")
    void deleteBook_shouldDelete() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedBook));
        doNothing().when(bookRepository).deleteById(1L);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBook: should throw BookNotFoundException when not found")
    void deleteBook_shouldThrow_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(BookNotFoundException.class);
    }
}
