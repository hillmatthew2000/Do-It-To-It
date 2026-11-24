package io.github.hillmatthew2000.do_it_to_it.console;

import io.github.hillmatthew2000.do_it_to_it.task.Task;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Renders tasks as a plain-text table with aligned columns. Output is ASCII only. */
final class TaskTable {

    /** Longer descriptions are cut short so that rows stay on one line in a typical terminal. */
    static final int MAX_DESCRIPTION_WIDTH = 50;

    private static final String[] HEADERS = {"ID", "Status", "Priority", "Due", "Description"};
    private static final String GAP = "  ";

    private TaskTable() {}

    static List<String> render(List<Task> tasks, LocalDate today) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADERS);
        for (Task task : tasks) {
            rows.add(new String[] {
                String.valueOf(task.getId()),
                task.getStatus().label(),
                task.getPriority().label(),
                due(task, today),
                truncate(task.getDescription())
            });
        }

        int[] widths = new int[HEADERS.length];
        for (String[] row : rows) {
            for (int column = 0; column < row.length; column++) {
                widths[column] = Math.max(widths[column], row[column].length());
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add(line(HEADERS, widths));
        lines.add(line(rule(widths), widths));
        rows.stream().skip(1).map(row -> line(row, widths)).forEach(lines::add);
        return lines;
    }

    private static String due(Task task, LocalDate today) {
        if (task.getDueDate() == null) {
            return "-";
        }
        return task.isOverdue(today)
                ? task.getDueDate() + " (overdue)"
                : task.getDueDate().toString();
    }

    static String truncate(String description) {
        String singleLine = description.replaceAll("\\s+", " ");
        return singleLine.length() <= MAX_DESCRIPTION_WIDTH
                ? singleLine
                : singleLine.substring(0, MAX_DESCRIPTION_WIDTH - 3) + "...";
    }

    private static String[] rule(int[] widths) {
        String[] rule = new String[widths.length];
        for (int column = 0; column < widths.length; column++) {
            rule[column] = "-".repeat(widths[column]);
        }
        return rule;
    }

    private static String line(String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int column = 0; column < cells.length; column++) {
            if (column > 0) {
                line.append(GAP);
            }
            // The id column is right-aligned so that numbers line up; the rest are left-aligned.
            String format = column == 0 ? "%" + widths[column] + "s" : "%-" + widths[column] + "s";
            line.append(format.formatted(cells[column]));
        }
        return line.toString().stripTrailing();
    }
}
