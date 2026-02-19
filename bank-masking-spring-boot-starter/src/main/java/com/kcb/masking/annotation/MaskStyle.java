package com.kcb.masking.annotation;

/**
 * Enum defining available masking styles.
 */
public enum MaskStyle {
    /**
     * Use the globally configured default style.
     */
    DEFAULT,

    /**
     * Mask the entire value: "john@gmail.com" → "**************"
     */
    FULL,

    /**
     * Partially mask the value preserving some characters:
     * "john@gmail.com" → "jo***@gmail.com"
     * "0712345678" → "071****678"
     */
    PARTIAL,

    /**
     * Show only the last 4 characters:
     * "4111111111111234" → "************1234"
     */
    LAST4
}
