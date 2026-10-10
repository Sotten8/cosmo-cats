package com.cosmocats.marketplace.web.error;

import com.cosmocats.marketplace.application.exception.DeliveryServiceException;
import com.cosmocats.marketplace.application.exception.DeliveryTimeoutException;
import com.cosmocats.marketplace.domain.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.exception.DuplicateProductNameException;
import com.cosmocats.marketplace.domain.exception.ProductNotFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_BASE = "https://cosmo-cats.market/errors/";
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    private static final List<String> CODE_PRIORITY = List.of("NotNull", "NotBlank", "NotEmpty");

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateProductNameException.class)
    public ProblemDetail handleDuplicateName(DuplicateProductNameException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "conflict", "Conflict",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ProblemDetail handleCategoryNotFound(CategoryNotFoundException ex, HttpServletRequest request) {
        FieldViolation violation = new FieldViolation("categoryId", "CategoryNotFound",
                "category '" + ex.categoryId() + "' does not exist");
        return validationProblem(List.of(violation), request.getRequestURI());
    }

    @ExceptionHandler(DeliveryTimeoutException.class)
    public ProblemDetail handleDeliveryTimeout(DeliveryTimeoutException ex, HttpServletRequest request) {
        log.warn("Delivery service timed out on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.GATEWAY_TIMEOUT, "gateway-timeout", "Upstream service timed out",
                "The delivery service did not respond in time. Please try again later.",
                request.getRequestURI());
    }

    @ExceptionHandler(DeliveryServiceException.class)
    public ProblemDetail handleDeliveryFailure(DeliveryServiceException ex, HttpServletRequest request) {
        log.warn("Delivery service call failed on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.BAD_GATEWAY, "bad-gateway", "Upstream service failed",
                "The delivery service is currently unavailable. Please try again later.",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal server error",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), codeOrDefault(e.getCode()), e.getDefaultMessage()))
                .toList();
        ProblemDetail body = validationProblem(violations, path(request));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                violations.add(new FieldViolation(name == null ? "parameter" : name,
                        lastCode(error), error.getDefaultMessage()));
            }
        }
        ProblemDetail body = validationProblem(violations, path(request));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body;
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            body = validationProblem(List.of(toViolation(mismatch)), path(request));
        } else {
            body = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                    "The request body is missing or is not valid JSON.", path(request));
        }
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException argument
                ? argument.getName()
                : ex.getPropertyName();
        FieldViolation violation = new FieldViolation(field == null ? "parameter" : field, "TypeMismatch",
                "must be a valid " + describe(ex.getRequiredType()));
        ProblemDetail body = validationProblem(List.of(violation), path(request));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        FieldViolation violation = new FieldViolation(ex.getParameterName(), "Required", "is required");
        ProblemDetail body = validationProblem(List.of(violation), path(request));
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

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

    private static ProblemDetail validationProblem(List<FieldViolation> violations, String instance) {
        List<FieldViolation> unique = onePerField(violations);
        String detail = unique.stream()
                .map(v -> "Field '" + v.field() + "' " + v.message())
                .collect(Collectors.joining("; ")) + ".";
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed", detail, instance);
        pd.setProperty("errors", unique);
        return pd;
    }

    private static List<FieldViolation> onePerField(List<FieldViolation> violations) {
        Map<String, FieldViolation> byField = new LinkedHashMap<>();
        violations.stream()
                .sorted(Comparator.comparing(FieldViolation::field)
                        .thenComparingInt(v -> priority(v.code()))
                        .thenComparing(FieldViolation::code)
                        .thenComparing(v -> String.valueOf(v.message())))
                .forEach(v -> byField.putIfAbsent(v.field(), v));
        return List.copyOf(byField.values());
    }

    private static int priority(String code) {
        int index = CODE_PRIORITY.indexOf(code);
        return index < 0 ? CODE_PRIORITY.size() : index;
    }

    private static FieldViolation toViolation(MismatchedInputException ex) {
        String field = pathOf(ex);
        if (ex instanceof UnrecognizedPropertyException) {
            return new FieldViolation(field, "UnknownField", "is not allowed");
        }
        if (ex instanceof InvalidFormatException invalid) {
            return new FieldViolation(field, "TypeMismatch", "must be a valid " + describe(invalid.getTargetType()));
        }
        return new FieldViolation(field, "TypeMismatch", "has an invalid type");
    }

    private static String pathOf(JsonMappingException ex) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : ex.getPath()) {
            if (reference.getFieldName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getFieldName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    private static String describe(Class<?> type) {
        if (type == null) {
            return "value";
        }
        if (type == int.class || type == long.class || type == short.class || type == byte.class
                || type == Integer.class || type == Long.class || type == Short.class || type == Byte.class) {
            return "integer";
        }
        if (Number.class.isAssignableFrom(type) || type == double.class || type == float.class) {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type == UUID.class) {
            return "UUID";
        }
        return type.getSimpleName();
    }

    private static String lastCode(MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        return codes == null || codes.length == 0 ? "Invalid" : codes[codes.length - 1];
    }

    private static String codeOrDefault(String code) {
        return code == null ? "Invalid" : code;
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
