package io.github.hillmatthew2000.do_it_to_it.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Health, info and API documentation endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationsEndpointsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsUpAndHidesDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void infoDescribesTheRuntime() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.java.version").exists());
    }

    @Test
    void onlyHealthAndInfoAreExposed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
    }

    @Test
    void openApiDocumentCoversTheTaskApiOnly() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Do-It-To-It API"))
                .andExpect(jsonPath("$.paths['/api/todos'].get.summary").value("List tasks"))
                .andExpect(jsonPath("$.paths['/api/todos/{id}'].patch").exists())
                .andExpect(jsonPath("$.paths['/api/todos/stats'].get").exists())
                .andExpect(jsonPath("$.paths['/actuator/health']").doesNotExist());
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui/index.html")));

        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                "Content-Security-Policy", SecurityHeadersFilter.SWAGGER_UI_CONTENT_SECURITY_POLICY));
    }
}
