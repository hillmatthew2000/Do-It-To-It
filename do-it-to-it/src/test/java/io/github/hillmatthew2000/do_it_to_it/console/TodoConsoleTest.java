package io.github.hillmatthew2000.do_it_to_it.console;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({TaskService.class, TestClockConfig.class})
class TodoConsoleTest {

    @Autowired
    TaskService service;

    private String run(String... lines) {
        var bytes = new ByteArrayOutputStream();
        var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var in = new BufferedReader(new StringReader(String.join("\n", lines) + "\n"));
        new TodoConsole(service, in, out).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void addsAndListsTasks() {
        String output = run("1", "Write tests", "2", "6");

        assertThat(output)
                .contains("Task added with ID")
                .contains("Write tests")
                .contains("Goodbye!");
        assertThat(service.listAll()).extracting(Task::getDescription).containsExactly("Write tests");
    }

    @Test
    void exitsCleanlyWhenInputEnds() {
        // Previously, end of input made the menu loop print errors forever.
        String output = run("1");

        assertThat(output).endsWith("Goodbye!" + System.lineSeparator());
    }

    @Test
    void updatesStatusUsingFriendlyNames() {
        Task task = service.create(TaskDetails.of("Refactor"));

        run("3", task.getId().toString(), "2", "in progress", "6");

        assertThat(service.get(task.getId()).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void rejectsUnknownStatusesInsteadOfStoringThem() {
        Task task = service.create(TaskDetails.of("Refactor"));

        String output = run("3", task.getId().toString(), "2", "someday", "6");

        assertThat(output).contains("Unknown status 'someday'");
        assertThat(service.get(task.getId()).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void reportsInvalidAndUnknownIds() {
        service.create(TaskDetails.of("Exists"));

        String output = run("5", "abc", "5", "999", "6");

        assertThat(output).contains("Please enter a valid task ID").contains("Task with ID 999 not found.");
    }

    @Test
    void deletesOnlyAfterConfirmation() {
        Task task = service.create(TaskDetails.of("Keep me"));

        run("4", task.getId().toString(), "no", "6");
        assertThat(service.listAll()).hasSize(1);

        run("4", task.getId().toString(), "yes", "6");
        assertThat(service.listAll()).isEmpty();
    }
}
