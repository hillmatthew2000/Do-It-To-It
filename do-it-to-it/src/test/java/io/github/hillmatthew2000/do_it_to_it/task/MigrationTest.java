package io.github.hillmatthew2000.do_it_to_it.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Checks that databases created by earlier releases are upgraded with their tasks intact. */
class MigrationTest {

    @Test
    void existingTasksStartAtVersionZero() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        flyway(dataSource, "1").migrate();
        jdbc.update("""
                insert into task (description, status, priority, created_at, updated_at)
                values ('Written before versions existed', 'PENDING', 'HIGH', current_timestamp, current_timestamp)
                """);

        flyway(dataSource, "latest").migrate();

        assertThat(jdbc.queryForMap("select description, version from task"))
                .containsEntry("DESCRIPTION", "Written before versions existed")
                .containsEntry("VERSION", 0L);
    }

    private static Flyway flyway(JdbcDataSource dataSource, String target) {
        return Flyway.configure().dataSource(dataSource).target(target).load();
    }
}
