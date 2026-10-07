package com.cosmocats.marketplace.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    private static final List<String> COSMIC_WORDS = List.of(
            "star", "galaxy", "comet", "cosmic", "nebula", "orbit", "planet",
            "moon", "space", "gravity", "asteroid", "meteor", "nova", "void");

    /**
     * A cosmic word counts only when it is a whole word: no letter or digit may stand right before or after it
     * ("Star-Dust" matches "star", "Avoid" does not match "void", "Renovate" does not match "nova").
     */
    private static final Pattern WHOLE_WORD = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:" + String.join("|", COSMIC_WORDS) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // emptiness is checked by @NotBlank
        }
        return WHOLE_WORD.matcher(value).find();
    }
}
