package io.github.hillmatthew2000.do_it_to_it.task;

/** Thrown when a task id does not refer to an existing task. */
public class TaskNotFoundException extends RuntimeException {

    private final long taskId;

    public TaskNotFoundException(long taskId) {
        super("Task " + taskId + " does not exist");
        this.taskId = taskId;
    }

    public long getTaskId() {
        return taskId;
    }
}
