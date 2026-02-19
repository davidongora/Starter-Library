package com.kcb.books.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a Book.
 *
 * <p>Note: {@code email} and {@code phoneNumber} are stored unmasked in the database.
 * Masking only applies at the logging layer via the masking starter.
 */
@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    /**
     * Sensitive field – will be masked in logs but stored as-is in the database.
     */
    @Column
    private String email;

    /**
     * Sensitive field – will be masked in logs but stored as-is in the database.
     */
    @Column
    private String phoneNumber;

    @Column
    private String publisher;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
