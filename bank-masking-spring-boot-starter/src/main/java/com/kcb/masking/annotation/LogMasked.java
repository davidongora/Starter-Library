package com.kcb.masking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for methods whose parameters should be automatically
 * logged with sensitive data masked by the {@code MaskingLoggingAspect}.
 *
 * <p>
 * Example:
 * 
 * <pre>
 *   {@literal @}LogMasked
 *   public BookDto createBook(BookDto dto) {
 *       // aspect logs: Entering BookService.createBook(dto={"email":"jo***@g.com", ...})
 *   }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMasked {
}
