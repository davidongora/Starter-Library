package com.kcb.masking.integration;

import com.kcb.masking.config.MaskingAutoConfiguration;
import com.kcb.masking.config.MaskingProperties;
import com.kcb.masking.service.MaskingService;
import com.kcb.masking.service.ObjectMaskingService;
import com.kcb.masking.annotation.MaskStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that validates the Spring auto-configuration loads correctly
 * and all beans are wired and functional.
 */
@SpringBootTest(classes = MaskingAutoConfiguration.class)
@TestPropertySource(properties = {
        "p11.masking.enabled=true",
        "p11.masking.mask-style=PARTIAL",
        "p11.masking.mask-character=*",
        "p11.masking.fields=email,phoneNumber,ssn"
})
@DisplayName("MaskingAutoConfiguration Integration Tests")
class MaskingAutoConfigurationTest {

    @Autowired
    private MaskingService maskingService;

    @Autowired
    private ObjectMaskingService objectMaskingService;

    @Autowired
    private MaskingProperties maskingProperties;

    @Test
    @DisplayName("MaskingService bean should be loaded by auto-configuration")
    void maskingService_shouldBeLoaded() {
        assertThat(maskingService).isNotNull();
    }

    @Test
    @DisplayName("ObjectMaskingService bean should be loaded by auto-configuration")
    void objectMaskingService_shouldBeLoaded() {
        assertThat(objectMaskingService).isNotNull();
    }

    @Test
    @DisplayName("MaskingProperties should be bound from test properties")
    void maskingProperties_shouldBeBound() {
        assertThat(maskingProperties).isNotNull();
        assertThat(maskingProperties.isEnabled()).isTrue();
        assertThat(maskingProperties.getMaskStyle()).isEqualTo(MaskStyle.PARTIAL);
        assertThat(maskingProperties.getMaskCharacter()).isEqualTo("*");
        assertThat(maskingProperties.getFields()).contains("email", "phoneNumber", "ssn");
    }

    @Test
    @DisplayName("End-to-end: configured masking service should mask email")
    void endToEnd_shouldMaskEmail() {
        String masked = maskingService.mask("john@gmail.com");
        assertThat(masked)
                .doesNotContain("john@gmail.com")
                .contains("@gmail.com")
                .contains("*");
    }

    @Test
    @DisplayName("End-to-end: isSensitiveField should detect configured fields")
    void endToEnd_shouldDetectSensitiveFields() {
        assertThat(maskingService.isSensitiveField("email")).isTrue();
        assertThat(maskingService.isSensitiveField("phoneNumber")).isTrue();
        assertThat(maskingService.isSensitiveField("title")).isFalse();
    }
}
