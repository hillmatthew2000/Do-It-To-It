package io.github.hillmatthew2000.do_it_to_it.web;

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Tells whoever started the web app where to open it and where their tasks are kept. */
@Component
class ReadyLogger implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ReadyLogger.class);

    private final Path dataDir;
    private final String contextPath;

    ReadyLogger(@Value("${todo.data-dir}") Path dataDir, @Value("${server.servlet.context-path:}") String contextPath) {
        this.dataDir = dataDir;
        this.contextPath = contextPath;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (event.getApplicationContext().getServerNamespace() != null) {
            return; // a secondary server, e.g. actuator on its own management port
        }
        String url = "http://localhost:" + event.getWebServer().getPort() + contextPath + "/";
        log.info("Do-It-To-It is ready at {} (API docs: {}swagger-ui.html)", url, url);
        log.info("Tasks are stored in {}", dataDir.toAbsolutePath().normalize());
    }
}
