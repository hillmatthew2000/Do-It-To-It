package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.TaskPriority;
import io.github.hillmatthew2000.do_it_to_it.task.TaskSort;
import io.github.hillmatthew2000.do_it_to_it.task.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC settings for the API.
 *
 * <ul>
 *   <li>Query parameters such as {@code ?status=in progress} or {@code ?sort=due_date} are parsed with
 *       the same leniency as JSON bodies.
 *   <li>The bundled web UI is served from the same origin and needs no CORS, so cross-origin access
 *       is off unless {@code todo.cors.allowed-origins} lists the origins to trust.
 * </ul>
 */
@Configuration
class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    WebConfig(@Value("${todo.cors.allowed-origins:}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, TaskStatus.class, TaskStatus::of);
        registry.addConverter(String.class, TaskPriority.class, TaskPriority::of);
        registry.addConverter(String.class, TaskSort.class, TaskSort::of);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length > 0) {
            registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                    .exposedHeaders(HttpHeaders.ETAG, HttpHeaders.LOCATION);
        }
    }
}
