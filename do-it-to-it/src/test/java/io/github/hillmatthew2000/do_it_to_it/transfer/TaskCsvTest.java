package io.github.hillmatthew2000.do_it_to_it.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskFixtures;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskCsvTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);
    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    @Test
    void writesAHeaderAndOneLinePerTask() {
        var late = TaskFixtures.task(
                1, new TaskDetails("Pay rent", TaskStatus.PENDING, TaskPriority.HIGH, TODAY.minusDays(1)), NOW);
        var plain = TaskFixtures.task(2, TaskDetails.of("Walk the dog"), NOW);

        String csv = TaskCsv.write(List.of(late, plain), TODAY);

        assertThat(csv)
                .isEqualTo(TaskCsv.BYTE_ORDER_MARK
                        + TaskCsv.HEADER + "\r\n"
                        + "1,Pay rent,PENDING,HIGH,2026-03-01,true,2026-03-02T09:00:00Z,2026-03-02T09:00:00Z\r\n"
                        + "2,Walk the dog,PENDING,MEDIUM,,false,2026-03-02T09:00:00Z,2026-03-02T09:00:00Z\r\n");
    }

    @Test
    void quotesFieldsThatContainSeparatorsQuotesOrLineBreaks() {
        assertThat(TaskCsv.field("plain")).isEqualTo("plain");
        assertThat(TaskCsv.field("milk, eggs")).isEqualTo("\"milk, eggs\"");
        assertThat(TaskCsv.field("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(TaskCsv.field("two\nlines")).isEqualTo("\"two\nlines\"");
    }

    @Test
    void neutralisesTextThatSpreadsheetsWouldRunAsAFormula() {
        assertThat(TaskCsv.field("=HYPERLINK(\"http://example.com\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"http://example.com\"\")\"");
        assertThat(TaskCsv.field("+1")).isEqualTo("'+1");
        assertThat(TaskCsv.field("- buy milk")).isEqualTo("'- buy milk");
        assertThat(TaskCsv.field("@home")).isEqualTo("'@home");
        assertThat(TaskCsv.field("")).isEmpty();
    }
}
