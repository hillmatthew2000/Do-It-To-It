package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.LocalDate;

/**
 * The user-editable fields of a task, normalized and validated.
 *
 * <p>The description is trimmed and must be non-blank and at most {@link Task#MAX_DESCRIPTION_LENGTH}
 * characters. A missing status defaults to {@link TaskStatus#PENDING} and a missing priority to
 * {@link TaskPriority#MEDIUM}. The due date is optional.
 */
public record TaskDetails(String description, TaskStatus status, TaskPriority priority, LocalDate dueDate) {

    public TaskDetails {
        description = normalizeDescription(description);
        status = status == null ? TaskStatus.PENDING : status;
        priority = priority == null ? TaskPriority.MEDIUM : priority;
    }

    /** Details for a new pending, medium-priority task with no due date. */
    public static TaskDetails of(String description) {
        return new TaskDetails(description, null, null, null);
    }

    static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }
        String trimmed = description.strip();
        if (trimmed.length() > Task.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Task description cannot be longer than " + Task.MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }
}
