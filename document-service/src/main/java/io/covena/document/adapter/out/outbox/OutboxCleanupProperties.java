package io.covena.document.adapter.out.outbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("covena.outbox.cleanup")
@Validated
record OutboxCleanupProperties(boolean enabled, @NotNull @DurationMin(days = 1) Duration retention,
                               @NotBlank String cron) {
}
