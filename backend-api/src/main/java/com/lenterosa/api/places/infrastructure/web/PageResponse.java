package com.lenterosa.api.places.infrastructure.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página de resultados con una forma JSON estable, independiente de la clase Page de Spring. */
record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
