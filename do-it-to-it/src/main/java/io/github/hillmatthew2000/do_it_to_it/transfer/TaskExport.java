package io.github.hillmatthew2000.do_it_to_it.transfer;

import java.time.Instant;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * The JSON document written by an export and read back by an import. It holds what was entered for
 * each task plus its timestamps; ids and versions are assigned afresh when the tasks are imported.
 *
 * @param app always {@value #APP}, so that the file says where it came from
 * @param formatVersion the layout of this document, {@value #FORMAT_VERSION} at present; files from a
 *     newer release are refused rather than half-read
 * @param exportedAt when the export was made
 * @param tasks the tasks, oldest first
 */
public record TaskExport(
        @Nullable String app,
        @Nullable Integer formatVersion,
        @Nullable Instant exportedAt,
        @Nullable List<ExportedTask> tasks) {

    public static final String APP = "do-it-to-it";
    public static final int FORMAT_VERSION = 1;
}
