package com.kcb.books.exception;

/**
 * Exception thrown when a Book is not found by its ID.
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }
}
