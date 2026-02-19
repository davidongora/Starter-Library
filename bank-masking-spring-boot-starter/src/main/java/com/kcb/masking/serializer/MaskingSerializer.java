package com.kcb.masking.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.kcb.masking.annotation.Mask;
import com.kcb.masking.service.MaskingService;

import java.io.IOException;

/**
 * Jackson serializer that applies masking when writing a String field
 * that is annotated with {@code @Mask}.
 *
 * <p>
 * This enables seamless integration with Jackson-based logging
 * (e.g., when using structured JSON loggers like Logstash).
 *
 * <p>
 * Used in conjunction with {@link MaskingModule}.
 */
public class MaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final MaskingService maskingService;
    private final Mask maskAnnotation;

    public MaskingSerializer(MaskingService maskingService, Mask maskAnnotation) {
        this.maskingService = maskingService;
        this.maskAnnotation = maskAnnotation;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (maskAnnotation != null) {
            gen.writeString(maskingService.mask(value, maskAnnotation.style(), maskAnnotation.maskCharacter()));
        } else {
            gen.writeString(maskingService.mask(value));
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Mask ann = property.getAnnotation(Mask.class);
            if (ann == null) {
                ann = property.getContextAnnotation(Mask.class);
            }
            return new MaskingSerializer(maskingService, ann);
        }
        return this;
    }
}
