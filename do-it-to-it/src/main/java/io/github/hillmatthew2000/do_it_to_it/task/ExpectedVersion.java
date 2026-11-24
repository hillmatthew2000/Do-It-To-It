package io.github.hillmatthew2000.do_it_to_it.task;

import java.util.Collection;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * The version(s) of a task that a change was based on. {@link TaskService} refuses a change whose
 * expected version is no longer current with a {@link TaskVersionConflictException}, so that edits
 * made elsewhere in the meantime (in another browser tab, or in the console while the web app is
 * open) are never silently overwritten.
 *
 * @param versions the versions the task may currently have, or {@code null} to accept any version
 */
public record ExpectedVersion(@Nullable Set<Long> versions) {

    /** Applies the change whatever the task's current version is. */
    public static final ExpectedVersion ANY = new ExpectedVersion(null);

    public ExpectedVersion {
        versions = versions == null ? null : Set.copyOf(versions);
    }

    /** Applies the change only if the task is still at {@code version}. */
    public static ExpectedVersion of(long version) {
        return new ExpectedVersion(Set.of(version));
    }

    /** Applies the change only if the task is at one of {@code versions}; an empty set never matches. */
    public static ExpectedVersion oneOf(Collection<Long> versions) {
        return new ExpectedVersion(Set.copyOf(versions));
    }

    public boolean matches(long version) {
        return versions == null || versions.contains(version);
    }
}
