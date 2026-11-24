package io.github.hillmatthew2000.do_it_to_it.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** A single to-do item. Instances are created and modified through {@link TaskService}. */
@Entity
@Table(name = "task")
public class Task {

    public static final int MAX_DESCRIPTION_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Task() {
        // for JPA
    }

    Task(TaskDetails details, Instant now) {
        apply(details);
        this.createdAt = now;
        this.updatedAt = now;
    }

    void update(TaskDetails details, Instant now) {
        apply(details);
        this.updatedAt = now;
    }

    private void apply(TaskDetails details) {
        this.description = details.description();
        this.status = details.status();
        this.priority = details.priority();
        this.dueDate = details.dueDate();
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TaskDetails details() {
        return new TaskDetails(description, status, priority, dueDate);
    }

    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    /** A task is overdue when it is not completed and its due date is before {@code today}. */
    public boolean isOverdue(LocalDate today) {
        return dueDate != null && !isCompleted() && dueDate.isBefore(today);
    }

    @Override
    public String toString() {
        return "Task[id=%d, status=%s, priority=%s, dueDate=%s, description=%s]"
                .formatted(id, status, priority, dueDate, description);
    }
}
