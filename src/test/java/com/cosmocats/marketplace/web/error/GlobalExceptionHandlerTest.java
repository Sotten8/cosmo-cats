package com.cosmocats.marketplace.web.error;

import com.cosmocats.marketplace.domain.exception.DuplicateCategoryNameException;
import com.cosmocats.marketplace.domain.exception.InsufficientStockException;
import com.cosmocats.marketplace.domain.exception.OrderNotFoundException;
import com.cosmocats.marketplace.domain.exception.ResourceInUseException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders/x");

    @Test
    void missingOrderBecomes404() {
        ProblemDetail problem = handler.handleNotFound(new OrderNotFoundException("x"), request);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getType().toString()).isEqualTo("https://cosmo-cats.market/errors/not-found");
    }

    @Test
    void stockDuplicateAndInUseConflictsBecome409() {
        assertThat(handler.handleConflict(new InsufficientStockException("Yarn", 5, 1), request).getStatus())
                .isEqualTo(409);
        assertThat(handler.handleConflict(new DuplicateCategoryNameException("Toys"), request).getStatus())
                .isEqualTo(409);
        ProblemDetail inUse = handler.handleConflict(new ResourceInUseException("Category", UUID.randomUUID()), request);
        assertThat(inUse.getStatus()).isEqualTo(409);
        assertThat(inUse.getType().toString()).isEqualTo("https://cosmo-cats.market/errors/conflict");
    }
}
