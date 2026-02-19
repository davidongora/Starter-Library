package com.kcb.masking.aop;

import com.kcb.masking.annotation.Mask;
import com.kcb.masking.config.MaskingProperties;
import com.kcb.masking.service.MaskingService;
import com.kcb.masking.service.ObjectMaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * AOP aspect that intercepts service layer method calls and logs their
 * arguments with sensitive data masked.
 *
 * <p>
 * This aspect targets methods annotated with {@code @LogMasked} and
 * automatically masks both:
 * <ul>
 * <li>Parameters annotated with {@code @Mask}</li>
 * <li>Objects whose fields are in the configured sensitive-fields list</li>
 * </ul>
 *
 * <p>
 * <strong>Design decision:</strong> AOP is used for cross-cutting logging
 * concerns
 * so that individual service methods don't need to repeat masking boilerplate.
 * The aspect only intercepts methods opt-in via {@code @LogMasked} to avoid
 * performance overhead on unrelated methods.
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class MaskingLoggingAspect {

    private final MaskingService maskingService;
    private final ObjectMaskingService objectMaskingService;
    private final MaskingProperties maskingProperties;

    /**
     * Intercepts any method annotated with {@code @LogMasked} and logs
     * method entry and exit with masked parameters.
     */
    @Around("@annotation(com.kcb.masking.annotation.LogMasked)")
    public Object aroundLogMasked(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!maskingProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

        Object[] args = joinPoint.getArgs();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        String[] paramNames = signature.getParameterNames();

        StringBuilder logBuilder = new StringBuilder("Entering ").append(methodName).append("(");

        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                logBuilder.append(", ");
            String paramName = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
            logBuilder.append(paramName).append("=");
            logBuilder.append(buildMaskedArg(args[i], paramAnnotations[i]));
        }
        logBuilder.append(")");

        log.debug("{}", logBuilder);

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        log.debug("Exiting {} [{}ms]", methodName, elapsed);
        return result;
    }

    /**
     * Generates a masked representation of a single method argument.
     */
    private String buildMaskedArg(Object arg, Annotation[] annotations) {
        if (arg == null)
            return "null";

        // Check if the parameter has @Mask annotation
        for (Annotation ann : annotations) {
            if (ann instanceof Mask maskAnn && arg instanceof String strArg) {
                return maskingService.mask(strArg, maskAnn.style(), maskAnn.maskCharacter());
            }
        }

        // Check if the argument is a complex object with sensitive fields
        if (arg instanceof String strArg) {
            // For plain string params, check field name is not applicable
            return "\"" + strArg + "\"";
        }

        // For complex objects, produce masked representation
        return objectMaskingService.toMaskedString(arg);
    }
}
