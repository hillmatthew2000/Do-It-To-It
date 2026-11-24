package io.github.hillmatthew2000.do_it_to_it.transfer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hillmatthew2000.do_it_to_it.task.ImportedTask;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Exports every task to JSON or CSV, and imports tasks from a JSON export. Imports add to the existing
 * tasks; they never change or remove any.
 */
@Service
public class TaskTransferService {

    /** Guards against importing something that is clearly not a task list, such as a huge file picked by mistake. */
    public static final int MAX_IMPORT_SIZE = 10_000;

    private final TaskService tasks;
    private final ObjectMapper json;
    private final Clock clock;

    public TaskTransferService(TaskService tasks, ObjectMapper json, Clock clock) {
        this.tasks = tasks;
        this.json = json;
        this.clock = clock;
    }

    public TaskExport export() {
        List<ExportedTask> exported =
                tasks.listAll().stream().map(ExportedTask::from).toList();
        return new TaskExport(TaskExport.APP, TaskExport.FORMAT_VERSION, clock.instant(), exported);
    }

    /** The export as indented JSON, readable enough to inspect or edit by hand. */
    public byte[] exportJson() {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsBytes(export());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write the export", e);
        }
    }

    public String exportCsv() {
        return TaskCsv.write(tasks.listAll(), tasks.today());
    }

    /** A file name for an export made today, such as {@code do-it-to-it-tasks-2026-11-24.json}. */
    public String fileName(String extension) {
        return "do-it-to-it-tasks-" + LocalDate.now(clock) + "." + extension;
    }

    /**
     * Adds the tasks in an export.
     *
     * @return how many tasks were added
     * @throws IllegalArgumentException if the document or any task in it is invalid, in which case
     *     nothing is added
     */
    public int importTasks(TaskExport export) {
        if (export.formatVersion() != null && export.formatVersion() > TaskExport.FORMAT_VERSION) {
            throw new IllegalArgumentException("This file was exported by a newer version of Do-It-To-It (format "
                    + export.formatVersion() + "), but this version only reads format " + TaskExport.FORMAT_VERSION
                    + ".");
        }
        List<ExportedTask> entries = export.tasks();
        if (entries == null) {
            throw new IllegalArgumentException("The file has no \"tasks\" list, so it is not a Do-It-To-It export.");
        }
        if (entries.size() > MAX_IMPORT_SIZE) {
            throw new IllegalArgumentException("The file has " + entries.size() + " tasks, but at most "
                    + MAX_IMPORT_SIZE + " can be imported at once.");
        }
        List<ImportedTask> imported = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            ExportedTask entry = entries.get(i);
            try {
                if (entry == null) {
                    throw new IllegalArgumentException("the entry is empty");
                }
                imported.add(entry.toImportedTask());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Task " + (i + 1) + " in the file: " + e.getMessage(), e);
            }
        }
        return tasks.importAll(imported);
    }

    /**
     * Reads a JSON export without importing it, e.g. to say how many tasks it holds first.
     *
     * @throws IllegalArgumentException if the content is not JSON in the export layout
     * @throws IOException if the content cannot be read
     */
    public TaskExport read(InputStream content) throws IOException {
        try {
            JsonNode document = json.readTree(content);
            if (document == null || document.isMissingNode()) {
                throw new IllegalArgumentException("The file is empty.");
            }
            return json.treeToValue(document, TaskExport.class);
        } catch (JacksonException e) {
            throw new IllegalArgumentException(describe(e), e);
        }
    }

    /** Explains a parse failure without the parser's internals, as the API does for request bodies. */
    private static String describe(JacksonException e) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
                return cause.getMessage();
            }
            if (cause instanceof DateTimeParseException) {
                return "Dates must use the ISO format yyyy-MM-dd";
            }
        }
        String line =
                e.getLocation() != null ? " (problem on line " + e.getLocation().getLineNr() + ")" : "";
        return "The file is not a valid Do-It-To-It JSON export" + line + ".";
    }
}
