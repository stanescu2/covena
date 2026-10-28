package io.covena.document.adapter.out.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(
        OutboxRelayConfiguration.class);

    @Test
    void shouldBindRelayProperties() {
        contextRunner
            .withPropertyValues("covena.outbox.relay.enabled=true",
                "covena.outbox.relay.fixed-delay=10s",
                "covena.outbox.relay.batch-size=10")
            .run(context -> assertThat(context.getBean(OutboxRelayProperties.class)).isEqualTo(new OutboxRelayProperties(
                true,
                Duration.ofSeconds(10),
                10)));
    }

    @Test
    void shouldReadFixedDelayWithoutUnitAsMilliseconds() {
        contextRunner
            .withPropertyValues("covena.outbox.relay.enabled=true",
                "covena.outbox.relay.fixed-delay=1000",
                "covena.outbox.relay.batch-size=10")
            .run(context -> assertThat(context
                .getBean(OutboxRelayProperties.class)
                .fixedDelay()).isEqualTo(Duration.ofMillis(1000)));
    }

    @Test
    void shouldFailWhenBatchSizeIsNotPositive() {
        contextRunner
            .withPropertyValues("covena.outbox.relay.enabled=true",
                "covena.outbox.relay.fixed-delay=1000",
                "covena.outbox.relay.batch-size=0")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining("batchSize");
            });
    }

    @Test
    void shouldFailWhenFixedDelayIsMissing() {
        contextRunner
            .withPropertyValues("covena.outbox.relay.enabled=true",
                "covena.outbox.relay.batch-size=10").run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(BindValidationException.class)
                    .rootCause()
                    .hasMessageContaining("fixedDelay");
            });
    }
}
