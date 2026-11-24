package io.github.hillmatthew2000.do_it_to_it.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds browser hardening headers to every response. The bundled UI loads its scripts and styles from
 * separate same-origin files, so the content security policy can forbid inline scripts and styles
 * outright: even if task text ever reached the DOM as markup, the browser would refuse to run it.
 */
@Component
class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String CONTENT_SECURITY_POLICY = String.join(
            "; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'self'");

    /** Swagger UI sets a few inline styles, so its pages may use them; scripts stay locked down. */
    static final String SWAGGER_UI_CONTENT_SECURITY_POLICY =
            CONTENT_SECURITY_POLICY.replace("style-src 'self'", "style-src 'self' 'unsafe-inline'");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean swaggerUi = request.getRequestURI().startsWith(request.getContextPath() + "/swagger-ui/");
        response.setHeader(
                "Content-Security-Policy", swaggerUi ? SWAGGER_UI_CONTENT_SECURITY_POLICY : CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(request, response);
    }
}
