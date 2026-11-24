package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for managing tasks. Shared by the REST API and the console. */
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;
    private final Clock clock;

    public TaskService(TaskRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Lists tasks.
     *
     * @param status only tasks with this status, or {@code null} for all
     * @param query case-insensitive text the description must contain, or {@code null}/blank for all
     * @param sort result ordering
     */
    @Transactional(readOnly = true)
    public List<Task> list(TaskStatus status, String query, TaskSort sort) {
        List<Task> tasks = repository.search(status, toLikePattern(query));
        return tasks.stream().sorted(sort.comparator()).toList();
    }

    @Transactional(readOnly = true)
    public List<Task> listAll() {
        return list(null, null, TaskSort.OLDEST);
    }

    @Transactional(readOnly = true)
    public Task get(long id) {
        return find(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Optional<Task> find(long id) {
        return repository.findById(id);
    }

    public Task create(TaskDetails details) {
        return repository.save(new Task(details, clock.instant()));
    }

    /**
     * Adds tasks restored from an export, keeping their original timestamps where known so that they
     * sort as they did before. Either every task is added or, if one is invalid, none is.
     *
     * @return how many tasks were added
     */
    public int importAll(List<ImportedTask> imported) {
        Instant now = clock.instant();
        List<Task> created = imported.stream()
                .map(task -> {
                    Instant createdAt = task.createdAt() != null ? task.createdAt() : now;
                    Instant updatedAt = task.updatedAt() != null ? task.updatedAt() : createdAt;
                    return new Task(task.details(), createdAt, updatedAt);
                })
                .toList();
        return repository.saveAll(created).size();
    }

    /** Replaces every editable field of the task. */
    public Task replace(long id, TaskDetails details, ExpectedVersion expected) {
        Task task = getForChange(id, expected);
        task.update(details, clock.instant());
        return flush(task);
    }

    /** Applies the non-null fields of {@code changes} to the task. */
    public Task update(long id, TaskChanges changes, ExpectedVersion expected) {
        Task task = getForChange(id, expected);
        task.update(changes.applyTo(task.details()), clock.instant());
        return flush(task);
    }

    /** Marks the task as completed. Completing an already completed task is a no-op. */
    public Task complete(long id, ExpectedVersion expected) {
        Task task = getForChange(id, expected);
        if (!task.isCompleted()) {
            task.update(TaskChanges.status(TaskStatus.COMPLETED).applyTo(task.details()), clock.instant());
        }
        return flush(task);
    }

    public void delete(long id, ExpectedVersion expected) {
        Task task = getForChange(id, expected);
        repository.delete(task);
        flush(task);
    }

    /** Deletes every completed task and returns how many were removed. */
    public int deleteCompleted() {
        List<Task> completed = repository.findByStatus(TaskStatus.COMPLETED);
        repository.deleteAll(completed);
        return completed.size();
    }

    @Transactional(readOnly = true)
    public TaskStats stats() {
        return new TaskStats(
                repository.count(),
                repository.countByStatus(TaskStatus.PENDING),
                repository.countByStatus(TaskStatus.IN_PROGRESS),
                repository.countByStatus(TaskStatus.COMPLETED),
                repository.countByDueDateBeforeAndStatusNot(today(), TaskStatus.COMPLETED));
    }

    private Task getForChange(long id, ExpectedVersion expected) {
        Task task = get(id);
        if (!expected.matches(task.getVersion())) {
            throw new TaskVersionConflictException(id);
        }
        return task;
    }

    /**
     * Writes the change now rather than at commit, so that a concurrent change that slipped in after
     * {@link #getForChange} is reported the same way as a stale expected version.
     */
    private Task flush(Task task) {
        try {
            repository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw new TaskVersionConflictException(task.getId(), e);
        }
        return task;
    }

    /** The current date in the application's clock zone, used to decide whether tasks are overdue. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    static String toLikePattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String escaped = query.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
