package io.github.hillmatthew2000.do_it_to_it;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the collection of tasks in the To-Do application.
 * Provides methods to add, list, update, delete, and mark tasks as completed.
 */
public class ToDoManager {
    private List<Task> tasks;
    
    /**
     * Constructor initializes an empty list of tasks.
     */
    public ToDoManager() {
        this.tasks = new ArrayList<>();
    }
    
    /**
     * Adds a new task with the given description.
     * 
     * @param description The description of the task to add
     * @return The ID of the newly created task
     */
    public int addTask(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }
        
        Task newTask = new Task(description.trim());
        tasks.add(newTask);
        return newTask.getId();
    }
    
    /**
     * Returns a list of all tasks.
     * 
     * @return List of all tasks
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks); // Return a copy to prevent external modification
    }
    
    /**
     * Finds a task by its ID.
     * 
     * @param id The ID of the task to find
     * @return The task with the given ID, or null if not found
     */
    public Task findTaskById(int id) {
        for (Task task : tasks) {
            if (task.getId() == id) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * Updates the description of a task with the given ID.
     * 
     * @param id The ID of the task to update
     * @param newDescription The new description for the task
     * @return true if the task was updated successfully, false if task not found
     */
    public boolean updateTaskDescription(int id, String newDescription) {
        if (newDescription == null || newDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be empty");
        }
        
        Task task = findTaskById(id);
        if (task != null) {
            task.setDescription(newDescription.trim());
            return true;
        }
        return false;
    }
    
    /**
     * Updates the status of a task with the given ID.
     * 
     * @param id The ID of the task to update
     * @param newStatus The new status for the task
     * @return true if the task was updated successfully, false if task not found
     */
    public boolean updateTaskStatus(int id, String newStatus) {
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("Task status cannot be empty");
        }
        
        Task task = findTaskById(id);
        if (task != null) {
            task.setStatus(newStatus.trim());
            return true;
        }
        return false;
    }
    
    /**
     * Marks a task as completed by its ID.
     * 
     * @param id The ID of the task to mark as completed
     * @return true if the task was marked as completed successfully, false if task not found
     */
    public boolean markTaskCompleted(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            task.markCompleted();
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a task by its ID.
     * 
     * @param id The ID of the task to delete
     * @return true if the task was deleted successfully, false if task not found
     */
    public boolean deleteTask(int id) {
        Task task = findTaskById(id);
        if (task != null) {
            tasks.remove(task);
            return true;
        }
        return false;
    }
    
    /**
     * Returns the total number of tasks.
     * 
     * @return The number of tasks in the list
     */
    public int getTaskCount() {
        return tasks.size();
    }
    
    /**
     * Checks if the task list is empty.
     * 
     * @return true if there are no tasks, false otherwise
     */
    public boolean isEmpty() {
        return tasks.isEmpty();
    }
    
    /**
     * Clears all tasks from the list.
     */
    public void clearAllTasks() {
        tasks.clear();
    }
}