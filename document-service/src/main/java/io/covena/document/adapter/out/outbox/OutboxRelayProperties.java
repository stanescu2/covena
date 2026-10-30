package io.covena.document.adapter.out.outbox;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("covena.outbox.relay")
@Validated
record OutboxRelayProperties(boolean enabled, @NotNull Duration fixedDelay, @Positive int batchSize) {
}
