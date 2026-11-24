package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.ExpectedVersion;
import io.github.hillmatthew2000.do_it_to_it.task.Task;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ETag;
import org.springframework.lang.Nullable;

/**
 * Maps task versions to HTTP entity tags and back. A task's entity tag is its version in quotes, for
 * example {@code "3"}; it changes every time the task does.
 */
final class TaskETags {

    private TaskETags() {}

    static String of(Task task) {
        return new ETag(String.valueOf(task.getVersion()), false).formattedTag();
    }

    /**
     * The versions an {@code If-Match} header allows a change to apply to. No header, or {@code *},
     * allows any version. Otherwise only the listed tags count, compared strongly as RFC 9110
     * requires, so weak tags never match.
     */
    static ExpectedVersion expectedVersion(@Nullable String ifMatch) {
        if (ifMatch == null) {
            return ExpectedVersion.ANY;
        }
        List<ETag> tags = ETag.parse(ifMatch);
        if (tags.stream().anyMatch(ETag::isWildcard)) {
            return ExpectedVersion.ANY;
        }
        Set<Long> versions = tags.stream()
                .filter(tag -> !tag.weak())
                .map(tag -> parseVersion(tag.tag()))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        return ExpectedVersion.oneOf(versions);
    }

    private static Optional<Long> parseVersion(String tag) {
        try {
            return Optional.of(Long.parseLong(tag));
        } catch (NumberFormatException e) {
            return Optional.empty(); // not one of ours, so it cannot match
        }
    }
}
