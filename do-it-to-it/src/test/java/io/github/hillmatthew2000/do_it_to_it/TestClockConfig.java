package io.github.hillmatthew2000.do_it_to_it;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the system clock with a {@link MutableClock} fixed at {@link #START}. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    /** Monday 2026-03-02 09:00 UTC. */
    public static final Instant START = Instant.parse("2026-03-02T09:00:00Z");

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(START);
    }
}
