package io.github.hillmatthew2000.do_it_to_it.task;

/** Thrown when a change was based on a version of a task that is no longer current. */
public class TaskVersionConflictException extends RuntimeException {

    private final long taskId;

    public TaskVersionConflictException(long taskId) {
        this(taskId, null);
    }

    TaskVersionConflictException(long taskId, Throwable cause) {
        super("Task " + taskId + " has been changed since it was loaded", cause);
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }
}
