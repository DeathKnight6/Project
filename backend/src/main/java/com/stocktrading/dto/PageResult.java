package com.stocktrading.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** 简单分页返回结构。 */
public record PageResult<T>(List<T> content, long total, int page, int size) {

    public static <E, T> PageResult<T> from(Page<E> source, Function<E, T> mapper) {
        return new PageResult<>(
                source.getContent().stream().map(mapper).toList(),
                source.getTotalElements(),
                source.getNumber(),
                source.getSize());
    }
}
