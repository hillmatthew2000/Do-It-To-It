package io.github.hillmatthew2000.do_it_to_it.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** How urgent a {@link Task} is. Declared from least to most urgent. */
public enum TaskPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }

    /** Human-readable name, e.g. {@code "High"}. */
    public String label() {
        return label;
    }

    /** Parses a priority, ignoring case and surrounding whitespace. */
    public static Optional<TaskPriority> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(priority -> priority.name().equals(normalized))
                .findFirst();
    }

    /**
     * Parses a priority like {@link #parse(String)}, failing with a message that lists the accepted
     * values.
     *
     * @throws IllegalArgumentException if the value is not a known priority
     */
    @JsonCreator
    public static TaskPriority of(String value) {
        return parse(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown priority '" + value + "'. Expected one of " + Arrays.toString(values())));
    }
}
