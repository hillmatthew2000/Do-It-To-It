package io.github.hillmatthew2000.do_it_to_it;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Java To-Do console application.
 * Provides a command-line interface for managing tasks.
 */
@SpringBootApplication
public class DoItToItApplication {
    private static ToDoManager todoManager;
    private static Scanner scanner;
    
    public static void main(String[] args) {
        // Check if running in Spring Boot context or console mode
        if (args.length > 0 && args[0].equals("--spring")) {
            SpringApplication.run(DoItToItApplication.class, args);
        } else {
            runConsoleApplication();
        }
    }
    
    /**
     * Runs the console-based to-do application.
     */
    private static void runConsoleApplication() {
        todoManager = new ToDoManager();
        scanner = new Scanner(System.in);
        
        System.out.println("Welcome to the Java To-Do App!");
        System.out.println("================================");
        
        boolean running = true;
        while (running) {
            try {
                displayMenu();
                int choice = getUserChoice();
                running = handleMenuChoice(choice);
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }
        
        scanner.close();
        System.out.println("Exiting the application. Goodbye!");
    }
    
    /**
     * Displays the main menu options to the user.
     */
    private static void displayMenu() {
        System.out.println("\nChoose an option:");
        System.out.println("1. Add Task");
        System.out.println("2. List Tasks");
        System.out.println("3. Update Task");
        System.out.println("4. Delete Task");
        System.out.println("5. Mark Task Completed");
        System.out.println("6. Exit");
        System.out.print("> ");
    }
    
    /**
     * Gets and validates the user's menu choice.
     * 
     * @return The user's menu choice as an integer
     */
    private static int getUserChoice() {
        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline character
            return choice;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear the invalid input
            System.out.println("Invalid input. Please enter a number between 1 and 6.");
            return -1; // Return invalid choice to trigger menu redisplay
        }
    }
    
    /**
     * Handles the user's menu choice and executes the corresponding action.
     * 
     * @param choice The user's menu choice
     * @return false if the user chooses to exit, true otherwise
     */
    private static boolean handleMenuChoice(int choice) {
        switch (choice) {
            case 1:
                addTask();
                break;
            case 2:
                listTasks();
                break;
            case 3:
                updateTask();
                break;
            case 4:
                deleteTask();
                break;
            case 5:
                markTaskCompleted();
                break;
            case 6:
                return false; // Exit the application
            default:
                System.out.println("Invalid option. Please choose a number between 1 and 6.");
        }
        return true;
    }
    
    /**
     * Handles adding a new task.
     */
    private static void addTask() {
        System.out.print("Enter task description: ");
        String description = scanner.nextLine().trim();
        
        if (description.isEmpty()) {
            System.out.println("Task description cannot be empty. Please try again.");
            return;
        }
        
        try {
            int taskId = todoManager.addTask(description);
            System.out.println("Task added with ID " + taskId + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handles listing all tasks.
     */
    private static void listTasks() {
        List<Task> tasks = todoManager.getAllTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks found. Add some tasks to get started!");
            return;
        }
        
        System.out.println("\n--- Your Tasks ---");
        for (Task task : tasks) {
            System.out.println(task.toString());
        }
        System.out.println("Total tasks: " + tasks.size());
    }
    
    /**
     * Handles updating a task's description or status.
     */
    private static void updateTask() {
        if (todoManager.isEmpty()) {
            System.out.println("No tasks available to update.");
            return;
        }
        
        int taskId = getValidTaskId("Enter task ID to update: ");
        if (taskId == -1) return; // Invalid input
        
        Task task = todoManager.findTaskById(taskId);
        if (task == null) {
            System.out.println("Task with ID " + taskId + " not found.");
            return;
        }
        
        System.out.println("Current task: " + task.toString());
        System.out.println("What would you like to update?");
        System.out.println("1. Description");
        System.out.println("2. Status");
        System.out.print("> ");
        
        try {
            int updateChoice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            switch (updateChoice) {
                case 1:
                    updateTaskDescription(taskId);
                    break;
                case 2:
                    updateTaskStatus(taskId);
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1 or 2.");
            }
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear invalid input
            System.out.println("Invalid input. Please enter 1 or 2.");
        }
    }
    
    /**
     * Updates a task's description.
     * 
     * @param taskId The ID of the task to update
     */
    private static void updateTaskDescription(int taskId) {
        System.out.print("Enter new description: ");
        String newDescription = scanner.nextLine().trim();
        
        if (newDescription.isEmpty()) {
            System.out.println("Description cannot be empty.");
            return;
        }
        
        try {
            if (todoManager.updateTaskDescription(taskId, newDescription)) {
                System.out.println("Task description updated successfully.");
            } else {
                System.out.println("Failed to update task description.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Updates a task's status.
     * 
     * @param taskId The ID of the task to update
     */
    private static void updateTaskStatus(int taskId) {
        System.out.print("Enter new status (e.g., Pending, In Progress, Completed): ");
        String newStatus = scanner.nextLine().trim();
        
        if (newStatus.isEmpty()) {
            System.out.println("Status cannot be empty.");
            return;
        }
        
        try {
            if (todoManager.updateTaskStatus(taskId, newStatus)) {
                System.out.println("Task status updated successfully.");
            } else {
                System.out.println("Failed to update task status.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    /**
     * Handles deleting a task.
     */
    private static void deleteTask() {
        if (todoManager.isEmpty()) {
            System.out.println("No tasks available to delete.");
            return;
        }
        
        int taskId = getValidTaskId("Enter task ID to delete: ");
        if (taskId == -1) return; // Invalid input
        
        Task task = todoManager.findTaskById(taskId);
        if (task == null) {
            System.out.println("Task with ID " + taskId + " not found.");
            return;
        }
        
        System.out.println("Are you sure you want to delete this task?");
        System.out.println(task.toString());
        System.out.print("Enter 'yes' to confirm: ");
        String confirmation = scanner.nextLine().trim().toLowerCase();
        
        if (confirmation.equals("yes")) {
            if (todoManager.deleteTask(taskId)) {
                System.out.println("Task " + taskId + " deleted successfully.");
            } else {
                System.out.println("Failed to delete task.");
            }
        } else {
            System.out.println("Task deletion cancelled.");
        }
    }
    
    /**
     * Handles marking a task as completed.
     */
    private static void markTaskCompleted() {
        if (todoManager.isEmpty()) {
            System.out.println("No tasks available to mark as completed.");
            return;
        }
        
        int taskId = getValidTaskId("Enter task ID to mark completed: ");
        if (taskId == -1) return; // Invalid input
        
        Task task = todoManager.findTaskById(taskId);
        if (task == null) {
            System.out.println("Task with ID " + taskId + " not found.");
            return;
        }
        
        if (task.getStatus().equalsIgnoreCase("Completed")) {
            System.out.println("Task " + taskId + " is already completed.");
            return;
        }
        
        if (todoManager.markTaskCompleted(taskId)) {
            System.out.println("Task " + taskId + " marked as completed.");
        } else {
            System.out.println("Failed to mark task as completed.");
        }
    }
    
    /**
     * Gets and validates a task ID from user input.
     * 
     * @param prompt The prompt to display to the user
     * @return The valid task ID, or -1 if input is invalid
     */
    private static int getValidTaskId(String prompt) {
        System.out.print(prompt);
        try {
            int taskId = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            if (taskId <= 0) {
                System.out.println("Task ID must be a positive number.");
                return -1;
            }
            
            return taskId;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear invalid input
            System.out.println("Invalid input. Please enter a valid task ID (positive number).");
            return -1;
        }
    }
}
