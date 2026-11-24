package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.TaskStats;

/** Body of {@code GET /api/todos/stats}. */
public record TaskStatsResponse(
        long totalTasks, long pendingTasks, long inProgressTasks, long completedTasks, long overdueTasks) {

    static TaskStatsResponse from(TaskStats stats) {
        return new TaskStatsResponse(
                stats.total(), stats.pending(), stats.inProgress(), stats.completed(), stats.overdue());
    }
}
