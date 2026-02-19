package com.kcb.masking.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kcb.masking.aop.MaskingLoggingAspect;
import com.kcb.masking.service.MaskingService;
import com.kcb.masking.service.ObjectMaskingService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Bank Masking Starter.
 *
 * <p>
 * This configuration is loaded automatically by Spring Boot via the
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * file.
 *
 * <p>
 * The entire masking infrastructure is activated only when:
 * <ul>
 * <li>{@code p11.masking.enabled=true} (default) is set, OR</li>
 * <li>The property is missing (defaults to enabled)</li>
 * </ul>
 *
 * <p>
 * <strong>Design decisions:</strong>
 * <ul>
 * <li>Uses {@code @ConditionalOnProperty} with {@code matchIfMissing=true} so
 * the
 * starter is active by default without explicit configuration.</li>
 * <li>Uses {@code @ConditionalOnMissingBean} on each bean so consumer
 * applications
 * can override any component.</li>
 * <li>AOP aspect is registered as a Spring bean for automatic proxy
 * creation.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "p11.masking", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MaskingProperties.class)
public class MaskingAutoConfiguration {

    /**
     * Core masking service for string-level masking operations.
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskingService maskingService(MaskingProperties maskingProperties) {
        return new MaskingService(maskingProperties);
    }

    /**
     * Object-level masking service for deep inspection of domain objects.
     * Requires an ObjectMapper – falls back to consumer app's mapper if available.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMaskingService objectMaskingService(MaskingService maskingService,
            MaskingProperties maskingProperties) {
        return new ObjectMaskingService(maskingService, maskingProperties, new ObjectMapper());
    }

    /**
     * AOP aspect for automatic masked logging on {@code @LogMasked} methods.
     */
    @Bean
    @ConditionalOnMissingBean
    public MaskingLoggingAspect maskingLoggingAspect(MaskingService maskingService,
            ObjectMaskingService objectMaskingService,
            MaskingProperties maskingProperties) {
        return new MaskingLoggingAspect(maskingService, objectMaskingService, maskingProperties);
    }
}
