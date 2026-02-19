package com.kcb.masking.wrapper;

import com.kcb.masking.service.ObjectMaskingService;
import lombok.RequiredArgsConstructor;

/**
 * A lightweight wrapper that defers masked string generation until the logger
 * actually calls {@link #toString()}.
 *
 * <p>
 * This is a performance optimization: if the log level is not enabled,
 * toString() is never called and no expensive reflection occurs.
 *
 * <p>
 * Usage in application code:
 * 
 * <pre>
 * log.info("Creating book: {}", MaskedObject.of(bookDto, maskingService));
 * </pre>
 */
@RequiredArgsConstructor(staticName = "of")
public class MaskedObject {

    private final Object target;
    private final ObjectMaskingService objectMaskingService;

    /**
     * Produces the masked JSON representation of the wrapped object.
     * This is called lazily by the logging framework only when the log entry
     * is actually written, saving reflection overhead when logging is disabled.
     */
    @Override
    public String toString() {
        return objectMaskingService.toMaskedString(target);
    }
}
