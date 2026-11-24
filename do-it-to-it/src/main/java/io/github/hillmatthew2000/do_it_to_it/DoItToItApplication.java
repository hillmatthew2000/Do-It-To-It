package io.github.hillmatthew2000.do_it_to_it;

import io.github.hillmatthew2000.do_it_to_it.console.TodoConsole;
import io.github.hillmatthew2000.do_it_to_it.task.TaskService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Entry point. Starts the interactive console by default, or the web application when launched with
 * {@code --spring}. Both modes share the same task database.
 */
@SpringBootApplication
public class DoItToItApplication {

    public static void main(String[] args) {
        String[] springArgs = LaunchMode.springArguments(args);
        if (LaunchMode.from(args) == LaunchMode.WEB) {
            SpringApplication.run(DoItToItApplication.class, springArgs);
        } else {
            runConsole(springArgs);
        }
    }

    private static void runConsole(String[] args) {
        // DevTools' restarter would relaunch the console on a new thread; it only makes sense for the web app.
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.out.println("Starting Do-It-To-It...");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(DoItToItApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("console")
                .run(args)) {
            Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
            var in = new BufferedReader(new InputStreamReader(System.in, charset));
            var out = new PrintStream(System.out, true, charset);
            new TodoConsole(context.getBean(TaskService.class), in, out).run();
        }
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
