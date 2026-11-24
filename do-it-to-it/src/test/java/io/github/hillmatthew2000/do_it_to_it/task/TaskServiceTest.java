package io.github.hillmatthew2000.do_it_to_it.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hillmatthew2000.do_it_to_it.MutableClock;
import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TaskService.class, TestClockConfig.class})
class TaskServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    @Autowired
    TaskService service;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        // The clock bean is shared by every test that reuses this cached context.
        clock.set(TestClockConfig.START);
    }

    @Test
    void createsTasksWithDefaultsAndTimestamps() {
        Task task = service.create(TaskDetails.of("Buy milk"));

        assertThat(task.getId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getCreatedAt()).isEqualTo(TestClockConfig.START);
        assertThat(task.getUpdatedAt()).isEqualTo(TestClockConfig.START);
        assertThat(service.get(task.getId()).getDescription()).isEqualTo("Buy milk");
    }

    @Test
    void getThrowsForUnknownIds() {
        assertThatThrownBy(() -> service.get(404))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Task 404 does not exist");
    }

    @Test
    void replaceOverwritesEveryFieldAndBumpsUpdatedAt() {
        Task task = service.create(new TaskDetails("Draft", TaskStatus.PENDING, TaskPriority.HIGH, TODAY));
        clock.advance(Duration.ofMinutes(5));

        service.replace(task.getId(), TaskDetails.of("Final"));

        Task replaced = service.get(task.getId());
        assertThat(replaced.details()).isEqualTo(TaskDetails.of("Final"));
        assertThat(replaced.getCreatedAt()).isEqualTo(TestClockConfig.START);
        assertThat(replaced.getUpdatedAt()).isEqualTo(TestClockConfig.START.plus(Duration.ofMinutes(5)));
    }

    @Test
    void updateKeepsFieldsThatAreNotChanged() {
        Task task = service.create(new TaskDetails("Draft", TaskStatus.PENDING, TaskPriority.HIGH, TODAY));

        service.update(task.getId(), new TaskChanges("  Edited  ", null, null, null));

        assertThat(service.get(task.getId()).details())
                .isEqualTo(new TaskDetails("Edited", TaskStatus.PENDING, TaskPriority.HIGH, TODAY));
    }

    @Test
    void completeIsIdempotent() {
        Task task = service.create(TaskDetails.of("Ship it"));
        clock.advance(Duration.ofHours(1));
        service.complete(task.getId());
        clock.advance(Duration.ofHours(1));

        Task completed = service.complete(task.getId());

        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.getUpdatedAt()).isEqualTo(TestClockConfig.START.plus(Duration.ofHours(1)));
    }

    @Test
    void deleteRemovesTheTaskAndRejectsUnknownIds() {
        Task task = service.create(TaskDetails.of("Temporary"));

        service.delete(task.getId());

        assertThat(service.listAll()).isEmpty();
        assertThatThrownBy(() -> service.delete(task.getId())).isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void deleteCompletedRemovesOnlyCompletedTasks() {
        service.create(new TaskDetails("Done 1", TaskStatus.COMPLETED, null, null));
        service.create(new TaskDetails("Done 2", TaskStatus.COMPLETED, null, null));
        Task open = service.create(TaskDetails.of("Still open"));

        assertThat(service.deleteCompleted()).isEqualTo(2);
        assertThat(service.listAll()).extracting(Task::getId).containsExactly(open.getId());
    }

    @Test
    void listFiltersByStatusAndCaseInsensitiveText() {
        service.create(TaskDetails.of("Call the BANK"));
        service.create(new TaskDetails("Bank statement", TaskStatus.COMPLETED, null, null));
        service.create(TaskDetails.of("Groceries"));

        assertThat(service.list(null, "bank", TaskSort.OLDEST))
                .extracting(Task::getDescription)
                .containsExactly("Call the BANK", "Bank statement");
        assertThat(service.list(TaskStatus.PENDING, "bank", TaskSort.OLDEST))
                .extracting(Task::getDescription)
                .containsExactly("Call the BANK");
        assertThat(service.list(null, "  ", TaskSort.OLDEST)).hasSize(3);
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() {
        service.create(TaskDetails.of("Reach 100% coverage"));
        service.create(TaskDetails.of("Rename snake_case fields"));
        service.create(TaskDetails.of("Plain task"));

        assertThat(service.list(null, "%", TaskSort.OLDEST))
                .extracting(Task::getDescription)
                .containsExactly("Reach 100% coverage");
        assertThat(service.list(null, "_", TaskSort.OLDEST))
                .extracting(Task::getDescription)
                .containsExactly("Rename snake_case fields");
    }

    @Test
    void listSortsByTheRequestedOrder() {
        Task low = service.create(new TaskDetails("b low", null, TaskPriority.LOW, TODAY.plusDays(1)));
        clock.advance(Duration.ofMinutes(1));
        Task high = service.create(new TaskDetails("C high", null, TaskPriority.HIGH, null));
        clock.advance(Duration.ofMinutes(1));
        Task medium = service.create(new TaskDetails("a medium", null, TaskPriority.MEDIUM, TODAY));

        assertThat(service.list(null, null, TaskSort.OLDEST)).containsExactly(low, high, medium);
        assertThat(service.list(null, null, TaskSort.NEWEST)).containsExactly(medium, high, low);
        assertThat(service.list(null, null, TaskSort.PRIORITY)).containsExactly(high, medium, low);
        assertThat(service.list(null, null, TaskSort.DUE_DATE)).containsExactly(medium, low, high);
        assertThat(service.list(null, null, TaskSort.DESCRIPTION)).containsExactly(medium, low, high);
    }

    @Test
    void statsCountTasksByStatusAndOverdue() {
        service.create(new TaskDetails("Late", TaskStatus.PENDING, null, TODAY.minusDays(1)));
        service.create(new TaskDetails("Late but done", TaskStatus.COMPLETED, null, TODAY.minusDays(1)));
        service.create(new TaskDetails("Due today", TaskStatus.IN_PROGRESS, null, TODAY));
        service.create(TaskDetails.of("No date"));

        assertThat(service.stats()).isEqualTo(new TaskStats(4, 2, 1, 1, 1));
    }
}
