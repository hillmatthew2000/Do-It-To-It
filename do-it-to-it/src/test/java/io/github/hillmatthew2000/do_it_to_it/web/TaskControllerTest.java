package io.github.hillmatthew2000.do_it_to_it.web;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hillmatthew2000.do_it_to_it.task.TaskDetails;
import io.github.hillmatthew2000.do_it_to_it.task.TaskFixtures;
import io.github.hillmatthew2000.do_it_to_it.task.TaskNotFoundException;
import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import io.github.hillmatthew2000.do_it_to_it.task.TaskSort;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 2);
    private static final Instant NOW = Instant.parse("2026-03-02T09:00:00Z");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TaskService service;

    @BeforeEach
    void setUp() {
        given(service.today()).willReturn(TODAY);
    }

    @Test
    void listsTasksWithFiltersAndSort() throws Exception {
        var task = TaskFixtures.task(
                7, new TaskDetails("Pay rent", TaskStatus.PENDING, TaskPriority.HIGH, TODAY.minusDays(1)), NOW);
        given(service.list(TaskStatus.PENDING, "rent", TaskSort.DUE_DATE)).willReturn(List.of(task));

        mvc.perform(get("/api/todos")
                        .param("status", "PENDING")
                        .param("q", "rent")
                        .param("sort", "DUE_DATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].description").value("Pay rent"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-03-01"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-02T09:00:00Z"));
    }

    @Test
    void listDefaultsToCreationOrder() throws Exception {
        given(service.list(null, null, TaskSort.OLDEST)).willReturn(List.of());

        mvc.perform(get("/api/todos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(any())).willReturn(TaskFixtures.task(3, TaskDetails.of("Write docs"), NOW));

        mvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Write docs\",\"priority\":\"low\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/todos/3")))
                .andExpect(jsonPath("$.id").value(3));

        verify(service).create(new TaskDetails("Write docs", null, TaskPriority.LOW, null));
    }

    @Test
    void createRejectsBlankDescriptionWithProblemDetail() throws Exception {
        mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.description").value("Description is required"));

        verify(service, never()).create(any());
    }

    @Test
    void createRejectsOverlongDescription() throws Exception {
        String body = "{\"description\":\"" + "x".repeat(501) + "\"}";

        mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").value("Description must be at most 500 characters"));
    }

    @Test
    void createRejectsUnknownStatusWithHelpfulMessage() throws Exception {
        mvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"status\":\"someday\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(startsWith("Unknown status 'someday'")));
    }

    @Test
    void createRejectsMalformedJsonAndDates() throws Exception {
        mvc.perform(post("/api/todos").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or is not valid JSON"));

        mvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"x\",\"dueDate\":\"03/04/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dates must use the ISO format yyyy-MM-dd"));

        verifyNoInteractions(service);
    }

    @Test
    void unknownTaskReturns404ProblemDetail() throws Exception {
        given(service.get(42)).willThrow(new TaskNotFoundException(42));

        mvc.perform(get("/api/todos/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Task not found"))
                .andExpect(jsonPath("$.detail").value("Task 42 does not exist"))
                .andExpect(jsonPath("$.taskId").value(42));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get("/api/todos/abc")).andExpect(status().isBadRequest());
    }

    @Test
    void replaceSendsAllFields() throws Exception {
        var details = new TaskDetails("New", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY);
        given(service.replace(5, details)).willReturn(TaskFixtures.task(5, details, NOW));

        mvc.perform(
                        put("/api/todos/5")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"description\":\"New\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"dueDate\":\"2026-03-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void patchRejectsBlankDescriptionButAllowsOmittingIt() throws Exception {
        mvc.perform(patch("/api/todos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").value("Description cannot be blank"));

        given(service.update(eq(5L), any()))
                .willReturn(TaskFixtures.task(
                        5, new TaskDetails("x", TaskStatus.COMPLETED, TaskPriority.MEDIUM, null), NOW));
        mvc.perform(patch("/api/todos/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/todos/5")).andExpect(status().isNoContent());

        verify(service).delete(5);
    }

    @Test
    void deleteUnknownTaskReturns404() throws Exception {
        willThrow(new TaskNotFoundException(9)).given(service).delete(anyLong());

        mvc.perform(delete("/api/todos/9")).andExpect(status().isNotFound());
    }

    @Test
    void invalidStatusFilterReturns400() throws Exception {
        mvc.perform(get("/api/todos").param("status", "nope")).andExpect(status().isBadRequest());

        verify(service, never()).list(any(), any(), any());
    }
}
