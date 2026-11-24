package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskSort;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST API for tasks. Errors are rendered as RFC 9457 problem details by {@link ApiExceptionHandler}.
 *
 * <p>Changes to a single task are optionally conditional: a client that sends the task's ETag in
 * {@code If-Match} gets {@code 412 Precondition Failed} instead of overwriting a change made since it
 * loaded the task.
 */
@RestController
@RequestMapping("/api/todos")
@Tag(name = "Tasks")
public class TaskController {

    private static final String IF_MATCH_DESCRIPTION =
            "The task's ETag. If given, the change is only made while the task is still at that version; otherwise"
                    + " the response is 412 Precondition Failed.";

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping
    @Operation(summary = "List tasks", description = "Optionally filtered by status and description text.")
    public List<TaskResponse> list(
            @Parameter(description = "Only tasks with this status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Case-insensitive text the description must contain")
                    @RequestParam(required = false)
                    String q,
            @Parameter(description = "Result order") @RequestParam(defaultValue = "OLDEST") TaskSort sort) {
        return tasks.list(status, q, sort).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a task",
            description = "The ETag header holds the task's version; send it back in If-Match to update safely.")
    public ResponseEntity<TaskResponse> get(@PathVariable long id) {
        return withETag(tasks.get(id));
    }

    @PostMapping
    @Operation(
            summary = "Create a task",
            description = "Omitted fields default to pending, medium priority, no due date.")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
        Task task = tasks.create(request.toDetails());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(task.getId())
                .toUri();
        return ResponseEntity.created(location).eTag(TaskETags.of(task)).body(toResponse(task));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task", description = "Sets every field; omitted ones take their defaults.")
    public ResponseEntity<TaskResponse> replace(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) @Parameter(description = IF_MATCH_DESCRIPTION)
                    String ifMatch,
            @Valid @RequestBody TaskRequest request) {
        return withETag(tasks.replace(id, request.toDetails(), TaskETags.expectedVersion(ifMatch)));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update some fields of a task",
            description = "Fields that are absent or null are left as they are.")
    public ResponseEntity<TaskResponse> update(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) @Parameter(description = IF_MATCH_DESCRIPTION)
                    String ifMatch,
            @Valid @RequestBody TaskPatchRequest request) {
        return withETag(tasks.update(id, request.toChanges(), TaskETags.expectedVersion(ifMatch)));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a task completed")
    public ResponseEntity<TaskResponse> complete(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) @Parameter(description = IF_MATCH_DESCRIPTION)
                    String ifMatch) {
        return withETag(tasks.complete(id, TaskETags.expectedVersion(ifMatch)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(
            @PathVariable long id,
            @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) @Parameter(description = IF_MATCH_DESCRIPTION)
                    String ifMatch) {
        tasks.delete(id, TaskETags.expectedVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/completed")
    @Operation(summary = "Delete every completed task", description = "Returns how many were deleted.")
    public Map<String, Integer> deleteCompleted() {
        return Map.of("deleted", tasks.deleteCompleted());
    }

    @GetMapping("/stats")
    @Operation(summary = "Count tasks by state")
    public TaskStatsResponse stats() {
        return TaskStatsResponse.from(tasks.stats());
    }

    private ResponseEntity<TaskResponse> withETag(Task task) {
        return ResponseEntity.ok().eTag(TaskETags.of(task)).body(toResponse(task));
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.from(task, tasks.today());
    }
}
