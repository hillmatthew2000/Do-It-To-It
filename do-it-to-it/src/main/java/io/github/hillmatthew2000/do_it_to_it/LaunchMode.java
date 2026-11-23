package io.github.hillmatthew2000.do_it_to_it;

import java.util.Arrays;
import java.util.Set;

/**
 * How the application was asked to start. Console mode is the default; {@code --spring} (or its
 * alias {@code --web}) starts the web server. Any other arguments are passed on to Spring, so options
 * such as {@code --server.port=9090} or {@code --todo.data-dir=...} work in both modes.
 */
enum LaunchMode {
    CONSOLE,
    WEB;

    private static final Set<String> WEB_FLAGS = Set.of("--spring", "--web");
    private static final Set<String> CONSOLE_FLAGS = Set.of("--console");

    static LaunchMode from(String[] args) {
        return Arrays.stream(args).anyMatch(WEB_FLAGS::contains) ? WEB : CONSOLE;
    }

    /** Returns the arguments with the mode flags removed. */
    static String[] springArguments(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> !WEB_FLAGS.contains(arg) && !CONSOLE_FLAGS.contains(arg))
                .toArray(String[]::new);
    }
}
