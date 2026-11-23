package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Body of {@code POST /api/todos} and {@code PUT /api/todos/{id}}. Omitted fields take their defaults
 * (pending status, medium priority, no due date).
 */
public record TaskRequest(
        @NotBlank(message = "Description is required")
        @Size(max = Task.MAX_DESCRIPTION_LENGTH, message = "Description must be at most {max} characters")
        String description,

        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {

    TaskDetails toDetails() {
        return new TaskDetails(description, status, priority, dueDate);
    }
}
