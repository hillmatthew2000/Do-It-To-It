package io.github.hillmatthew2000.do_it_to_it.transfer;

import io.github.hillmatthew2000.do_it_to_it.task.ImportedTask;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.lang.Nullable;

/**
 * One task in a {@link TaskExport}. On import only the description is required; the other fields take
 * the same defaults as when a task is created.
 */
public record ExportedTask(
        @Nullable String description,
        @Nullable TaskStatus status,
        @Nullable TaskPriority priority,
        @Nullable LocalDate dueDate,
        @Nullable Instant createdAt,
        @Nullable Instant updatedAt) {

    static ExportedTask from(Task task) {
        return new ExportedTask(
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    /**
     * Validates and converts this entry.
     *
     * @throws IllegalArgumentException if the task is not valid, e.g. its description is blank
     */
    ImportedTask toImportedTask() {
        return new ImportedTask(new TaskDetails(description, status, priority, dueDate), createdAt, updatedAt);
    }
}
