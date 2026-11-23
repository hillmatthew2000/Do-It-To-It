package io.github.hillmatthew2000.do_it_to_it.console;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskChanges;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskNotFoundException;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/** Menu-driven command-line interface over {@link TaskService}. */
public class TodoConsole {

    private final TaskService tasks;
    private final BufferedReader in;
    private final PrintStream out;

    public TodoConsole(TaskService tasks, BufferedReader in, PrintStream out) {
        this.tasks = tasks;
        this.in = in;
        this.out = out;
    }

    /** Runs the menu loop until the user exits or input ends. */
    public void run() {
        out.println("Welcome to the Java To-Do App!");
        out.println("================================");
        try {
            boolean running = true;
            while (running) {
                displayMenu();
                Optional<String> choice = readLine();
                running = choice.isPresent() && handleMenuChoice(choice.get());
            }
        } catch (EndOfInputException e) {
            // Input closed (Ctrl+D / Ctrl+Z or end of a piped script): exit cleanly.
        }
        out.println("Exiting the application. Goodbye!");
    }

    private void displayMenu() {
        out.println();
        out.println("Choose an option:");
        out.println("1. Add Task");
        out.println("2. List Tasks");
        out.println("3. Update Task");
        out.println("4. Delete Task");
        out.println("5. Mark Task Completed");
        out.println("6. Exit");
        out.print("> ");
    }

    private boolean handleMenuChoice(String choice) {
        switch (choice.trim()) {
            case "1" -> addTask();
            case "2" -> listTasks();
            case "3" -> updateTask();
            case "4" -> deleteTask();
            case "5" -> markTaskCompleted();
            case "6" -> {
                return false;
            }
            default -> out.println("Invalid option. Please choose a number between 1 and 6.");
        }
        return true;
    }

    private void addTask() {
        String description = prompt("Enter task description: ");
        try {
            Task task = tasks.create(TaskDetails.of(description));
            out.println("Task added with ID " + task.getId() + ".");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void listTasks() {
        List<Task> all = tasks.listAll();
        if (all.isEmpty()) {
            out.println("No tasks found. Add some tasks to get started!");
            return;
        }
        out.println();
        out.println("--- Your Tasks ---");
        all.forEach(task -> out.println(format(task)));
        out.println("Total tasks: " + all.size());
    }

    private void updateTask() {
        Optional<Task> task = promptForTask("Enter task ID to update: ");
        if (task.isEmpty()) {
            return;
        }
        out.println("Current task: " + format(task.get()));
        out.println("What would you like to update?");
        out.println("1. Description");
        out.println("2. Status");
        String choice = prompt("> ");
        try {
            switch (choice) {
                case "1" -> {
                    String description = prompt("Enter new description: ");
                    tasks.update(task.get().getId(), new TaskChanges(description, null, null, null));
                    out.println("Task description updated successfully.");
                }
                case "2" -> {
                    String input = prompt("Enter new status (Pending, In Progress, Completed): ");
                    Optional<TaskStatus> status = TaskStatus.parse(input);
                    if (status.isEmpty()) {
                        out.println("Unknown status '" + input + "'. Use Pending, In Progress or Completed.");
                        return;
                    }
                    tasks.update(task.get().getId(), TaskChanges.status(status.get()));
                    out.println("Task status updated successfully.");
                }
                default -> out.println("Invalid option. Please choose 1 or 2.");
            }
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void deleteTask() {
        Optional<Task> task = promptForTask("Enter task ID to delete: ");
        if (task.isEmpty()) {
            return;
        }
        out.println("Are you sure you want to delete this task?");
        out.println(format(task.get()));
        if (prompt("Enter 'yes' to confirm: ").equalsIgnoreCase("yes")) {
            tasks.delete(task.get().getId());
            out.println("Task " + task.get().getId() + " deleted successfully.");
        } else {
            out.println("Task deletion cancelled.");
        }
    }

    private void markTaskCompleted() {
        Optional<Task> task = promptForTask("Enter task ID to mark completed: ");
        if (task.isEmpty()) {
            return;
        }
        if (task.get().isCompleted()) {
            out.println("Task " + task.get().getId() + " is already completed.");
            return;
        }
        tasks.complete(task.get().getId());
        out.println("Task " + task.get().getId() + " marked as completed.");
    }

    private Optional<Task> promptForTask(String message) {
        if (tasks.listAll().isEmpty()) {
            out.println("No tasks available.");
            return Optional.empty();
        }
        String input = prompt(message);
        long id;
        try {
            id = Long.parseLong(input);
        } catch (NumberFormatException e) {
            out.println("Invalid input. Please enter a valid task ID (positive number).");
            return Optional.empty();
        }
        try {
            return Optional.of(tasks.get(id));
        } catch (TaskNotFoundException e) {
            out.println("Task with ID " + id + " not found.");
            return Optional.empty();
        }
    }

    private static String format(Task task) {
        return String.format(
                "ID: %d | Description: %s | Status: %s",
                task.getId(), task.getDescription(), task.getStatus().label());
    }

    private String prompt(String message) {
        out.print(message);
        return readLine().orElseThrow(EndOfInputException::new).trim();
    }

    private Optional<String> readLine() {
        try {
            return Optional.ofNullable(in.readLine());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class EndOfInputException extends RuntimeException {}
}
