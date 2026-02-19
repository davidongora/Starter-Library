package com.kcb.masking.serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.kcb.masking.service.MaskingService;

/**
 * Jackson module that registers the {@link MaskingSerializer} for String
 * fields.
 *
 * <p>
 * This module is registered into the application's
 * {@link com.fasterxml.jackson.databind.ObjectMapper}
 * so that any field annotated with {@code @Mask} is automatically masked during
 * serialization.
 */
public class MaskingModule extends SimpleModule {

    public MaskingModule(MaskingService maskingService) {
        super("MaskingModule");
        addSerializer(String.class, new MaskingSerializer(maskingService, null));
    }
}
