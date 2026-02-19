package com.kcb.masking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields or parameters that should be masked in logs.
 * Can be applied at field level or method parameter level.
 *
 * <p>Example usage on a DTO field:
 * <pre>
 *   public class UserDto {
 *       {@literal @}Mask
 *       private String email;
 *
 *       {@literal @}Mask(style = MaskStyle.LAST4)
 *       private String creditCardNumber;
 *   }
 * </pre>
 *
 * <p>When the DTO is logged, the annotated fields will be masked
 * according to the specified style.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Mask {

    /**
     * The masking style to apply. Defaults to the globally configured style.
     */
    MaskStyle style() default MaskStyle.DEFAULT;

    /**
     * The character to use for masking. Defaults to the globally configured character.
     */
    String maskCharacter() default "";
}
