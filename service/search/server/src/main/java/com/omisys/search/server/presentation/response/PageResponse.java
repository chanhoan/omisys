package com.omisys.search.server.presentation.response;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int pageSize;

    public PageResponse(List<T> content, long totalElements, int totalPages, int page, int pageSize) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.pageSize = pageSize;
    }
}
