package com.reejuven8.common.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PagedResponse<T> {
    private final List<T> data;
    private final Pagination pagination;

    @Getter
    @Builder
    public static class Pagination {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;
        private final boolean hasNext;
    }
}
