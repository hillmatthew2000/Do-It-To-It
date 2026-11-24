package io.github.hillmatthew2000.do_it_to_it.console;

import io.github.hillmatthew2000.do_it_to_it.task.ExpectedVersion;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskNotFoundException;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskSort;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStats;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import io.github.hillmatthew2000.do_it_to_it.task.TaskVersionConflictException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Menu-driven command-line interface over {@link TaskService}. */
public class TodoConsole {

    private static final String MENU = """
            What would you like to do?
              1  List tasks
              2  Add a task
              3  Edit a task
              4  Mark a task completed
              5  Delete a task
              6  Search tasks
              7  Clear completed tasks
              0  Exit""";

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
        out.println("Do-It-To-It");
        out.println("===========");
        printSummary();
        try {
            boolean running = true;
            while (running) {
                out.println();
                out.println(MENU);
                running = handleMenuChoice(prompt("> "));
            }
        } catch (EndOfInputException e) {
            // Input closed (Ctrl+D / Ctrl+Z or end of a piped script): exit cleanly.
            out.println();
        }
        out.println("Goodbye!");
    }

    private boolean handleMenuChoice(String choice) {
        switch (choice.toLowerCase(Locale.ROOT)) {
            case "1", "l", "list" -> listTasks();
            case "2", "a", "add" -> addTask();
            case "3", "e", "edit" -> editTask();
            case "4", "c", "complete", "done" -> completeTask();
            case "5", "d", "delete" -> deleteTask();
            case "6", "s", "search" -> searchTasks();
            case "7", "clear" -> clearCompleted();
            case "0", "q", "quit", "exit" -> {
                return false;
            }
            case "" -> {
                // Just show the menu again.
            }
            default -> out.println("Unknown option '" + choice + "'. Enter a number from the menu.");
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------
    // Menu actions
    // -------------------------------------------------------------------------------------------

    private void listTasks() {
        List<Task> all = tasks.listAll();
        if (all.isEmpty()) {
            out.println("No tasks yet. Choose 2 to add one.");
            return;
        }
        printTable(all);
        printSummary();
    }

    private void searchTasks() {
        String query = prompt("Search for: ");
        if (query.isEmpty()) {
            out.println("Search cancelled.");
            return;
        }
        List<Task> matches = tasks.list(null, query, TaskSort.OLDEST);
        if (matches.isEmpty()) {
            out.println("No tasks contain '" + query + "'.");
            return;
        }
        printTable(matches);
        out.println(matches.size() == 1 ? "1 matching task." : matches.size() + " matching tasks.");
    }

    private void addTask() {
        String description = prompt("Description: ");
        if (description.isEmpty()) {
            out.println("Nothing added: the description cannot be empty.");
            return;
        }
        TaskPriority priority = promptForPriority("Priority [low/MEDIUM/high]: ", TaskPriority.MEDIUM);
        LocalDate dueDate = promptForDueDate("Due date (yyyy-mm-dd, 'today', 'tomorrow', blank for none): ", null);
        try {
            Task task = tasks.create(new TaskDetails(description, null, priority, dueDate));
            out.println("Added task " + task.getId() + ".");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void editTask() {
        Optional<Task> found = promptForTask("ID of the task to edit: ");
        if (found.isEmpty()) {
            return;
        }
        Task task = found.get();
        TaskDetails current = task.details();
        printTable(List.of(task));
        out.println("Press Enter to keep the current value.");

        String description = prompt("Description [" + current.description() + "]: ");
        TaskStatus status = promptForChoice(
                "Status [" + current.status().label() + "] (pending/in progress/completed): ",
                current.status(),
                TaskStatus::parse);
        TaskPriority priority = promptForPriority(
                "Priority [" + current.priority().label() + "] (low/medium/high): ", current.priority());
        LocalDate dueDate = promptForDueDate(
                "Due date [" + (current.dueDate() == null ? "none" : current.dueDate()) + "] ('none' to clear): ",
                current.dueDate());

        try {
            TaskDetails updated = new TaskDetails(
                    description.isEmpty() ? current.description() : description, status, priority, dueDate);
            if (updated.equals(current)) {
                out.println("No changes made.");
                return;
            }
            tasks.replace(task.getId(), updated, ExpectedVersion.of(task.getVersion()));
            out.println("Updated task " + task.getId() + ".");
        } catch (IllegalArgumentException e) {
            out.println("Error: " + e.getMessage());
        } catch (TaskVersionConflictException | TaskNotFoundException e) {
            reportChangedElsewhere(task, "your edits were not saved");
        }
    }

    private void completeTask() {
        Optional<Task> task = promptForTask("ID of the task to mark completed: ");
        if (task.isEmpty()) {
            return;
        }
        if (task.get().isCompleted()) {
            out.println("Task " + task.get().getId() + " is already completed.");
            return;
        }
        try {
            tasks.complete(task.get().getId(), ExpectedVersion.of(task.get().getVersion()));
            out.println("Marked task " + task.get().getId() + " as completed.");
        } catch (TaskVersionConflictException | TaskNotFoundException e) {
            reportChangedElsewhere(task.get(), "it was not marked completed");
        }
    }

    private void deleteTask() {
        Optional<Task> task = promptForTask("ID of the task to delete: ");
        if (task.isEmpty()) {
            return;
        }
        printTable(List.of(task.get()));
        if (confirm("Delete this task?")) {
            try {
                tasks.delete(task.get().getId(), ExpectedVersion.of(task.get().getVersion()));
                out.println("Deleted task " + task.get().getId() + ".");
            } catch (TaskNotFoundException e) {
                out.println("Task " + task.get().getId() + " had already been deleted elsewhere.");
            } catch (TaskVersionConflictException e) {
                reportChangedElsewhere(task.get(), "it was not deleted");
            }
        } else {
            out.println("Nothing deleted.");
        }
    }

    private void clearCompleted() {
        long completed = tasks.stats().completed();
        if (completed == 0) {
            out.println("There are no completed tasks to clear.");
            return;
        }
        if (confirm("Delete " + plural(completed, "completed task") + "?")) {
            int deleted = tasks.deleteCompleted();
            out.println("Deleted " + plural(deleted, "completed task") + ".");
        } else {
            out.println("Nothing deleted.");
        }
    }

    // -------------------------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------------------------

    private void printTable(List<Task> rows) {
        out.println();
        TaskTable.render(rows, tasks.today()).forEach(out::println);
    }

    /**
     * The task was changed or deleted (for example in the web app) after this console read it, so the
     * change was refused rather than overwriting someone else's work.
     */
    private void reportChangedElsewhere(Task stale, String outcome) {
        long id = stale.getId();
        Optional<Task> current = tasks.find(id);
        if (current.isEmpty()) {
            out.println("Task " + id + " was deleted elsewhere in the meantime, so " + outcome + ".");
            return;
        }
        out.println("Task " + id + " was changed elsewhere in the meantime, so " + outcome + ". It now reads:");
        printTable(List.of(current.get()));
    }

    private void printSummary() {
        TaskStats stats = tasks.stats();
        if (stats.total() == 0) {
            out.println("You have no tasks.");
            return;
        }
        StringBuilder summary = new StringBuilder()
                .append(plural(stats.total(), "task"))
                .append(": ")
                .append(stats.pending())
                .append(" pending, ")
                .append(stats.inProgress())
                .append(" in progress, ")
                .append(stats.completed())
                .append(" completed");
        if (stats.overdue() > 0) {
            summary.append(" (").append(stats.overdue()).append(" overdue)");
        }
        out.println(summary.append('.'));
    }

    private static String plural(long count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    // -------------------------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------------------------

    private Optional<Task> promptForTask(String message) {
        if (tasks.stats().total() == 0) {
            out.println("There are no tasks yet.");
            return Optional.empty();
        }
        String input = prompt(message);
        long id;
        try {
            id = Long.parseLong(input);
        } catch (NumberFormatException e) {
            out.println("'" + input + "' is not a task ID. Choose 1 to list tasks and their IDs.");
            return Optional.empty();
        }
        try {
            return Optional.of(tasks.get(id));
        } catch (TaskNotFoundException e) {
            out.println("There is no task with ID " + id + ".");
            return Optional.empty();
        }
    }

    private TaskPriority promptForPriority(String message, TaskPriority current) {
        return promptForChoice(message, current, TaskPriority::parse);
    }

    /** Asks until the answer parses; a blank answer keeps {@code current}. */
    private <T> T promptForChoice(String message, T current, Function<String, Optional<T>> parser) {
        while (true) {
            String input = prompt(message);
            if (input.isEmpty()) {
                return current;
            }
            Optional<T> parsed = parser.apply(input);
            if (parsed.isPresent()) {
                return parsed.get();
            }
            out.println("'" + input + "' is not one of the options. Try again, or press Enter to skip.");
        }
    }

    /** Asks until the answer is a date; a blank answer keeps {@code current} and "none" clears it. */
    private LocalDate promptForDueDate(String message, LocalDate current) {
        while (true) {
            String input = prompt(message).toLowerCase(Locale.ROOT);
            switch (input) {
                case "" -> {
                    return current;
                }
                case "none", "-" -> {
                    return null;
                }
                case "today" -> {
                    return tasks.today();
                }
                case "tomorrow" -> {
                    return tasks.today().plusDays(1);
                }
                default -> {
                    try {
                        return LocalDate.parse(input);
                    } catch (DateTimeParseException e) {
                        out.println("'" + input + "' is not a date. Use the format yyyy-mm-dd, e.g. 2026-12-31.");
                    }
                }
            }
        }
    }

    private boolean confirm(String question) {
        String answer = prompt(question + " [y/N]: ").toLowerCase(Locale.ROOT);
        return answer.equals("y") || answer.equals("yes");
    }

    private String prompt(String message) {
        out.print(message);
        out.flush();
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
