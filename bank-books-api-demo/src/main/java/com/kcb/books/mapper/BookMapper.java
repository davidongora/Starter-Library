package com.kcb.books.mapper;

import com.kcb.books.dto.BookRequestDto;
import com.kcb.books.dto.BookResponseDto;
import com.kcb.books.entity.Book;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between {@link Book} entity and DTOs.
 *
 * <p>Using a manual mapper (rather than MapStruct) to keep dependencies minimal
 * while still maintaining a clean separation between entity and DTO layers.
 */
@Component
public class BookMapper {

    /**
     * Maps a {@link BookRequestDto} to a new {@link Book} entity.
     * Does not set the ID (auto-generated).
     *
     * @param dto the request DTO
     * @return a new Book entity
     */
    public Book toEntity(BookRequestDto dto) {
        return Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .publisher(dto.getPublisher())
                .build();
    }

    /**
     * Applies the fields from a {@link BookRequestDto} to an existing {@link Book} entity.
     * Used during update operations to preserve the entity's ID and timestamps.
     *
     * @param dto    the update payload
     * @param entity the existing entity to update
     */
    public void updateEntity(BookRequestDto dto, Book entity) {
        entity.setTitle(dto.getTitle());
        entity.setAuthor(dto.getAuthor());
        entity.setEmail(dto.getEmail());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setPublisher(dto.getPublisher());
    }

    /**
     * Maps a {@link Book} entity to a {@link BookResponseDto}.
     *
     * @param book the entity
     * @return the response DTO
     */
    public BookResponseDto toResponse(Book book) {
        return BookResponseDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .email(book.getEmail())
                .phoneNumber(book.getPhoneNumber())
                .publisher(book.getPublisher())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .build();
    }
}
