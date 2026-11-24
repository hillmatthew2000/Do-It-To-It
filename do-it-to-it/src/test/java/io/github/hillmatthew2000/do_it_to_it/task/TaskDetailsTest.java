package io.github.hillmatthew2000.do_it_to_it.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskDetailsTest {

    @Test
    void trimsDescriptionAndAppliesDefaults() {
        TaskDetails details = TaskDetails.of("  Water the plants \n");

        assertThat(details.description()).isEqualTo("Water the plants");
        assertThat(details.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(details.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(details.dueDate()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsBlankDescriptions(String description) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TaskDetails.of(description))
                .withMessage("Task description cannot be empty");
    }

    @Test
    void rejectsOverlongDescriptions() {
        String atLimit = "x".repeat(Task.MAX_DESCRIPTION_LENGTH);
        assertThat(TaskDetails.of(atLimit).description()).hasSize(Task.MAX_DESCRIPTION_LENGTH);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> TaskDetails.of(atLimit + "x"))
                .withMessageContaining("longer than " + Task.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void changesOnlyOverrideTheFieldsTheySet() {
        LocalDate due = LocalDate.of(2026, 3, 9);
        TaskDetails current = new TaskDetails("Write report", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, due);

        TaskDetails changed = TaskChanges.status(TaskStatus.COMPLETED).applyTo(current);

        assertThat(changed).isEqualTo(new TaskDetails("Write report", TaskStatus.COMPLETED, TaskPriority.HIGH, due));
    }

    @Test
    void overdueOnlyAppliesToUnfinishedTasksPastTheirDueDate() {
        LocalDate today = LocalDate.of(2026, 3, 2);
        Instant now = Instant.EPOCH;

        assertThat(new Task(new TaskDetails("late", null, null, today.minusDays(1)), now).isOverdue(today))
                .isTrue();
        assertThat(new Task(new TaskDetails("due today", null, null, today), now).isOverdue(today))
                .isFalse();
        assertThat(new Task(TaskDetails.of("no date"), now).isOverdue(today)).isFalse();
        assertThat(new Task(new TaskDetails("done", TaskStatus.COMPLETED, null, today.minusDays(1)), now)
                        .isOverdue(today))
                .isFalse();
    }
}
