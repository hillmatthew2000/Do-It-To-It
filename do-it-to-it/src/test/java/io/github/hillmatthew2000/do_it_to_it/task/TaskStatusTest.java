package io.github.hillmatthew2000.do_it_to_it.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaskStatusTest {

    @ParameterizedTest
    @CsvSource({
        "pending, PENDING",
        "Pending, PENDING",
        "in progress, IN_PROGRESS",
        "In-Progress, IN_PROGRESS",
        "IN_PROGRESS, IN_PROGRESS",
        "'  completed ', COMPLETED"
    })
    void parsesStatusesLeniently(String input, TaskStatus expected) {
        assertThat(TaskStatus.parse(input)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "done", "inprogress"})
    void rejectsUnknownStatuses(String input) {
        assertThat(TaskStatus.parse(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"low, LOW", "' High ', HIGH"})
    void parsesPriorities(String input, TaskPriority expected) {
        assertThat(TaskPriority.parse(input)).contains(expected);
    }
}
