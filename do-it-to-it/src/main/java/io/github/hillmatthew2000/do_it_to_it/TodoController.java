package io.github.hillmatthew2000.do_it_to_it;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Singleton ToDoManager to ensure data consistency across the application.
 */
@Component
class SingletonToDoManager {
    private static ToDoManager instance;
    
    public static synchronized ToDoManager getInstance() {
        if (instance == null) {
            instance = new ToDoManager();
            // Add some sample tasks for demonstration
            instance.addTask("Learn Spring Boot");
            instance.addTask("Build a REST API");
            instance.addTask("Deploy the application");
        }
        return instance;
    }
}

/**
 * REST Controller for the To-Do application.
 * Provides web endpoints for managing tasks via HTTP requests.
 */
@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
public class TodoController {
    
    private final ToDoManager todoManager;
    
    public TodoController() {
        this.todoManager = SingletonToDoManager.getInstance();
    }
    
    /**
     * Get all tasks
     * GET /api/todos
     */
    @GetMapping
    public List<Task> getAllTasks() {
        return todoManager.getAllTasks();
    }
    
    /**
     * Get a specific task by ID
     * GET /api/todos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable int id) {
        Task task = todoManager.findTaskById(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Create a new task
     * POST /api/todos
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Description is required"));
        }
        
        try {
            int taskId = todoManager.addTask(description.trim());
            Task newTask = todoManager.findTaskById(taskId);
            return ResponseEntity.ok(Map.of(
                "message", "Task created successfully",
                "taskId", taskId,
                "task", newTask
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update a task's description
     * PUT /api/todos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable int id, @RequestBody Map<String, String> request) {
        String description = request.get("description");
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Description is required"));
        }
        
        try {
            boolean updated = todoManager.updateTaskDescription(id, description.trim());
            if (updated) {
                Task updatedTask = todoManager.findTaskById(id);
                return ResponseEntity.ok(Map.of(
                    "message", "Task updated successfully",
                    "task", updatedTask
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark a task as completed
     * PATCH /api/todos/{id}/complete
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Map<String, Object>> markTaskCompleted(@PathVariable int id) {
        boolean updated = todoManager.markTaskCompleted(id);
        if (updated) {
            Task updatedTask = todoManager.findTaskById(id);
            return ResponseEntity.ok(Map.of(
                "message", "Task marked as completed",
                "task", updatedTask
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete a task
     * DELETE /api/todos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable int id) {
        boolean deleted = todoManager.deleteTask(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get application statistics
     * GET /api/todos/stats
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Task> allTasks = todoManager.getAllTasks();
        long completedTasks = allTasks.stream()
            .filter(task -> "Completed".equalsIgnoreCase(task.getStatus()))
            .count();
        
        return Map.of(
            "totalTasks", allTasks.size(),
            "completedTasks", completedTasks,
            "pendingTasks", allTasks.size() - completedTasks
        );
    }
}