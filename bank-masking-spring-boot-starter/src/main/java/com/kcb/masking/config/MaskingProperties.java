package com.kcb.masking.config;

import com.kcb.masking.annotation.MaskStyle;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the P11 masking starter.
 *
 * <p>Configure via application.yaml:
 * <pre>
 * p11:
 *   masking:
 *     enabled: true
 *     fields:
 *       - email
 *       - phoneNumber
 *       - ssn
 *       - creditCardNumber
 *     mask-style: PARTIAL
 *     mask-character: "*"
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "p11.masking")
public class MaskingProperties {

    /**
     * Whether masking is enabled globally. Defaults to true.
     */
    private boolean enabled = true;

    /**
     * List of field names to mask. Case-insensitive matching is applied.
     * Default includes common sensitive fields.
     */
    private List<String> fields = new ArrayList<>(List.of(
            "email",
            "phoneNumber",
            "ssn",
            "creditCardNumber",
            "password",
            "cardNumber"
    ));

    /**
     * The masking style to apply globally when masking is not overridden
     * at the field level via {@code @Mask(style=...)}.
     * Defaults to PARTIAL.
     */
    private MaskStyle maskStyle = MaskStyle.PARTIAL;

    /**
     * The character used for masking. Defaults to "*".
     */
    private String maskCharacter = "*";
}
