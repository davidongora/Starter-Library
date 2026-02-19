package com.kcb.masking.service;

import com.kcb.masking.annotation.MaskStyle;
import com.kcb.masking.config.MaskingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core service responsible for applying masking logic to string values.
 *
 * <p>
 * This service is stateless and thread-safe. It does NOT modify original
 * objects; instead it returns a new masked string or object representation.
 *
 * <p>
 * Supported masking strategies:
 * <ul>
 * <li>{@link MaskStyle#FULL} – Replace entire value with mask characters</li>
 * <li>{@link MaskStyle#PARTIAL} – Keep contextual prefix/suffix, mask
 * middle</li>
 * <li>{@link MaskStyle#LAST4} – Show only the last 4 characters</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class MaskingService {

    private final MaskingProperties properties;

    /**
     * Masks a value using the globally configured style.
     *
     * @param value the value to mask
     * @return masked value, or null if input is null
     */
    public String mask(String value) {
        return mask(value, properties.getMaskStyle(), properties.getMaskCharacter());
    }

    /**
     * Masks a value using an explicit style, falling back to global config for
     * DEFAULT.
     *
     * @param value         the value to mask
     * @param style         the masking style; use DEFAULT to apply global config
     * @param maskCharacter the character to use (empty string falls back to global
     *                      config)
     * @return masked value, or null if input is null
     */
    public String mask(String value, MaskStyle style, String maskCharacter) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return value;
        }

        // Resolve effective style and character
        MaskStyle effectiveStyle = (style == null || style == MaskStyle.DEFAULT)
                ? properties.getMaskStyle()
                : style;

        String effectiveChar = (maskCharacter == null || maskCharacter.isBlank())
                ? properties.getMaskCharacter()
                : maskCharacter;

        return switch (effectiveStyle) {
            case FULL -> maskFull(value, effectiveChar);
            case LAST4 -> maskLast4(value, effectiveChar);
            case PARTIAL, DEFAULT -> maskPartial(value, effectiveChar);
        };
    }

    /**
     * Checks whether the given field name should be masked.
     * Matching is case-insensitive and also handles camelCase → snake_case
     * variations.
     *
     * @param fieldName the field name to check
     * @return true if the field should be masked
     */
    public boolean isSensitiveField(String fieldName) {
        if (!properties.isEnabled() || fieldName == null) {
            return false;
        }
        String normalized = fieldName.toLowerCase().replace("_", "").replace("-", "");
        return properties.getFields().stream()
                .map(f -> f.toLowerCase().replace("_", "").replace("-", ""))
                .anyMatch(f -> f.equals(normalized));
    }

    /**
     * FULL masking: replaces every character with the mask character.
     * Example: "hello" → "***** "
     */
    private String maskFull(String value, String maskChar) {
        return maskChar.repeat(value.length());
    }

    /**
     * PARTIAL masking: smart context-aware masking.
     * - Emails: keep first 2 chars before @, mask username middle, keep domain.
     * "john@gmail.com" → "jo***@gmail.com"
     * - Phone numbers (detects digit-heavy strings):
     * "0712345678" → "071****678"
     * - General strings (≥ 6 chars): keep first 1/4, mask middle, keep last 1/4
     * "password123" → "pa*******23"
     * - Short strings (< 6 chars): full mask
     */
    private String maskPartial(String value, String maskChar) {
        if (value.contains("@")) {
            return maskEmail(value, maskChar);
        }

        // Phone / numeric heavy
        String digitsOnly = value.replaceAll("[^0-9]", "");
        if ((double) digitsOnly.length() / value.length() > 0.5) {
            return maskPhone(value, maskChar);
        }

        // Generic partial masking
        int len = value.length();
        if (len < 6) {
            return maskFull(value, maskChar);
        }
        int showStart = Math.max(1, len / 4);
        int showEnd = Math.max(1, len / 4);
        int maskLen = len - showStart - showEnd;
        return value.substring(0, showStart)
                + maskChar.repeat(maskLen)
                + value.substring(len - showEnd);
    }

    /**
     * Email partial masking.
     * "john.doe@gmail.com" → "jo***@gmail.com"
     */
    private String maskEmail(String email, String maskChar) {
        int atIdx = email.indexOf('@');
        if (atIdx <= 0) {
            return maskFull(email, maskChar);
        }
        String username = email.substring(0, atIdx);
        String domain = email.substring(atIdx); // includes '@'

        if (username.length() <= 2) {
            return maskChar.repeat(username.length()) + domain;
        }
        int showChars = Math.min(2, username.length() / 2);
        return username.substring(0, showChars)
                + maskChar.repeat(username.length() - showChars)
                + domain;
    }

    /**
     * Phone number partial masking.
     * "0712345678" → "071****678"
     */
    private String maskPhone(String phone, String maskChar) {
        int len = phone.length();
        if (len <= 4) {
            return maskFull(phone, maskChar);
        }
        int showStart = Math.min(3, len / 3);
        int showEnd = Math.min(3, len / 3);
        int maskLen = len - showStart - showEnd;
        return phone.substring(0, showStart)
                + maskChar.repeat(Math.max(1, maskLen))
                + phone.substring(len - showEnd);
    }

    /**
     * LAST4 masking: shows only last 4 characters.
     * "4111111111111234" → "************1234"
     */
    private String maskLast4(String value, String maskChar) {
        int len = value.length();
        if (len <= 4) {
            return value; // Too short to mask meaningfully
        }
        return maskChar.repeat(len - 4) + value.substring(len - 4);
    }
}
