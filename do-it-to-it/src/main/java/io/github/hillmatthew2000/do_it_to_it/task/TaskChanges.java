package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.LocalDate;

/**
 * A partial update to a task. {@code null} fields are left unchanged, which means a due date cannot
 * be cleared through this type; replace the task's {@link TaskDetails} for that.
 */
public record TaskChanges(String description, TaskStatus status, TaskPriority priority, LocalDate dueDate) {

    public static TaskChanges status(TaskStatus status) {
        return new TaskChanges(null, status, null, null);
    }

    /** Returns {@code current} with every non-null field of this change applied. */
    TaskDetails applyTo(TaskDetails current) {
        return new TaskDetails(
                description != null ? description : current.description(),
                status != null ? status : current.status(),
                priority != null ? priority : current.priority(),
                dueDate != null ? dueDate : current.dueDate());
    }
}
