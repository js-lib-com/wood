package com.jslib.wood.lang;

/**
 * Functional interface that accepts checked exception.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface CheckedFunction<T, R> {
    R apply(T argument) throws Exception;
}
