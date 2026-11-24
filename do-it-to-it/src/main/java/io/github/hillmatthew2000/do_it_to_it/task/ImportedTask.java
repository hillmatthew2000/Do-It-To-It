package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.Instant;
import org.springframework.lang.Nullable;

/**
 * A task read from an export file, to be added with {@link TaskService#importAll}. Timestamps that
 * are missing from the file default to the time of the import.
 */
public record ImportedTask(
        TaskDetails details,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {}
