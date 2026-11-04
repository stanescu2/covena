package io.covena.document.adapter.out.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxCleanupPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(
        OutboxCleanupConfiguration.class);

    @Test
    void shouldBindCleanupProperties() {
        contextRunner
            .withPropertyValues("covena.outbox.cleanup.enabled=true",
                "covena.outbox.cleanup.retention=7d",
                "covena.outbox.cleanup.cron=0 0 3 * * *")
            .run(context -> assertThat(context.getBean(OutboxCleanupProperties.class)).isEqualTo(new OutboxCleanupProperties(
                true,
                Duration.ofDays(7),
                "0 0 3 * * *")));
    }

    @Test
    void shouldFailWhenRetentionIsMissing() {
        contextRunner
            .withPropertyValues("covena.outbox.cleanup.enabled=true", "covena.outbox.cleanup.cron=0 0 3 * * *")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining("retention");
            });
    }

    @Test
    void shouldFailWhenRetentionIsShorterThanOneDay() {
        contextRunner
            .withPropertyValues("covena.outbox.cleanup.enabled=true",
                "covena.outbox.cleanup.retention=23h",
                "covena.outbox.cleanup.cron=0 0 3 * * *")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining("retention");
            });
    }

    @Test
    void shouldAcceptRetentionOfExactlyOneDay() {
        contextRunner
            .withPropertyValues("covena.outbox.cleanup.enabled=true",
                "covena.outbox.cleanup.retention=1d",
                "covena.outbox.cleanup.cron=0 0 3 * * *")
            .run(context -> assertThat(context.getBean(OutboxCleanupProperties.class)).isEqualTo(new OutboxCleanupProperties(
                true,
                Duration.ofDays(1),
                "0 0 3 * * *")));
    }

    @Test
    void shouldFailWhenCronIsBlank() {
        contextRunner
            .withPropertyValues("covena.outbox.cleanup.enabled=true",
                "covena.outbox.cleanup.retention=1d",
                "covena.outbox.cleanup.cron=")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining("cron");
            });
    }
}
