package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds detached {@link Task} instances for tests that do not touch the database. */
public final class TaskFixtures {

    private TaskFixtures() {}

    public static Task task(long id, TaskDetails details, Instant createdAt) {
        Task task = new Task(details, createdAt);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }
}
