package com.cosmocats.marketplace.web.error;

import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.integration.delivery.DeliveryServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Global error handling in RFC 9457 (Problem Details) format.
 * {@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}.
 * Extending ResponseEntityExceptionHandler gives correct 404/405/415/400 for framework-level errors too.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE = "https://cosmo-cats.market/errors/";
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    // ---- our own exceptions ----

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
                ex.getMessage(), request.getRequestURI());
    }

    /** Validation of query/path parameters (@Min, @Max on controller arguments). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    return new FieldViolation(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                })
                .toList();
        return validationProblem(violations, request.getRequestURI());
    }

    /** A 3rd-party service failed, timed out or is unreachable. */
    @ExceptionHandler(DeliveryServiceException.class)
    public ProblemDetail handleDeliveryFailure(DeliveryServiceException ex, HttpServletRequest request) {
        log.warn("Delivery service call failed: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "bad-gateway", "Upstream service failed",
                "The delivery service is currently unavailable. Please try again later.",
                request.getRequestURI());
    }

    /** Last line of defence: never leak a stack trace or return a bare 500. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal server error",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    // ---- framework exceptions ----

    /** Invalid @RequestBody (@Valid). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail body = validationProblem(violations, path(request));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Makes every framework-generated problem use our type URIs and always carry "instance". */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail pd) {
            if (ABOUT_BLANK.equals(pd.getType())) {
                pd.setType(URI.create(ERROR_BASE + slug(statusCode)));
            }
            if (pd.getInstance() == null) {
                pd.setInstance(URI.create(path(request)));
            }
        }
        return response;
    }

    // ---- helpers ----

    private static ProblemDetail validationProblem(List<FieldViolation> violations, String instance) {
        String detail = violations.stream()
                .map(v -> "Field '" + v.field() + "' " + v.message())
                .collect(Collectors.joining("; ")) + ".";
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed", detail, instance);
        pd.setProperty("errors", violations);
        return pd;
    }

    private static ProblemDetail problem(HttpStatus status, String typeSlug, String title,
                                         String detail, String instance) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(ERROR_BASE + typeSlug));
        pd.setTitle(title);
        pd.setInstance(URI.create(instance));
        return pd;
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }

    private static String slug(HttpStatusCode code) {
        HttpStatus status = HttpStatus.resolve(code.value());
        return status == null ? "error" : status.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
