package io.github.hillmatthew2000.do_it_to_it.task;

import java.util.Comparator;

/**
 * Orderings offered for task lists. Every ordering falls back to creation order (then id) so results
 * are deterministic.
 */
public enum TaskSort {
    /** Oldest first, i.e. the order tasks were added in. */
    OLDEST(Comparator.comparing(Task::getCreatedAt)),
    /** Newest first. */
    NEWEST(Comparator.comparing(Task::getCreatedAt).reversed()),
    /** Most urgent first. */
    PRIORITY(Comparator.comparing(Task::getPriority).reversed()),
    /** Earliest due date first; tasks without a due date go last. */
    DUE_DATE(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))),
    /** Alphabetical by description, ignoring case. */
    DESCRIPTION(Comparator.comparing(Task::getDescription, String.CASE_INSENSITIVE_ORDER));

    private final Comparator<Task> comparator;

    TaskSort(Comparator<Task> primary) {
        this.comparator = primary.thenComparing(Task::getCreatedAt).thenComparing(Task::getId);
    }

    public Comparator<Task> comparator() {
        return comparator;
    }
}
