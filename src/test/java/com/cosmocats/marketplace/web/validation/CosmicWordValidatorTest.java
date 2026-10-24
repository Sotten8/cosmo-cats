package com.cosmocats.marketplace.web.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CosmicWordValidatorTest {

    private final CosmicWordValidator validator = new CosmicWordValidator();

    @ParameterizedTest
    @ValueSource(strings = {
            "Star-Dust Anti-Gravity Yarn Ball",
            "Cosmic Milk 1L",
            "Comet Tail Catnip",
            "COMET",
            "my galaxy toy",
            "Deep Space Mouse",
            "Orbit, the toy"})
    void acceptsNamesWithCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Fluffy Ball",
            "Starship",
            "Moonlight",
            "Nova1",
            "Catnip"})
    void rejectsNamesWithoutWholeCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isFalse();
    }

    @Test
    void nullAndBlankAreLeftToOtherConstraints() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
