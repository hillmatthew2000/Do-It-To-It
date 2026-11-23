package io.github.hillmatthew2000.do_it_to_it.task;

/** Task counts by state. {@code overdue} counts unfinished tasks whose due date has passed. */
public record TaskStats(long total, long pending, long inProgress, long completed, long overdue) {}
