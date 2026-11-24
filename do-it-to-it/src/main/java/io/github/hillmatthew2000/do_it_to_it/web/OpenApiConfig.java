package io.github.hillmatthew2000.do_it_to_it.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata for the generated OpenAPI document served at {@code /v3/api-docs}. */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI openApi(ObjectProvider<BuildProperties> build) {
        // Build properties are only present when the app was packaged by Maven.
        String version = build.getIfAvailable() != null ? build.getIfAvailable().getVersion() : "dev";
        return new OpenAPI()
                .info(new Info()
                        .title("Do-It-To-It API")
                        .version(version)
                        .description("""
                                Create, organise and track tasks. Errors are returned as \
                                [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details.""")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
