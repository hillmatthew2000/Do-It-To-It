package io.github.hillmatthew2000.do_it_to_it.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.hillmatthew2000.do_it_to_it.MutableClock;
import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import io.github.hillmatthew2000.do_it_to_it.task.ExpectedVersion;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TaskService.class, TaskTransferService.class, TestClockConfig.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class TaskTransferServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);

    @Autowired
    TaskService service;

    @Autowired
    TaskTransferService transfer;

    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(TestClockConfig.START);
    }

    @Test
    void aJsonExportImportsBackIntoAnIdenticalList() throws IOException {
        service.create(new TaskDetails("Pay rent", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY));
        clock.advance(Duration.ofHours(1));
        service.create(TaskDetails.of("Walk the dog"));
        List<Task> original = service.listAll();
        byte[] json = transfer.exportJson();
        original.forEach(task -> service.delete(task.getId(), ExpectedVersion.ANY));
        clock.advance(Duration.ofDays(30));

        int imported = transfer.importTasks(transfer.read(new ByteArrayInputStream(json)));

        assertThat(imported).isEqualTo(2);
        assertThat(service.listAll())
                .extracting(Task::details, Task::getCreatedAt, Task::getUpdatedAt)
                .containsExactlyElementsOf(original.stream()
                        .map(task -> tuple(task.details(), task.getCreatedAt(), task.getUpdatedAt()))
                        .toList());
    }

    @Test
    void theExportSaysWhatItIs() {
        service.create(TaskDetails.of("Walk the dog"));

        String json = new String(transfer.exportJson(), StandardCharsets.UTF_8);

        assertThat(json)
                .contains("\"app\" : \"do-it-to-it\"")
                .contains("\"formatVersion\" : 1")
                .contains("\"exportedAt\" : \"2026-03-02T09:00:00Z\"")
                .contains("\"description\" : \"Walk the dog\"");
    }

    @Test
    void handWrittenFilesNeedOnlyDescriptions() throws IOException {
        String json =
                "{\"tasks\": [{\"description\": \"Call mum\"}, {\"description\": \"Fix bike\", \"status\": \"completed\"}]}";

        transfer.importTasks(read(json));

        assertThat(service.listAll())
                .extracting(Task::getDescription, Task::getStatus, Task::getCreatedAt)
                .containsExactly(
                        tuple("Call mum", TaskStatus.PENDING, TestClockConfig.START),
                        tuple("Fix bike", TaskStatus.COMPLETED, TestClockConfig.START));
    }

    @Test
    void oneInvalidTaskMeansNothingIsImported() throws IOException {
        String json = "{\"tasks\": [{\"description\": \"Fine\"}, {\"description\": \"\"}]}";

        assertThatThrownBy(() -> transfer.importTasks(read(json)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task 2 in the file: Task description cannot be empty");
        assertThat(service.listAll()).isEmpty();
    }

    @Test
    void refusesFilesItCannotUnderstand() {
        assertThatThrownBy(() -> read("{\"tasks\": [{\"description\": \"x\", \"status\": \"someday\"}]}"))
                .hasMessageStartingWith("Unknown status 'someday'");
        assertThatThrownBy(() -> read("{\"tasks\": [{\"description\": \"x\", \"dueDate\": \"tomorrow\"}]}"))
                .hasMessage("Dates must use the ISO format yyyy-MM-dd");
        assertThatThrownBy(() -> read("[1, 2, 3]"))
                .hasMessageStartingWith("The file is not a valid Do-It-To-It JSON export");
        assertThatThrownBy(() -> read("")).hasMessage("The file is empty.");
        assertThatThrownBy(() -> transfer.importTasks(new TaskExport(null, null, null, null)))
                .hasMessageContaining("no \"tasks\" list");
        assertThatThrownBy(() -> transfer.importTasks(new TaskExport(TaskExport.APP, 2, Instant.now(), List.of())))
                .hasMessageContaining("newer version of Do-It-To-It (format 2)");
        assertThatThrownBy(
                        () -> transfer.importTasks(new TaskExport(null, null, null, Collections.singletonList(null))))
                .hasMessage("Task 1 in the file: the entry is empty");
    }

    @Test
    void limitsHowManyTasksOneImportMayAdd() {
        List<ExportedTask> tooMany = new ArrayList<>(Collections.nCopies(
                TaskTransferService.MAX_IMPORT_SIZE + 1, new ExportedTask("x", null, null, null, null, null)));

        assertThatThrownBy(() -> transfer.importTasks(new TaskExport(null, null, null, tooMany)))
                .hasMessageContaining("at most 10000 can be imported at once");
    }

    @Test
    void namesFilesAfterToday() {
        assertThat(transfer.fileName("csv")).isEqualTo("do-it-to-it-tasks-2026-03-02.csv");
    }

    private TaskExport read(String json) throws IOException {
        return transfer.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
