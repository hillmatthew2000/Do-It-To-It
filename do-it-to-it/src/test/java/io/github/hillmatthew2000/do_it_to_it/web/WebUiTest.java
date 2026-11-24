package io.github.hillmatthew2000.do_it_to_it.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Checks that the bundled web UI is served and protected by the expected headers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebUiTest {

    private static final Pattern ASSET_REFERENCE = Pattern.compile("(?:href|src)=\"([^\"#:]+)\"");

    @Autowired
    MockMvc mvc;

    @Test
    void rootServesTheWebUi() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
    }

    @Test
    void everyAssetReferencedByTheIndexPageExists() throws Exception {
        String index = new ClassPathResource("static/index.html").getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = ASSET_REFERENCE.matcher(index);
        int assets = 0;
        while (matcher.find()) {
            mvc.perform(get("/" + matcher.group(1))).andExpect(status().isOk());
            assets++;
        }
        assertThat(assets).as("assets referenced by index.html").isGreaterThanOrEqualTo(4);
    }

    @Test
    void indexPageHasNoInlineScriptsOrStyles() throws IOException {
        // The content security policy blocks inline code, so any that crept in would silently break.
        String index = new ClassPathResource("static/index.html").getContentAsString(StandardCharsets.UTF_8);
        assertThat(index)
                .doesNotContainPattern("<script(?![^>]*\\bsrc=)")
                .doesNotContain("<style")
                .doesNotContainPattern("\\sstyle=")
                .doesNotContainPattern("\\son[a-z]+=");
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Do-It-To-It</title>")))
                .andExpect(header().string("Content-Security-Policy", SecurityHeadersFilter.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        mvc.perform(get("/api/todos")).andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
