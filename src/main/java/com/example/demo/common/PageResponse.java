package com.example.demo.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Formato de paginación que piden los enunciados: { content, page, size, totalElements } */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    /** Paginación manual sobre una lista ya ordenada (útil para combinar dos tablas). */
    public static <T> PageResponse<T> ofList(List<T> all, int page, int size) {
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        return new PageResponse<>(all.subList(from, to), page, size, all.size());
    }
}
