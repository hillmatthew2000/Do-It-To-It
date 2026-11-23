package io.github.hillmatthew2000.do_it_to_it;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DoItToItApplicationTests {

    @Autowired
    TaskService taskService;

    @Test
    void contextLoadsAndMigratesTheSchema() {
        // ddl-auto=validate fails startup if the Flyway schema and the entity mapping disagree.
        assertThat(taskService.listAll()).isEmpty();
    }
}
