package com.service.report.generator.experimental;

@FunctionalInterface
public interface ImplProvider<T> {
    void execute(T t) throws Exception;
}
