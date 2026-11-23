package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
        return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    public Task create(TaskDetails details) {
        return repository.save(new Task(details, clock.instant()));
    }

    /** Replaces every editable field of the task. */
    public Task replace(long id, TaskDetails details) {
        Task task = get(id);
        task.update(details, clock.instant());
        return task;
    }

    /** Applies the non-null fields of {@code changes} to the task. */
    public Task update(long id, TaskChanges changes) {
        Task task = get(id);
        task.update(changes.applyTo(task.details()), clock.instant());
        return task;
    }

    /** Marks the task as completed. Completing an already completed task is a no-op. */
    public Task complete(long id) {
        Task task = get(id);
        if (!task.isCompleted()) {
            task.update(TaskChanges.status(TaskStatus.COMPLETED).applyTo(task.details()), clock.instant());
        }
        return task;
    }

    public void delete(long id) {
        repository.delete(get(id));
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
