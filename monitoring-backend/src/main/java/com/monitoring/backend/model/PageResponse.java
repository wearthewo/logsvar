package com.monitoring.backend.model;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int page,
    int size
) {
    public PageResponse {
        if (totalPages == 0 && totalElements > 0) {
            totalPages = (int) Math.ceil((double) totalElements / size);
        }
    }
}
