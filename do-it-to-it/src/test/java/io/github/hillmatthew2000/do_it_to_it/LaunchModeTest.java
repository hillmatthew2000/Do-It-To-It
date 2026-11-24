package io.github.hillmatthew2000.do_it_to_it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LaunchModeTest {

    @Test
    void defaultsToConsole() {
        assertThat(LaunchMode.from(new String[0])).isEqualTo(LaunchMode.CONSOLE);
        assertThat(LaunchMode.from(new String[] {"--server.port=9090"})).isEqualTo(LaunchMode.CONSOLE);
    }

    @Test
    void springOrWebFlagSelectsWebMode() {
        assertThat(LaunchMode.from(new String[] {"--spring"})).isEqualTo(LaunchMode.WEB);
        assertThat(LaunchMode.from(new String[] {"--server.port=9090", "--web"}))
                .isEqualTo(LaunchMode.WEB);
    }

    @Test
    void modeFlagsAreNotPassedToSpring() {
        assertThat(LaunchMode.springArguments(new String[] {"--spring", "--server.port=9090", "--console"}))
                .containsExactly("--server.port=9090");
    }
}
