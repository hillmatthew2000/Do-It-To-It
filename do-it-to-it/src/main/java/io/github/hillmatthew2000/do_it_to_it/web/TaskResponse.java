package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A task as returned by the REST API. {@code version} changes on every update and is also sent as the
 * {@code ETag} header of single-task responses.
 */
public record TaskResponse(
        long id,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static TaskResponse from(Task task, LocalDate today) {
        return new TaskResponse(
                task.getId(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.isOverdue(today),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getVersion());
    }
}
