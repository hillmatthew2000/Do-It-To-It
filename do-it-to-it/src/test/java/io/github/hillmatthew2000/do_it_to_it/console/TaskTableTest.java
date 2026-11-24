package io.github.hillmatthew2000.do_it_to_it.console;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskFixtures;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskTableTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);
    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    @Test
    void alignsEveryColumn() {
        List<String> lines = TaskTable.render(
                List.of(
                        TaskFixtures.task(
                                7, new TaskDetails("Pay rent", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY), NOW),
                        TaskFixtures.task(12, TaskDetails.of("Walk the dog"), NOW)),
                TODAY);

        assertThat(lines)
                .containsExactly(
                        "ID  Status       Priority  Due         Description",
                        "--  -----------  --------  ----------  ------------",
                        " 7  In progress  High      2026-03-02  Pay rent",
                        "12  Pending      Medium    -           Walk the dog");
    }

    @Test
    void marksOverdueTasks() {
        var overdue = TaskFixtures.task(1, new TaskDetails("Late", null, null, TODAY.minusDays(1)), NOW);
        var completed =
                TaskFixtures.task(2, new TaskDetails("Done", TaskStatus.COMPLETED, null, TODAY.minusDays(1)), NOW);

        List<String> lines = TaskTable.render(List.of(overdue, completed), TODAY);

        assertThat(lines.get(2)).contains("2026-03-01 (overdue)");
        assertThat(lines.get(3)).contains("2026-03-01").doesNotContain("overdue");
    }

    @Test
    void shortensLongDescriptionsAndFlattensLineBreaks() {
        assertThat(TaskTable.truncate("first\nsecond")).isEqualTo("first second");

        String shortened = TaskTable.truncate("x".repeat(80));
        assertThat(shortened).hasSize(TaskTable.MAX_DESCRIPTION_WIDTH).endsWith("...");
    }
}
