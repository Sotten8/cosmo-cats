package com.cosmocats.marketplace.service;

import java.util.List;

public record PageResult<T>(List<T> items, long totalElements) {
}
