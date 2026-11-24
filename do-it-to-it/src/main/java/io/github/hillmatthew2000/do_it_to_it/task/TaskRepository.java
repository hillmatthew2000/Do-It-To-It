package io.github.hillmatthew2000.do_it_to_it.task;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Task}s. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Finds tasks matching the optional filters.
     *
     * @param status only tasks with this status, or {@code null} for any status
     * @param descriptionPattern a lower-case SQL {@code LIKE} pattern escaped with {@code \}, or
     *     {@code null} to match every description
     */
    @Query("""
            select t from Task t
            where (:status is null or t.status = :status)
              and (:pattern is null or lower(t.description) like :pattern escape '\\')
            """)
    List<Task> search(@Param("status") TaskStatus status, @Param("pattern") String descriptionPattern);

    long countByStatus(TaskStatus status);

    long countByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

    List<Task> findByStatus(TaskStatus status);
}
