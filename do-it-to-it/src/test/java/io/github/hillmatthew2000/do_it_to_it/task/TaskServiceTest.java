package io.github.hillmatthew2000.do_it_to_it.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hillmatthew2000.do_it_to_it.MutableClock;
import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TaskService.class, TestClockConfig.class})
class TaskServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    @Autowired
    TaskService service;

    @Autowired
    MutableClock clock;

    @Autowired
    JdbcTemplate jdbc;

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

        service.replace(task.getId(), TaskDetails.of("Final"), ExpectedVersion.ANY);

        Task replaced = service.get(task.getId());
        assertThat(replaced.details()).isEqualTo(TaskDetails.of("Final"));
        assertThat(replaced.getCreatedAt()).isEqualTo(TestClockConfig.START);
        assertThat(replaced.getUpdatedAt()).isEqualTo(TestClockConfig.START.plus(Duration.ofMinutes(5)));
    }

    @Test
    void updateKeepsFieldsThatAreNotChanged() {
        Task task = service.create(new TaskDetails("Draft", TaskStatus.PENDING, TaskPriority.HIGH, TODAY));

        service.update(task.getId(), new TaskChanges("  Edited  ", null, null, null), ExpectedVersion.ANY);

        assertThat(service.get(task.getId()).details())
                .isEqualTo(new TaskDetails("Edited", TaskStatus.PENDING, TaskPriority.HIGH, TODAY));
    }

    @Test
    void completeIsIdempotent() {
        Task task = service.create(TaskDetails.of("Ship it"));
        clock.advance(Duration.ofHours(1));
        service.complete(task.getId(), ExpectedVersion.ANY);
        clock.advance(Duration.ofHours(1));

        Task completed = service.complete(task.getId(), ExpectedVersion.ANY);

        assertThat(completed.isCompleted()).isTrue();
        assertThat(completed.getUpdatedAt()).isEqualTo(TestClockConfig.START.plus(Duration.ofHours(1)));
    }

    @Test
    void deleteRemovesTheTaskAndRejectsUnknownIds() {
        Task task = service.create(TaskDetails.of("Temporary"));

        service.delete(task.getId(), ExpectedVersion.ANY);

        assertThat(service.listAll()).isEmpty();
        assertThatThrownBy(() -> service.delete(task.getId(), ExpectedVersion.ANY))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void everyChangeBumpsTheVersion() {
        Task task = service.create(TaskDetails.of("Draft"));
        assertThat(task.getVersion()).isZero();

        Task edited = service.update(task.getId(), new TaskChanges("Edited", null, null, null), ExpectedVersion.of(0));
        assertThat(edited.getVersion()).isEqualTo(1);

        Task completed = service.complete(task.getId(), ExpectedVersion.of(1));
        assertThat(completed.getVersion()).isEqualTo(2);

        // Completing a completed task changes nothing, so the version stays put.
        assertThat(service.complete(task.getId(), ExpectedVersion.of(2)).getVersion())
                .isEqualTo(2);
    }

    @Test
    void changesBasedOnAStaleVersionAreRefused() {
        Task task = service.create(TaskDetails.of("Draft"));
        service.replace(task.getId(), TaskDetails.of("Changed elsewhere"), ExpectedVersion.of(0));
        ExpectedVersion stale = ExpectedVersion.of(0);

        assertThatThrownBy(() -> service.replace(task.getId(), TaskDetails.of("Mine"), stale))
                .isInstanceOf(TaskVersionConflictException.class)
                .hasMessage("Task " + task.getId() + " has been changed since it was loaded");
        assertThatThrownBy(() -> service.update(task.getId(), TaskChanges.status(TaskStatus.COMPLETED), stale))
                .isInstanceOf(TaskVersionConflictException.class);
        assertThatThrownBy(() -> service.complete(task.getId(), stale))
                .isInstanceOf(TaskVersionConflictException.class);
        assertThatThrownBy(() -> service.delete(task.getId(), stale)).isInstanceOf(TaskVersionConflictException.class);

        Task current = service.get(task.getId());
        assertThat(current.getDescription()).isEqualTo("Changed elsewhere");
        assertThat(current.getVersion()).isEqualTo(1);
    }

    @Test
    void aConcurrentChangeBetweenReadingAndWritingIsReportedAsAConflict() {
        Task task = service.create(TaskDetails.of("Draft"));
        // Another connection changes the row after this transaction has read it.
        jdbc.update("update task set version = version + 1 where id = ?", task.getId());

        assertThatThrownBy(() -> service.replace(task.getId(), TaskDetails.of("Mine"), ExpectedVersion.ANY))
                .isInstanceOf(TaskVersionConflictException.class)
                .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void anyOfSeveralExpectedVersionsMayMatch() {
        Task task = service.create(TaskDetails.of("Draft"));

        service.delete(task.getId(), ExpectedVersion.oneOf(List.of(5L, 0L)));

        assertThat(service.find(task.getId())).isEmpty();
    }

    @Test
    void anUnknownTaskIsReportedAsMissingRatherThanChanged() {
        assertThatThrownBy(() -> service.delete(404, ExpectedVersion.of(0))).isInstanceOf(TaskNotFoundException.class);
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
