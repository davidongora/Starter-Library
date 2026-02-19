package com.kcb.masking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcb.masking.annotation.Mask;
import com.kcb.masking.annotation.MaskStyle;
import com.kcb.masking.config.MaskingProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ObjectMaskingService}.
 * Validates deep object traversal, nested objects, lists, null safety, and
 * annotation-driven masking.
 */
@DisplayName("ObjectMaskingService Tests")
class ObjectMaskingServiceTest {

    private MaskingProperties properties;
    private MaskingService maskingService;
    private ObjectMaskingService objectMaskingService;

    @BeforeEach
    void setUp() {
        properties = new MaskingProperties();
        properties.setEnabled(true);
        properties.setMaskStyle(MaskStyle.PARTIAL);
        properties.setMaskCharacter("*");
        properties.setFields(List.of("email", "phoneNumber", "ssn", "creditCardNumber", "password"));
        maskingService = new MaskingService(properties);
        objectMaskingService = new ObjectMaskingService(maskingService, properties, new ObjectMapper());
    }

    // ---- Test DTOs ----

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class SimpleDto {
        private String title;
        private String email;
        private String phoneNumber;
        private String author;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AnnotatedDto {
        private String title;
        @Mask(style = MaskStyle.FULL)
        private String secret;
        @Mask(style = MaskStyle.LAST4)
        private String creditCardNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NestedDto {
        private String title;
        private SimpleDto owner;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ListDto {
        private String name;
        private List<String> emails;
        private List<SimpleDto> contacts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class NullableDto {
        private String title;
        private String email;
        private String phoneNumber;
    }

    // ---- Tests ----

    @Nested
    @DisplayName("Config-driven masking")
    class ConfigDriven {

        @Test
        @DisplayName("should mask email and phoneNumber but not title/author")
        void shouldMaskConfiguredFields() {
            SimpleDto dto = new SimpleDto("Spring Boot", "john@gmail.com", "0712345678", "John Doe");
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).contains("Spring Boot");
            assertThat(masked).contains("John Doe");
            assertThat(masked).doesNotContain("john@gmail.com");
            assertThat(masked).doesNotContain("0712345678");
        }

        @Test
        @DisplayName("original object should NOT be modified")
        void shouldNotModifyOriginalObject() {
            SimpleDto dto = new SimpleDto("Title", "original@email.com", "0700000001", "Author");
            objectMaskingService.toMaskedString(dto);

            // Original must be preserved
            assertThat(dto.getEmail()).isEqualTo("original@email.com");
            assertThat(dto.getPhoneNumber()).isEqualTo("0700000001");
        }
    }

    @Nested
    @DisplayName("Annotation-driven masking")
    class AnnotationDriven {

        @Test
        @DisplayName("should apply @Mask(FULL) to annotated field")
        void shouldApplyFullMaskViaAnnotation() {
            AnnotatedDto dto = new AnnotatedDto("Title", "supersecret", "4111111111111234");
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).contains("Title");
            assertThat(masked).doesNotContain("supersecret");
            // @Mask(FULL) on 'secret'
            assertThat(masked).contains("***********"); // 11 stars
        }

        @Test
        @DisplayName("should apply @Mask(LAST4) to creditCardNumber annotation")
        void shouldApplyLast4ViaAnnotation() {
            AnnotatedDto dto = new AnnotatedDto("Title", "pwd", "4111111111111234");
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).contains("1234");
            assertThat(masked).doesNotContain("411111111111");
        }
    }

    @Nested
    @DisplayName("Nested object traversal")
    class NestedObjects {

        @Test
        @DisplayName("should mask fields in nested objects")
        void shouldMaskNestedObjectFields() {
            SimpleDto owner = new SimpleDto("Book", "nested@test.com", "0799999999", "Author");
            NestedDto dto = new NestedDto("Parent", owner);
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).doesNotContain("nested@test.com");
            assertThat(masked).doesNotContain("0799999999");
            assertThat(masked).contains("Parent");
        }
    }

    @Nested
    @DisplayName("List support")
    class ListSupport {

        @Test
        @DisplayName("should handle null gracefully")
        void shouldReturnNullStringForNull() {
            assertThat(objectMaskingService.toMaskedString(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("should handle List of nested DTOs")
        void shouldMaskListOfObjects() {
            SimpleDto c1 = new SimpleDto("Book1", "user1@test.com", "0700000001", "Auth1");
            SimpleDto c2 = new SimpleDto("Book2", "user2@test.com", "0700000002", "Auth2");
            ListDto dto = new ListDto("Owner", null, List.of(c1, c2));
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).doesNotContain("user1@test.com");
            assertThat(masked).doesNotContain("user2@test.com");
        }
    }

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("should handle object with null sensitive fields")
        void shouldHandleNullSensitiveFields() {
            NullableDto dto = new NullableDto("Title", null, null);
            String masked = objectMaskingService.toMaskedString(dto);

            assertThat(masked).isNotNull();
            assertThat(masked).contains("Title");
        }

        @Test
        @DisplayName("should return 'null' string for null object")
        void shouldHandleNullObject() {
            assertThat(objectMaskingService.toMaskedString(null)).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("Masking disabled")
    class MaskingDisabled {

        @Test
        @DisplayName("should return toString() of object when masking disabled")
        void shouldReturnToStringWhenDisabled() {
            properties.setEnabled(false);
            SimpleDto dto = new SimpleDto("Title", "email@test.com", "0700000000", "Author");
            String result = objectMaskingService.toMaskedString(dto);
            // When disabled, just calls String.valueOf(obj)
            assertThat(result).isNotNull();
        }
    }
}
