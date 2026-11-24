package io.github.hillmatthew2000.do_it_to_it.transfer;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes tasks as CSV (RFC 4180) for spreadsheets. This is an export-only view; JSON is the format
 * that can be imported again.
 */
final class TaskCsv {

    /** Lets Excel recognise the file as UTF-8 instead of guessing the system code page. */
    static final String BYTE_ORDER_MARK = "﻿";

    static final String HEADER = "id,description,status,priority,dueDate,overdue,createdAt,updatedAt";

    private static final String LINE_END = "\r\n";

    /** Spreadsheets treat cells starting with one of these as formulas. */
    private static final String FORMULA_PREFIXES = "=+-@\t\r";

    private TaskCsv() {}

    static String write(List<Task> tasks, LocalDate today) {
        StringBuilder csv = new StringBuilder(BYTE_ORDER_MARK).append(HEADER).append(LINE_END);
        for (Task task : tasks) {
            String row = Stream.of(
                            task.getId(),
                            task.getDescription(),
                            task.getStatus(),
                            task.getPriority(),
                            task.getDueDate(),
                            task.isOverdue(today),
                            task.getCreatedAt(),
                            task.getUpdatedAt())
                    .map(value -> field(Objects.toString(value, "")))
                    .collect(Collectors.joining(","));
            csv.append(row).append(LINE_END);
        }
        return csv.toString();
    }

    static String field(String value) {
        // A leading apostrophe stops a description such as "=1+1" from being run as a formula when the
        // file is opened (the OWASP advice on CSV injection).
        String text = !value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}
