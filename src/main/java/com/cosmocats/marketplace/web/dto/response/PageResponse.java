package com.cosmocats.marketplace.web.dto.response;

import java.util.List;

/** Paged envelope. {@code page} is zero-based. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
