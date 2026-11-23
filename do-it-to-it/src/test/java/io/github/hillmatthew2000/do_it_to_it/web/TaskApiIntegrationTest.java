package io.github.hillmatthew2000.do_it_to_it.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.hillmatthew2000.do_it_to_it.TestClockConfig;
import io.github.hillmatthew2000.do_it_to_it.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the API end to end against the real database schema. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class TaskApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    TaskRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    private long create(String json) throws Exception {
        String body = mvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void fullTaskLifecycle() throws Exception {
        long id = create("{\"description\":\"  Plan sprint  \",\"priority\":\"HIGH\",\"dueDate\":\"2026-03-01\"}");

        mvc.perform(get("/api/todos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Plan sprint"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.overdue").value(true));

        mvc.perform(patch("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"in progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mvc.perform(put("/api/todos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Plan sprint 12\",\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.dueDate").doesNotExist());

        mvc.perform(patch("/api/todos/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(delete("/api/todos/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/todos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void filtersSearchesAndReportsStats() throws Exception {
        create("{\"description\":\"Email Alice\"}");
        create("{\"description\":\"Email Bob\",\"status\":\"COMPLETED\"}");
        create("{\"description\":\"Overdue report\",\"dueDate\":\"2026-02-01\"}");

        mvc.perform(get("/api/todos").param("q", "email")).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/todos").param("status", "completed").param("q", "EMAIL"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Email Bob"));
        mvc.perform(get("/api/todos").param("sort", "description"))
                .andExpect(jsonPath("$[0].description").value("Email Alice"))
                .andExpect(jsonPath("$[2].description").value("Overdue report"));

        mvc.perform(get("/api/todos/stats"))
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.pendingTasks").value(2))
                .andExpect(jsonPath("$.inProgressTasks").value(0))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.overdueTasks").value(1));

        mvc.perform(delete("/api/todos/completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(1));
        mvc.perform(get("/api/todos")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void servesTheWebUi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
