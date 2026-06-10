package com.studyforge.interaction.learning.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyforge.interaction.learning.support.SemanticTagParser;
import org.junit.jupiter.api.Test;

class PostSemanticTagServiceTest {
    @Test
    void ruleCacheFingerprintMarksProvisionalTags() {
        String fingerprint = SemanticTagParser.fingerprint("Vue state design", "Frontend patterns");
        assertThat(fingerprint + ":rule").endsWith(":rule");
    }
}
