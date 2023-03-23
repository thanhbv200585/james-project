package org.apache.james.smtpserver;

import org.apache.james.smtpserver.futurerelease.FutureReleaseEHLOHook;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FutureReleaseEHLOHookTest {
    @Test
    void test() {
        FutureReleaseEHLOHook futureReleaseHook = new FutureReleaseEHLOHook();
        assertThat(futureReleaseHook.implementedEsmtpFeatures())
                .isEqualTo(Set.of("FUTURERELEASE"));
    }
}
