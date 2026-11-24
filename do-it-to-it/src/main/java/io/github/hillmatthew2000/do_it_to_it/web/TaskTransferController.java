package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.transfer.TaskExport;
import io.github.hillmatthew2000.do_it_to_it.transfer.TaskTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Downloads every task as a file, and adds tasks from a previous JSON export. */
@RestController
@RequestMapping("/api/todos")
@Tag(name = "Export and import")
public class TaskTransferController {

    static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final TaskTransferService transfer;

    public TaskTransferController(TaskTransferService transfer) {
        this.transfer = transfer;
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export every task as a file",
            description = "JSON (the default) can be imported again; CSV is for spreadsheets.")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "json or csv") @RequestParam(defaultValue = "json") String format) {
        return switch (format.strip().toLowerCase(Locale.ROOT)) {
            case "json" -> download(transfer.exportJson(), MediaType.APPLICATION_JSON, "json");
            case "csv" -> download(transfer.exportCsv().getBytes(StandardCharsets.UTF_8), TEXT_CSV, "csv");
            default -> throw new IllegalArgumentException("Unknown format '" + format + "'. Expected json or csv");
        };
    }

    @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Import tasks from a JSON export",
            description = "Adds the tasks to the existing ones, keeping their timestamps. If any task is invalid,"
                    + " none are added. Returns how many were imported.")
    public Map<String, Integer> importTasks(@RequestBody TaskExport export) {
        return Map.of("imported", transfer.importTasks(export));
    }

    private ResponseEntity<byte[]> download(byte[] content, MediaType type, String extension) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(transfer.fileName(extension))
                .build();
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
