package io.github.hillmatthew2000.do_it_to_it.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Lifecycle state of a {@link Task}. */
public enum TaskStatus {
    PENDING("Pending"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    /** Human-readable name, e.g. {@code "In progress"}. */
    public String label() {
        return label;
    }

    /**
     * Parses a status leniently: case is ignored and spaces or dashes are treated as underscores, so
     * {@code "in progress"}, {@code "In-Progress"} and {@code "IN_PROGRESS"} are all accepted.
     */
    public static Optional<TaskStatus> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().replaceAll("[\\s-]+", "_").toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst();
    }

    /**
     * Parses a status like {@link #parse(String)}, failing with a message that lists the accepted values.
     *
     * @throws IllegalArgumentException if the value is not a known status
     */
    @JsonCreator
    public static TaskStatus of(String value) {
        return parse(value)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown status '" + value + "'. Expected one of " + Arrays.toString(values())));
    }
}
