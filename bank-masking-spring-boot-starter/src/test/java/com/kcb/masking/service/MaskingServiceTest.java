package com.kcb.masking.service;

import com.kcb.masking.annotation.MaskStyle;
import com.kcb.masking.config.MaskingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MaskingService}.
 * Validates all three masking styles and edge cases.
 */
@DisplayName("MaskingService Tests")
class MaskingServiceTest {

    private MaskingProperties properties;
    private MaskingService maskingService;

    @BeforeEach
    void setUp() {
        properties = new MaskingProperties();
        properties.setEnabled(true);
        properties.setMaskStyle(MaskStyle.PARTIAL);
        properties.setMaskCharacter("*");
        properties.setFields(List.of("email", "phoneNumber", "ssn", "creditCardNumber", "password"));
        maskingService = new MaskingService(properties);
    }

    @Nested
    @DisplayName("FULL masking")
    class FullMasking {

        @Test
        @DisplayName("should replace all characters with mask char")
        void shouldMaskEntireValue() {
            String result = maskingService.mask("hello", MaskStyle.FULL, "*");
            assertThat(result).isEqualTo("*****");
        }

        @Test
        @DisplayName("should handle single character")
        void shouldHandleSingleChar() {
            assertThat(maskingService.mask("a", MaskStyle.FULL, "*")).isEqualTo("*");
        }

        @Test
        @DisplayName("should handle email with FULL style")
        void shouldMaskEmailFully() {
            assertThat(maskingService.mask("john@test.com", MaskStyle.FULL, "*"))
                    .isEqualTo("*".repeat("john@test.com".length()));
        }
    }

    @Nested
    @DisplayName("PARTIAL masking")
    class PartialMasking {

        @ParameterizedTest(name = "email ''{0}'' → should mask username middle")
        @CsvSource({
                "john@gmail.com, jo**@gmail.com",
                "a@test.com, *@test.com",
                "ab@test.com, **@test.com"
        })
        @DisplayName("should partially mask email addresses")
        void shouldPartiallyMaskEmails(String input, String expected) {
            String result = maskingService.mask(input, MaskStyle.PARTIAL, "*");
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should partially mask phone numbers")
        void shouldPartiallyMaskPhoneNumbers() {
            String result = maskingService.mask("0712345678", MaskStyle.PARTIAL, "*");
            assertThat(result)
                    .startsWith("071")
                    .endsWith("678")
                    .contains("*");
        }

        @Test
        @DisplayName("should mask short values fully in PARTIAL mode")
        void shouldMaskShortValuesFully() {
            String result = maskingService.mask("abc", MaskStyle.PARTIAL, "*");
            assertThat(result).isEqualTo("***");
        }

        @Test
        @DisplayName("should handle generic string partial masking")
        void shouldHandleGenericStringPartial() {
            String result = maskingService.mask("password123", MaskStyle.PARTIAL, "*");
            assertThat(result)
                    .hasSameSizeAs("password123")
                    .contains("*");
        }
    }

    @Nested
    @DisplayName("LAST4 masking")
    class Last4Masking {

        @Test
        @DisplayName("should show only last 4 characters")
        void shouldShowLast4() {
            String result = maskingService.mask("4111111111111234", MaskStyle.LAST4, "*");
            assertThat(result).isEqualTo("************1234");
        }

        @Test
        @DisplayName("should return value unchanged if 4 or fewer chars")
        void shouldReturnShortValueUnchanged() {
            assertThat(maskingService.mask("1234", MaskStyle.LAST4, "*")).isEqualTo("1234");
        }

        @Test
        @DisplayName("should handle 5-char value")
        void shouldHandle5CharValue() {
            String result = maskingService.mask("12345", MaskStyle.LAST4, "*");
            assertThat(result).isEqualTo("*2345");
        }
    }

    @Nested
    @DisplayName("Null and edge case handling")
    class NullAndEdgeCases {

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNull() {
            assertThat(maskingService.mask(null)).isNull();
        }

        @Test
        @DisplayName("should return blank string unchanged")
        void shouldReturnBlankUnchanged() {
            assertThat(maskingService.mask("")).isEqualTo("");
            assertThat(maskingService.mask("   ")).isEqualTo("   ");
        }

        @Test
        @DisplayName("should use global config when style is DEFAULT")
        void shouldUseGlobalStyleForDefault() {
            properties.setMaskStyle(MaskStyle.FULL);
            String result = maskingService.mask("hello@test.com", MaskStyle.DEFAULT, "*");
            assertThat(result).isEqualTo("*".repeat("hello@test.com".length()));
        }
    }

    @Nested
    @DisplayName("Sensitive field detection")
    class SensitiveFieldDetection {

        @ParameterizedTest(name = "''{0}'' should be detected as sensitive")
        @ValueSource(strings = { "email", "Email", "EMAIL", "phoneNumber", "PhoneNumber",
                "ssn", "creditCardNumber", "password" })
        @DisplayName("should detect configured sensitive fields case-insensitively")
        void shouldDetectSensitiveFields(String fieldName) {
            assertThat(maskingService.isSensitiveField(fieldName)).isTrue();
        }

        @ParameterizedTest(name = "''{0}'' should NOT be detected as sensitive")
        @ValueSource(strings = { "title", "author", "publisher", "id", "name" })
        @DisplayName("should not flag non-sensitive fields")
        void shouldNotFlagNonSensitiveFields(String fieldName) {
            assertThat(maskingService.isSensitiveField(fieldName)).isFalse();
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("should return false for null field name")
        void shouldReturnFalseForNull(String fieldName) {
            assertThat(maskingService.isSensitiveField(fieldName)).isFalse();
        }

        @Test
        @DisplayName("should return false when masking is disabled")
        void shouldReturnFalseWhenDisabled() {
            properties.setEnabled(false);
            assertThat(maskingService.isSensitiveField("email")).isFalse();
        }

        @Test
        @DisplayName("should handle hyphenated and underscore field name variants")
        void shouldHandleVariants() {
            assertThat(maskingService.isSensitiveField("phone_number")).isTrue();
            assertThat(maskingService.isSensitiveField("credit-card-number")).isTrue();
        }
    }
}
