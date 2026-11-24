package io.github.hillmatthew2000.do_it_to_it.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.HttpHeaders;
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
    void conditionalChangesAreRefusedOnceTheTaskHasMovedOn() throws Exception {
        long id = create("{\"description\":\"Draft\"}");
        mvc.perform(get("/api/todos/{id}", id))
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.version").value(0));

        // One client saves, based on version 0...
        mvc.perform(put("/api/todos/{id}", id)
                        .header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Saved from the console\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.version").value(1));

        // ...so every other change still based on version 0 is refused.
        mvc.perform(patch("/api/todos/{id}", id)
                        .header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Saved from the browser\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Task changed"))
                .andExpect(jsonPath("$.taskId").value(id));
        mvc.perform(patch("/api/todos/{id}/complete", id).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(delete("/api/todos/{id}", id).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isPreconditionFailed());
        // Weak tags never match, as RFC 9110 requires strong comparison for If-Match.
        mvc.perform(delete("/api/todos/{id}", id).header(HttpHeaders.IF_MATCH, "W/\"1\""))
                .andExpect(status().isPreconditionFailed());

        mvc.perform(get("/api/todos/{id}", id))
                .andExpect(jsonPath("$.description").value("Saved from the console"));

        // Any of several tags may match, "*" matches whatever the version, and no header means no check.
        mvc.perform(patch("/api/todos/{id}/complete", id).header(HttpHeaders.IF_MATCH, "\"0\", \"1\""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"2\""));
        mvc.perform(patch("/api/todos/{id}", id)
                        .header(HttpHeaders.IF_MATCH, "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/todos/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void conditionalChangesToMissingTasksAreReportedAsNotFound() throws Exception {
        mvc.perform(delete("/api/todos/{id}", 12345).header(HttpHeaders.IF_MATCH, "\"0\""))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportsAsDownloadsAndImportsTheJsonBack() throws Exception {
        create("{\"description\":\"Pay rent\",\"priority\":\"HIGH\",\"dueDate\":\"2026-03-01\"}");
        create("{\"description\":\"Walk the dog\",\"status\":\"COMPLETED\"}");

        String json = mvc.perform(get("/api/todos/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"do-it-to-it-tasks-2026-03-02.json\""))
                .andExpect(jsonPath("$.formatVersion").value(1))
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].description").value("Pay rent"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mvc.perform(get("/api/todos/export").param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"do-it-to-it-tasks-2026-03-02.csv\""))
                .andExpect(content().string(containsString(",Walk the dog,COMPLETED,MEDIUM,,false,")));

        mvc.perform(post("/api/todos/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));
        mvc.perform(get("/api/todos/stats")).andExpect(jsonPath("$.totalTasks").value(4));
    }

    @Test
    void rejectsUnknownExportFormatsAndInvalidImports() throws Exception {
        mvc.perform(get("/api/todos/export").param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown format 'xml'. Expected json or csv"));

        mvc.perform(post("/api/todos/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tasks\":[{\"description\":\"ok\"},{\"priority\":\"HIGH\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Task 2 in the file: Task description cannot be empty"));
        mvc.perform(get("/api/todos/stats")).andExpect(jsonPath("$.totalTasks").value(0));
    }

    @Test
    void servesTheWebUi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
    }
}
