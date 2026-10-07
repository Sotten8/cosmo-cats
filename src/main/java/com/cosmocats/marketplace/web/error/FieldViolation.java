package com.cosmocats.marketplace.web.error;

/**
 * One problem with one field. {@code code} is stable and machine-readable (e.g. {@code NotBlank}, {@code Size},
 * {@code TypeMismatch}); {@code message} is human-readable text that may change.
 */
public record FieldViolation(String field, String code, String message) {
}
