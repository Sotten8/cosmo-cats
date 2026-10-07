package com.cosmocats.marketplace.web.error;

public record FieldViolation(String field, String code, String message) {
}
