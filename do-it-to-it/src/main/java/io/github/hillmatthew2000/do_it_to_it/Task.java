package io.github.hillmatthew2000.do_it_to_it;

/**
 * Represents a task in the To-Do application.
 * Each task has a unique ID, description, and status.
 */
public class Task {
    private int id;
    private String description;
    private String status;
    
    // Static counter to auto-generate unique IDs
    private static int nextId = 1;
    
    /**
     * Constructor to create a new task with a description.
     * The task is assigned a unique ID and default status of "Pending".
     * 
     * @param description The description of the task
     */
    public Task(String description) {
        this.id = nextId++;
        this.description = description;
        this.status = "Pending";
    }
    
    /**
     * Constructor to create a task with specific ID, description, and status.
     * Used for updating existing tasks.
     * 
     * @param id The task ID
     * @param description The task description
     * @param status The task status
     */
    public Task(int id, String description, String status) {
        this.id = id;
        this.description = description;
        this.status = status;
    }
    
    // Getter methods
    public int getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getStatus() {
        return status;
    }
    
    // Setter methods
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Marks the task as completed.
     */
    public void markCompleted() {
        this.status = "Completed";
    }
    
    /**
     * Returns a string representation of the task.
     * 
     * @return Formatted string with ID, description, and status
     */
    @Override
    public String toString() {
        return String.format("ID: %d | Description: %s | Status: %s", id, description, status);
    }
    
    /**
     * Static method to set the next ID counter.
     * Useful when loading tasks from storage to avoid ID conflicts.
     * 
     * @param nextId The next ID to use
     */
    public static void setNextId(int nextId) {
        Task.nextId = nextId;
    }
    
    /**
     * Static method to get the current next ID value.
     * 
     * @return The next ID that will be assigned
     */
    public static int getNextId() {
        return nextId;
    }
}