package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskChanges;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Body of {@code PATCH /api/todos/{id}}. Only the fields that are present are changed. */
public record TaskPatchRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Description cannot be blank")
        @Size(max = Task.MAX_DESCRIPTION_LENGTH, message = "Description must be at most {max} characters")
        String description,

        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {

    TaskChanges toChanges() {
        return new TaskChanges(description, status, priority, dueDate);
    }
}
