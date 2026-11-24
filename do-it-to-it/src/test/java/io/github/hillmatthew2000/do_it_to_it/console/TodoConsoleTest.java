package io.github.hillmatthew2000.do_it_to_it.console;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import io.github.hillmatthew2000.do_it_to_it.task.ExpectedVersion;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskChanges;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import io.github.hillmatthew2000.do_it_to_it.transfer.TaskTransferService;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({TaskService.class, TaskTransferService.class, TestClockConfig.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class TodoConsoleTest {

    /** The date of {@link TestClockConfig#START}. */
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    @Autowired
    TaskService service;

    @Autowired
    TaskTransferService transfer;

    @TempDir
    Path tempDir;

    private String run(String... lines) {
        return run(new BufferedReader(new StringReader(String.join("\n", lines) + "\n")));
    }

    private String run(BufferedReader in) {
        var bytes = new ByteArrayOutputStream();
        var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        new TodoConsole(service, transfer, in, out).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private Task create(String description) {
        return service.create(TaskDetails.of(description));
    }

    @Test
    void addsAndListsTasks() {
        String output = run("2", "Write tests", "", "", "1", "0");

        assertThat(output)
                .contains("Added task")
                .contains("Write tests")
                .contains("1 task: 1 pending, 0 in progress, 0 completed.")
                .endsWith("Goodbye!" + System.lineSeparator());
        assertThat(service.listAll()).extracting(Task::getDescription).containsExactly("Write tests");
    }

    @Test
    void addsTaskWithPriorityAndDueDate() {
        run("2", "Pay rent", "high", "tomorrow", "0");

        Task task = service.listAll().getFirst();
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(task.getDueDate()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void asksAgainForUnreadablePrioritiesAndDates() {
        String output = run("2", "Pay rent", "urgent", "low", "31/12", "2026-12-31", "0");

        assertThat(output).contains("'urgent' is not one of the options").contains("'31/12' is not a date");
        Task task = service.listAll().getFirst();
        assertThat(task.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void doesNotAddBlankTasks() {
        String output = run("2", "   ", "0");

        assertThat(output).contains("the description cannot be empty");
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    void exitsCleanlyWhenInputEnds() {
        // Previously, end of input made the menu loop print errors forever.
        String output = run("2");

        assertThat(output).endsWith("Goodbye!" + System.lineSeparator());
    }

    @Test
    void acceptsWordsAsWellAsNumbersInTheMenu() {
        String output = run("list", "quit");

        assertThat(output).contains("No tasks yet").endsWith("Goodbye!" + System.lineSeparator());
    }

    @Test
    void listsTasksInAnAlignedTable() {
        service.create(new TaskDetails("Pay rent", null, TaskPriority.HIGH, TODAY.minusDays(1)));
        create("Write the quarterly report");

        String output = run("1", "0");

        assertThat(output)
                .containsPattern("ID +Status +Priority +Due +Description")
                .contains(TODAY.minusDays(1) + " (overdue)")
                .contains("2 tasks: 2 pending, 0 in progress, 0 completed (1 overdue).");
    }

    @Test
    void editsStatusUsingFriendlyNames() {
        Task task = create("Refactor");

        run("3", task.getId().toString(), "", "in progress", "", "", "0");

        assertThat(service.get(task.getId()).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void editsEveryFieldAndCanClearTheDueDate() {
        Task task = service.create(new TaskDetails("Refactor", null, null, TODAY));

        String output = run("3", task.getId().toString(), "Refactor the parser", "", "high", "none", "0");

        assertThat(output).contains("Updated task " + task.getId());
        TaskDetails details = service.get(task.getId()).details();
        assertThat(details)
                .isEqualTo(new TaskDetails("Refactor the parser", TaskStatus.PENDING, TaskPriority.HIGH, null));
    }

    @Test
    void keepsEverythingWhenEditIsSkipped() {
        Task task = create("Refactor");

        String output = run("3", task.getId().toString(), "", "", "", "", "0");

        assertThat(output).contains("No changes made.");
    }

    @Test
    void rejectsUnknownStatusesInsteadOfStoringThem() {
        Task task = create("Refactor");

        String output = run("3", task.getId().toString(), "", "someday", "", "", "", "0");

        assertThat(output).contains("'someday' is not one of the options");
        assertThat(service.get(task.getId()).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void reportsInvalidAndUnknownIds() {
        create("Exists");

        String output = run("4", "abc", "4", "999", "0");

        assertThat(output).contains("'abc' is not a task ID").contains("There is no task with ID 999.");
    }

    @Test
    void marksTasksCompleted() {
        Task task = create("Ship it");

        String output = run("4", task.getId().toString(), "4", task.getId().toString(), "0");

        assertThat(output)
                .contains("Marked task " + task.getId() + " as completed.")
                .contains("already completed");
        assertThat(service.get(task.getId()).isCompleted()).isTrue();
    }

    @Test
    void deletesOnlyAfterConfirmation() {
        Task task = create("Keep me");

        run("5", task.getId().toString(), "no", "0");
        assertThat(service.listAll()).hasSize(1);

        run("5", task.getId().toString(), "y", "0");
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    void searchesDescriptions() {
        create("Pay rent");
        create("Walk the dog");

        String output = run("6", "RENT", "0");

        assertThat(output).contains("Pay rent").doesNotContain("Walk the dog").contains("1 matching task.");
        assertThat(run("6", "taxes", "0")).contains("No tasks contain 'taxes'.");
    }

    @Test
    void clearsCompletedTasksAfterConfirmation() {
        Task done = create("Done already");
        service.update(done.getId(), TaskChanges.status(TaskStatus.COMPLETED), ExpectedVersion.ANY);
        create("Still to do");

        assertThat(run("7", "n", "0")).contains("Delete 1 completed task?").contains("Nothing deleted.");
        assertThat(service.listAll()).hasSize(2);

        assertThat(run("7", "yes", "0")).contains("Deleted 1 completed task.");
        assertThat(service.listAll()).extracting(Task::getDescription).containsExactly("Still to do");

        assertThat(run("7", "0")).contains("There are no completed tasks to clear.");
    }

    @Test
    void exportsToJsonAndImportsItBack() {
        service.create(new TaskDetails("Pay rent", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY));
        create("Walk the dog");
        Path file = tempDir.resolve("backup.json");

        assertThat(run("8", file.toString(), "0")).contains("Exported 2 tasks to " + file + ".");
        assertThat(file).content(StandardCharsets.UTF_8).contains("\"description\" : \"Pay rent\"");

        String output = run("9", "\"" + file + "\"", "y", "0");

        assertThat(output)
                .contains("Add the 2 tasks in this file to your list?")
                .contains("Imported 2 tasks.");
        assertThat(service.listAll())
                .extracting(Task::details)
                .containsExactly(
                        new TaskDetails("Pay rent", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY),
                        TaskDetails.of("Walk the dog"),
                        new TaskDetails("Pay rent", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY),
                        TaskDetails.of("Walk the dog"));
    }

    @Test
    void exportsCsvWhenTheFileNameSaysSoAndAsksBeforeOverwriting() throws IOException {
        create("Walk the dog");
        Path file = tempDir.resolve("tasks.csv");
        Files.writeString(file, "keep me");

        assertThat(run("8", file.toString(), "n", "0"))
                .contains("already exists")
                .contains("Nothing exported.");
        assertThat(file).hasContent("keep me");

        assertThat(run("8", file.toString(), "y", "0")).contains("as CSV.");
        assertThat(file).content(StandardCharsets.UTF_8).contains("Walk the dog");
    }

    @Test
    void exportsIntoAFolderUnderTheDefaultName() {
        String output = run("8", tempDir.toString(), "0");

        assertThat(output).contains("Exported 0 tasks to " + tempDir.resolve("do-it-to-it-tasks-2026-03-02.json"));
    }

    @Test
    void explainsWhyAFileCannotBeImported() throws IOException {
        Path notJson = Files.writeString(tempDir.resolve("notes.txt"), "just some notes");
        Path invalid = Files.writeString(
                tempDir.resolve("invalid.json"), "{\"tasks\": [{\"description\": \"ok\"}, {\"description\": \" \"}]}");

        assertThat(run("9", tempDir.resolve("missing.json").toString(), "0")).contains("There is no file at");
        assertThat(run("9", notJson.toString(), "0")).contains("Nothing imported. The file is not a valid");
        assertThat(run("9", invalid.toString(), "y", "0"))
                .contains("Nothing imported. Task 2 in the file: Task description cannot be empty");
        assertThat(run("9", "", "0")).contains("Import cancelled.");
        assertThat(service.listAll()).isEmpty();
    }

    // The tests below run outside the test transaction so that, as in the real console, every service
    // call works on its own copy of a task and a change made "elsewhere" is not visible to the console.

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void doesNotOverwriteATaskChangedElsewhereWhileEditing() {
        Task task = create("Refactor");
        try {
            var input = new ScriptedInput("3", task.getId().toString(), "My edit", "", "", "", "0")
                    .before(
                            3,
                            () -> service.replace(
                                    task.getId(), TaskDetails.of("Changed in the browser"), ExpectedVersion.ANY));

            String output = run(input);

            assertThat(output)
                    .contains("Task " + task.getId()
                            + " was changed elsewhere in the meantime, so your edits were not saved.")
                    .contains("Changed in the browser");
            assertThat(service.get(task.getId()).getDescription()).isEqualTo("Changed in the browser");
        } finally {
            service.find(task.getId()).ifPresent(t -> service.delete(t.getId(), ExpectedVersion.ANY));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reportsATaskDeletedElsewhereWhileConfirmingItsDeletion() {
        Task task = create("Delete me");
        var input = new ScriptedInput("5", task.getId().toString(), "y", "0")
                .before(2, () -> service.delete(task.getId(), ExpectedVersion.ANY));

        String output = run(input);

        assertThat(output).contains("Task " + task.getId() + " had already been deleted elsewhere.");
    }

    /** Console input that runs an action, as someone using the web app might, just before a given line is read. */
    private static final class ScriptedInput extends BufferedReader {

        private final List<String> lines;
        private int next;
        private int triggerIndex = -1;
        private Runnable action;

        ScriptedInput(String... lines) {
            super(Reader.nullReader());
            this.lines = List.of(lines);
        }

        /** Runs {@code action} just before the line at {@code index} (counting from zero) is read. */
        ScriptedInput before(int index, Runnable action) {
            this.triggerIndex = index;
            this.action = action;
            return this;
        }

        @Override
        public String readLine() {
            if (next == triggerIndex) {
                action.run();
            }
            return next < lines.size() ? lines.get(next++) : null;
        }
    }
}
