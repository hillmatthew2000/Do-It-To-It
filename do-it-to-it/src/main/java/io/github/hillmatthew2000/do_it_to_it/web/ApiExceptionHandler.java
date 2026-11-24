package io.github.hillmatthew2000.do_it_to_it.web;

import io.github.hillmatthew2000.do_it_to_it.task.TaskNotFoundException;
import io.github.hillmatthew2000.do_it_to_it.task.TaskVersionConflictException;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders API errors as RFC 9457 {@link ProblemDetail} responses. Validation failures carry an extra
 * {@code errors} member mapping each invalid field to its message.
 */
@RestControllerAdvice(assignableTypes = {TaskController.class, TaskTransferController.class})
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INVALID_REQUEST = "Invalid request";

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Task not found");
        problem.setProperty("taskId", ex.getTaskId());
        return problem;
    }

    /**
     * A client that sent {@code If-Match} asked for the change to be conditional, so a stale version is a
     * failed precondition (412). Without it, the only way to get here is two changes racing each other,
     * which is a plain conflict (409).
     */
    @ExceptionHandler(TaskVersionConflictException.class)
    ProblemDetail handleVersionConflict(TaskVersionConflictException ex, WebRequest request) {
        HttpStatus status =
                request.getHeader(HttpHeaders.IF_MATCH) != null ? HttpStatus.PRECONDITION_FAILED : HttpStatus.CONFLICT;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status, ex.getMessage() + ". Load it again to see the latest version, then retry.");
        problem.setTitle("Task changed");
        problem.setProperty("taskId", ex.getTaskId());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return invalidRequest(ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem =
                invalidRequest(errors.isEmpty() ? "Request validation failed" : String.join("; ", errors.values()));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = invalidRequest(readableCause(ex));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Invalid query parameters or path variables, such as {@code ?status=someday} or {@code /api/todos/abc}. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = invalidRequest(typeMismatchDetail(ex));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled error while processing an API request", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.");
        problem.setTitle("Internal error");
        return problem;
    }

    private static ProblemDetail invalidRequest(@Nullable String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(INVALID_REQUEST);
        return problem;
    }

    /** Uses the converters' own messages (they list the accepted values); otherwise names the bad value. */
    private static String typeMismatchDetail(TypeMismatchException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException
                && !(cause instanceof NumberFormatException)
                && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return "'%s' is not a valid value for '%s'".formatted(ex.getValue(), ex.getPropertyName());
    }

    /** Surfaces our own enum-parsing messages; hides parser internals for everything else. */
    private static String readableCause(HttpMessageNotReadableException ex) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
                return cause.getMessage();
            }
            if (cause instanceof DateTimeParseException) {
                return "Dates must use the ISO format yyyy-MM-dd";
            }
        }
        return "Request body is missing or is not valid JSON";
    }
}
